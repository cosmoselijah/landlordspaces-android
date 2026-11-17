package com.landlordspaces.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

const val SITE = "https://landlordspaces.com"
private const val BASE = "$SITE/api/mobile/v1"

/** A refresh token works only once, so every call in this app shares one renewal at a time. */
private val RENEW_LOCK = Any()
private val OUTBOX_LOCK = Any()

/** An error answer from the LandlordSpaces server (message is safe to show to the person). */
class ApiError(val code: String, message: String, val status: Int) : Exception(message)

object Api {
    fun request(method: String, path: String, body: JSONObject? = null, token: String? = null): JSONObject =
        exec(BASE + path, method, body, token, true)

    /** Calls an existing website endpoint (same rules as the site) with the member's bearer token. */
    fun site(method: String, path: String, body: JSONObject? = null, token: String? = null): JSONObject =
        exec(SITE + path, method, body, token, false)

    /** Sends a chat attachment (with optional text) to the website's own message endpoint. */
    fun siteUpload(path: String, field: String, file: java.io.File, mime: String, text: String, token: String?): JSONObject {
        val boundary = "ls" + UUID.randomUUID().toString().replace("-", "")
        val c = URL(SITE + path).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"
            c.connectTimeout = 15000
            c.readTimeout = 90000
            c.doOutput = true
            c.setChunkedStreamingMode(0)
            c.setRequestProperty("Accept", "application/json")
            c.setRequestProperty("User-Agent", "LandlordSpacesApp/" + BuildConfig.VERSION_NAME)
            if (token != null) c.setRequestProperty("Authorization", "Bearer $token")
            c.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            c.outputStream.use { o ->
                o.write(("--$boundary\r\nContent-Disposition: form-data; name=\"body\"\r\n\r\n" + text + "\r\n").toByteArray())
                o.write(("--$boundary\r\nContent-Disposition: form-data; name=\"" + field + "\"; filename=\"" + file.name + "\"\r\nContent-Type: " + mime + "\r\n\r\n").toByteArray())
                file.inputStream().use { it.copyTo(o) }
                o.write(("\r\n--$boundary--\r\n").toByteArray())
            }
            val status = c.responseCode
            val stream = if (status in 200..299) c.inputStream else c.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            val json = try { JSONObject(body) } catch (e: Exception) { JSONObject() }
            if (status !in 200..299) throw ApiError("http_$status", json.optString("message", "The file could not be sent."), status)
            return json
        } finally {
            c.disconnect()
        }
    }

    /** Sends one photo to the server as a multipart upload. */
    fun upload(path: String, file: java.io.File, token: String?): JSONObject {
        val boundary = "ls" + UUID.randomUUID().toString().replace("-", "")
        val c = URL(BASE + path).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"
            c.connectTimeout = 15000
            c.readTimeout = 90000
            c.doOutput = true
            c.setChunkedStreamingMode(0)
            c.setRequestProperty("Accept", "application/json")
            c.setRequestProperty("User-Agent", "LandlordSpacesApp/" + BuildConfig.VERSION_NAME)
            if (token != null) c.setRequestProperty("Authorization", "Bearer $token")
            c.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            c.outputStream.use { o ->
                o.write(("--$boundary\r\nContent-Disposition: form-data; name=\"photos\"; filename=\"" + file.name + "\"\r\nContent-Type: image/jpeg\r\n\r\n").toByteArray())
                file.inputStream().use { it.copyTo(o) }
                o.write(("\r\n--$boundary--\r\n").toByteArray())
            }
            val status = c.responseCode
            val stream = if (status in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            val json = try { JSONObject(text) } catch (e: Exception) { JSONObject() }
            if (status !in 200..299 || !json.optBoolean("ok", false)) {
                throw ApiError(json.optString("error", "http_$status"), json.optString("message", "Something went wrong. Please try again."), status)
            }
            return json
        } finally {
            c.disconnect()
        }
    }

    private fun exec(url: String, method: String, body: JSONObject?, token: String?, needOk: Boolean): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = 15000
            c.readTimeout = 20000
            c.setRequestProperty("Accept", "application/json")
            c.setRequestProperty("User-Agent", "LandlordSpacesApp/" + BuildConfig.VERSION_NAME)
            if (token != null) c.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val status = c.responseCode
            val stream = if (status in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            val json = try { JSONObject(text) } catch (e: Exception) { JSONObject() }
            if (status !in 200..299 || (needOk && !json.optBoolean("ok", false))) {
                throw ApiError(
                    json.optString("error", "http_$status"),
                    json.optString("message", "Something went wrong. Please try again."),
                    status
                )
            }
            return json
        } finally {
            c.disconnect()
        }
    }
}

