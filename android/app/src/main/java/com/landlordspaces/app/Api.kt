package com.landlordspaces.app

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

const val SITE = "https://landlordspaces.com"
private const val BASE = "$SITE/api/mobile/v1"

/** An error answer from the LandlordSpaces server (message is safe to show to the person). */
class ApiError(val code: String, message: String, val status: Int) : Exception(message)

object Api {
    fun request(method: String, path: String, body: JSONObject? = null, token: String? = null): JSONObject =
        exec(BASE + path, method, body, token, true)

    /** Calls an existing website endpoint (same rules as the site) with the member's bearer token. */
    fun site(method: String, path: String, body: JSONObject? = null, token: String? = null): JSONObject =
        exec(SITE + path, method, body, token, false)

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
    fun cachePut(k: String, v: String) { try { cacheStore.edit().putString(k, v).apply() } catch (e: Exception) { } }
    fun cacheGet(k: String): String? = cacheStore.getString(k, null)
    fun cacheClear() { cacheStore.edit().clear().apply() }

    var access: String?
        get() = prefs.getString("access", null)
        set(v) { prefs.edit().putString("access", v).apply() }

    var refresh: String?
        get() = prefs.getString("refresh", null)
        set(v) { prefs.edit().putString("refresh", v).apply() }

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
        prefs.edit().remove("access").remove("refresh").remove("name").apply()
        cacheClear()
    }

    /** Call a signed-in endpoint; if the access token has expired, renew it once and retry. */
    fun call(method: String, path: String, body: JSONObject? = null): JSONObject {
        try {
            return Api.request(method, path, body, access)
        } catch (e: ApiError) {
            val r = refresh
            if (e.status == 401 && r != null) {
                try {
                    save(Api.request("POST", "/auth/refresh", JSONObject().put("refresh_token", r)))
                } catch (re: ApiError) {
                    if (re.status == 401) clear()
                    throw re
                }
                return Api.request(method, path, body, access)
            }
            throw e
        }
    }

    /** Same as [call] but for the website's own JSON endpoints (favourites, enquiries, chat). */
    fun callSite(method: String, path: String, body: JSONObject? = null): JSONObject {
        try {
            return Api.site(method, path, body, access)
        } catch (e: ApiError) {
            val r = refresh
            if (e.status == 401 && r != null) {
                try {
                    save(Api.request("POST", "/auth/refresh", JSONObject().put("refresh_token", r)))
                } catch (re: ApiError) {
                    if (re.status == 401) clear()
                    throw re
                }
                return Api.site(method, path, body, access)
            }
            throw e
        }
    }

    /** A valid access token, renewed if needed (used to open the website pages inside the app). */
    fun freshAccess(): String {
        call("GET", "/me")
        return access ?: ""
    }

    /** Marketplace reads work signed out too; when signed in the token adds the member's "saved" flags. */
    fun open(method: String, path: String): JSONObject = if (signedIn()) call(method, path) else Api.request(method, path, null, null)

    fun signOut() {
        try {
            call("POST", "/auth/logout", JSONObject().put("device_id", deviceId))
        } catch (e: Exception) {
            // Signing out on this phone always works, even offline.
        }
        clear()
    }
}
