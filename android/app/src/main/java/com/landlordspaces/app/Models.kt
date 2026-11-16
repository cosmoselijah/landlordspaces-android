package com.landlordspaces.app

import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

fun JSONObject.str(k: String): String = if (isNull(k)) "" else optString(k, "")
fun JSONObject.intOrNull(k: String): Int? = if (isNull(k) || !has(k)) null else optInt(k)
fun JSONArray?.objects(): List<JSONObject> {
    val out = ArrayList<JSONObject>()
    if (this == null) return out
    for (i in 0 until length()) optJSONObject(i)?.let { out.add(it) }
    return out
}
fun JSONArray?.strings(): List<String> {
    val out = ArrayList<String>()
    if (this == null) return out
    for (i in 0 until length()) out.add(optString(i))
    return out
}

/** Site-relative image paths become full URLs. */
fun img(u: String): String = when {
    u.isEmpty() -> ""
    u.startsWith("http") -> u
    else -> SITE + u
}

fun money(price: Double, currency: String): String {
    val sym = when (currency) { "NGN" -> "₦"; "USD" -> "$"; "GBP" -> "£"; "EUR" -> "€"; "GHS" -> "GH₵"; "KES" -> "KSh "; "ZAR" -> "R"; else -> "$currency " }
    val nf = NumberFormat.getIntegerInstance(Locale.US)
    return sym + nf.format(Math.round(price))
}

class Listing(
    val id: Int, val title: String, val market: String, val type: String, val price: Double, val currency: String, val period: String,
    val bedrooms: Int?, val bathrooms: Int?, val parking: Int?, val size: Double, val sizeUnit: String, val area: String, val cover: String,
    val photos: Int, val verified: Boolean, val featured: Boolean, var saved: Boolean, val ownerName: String, val ownerVerified: Boolean,
) {
    val priceText: String get() = money(price, currency)
    val periodText: String get() = if (period.isNotEmpty() && market == "rent") "/" + period else ""
    val badge: String get() = if (market == "rent") "For Rent" else "For Sale"

    companion object {
        fun from(o: JSONObject): Listing {
            val ow = o.optJSONObject("owner") ?: JSONObject()
            return Listing(
                o.optInt("id"), o.str("title"), o.str("market"), o.str("type"), o.optDouble("price", 0.0), o.str("currency").ifEmpty { "NGN" }, o.str("period"),
                o.intOrNull("bedrooms"), o.intOrNull("bathrooms"), o.intOrNull("parking"), o.optDouble("size", 0.0), o.str("size_unit").ifEmpty { "sqm" },
                o.str("area"), img(o.str("cover")), o.optInt("photos"), o.optBoolean("verified"), o.optBoolean("featured"), o.optBoolean("saved"),
                ow.str("name"), ow.optBoolean("verified"),
            )
        }
    }
}

class Detail(
    val base: Listing, val description: String, val photos: List<String>, val amenities: List<String>, val furnishing: String, val yearBuilt: Int?,
    val condition: String, val address: String, val views: Int, val url: String, val floorPlanUrl: String, val hasFloorPlan: Boolean, val own: Boolean,
    val phone: String, val whatsapp: String, val toilets: Int?, val ref: String, val ownerType: String, val similar: List<Listing>,
) {
    companion object {
        fun from(root: JSONObject): Detail {
            val o = root.getJSONObject("listing")
            val c = o.optJSONObject("contact")
            val ow = o.optJSONObject("owner") ?: JSONObject()
            return Detail(
                Listing.from(o), o.str("description"), o.optJSONArray("photos_list").strings().map { img(it) }, o.optJSONArray("amenities").strings(),
                o.str("furnishing"), o.intOrNull("year_built"), o.str("condition"), o.str("address"), o.optInt("views"), o.str("url"), o.str("floor_plan_url"),
                o.optBoolean("has_floor_plan"), o.optBoolean("own"), c?.str("phone") ?: "", c?.str("whatsapp") ?: "", o.intOrNull("toilets"), o.str("ref"),
                ow.str("type"), root.optJSONArray("similar").objects().map { Listing.from(it) },
            )
        }
    }
}

class Convo(
    val id: Int, val propertyTitle: String, val cover: String, val otherName: String, val otherAvatar: String, val last: String, val lastAt: String, val unread: Int,
) {
    companion object {
        fun from(o: JSONObject) = Convo(
            o.optInt("id"), o.str("property_title"), img(o.str("cover")), o.str("other_name").ifEmpty { "Member" }, img(o.str("other_avatar")),
            o.str("last_body"), o.str("last_message_at"), o.optInt("unread"),
        )
    }
}

class Msg(val id: Int, val mine: Boolean, val body: String, val attachment: String, val createdAt: String) {
    companion object {
        fun from(o: JSONObject) = Msg(o.optInt("id"), o.optBoolean("mine"), o.str("body"), img(o.str("attachment_url")), o.str("created_at"))
    }
}

class Notice(val id: Int, val type: String, val title: String, val body: String, val url: String, val read: Boolean, val createdAt: String) {
    companion object {
        fun from(o: JSONObject) = Notice(o.optInt("id"), o.str("type"), o.str("title"), o.str("body"), o.str("url"), o.optInt("read") == 1 || o.optBoolean("read"), o.str("created_at"))
    }
}

/** "2026-11-16 10:12:00" -> "10:12 AM" today, otherwise "Nov 16". Times from the server are UTC. */
fun shortTime(raw: String): String {
    if (raw.length < 16) return ""
    return try {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val d = fmt.parse(raw.take(19)) ?: return ""
        val now = java.util.Calendar.getInstance(); val then = java.util.Calendar.getInstance(); then.time = d
        val same = now.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR) && now.get(java.util.Calendar.DAY_OF_YEAR) == then.get(java.util.Calendar.DAY_OF_YEAR)
        if (same) java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(d) else java.text.SimpleDateFormat("MMM d", Locale.getDefault()).format(d)
    } catch (e: Exception) { "" }
}
