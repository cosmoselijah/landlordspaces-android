package com.landlordspaces.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.net.URLEncoder

/** Everything the screens read or change goes through here, so a screen never talks to the network directly. */
class Repo(val session: Session) {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /** True while the screen is showing saved data because the phone is offline. */
    var offline: Boolean
        get() = session.offline
        set(v) { session.offline = v }

    /** Fetch fresh data and remember it; if the phone is offline, fall back to what was remembered. */
    private fun cached(key: String, fetch: () -> JSONObject): JSONObject {
        try {
            val j = fetch()
            session.cachePut(key, j.toString())
            offline = false
            return j
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            val old = session.cacheGet(key) ?: throw e
            offline = true
            return JSONObject(old)
        }
    }

    fun listings(query: String, market: String, cat: String, sort: String, city: String, page: Int, extra: String = ""): Pair<List<Listing>, JSONObject> {
        val p = StringBuilder("/listings?page=$page&per=12")
        if (query.isNotBlank()) p.append("&q=").append(enc(query.trim()))
        if (market.isNotEmpty()) p.append("&market=").append(market)
        if (cat.isNotEmpty()) p.append("&cat=").append(cat)
        if (sort.isNotEmpty()) p.append("&sort=").append(sort)
        if (city.isNotEmpty()) p.append("&city=").append(enc(city))
        p.append(extra)
        val path = p.toString()
        val j = if (page == 1 && extra.isEmpty()) cached("l:" + path) { session.open("GET", path) } else session.open("GET", path)
        return Pair(j.optJSONArray("listings").objects().map { Listing.from(it) }, j)
    }

    fun detail(key: String): Detail = Detail.from(cached("d:$key") { session.open("GET", "/listings/" + enc(key)) })
    fun meta(): Pair<List<String>, List<String>> {
        val j = session.open("GET", "/meta")
        return Pair(j.optJSONArray("cities").strings(), j.optJSONArray("types").strings())
    }
    fun saved(): List<Listing> = session.call("GET", "/saved").optJSONArray("listings").objects().map { Listing.from(it) }
    fun toggleSave(id: Int): Boolean {
        return try {
            val s = session.callSite("POST", "/api/favourite/$id", JSONObject()).optBoolean("saved")
            session.cachePut("sv:$id", if (s) "1" else "0")
            s
        } catch (e: java.io.IOException) {
            val now = session.cacheGet("sv:$id") != "1"
            session.enqueue("POST", "/api/favourite/$id", JSONObject())
            session.cachePut("sv:$id", if (now) "1" else "0")
            offline = true
            now
        }
    }
    fun listingForm(): JSONObject = session.call("GET", "/listing-form")
    fun home(): JSONObject = cached("home") { session.call("GET", "/home") }
    fun conversations(): List<Convo> = session.call("GET", "/conversations").optJSONArray("conversations").objects().map { Convo.from(it) }
    fun thread(id: Int): Triple<String, String, List<Msg>> {
        val j = session.call("GET", "/conversations/$id")
        val c = j.getJSONObject("conversation")
        val other = c.optJSONObject("other") ?: JSONObject()
        val prop = c.optJSONObject("property")
        return Triple(other.str("name"), prop?.str("title") ?: "", j.optJSONArray("messages").objects().map { Msg.from(it) })
    }
    fun send(conv: Int, text: String) {
        val b = JSONObject().put("body", text)
        try {
            session.callSite("POST", "/api/message/$conv", b)
        } catch (e: java.io.IOException) {
            session.enqueue("POST", "/api/message/$conv", b)
            offline = true
        }
    }
    fun enquire(propertyId: Int, text: String): Int =
        session.callSite("POST", "/api/connect/$propertyId", JSONObject().put("message", text)).optInt("conversation_id")
    fun notifications(): List<Notice> = session.call("GET", "/notifications").optJSONArray("notifications").objects().map { Notice.from(it) }
    fun markAllRead() { session.call("POST", "/notifications/read", JSONObject()) }
    fun workspace(): JSONObject = session.call("GET", "/workspace")
    fun me(): JSONObject = session.call("GET", "/me").getJSONObject("user")
    fun config(): JSONObject = Api.request("GET", "/config")

    /** Asks the website's own assistant; the app sends the last few turns so follow-up questions make sense. */
    fun guide(question: String, history: List<Pair<String, String>>): JSONObject {
        val h = org.json.JSONArray()
        history.takeLast(10).forEach { (role, text) -> h.put(JSONObject().put("role", role).put("text", text)) }
        return session.callSite("POST", "/api/guide", JSONObject().put("q", question).put("history", h))
    }
    fun tenants(): JSONObject = session.call("GET", "/tenants")
    fun saveProfile(name: String, phone: String, city: String): JSONObject =
        session.call("POST", "/profile", JSONObject().put("name", name).put("phone", phone).put("city", city))
    fun sessions(): List<JSONObject> = session.call("GET", "/sessions").optJSONArray("sessions").objects()
    fun revokeAll() { session.call("POST", "/sessions/revoke-all", JSONObject()) }
}
