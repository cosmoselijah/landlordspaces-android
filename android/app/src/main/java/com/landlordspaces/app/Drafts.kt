package com.landlordspaces.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

private val DRAFT_LOCK = Any()
private val SYNC_LOCK = Any()

/** One property listing written on the phone. It lives here, with its photos, until the server has it. */
class Draft(
    val id: String,
    var fields: JSONObject,
    var photos: MutableList<String>,
    var state: String,
    var serverId: Int,
    var uploaded: Int,
    var note: String,
    var updated: Long,
) {
    val title: String get() = fields.optString("title").ifBlank { "Untitled property" }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("f", fields).put("p", JSONArray(photos)).put("s", state)
        .put("sid", serverId).put("up", uploaded).put("n", note).put("t", updated)

    companion object {
        fun from(j: JSONObject): Draft {
            val ph = mutableListOf<String>()
            val a = j.optJSONArray("p")
            if (a != null) for (i in 0 until a.length()) ph.add(a.getString(i))
            return Draft(j.getString("id"), j.optJSONObject("f") ?: JSONObject(), ph, j.optString("s", "draft"), j.optInt("sid"), j.optInt("up"), j.optString("n"), j.optLong("t"))
        }
    }
}

/**
 * Listings written offline. States: draft (kept on the phone), queued (will be sent), sending,
 * sent (the server has it as a draft in My Listings), failed (the server refused it; the note says why).
 */
object Drafts {
    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("ls_drafts", Context.MODE_PRIVATE)

    private fun ids(c: Context): List<String> = (prefs(c).getString("ids", "") ?: "").split("\n").filter { it.isNotEmpty() }

    fun all(c: Context): List<Draft> = synchronized(DRAFT_LOCK) {
        val p = prefs(c)
        ids(c).mapNotNull { id ->
            try { Draft.from(JSONObject(p.getString("d:$id", "") ?: "")) } catch (e: Exception) { null }
        }.sortedByDescending { it.updated }
    }

    fun get(c: Context, id: String): Draft? = synchronized(DRAFT_LOCK) {
        try { Draft.from(JSONObject(prefs(c).getString("d:$id", "") ?: "")) } catch (e: Exception) { null }
    }

    fun save(c: Context, d: Draft) {
        synchronized(DRAFT_LOCK) {
            d.updated = System.currentTimeMillis()
            val list = ids(c).toMutableList()
            if (!list.contains(d.id)) list.add(d.id)
            prefs(c).edit().putString("d:${d.id}", d.toJson().toString()).putString("ids", list.joinToString("\n")).apply()
        }
    }

    /** Changes one draft in place (used while sending, so edits made meanwhile are not overwritten). */
    private fun update(c: Context, id: String, change: (Draft) -> Unit) {
        synchronized(DRAFT_LOCK) {
            val d = get(c, id) ?: return
            change(d)
            prefs(c).edit().putString("d:$id", d.toJson().toString()).apply()
        }
    }

    fun remove(c: Context, id: String) {
        synchronized(DRAFT_LOCK) {
            get(c, id)?.photos?.forEach { try { File(it).delete() } catch (e: Exception) { } }
            val list = ids(c).filter { it != id }
            prefs(c).edit().remove("d:$id").putString("ids", list.joinToString("\n")).apply()
        }
    }

    fun clearAll(c: Context) {
        all(c).forEach { remove(c, it.id) }
        c.applicationContext.getSharedPreferences("ls_studio", Context.MODE_PRIVATE).edit().clear().apply()
    }

    /** How many listings are waiting to be sent. */
    fun waiting(c: Context): Int = all(c).count { it.state == "queued" || it.state == "sending" }

    /** Shrinks a picked photo (long side 1920 px, upright) and keeps a private copy; returns its path. */
    fun addPhoto(c: Context, uri: Uri): String? {
        try {
            val cr = c.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            val big = maxOf(bounds.outWidth, bounds.outHeight)
            while (big / (sample * 2) >= 1920) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
            val rot = try {
                cr.openInputStream(uri)?.use { android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1) } ?: 1
            } catch (e: Exception) { 1 }
            val deg = when (rot) { 6 -> 90f; 3 -> 180f; 8 -> 270f; else -> 0f }
            val m = Matrix()
            val scale = 1920f / maxOf(bmp.width, bmp.height)
            if (scale < 1f) m.postScale(scale, scale)
            if (deg != 0f) m.postRotate(deg)
            if (!m.isIdentity) bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            val dir = File(c.filesDir, "drafts")
            dir.mkdirs()
            val out = File(dir, UUID.randomUUID().toString() + ".jpg")
            out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            return out.absolutePath
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Sends every queued listing, then its photos one by one, resuming where it stopped if the network drops.
     * Returns how many listings were completed.
     */
    fun sync(c: Context, session: Session): Int {
        if (!session.signedIn()) return 0
        return synchronized(SYNC_LOCK) {
            var done = 0
            for (d0 in all(c).filter { it.state == "queued" || it.state == "sending" }.reversed()) {
                val id = d0.id
                update(c, id) { it.state = "sending"; it.note = "" }
                try {
                    var d = get(c, id) ?: continue
                    if (d.serverId == 0) {
                        val j = session.call("POST", "/listings", d.fields)
                        update(c, id) { it.serverId = j.optInt("id") }
                        d = get(c, id) ?: continue
                    }
                    while (d.uploaded < d.photos.size) {
                        val f = File(d.photos[d.uploaded])
                        if (f.exists()) {
                            try {
                                session.upload("/listings/${d.serverId}/photos", f)
                            } catch (e: ApiError) {
                                // A photo the server refuses (too small or blurry) is skipped; connection trouble stops here.
                                if (e.status == 401 || e.status == 429 || e.status >= 500) throw e
                            }
                        }
                        update(c, id) { it.uploaded += 1 }
                        d = get(c, id) ?: break
                    }
                    update(c, id) { x ->
                        x.photos.forEach { try { File(it).delete() } catch (e: Exception) { } }
                        x.state = "sent"
                        x.note = ""
                    }
                    done++
                } catch (e: ApiError) {
                    if (e.status == 401 || e.status == 429 || e.status >= 500) {
                        update(c, id) { it.state = "queued" }
                        break
                    }
                    update(c, id) { it.state = "failed"; it.note = e.message ?: "The server could not accept this property." }
                } catch (e: java.io.IOException) {
                    update(c, id) { it.state = "queued" }
                    break
                }
            }
            done
        }
    }
}
