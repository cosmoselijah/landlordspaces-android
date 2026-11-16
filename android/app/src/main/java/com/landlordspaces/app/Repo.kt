package com.landlordspaces.app

import org.json.JSONObject
import java.net.URLEncoder

/** Everything the screens read or change goes through here, so a screen never talks to the network directly. */
class Repo(val session: Session) {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    fun listings(query: String, market: String, cat: String, sort: String, city: String, page: Int, extra: String = ""): Pair<List<Listing>, JSONObject> {
        val p = StringBuilder("/listings?page=$page&per=12")
        if (query.isNotBlank()) p.append("&q=").append(enc(query.trim()))
        if (market.isNotEmpty()) p.append("&market=").append(market)
        if (cat.isNotEmpty()) p.append("&cat=").append(cat)
        if (sort.isNotEmpty()) p.append("&sort=").append(sort)
        if (city.isNotEmpty()) p.append("&city=").append(enc(city))
        p.append(extra)
        val j = session.open("GET", p.toString())
        return Pair(j.optJSONArray("listings").objects().map { Listing.from(it) }, j)
    }

    fun detail(id: Int): Detail = Detail.from(session.open("GET", "/listings/$id"))
    fun meta(): Pair<List<String>, List<String>> {
        val j = session.open("GET", "/meta")
        return Pair(j.optJSONArray("cities").strings(), j.optJSONArray("types").strings())
    }
    fun saved(): List<Listing> = session.call("GET", "/saved").optJSONArray("listings").objects().map { Listing.from(it) }
    fun toggleSave(id: Int): Boolean = session.callSite("POST", "/api/favourite/$id", JSONObject()).optBoolean("saved")
    fun home(): JSONObject = session.call("GET", "/home")
    fun conversations(): List<Convo> = session.call("GET", "/conversations").optJSONArray("conversations").objects().map { Convo.from(it) }
    fun thread(id: Int): Triple<String, String, List<Msg>> {
        val j = session.call("GET", "/conversations/$id")
        val c = j.getJSONObject("conversation")
        val other = c.optJSONObject("other") ?: JSONObject()
        val prop = c.optJSONObject("property")
        return Triple(other.str("name"), prop?.str("title") ?: "", j.optJSONArray("messages").objects().map { Msg.from(it) })
    }
    fun send(conv: Int, text: String) { session.callSite("POST", "/api/message/$conv", JSONObject().put("body", text)) }
    fun enquire(propertyId: Int, text: String): Int =
        session.callSite("POST", "/api/connect/$propertyId", JSONObject().put("message", text)).optInt("conversation_id")
    fun notifications(): List<Notice> = session.call("GET", "/notifications").optJSONArray("notifications").objects().map { Notice.from(it) }
    fun markAllRead() { session.call("POST", "/notifications/read", JSONObject()) }
    fun workspace(): JSONObject = session.call("GET", "/workspace")
    fun me(): JSONObject = session.call("GET", "/me").getJSONObject("user")
    fun config(): JSONObject = Api.request("GET", "/config")
}