/** Holds the signed-in tokens (private app storage) and renews the short access token when it expires. */
class Session(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ls_session", Context.MODE_PRIVATE)

    private val cacheStore = context.applicationContext.getSharedPreferences("ls_cache", Context.MODE_PRIVATE)
    /** True while a screen is showing the phone's saved copy because there is no connection. */
    var offline by mutableStateOf(false)

    fun cachePut(k: String, v: String) {
        try {
            val e = cacheStore.edit().putString(k, v)
            if (k.startsWith("g:")) {
                // Keep only the 40 most recent reads so the saved copies never grow without limit.
                val idx = (cacheStore.getString("idx", "") ?: "").split("\n").filter { it.isNotEmpty() && it != k }.toMutableList()
                idx.add(k)
                while (idx.size > 40) e.remove(idx.removeAt(0))
                e.putString("idx", idx.joinToString("\n"))
            }
            e.apply()
        } catch (ex: Exception) { }
    }
    fun cacheGet(k: String): String? = cacheStore.getString(k, null)
    fun cacheClear() { cacheStore.edit().clear().apply() }

    // Tokens are encrypted with an Android Keystore key before they touch the disk; a copy lives in memory while the app runs.
    private var memAccess: String? = null
    private var memRefresh: String? = null

    private fun readSecret(k: String): String? {
        val v = prefs.getString(k, null) ?: return null
        return if (v.startsWith("e1:")) SecureStore.decrypt(v) else v
    }

    private fun writeSecret(k: String, v: String?) {
        val e = prefs.edit()
        if (v == null) e.remove(k) else {
            val enc = SecureStore.encrypt(v)
            if (enc != null) e.putString(k, enc) else e.remove(k)
        }
        e.apply()
    }

    var access: String?
        get() = memAccess ?: readSecret("access").also { memAccess = it }
        set(v) { memAccess = v; writeSecret("access", v) }

    var refresh: String?
        get() = memRefresh ?: readSecret("refresh").also { memRefresh = it }
        set(v) { memRefresh = v; writeSecret("refresh", v) }

    /** "Keep me signed in": when off, the sign-in ends the next time the app is started from scratch. */
    var keep: Boolean
        get() = prefs.getBoolean("keep", true)
        set(v) { prefs.edit().putBoolean("keep", v).apply() }

    /** Fingerprint unlock is on for the saved sign-in. */
    var bio: Boolean
        get() = prefs.getBoolean("bio", false)
        set(v) { prefs.edit().putBoolean("bio", v).apply() }

    var name: String
        get() = prefs.getString("name", "") ?: ""
        set(v) { prefs.edit().putString("name", v).apply() }

    val deviceId: String
        get() {
            var id = prefs.getString("device", null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString("device", id).apply()
            }
            return id
        }

    fun signedIn(): Boolean = refresh != null

    fun deviceJson(): JSONObject = JSONObject()
        .put("id", deviceId)
        .put("name", android.os.Build.MODEL ?: "Android")
        .put("app_version", BuildConfig.VERSION_NAME)

    fun save(json: JSONObject) {
        access = json.optString("access_token")
        refresh = json.optString("refresh_token")
        val user = json.optJSONObject("user")
        if (user != null) name = user.optString("name")
    }

    fun clear() {
        memAccess = null
        memRefresh = null
        prefs.edit().remove("access").remove("refresh").remove("name").remove("outbox").remove("bio").apply()
        cacheClear()
    }

    /** Signing out: the server is told to end this session (best effort), then everything saved on the phone is removed. */
    fun signOut() {
        val a = access
        val dev = deviceId
        if (a != null) Thread { try { Api.request("POST", "/auth/logout", JSONObject().put("device_id", dev), a) } catch (e: Exception) { } }.start()
        clear()
    }

    /** Renews the access token once, even when several calls found it expired at the same moment. */
    private fun renew(used: String?) {
        synchronized(RENEW_LOCK) {
            val current = access
            if (current != null && current != used) return
            val r = refresh ?: throw ApiError("unauthorized", "Please sign in again.", 401)
            try {
                save(Api.request("POST", "/auth/refresh", JSONObject().put("refresh_token", r)))
            } catch (re: ApiError) {
                if (re.status == 401 && refresh == r) clear()
                throw re
            }
        }
    }

    private fun live(site: Boolean, method: String, path: String, body: JSONObject?): JSONObject {
        val used = access
        try {
            return if (site) Api.site(method, path, body, used) else Api.request(method, path, body, used)
        } catch (e: ApiError) {
            if (e.status == 401 && refresh != null) {
                renew(used)
                return if (site) Api.site(method, path, body, access) else Api.request(method, path, body, access)
            }
            throw e
        }
    }

    /** Reads are remembered on the phone; with no connection the saved copy is shown instead. */
    private fun withCopy(read: Boolean, key: String, fetch: () -> JSONObject): JSONObject {
        if (!read) return fetch()
        try {
            val j = fetch()
            val text = j.toString()
            if (text.length < 400000) cachePut(key, text)
            offline = false
            return j
        } catch (e: ApiError) {
            throw e
        } catch (e: java.io.IOException) {
            val old = cacheGet(key) ?: throw e
            offline = true
            return JSONObject(old)
        }
    }

    /** Call a signed-in endpoint; an expired access token is renewed once and the call retried. */
    fun call(method: String, path: String, body: JSONObject? = null): JSONObject =
        withCopy(method == "GET", "g:$path") { live(false, method, path, body) }

    /** Uploads one photo; an expired access token is renewed once and the upload retried. */
    fun upload(path: String, file: java.io.File): JSONObject {
        val used = access
        try {
            return Api.upload(path, file, used)
        } catch (e: ApiError) {
            if (e.status == 401 && refresh != null) {
                renew(used)
                return Api.upload(path, file, access)
            }
            throw e
        }
    }

    fun siteUpload(path: String, field: String, file: java.io.File, mime: String, text: String): JSONObject {
        val used = access
        try {
            return Api.siteUpload(path, field, file, mime, text, used)
        } catch (e: ApiError) {
            if (e.status == 401 && refresh != null) {
                renew(used)
                return Api.siteUpload(path, field, file, mime, text, access)
            }
            throw e
        }
    }

    /** Same as [call] but for the website's own JSON endpoints (favourites, enquiries, chat). */
    fun callSite(method: String, path: String, body: JSONObject? = null): JSONObject = live(true, method, path, body)

    /** Saves an action made while offline; it is sent, in order, when the connection returns. */
    fun enqueue(method: String, path: String, body: JSONObject) {
        synchronized(OUTBOX_LOCK) {
            val arr = try { JSONArray(prefs.getString("outbox", "[]")) } catch (e: Exception) { JSONArray() }
            arr.put(JSONObject().put("m", method).put("p", path).put("b", body))
            prefs.edit().putString("outbox", arr.toString()).apply()
        }
    }

    fun pending(): Int = try { JSONArray(prefs.getString("outbox", "[]")).length() } catch (e: Exception) { 0 }

    /** Sends the actions saved while offline. Stops at the first one the network cannot carry yet. */
    fun flush(): Int {
        if (!signedIn()) return 0
        return synchronized(OUTBOX_LOCK) {
            val arr = try { JSONArray(prefs.getString("outbox", "[]")) } catch (e: Exception) { JSONArray() }
            var sent = 0
            while (arr.length() > 0) {
                val a = arr.getJSONObject(0)
                try {
                    live(true, a.getString("m"), a.getString("p"), a.optJSONObject("b"))
                } catch (e: ApiError) {
                    if (e.status == 401 || e.status == 429 || e.status >= 500) break
                } catch (e: java.io.IOException) {
                    break
                }
                arr.remove(0)
                sent++
                prefs.edit().putString("outbox", arr.toString()).apply()
            }
            sent
        }
    }

    /** A valid access token, renewed if needed (used to open the website pages inside the app). */
    fun freshAccess(): String {
        call("GET", "/me")
        return access ?: ""
    }

    /** Marketplace reads work signed out too; when signed in the token adds the member's "saved" flags. */
    fun open(method: String, path: String): JSONObject =
        if (signedIn()) call(method, path) else withCopy(method == "GET", "g:$path") { Api.request(method, path, null, null) }

    fun signOut() {
        try {
            call("POST", "/auth/logout", JSONObject().put("device_id", deviceId))
        } catch (e: Exception) {
            // Signing out on this phone always works, even offline.
        }
        clear()
    }
}
