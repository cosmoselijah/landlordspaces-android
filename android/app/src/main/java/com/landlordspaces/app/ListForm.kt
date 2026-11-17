package com.landlordspaces.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private fun JSONArray?.texts(): List<String> {
    val out = ArrayList<String>()
    if (this != null) for (i in 0 until length()) out.add(optString(i))
    return out
}

internal fun statusText(d: Draft): String = when (d.state) {
    "queued" -> "Waiting for network. It will send by itself."
    "sending" -> "Sending..."
    "sent" -> "Saved to your account as a draft. Open My Properties to preview and publish."
    "failed" -> d.note.ifBlank { "The server could not accept this property." }
    else -> "Draft saved on this phone."
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ListingEditor(session: Session, form: JSONObject, existing: Draft?, ctx: android.content.Context, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val markets = form.optJSONArray("markets").texts().ifEmpty { listOf("sell", "rent", "buy") }
    val types = form.optJSONArray("types").texts()
    val currencies = form.optJSONArray("currencies").texts().ifEmpty { listOf("NGN") }
    val amenityList = form.optJSONArray("amenities").texts()
    val furnishList = form.optJSONArray("furnishing").texts()
    val defaults = form.optJSONObject("defaults") ?: JSONObject()
    val f = existing?.fields ?: JSONObject()

    var title by remember { mutableStateOf(f.optString("title")) }
    var description by remember { mutableStateOf(f.optString("description")) }
    var market by remember { mutableStateOf(f.optString("market").ifEmpty { markets.first() }) }
    var type by remember { mutableStateOf(f.optString("type")) }
    var price by remember { mutableStateOf(f.optString("price")) }
    var currency by remember { mutableStateOf(f.optString("currency").ifEmpty { defaults.optString("currency", currencies.first()) }) }
    var period by remember { mutableStateOf(f.optString("price_period").ifEmpty { "year" }) }
    var negotiable by remember { mutableStateOf(f.optBoolean("negotiable", false)) }
    var beds by remember { mutableStateOf(f.optString("bedrooms")) }
    var baths by remember { mutableStateOf(f.optString("bathrooms")) }
    var toilets by remember { mutableStateOf(f.optString("toilets")) }
    var parking by remember { mutableStateOf(f.optString("parking")) }
    var furnishing by remember { mutableStateOf(f.optString("furnishing")) }
    var city by remember { mutableStateOf(f.optString("city").ifEmpty { defaults.optString("city") }) }
    var state by remember { mutableStateOf(f.optString("state").ifEmpty { defaults.optString("state") }) }
    var address by remember { mutableStateOf(f.optString("address")) }
    val amenities = remember { mutableStateListOf<String>().also { l -> f.optJSONArray("amenities").texts().forEach { l.add(it) } } }
    val photos = remember { mutableStateListOf<String>().also { l -> existing?.photos?.forEach { l.add(it) } } }
    val addedNow = remember { mutableListOf<String>() }
    val removedOld = remember { mutableListOf<String>() }
    var adding by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            adding = true
            val room = 20 - photos.size
            val paths = withContext(Dispatchers.IO) { uris.take(room).mapNotNull { Drafts.addPhoto(ctx, it) } }
            paths.forEach { photos.add(it); addedNow.add(it) }
            adding = false
        }
    }

    fun cancel() {
        addedNow.forEach { try { File(it).delete() } catch (e: Exception) { } }
        onClose()
    }

    fun commit(send: Boolean) {
        val n = price.replace(",", "").trim().toDoubleOrNull()
        error = when {
            title.trim().length < 8 -> "Give the property a descriptive title (at least 8 characters)."
            description.trim().length < 30 -> "Add a description of at least 30 characters."
            type.isEmpty() -> "Choose a property type."
            n == null || n < 0 -> "Enter a valid price."
            city.trim().isEmpty() -> "Enter the city."
            else -> ""
        }
        if (error.isNotEmpty()) return
        val j = JSONObject()
            .put("title", title.trim()).put("description", description.trim()).put("market", market).put("type", type)
            .put("price", n).put("currency", currency).put("negotiable", negotiable)
            .put("bedrooms", beds).put("bathrooms", baths).put("toilets", toilets).put("parking", parking)
            .put("furnishing", furnishing).put("country", defaults.optString("country")).put("state", state.trim()).put("city", city.trim()).put("address", address.trim())
            .put("amenities", JSONArray(amenities.toList()))
        if (market == "rent") j.put("price_period", period)
        removedOld.forEach { try { File(it).delete() } catch (e: Exception) { } }
        val d = existing ?: Draft(java.util.UUID.randomUUID().toString(), j, mutableListOf(), "draft", 0, 0, "", 0L)
        d.fields = j
        d.photos = photos.toMutableList()
        d.state = if (send) "queued" else "draft"
        d.note = ""
        Drafts.save(ctx, d)
        if (send) scope.launch { withContext(Dispatchers.IO) { try { Drafts.sync(ctx, session) } catch (e: Exception) { } } }
        onClose()
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        StatusBar(darkBackground = false)
        TopBar(if (existing == null) "New property" else "Edit property", { cancel() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Saved on this phone as you go. Nothing needs a connection until you press Save and send.", color = Brand.Muted, fontSize = 13.sp)
            Text("What are you doing?", fontWeight = FontWeight.SemiBold, color = Brand.Ink)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                markets.forEach { m -> Pill(when (m) { "sell" -> "Sell"; "rent" -> "Rent out"; "buy" -> "Wanted"; else -> m }, market == m, { market = m }) }
            }
            LsField(title, { title = it }, "Title, for example: 3 bedroom flat in Lekki")
            LsField(description, { description = it }, "Describe the property (at least 30 characters)", singleLine = false, minLines = 4)
            Text("Property type", fontWeight = FontWeight.SemiBold, color = Brand.Ink)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                types.forEach { t -> Pill(t, type == t, { type = t }) }
            }
            LsField(price, { price = it }, if (market == "buy") "Budget" else "Price", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                currencies.forEach { c -> Pill(c, currency == c, { currency = c }) }
            }
            if (market == "rent") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("year", "month", "week", "night").forEach { p -> Pill("per $p", period == p, { period = p }) }
                }
            }
            Pill(if (negotiable) "Price is negotiable" else "Price is fixed", negotiable, { negotiable = !negotiable })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { LsField(beds, { beds = it }, "Bedrooms", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number)) }
                Box(Modifier.weight(1f)) { LsField(baths, { baths = it }, "Bathrooms", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { LsField(toilets, { toilets = it }, "Toilets", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number)) }
                Box(Modifier.weight(1f)) { LsField(parking, { parking = it }, "Parking spaces", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number)) }
            }
            if (furnishList.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    furnishList.forEach { x -> Pill(x.replaceFirstChar { it.uppercase() }, furnishing == x, { furnishing = if (furnishing == x) "" else x }) }
                }
            }
            LsField(city, { city = it }, "City")
            LsField(state, { state = it }, "State or region")
            LsField(address, { address = it }, "Street or area (optional)")
            if (amenityList.isNotEmpty()) {
                Text("Features", fontWeight = FontWeight.SemiBold, color = Brand.Ink)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    amenityList.forEach { a -> Pill(a, amenities.contains(a), { if (amenities.contains(a)) amenities.remove(a) else amenities.add(a) }) }
                }
            }
            Text("Photos (${photos.size}/20)", fontWeight = FontWeight.SemiBold, color = Brand.Ink)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(Modifier.size(88.dp).clickable { if (!adding && photos.size < 20) picker.launch("image/*") }, shape = RoundedCornerShape(12.dp), color = Brand.MintSoft) {
                    Box(contentAlignment = Alignment.Center) {
                        if (adding) Text("...", color = Brand.Primary) else Icon(Icons.Filled.AddAPhoto, "Add photos", tint = Brand.Primary)
                    }
                }
                photos.toList().forEach { p ->
                    Box(Modifier.size(88.dp)) {
                        AsyncImage(File(p), null, Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        Surface(Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).clickable {
                            photos.remove(p)
                            if (addedNow.contains(p)) { addedNow.remove(p); try { File(p).delete() } catch (e: Exception) { } } else removedOld.add(p)
                        }, shape = RoundedCornerShape(11.dp), color = Color(0xAA000000)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, "Remove", Modifier.size(14.dp), tint = Color.White) }
                        }
                    }
                }
            }
            if (error.isNotEmpty()) Text(error, color = Brand.Danger, fontSize = 14.sp)
            PrimaryButton("Save and send when online", { commit(true) }, Modifier.fillMaxWidth())
            OutlineButton("Save draft on phone only", { commit(false) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
        }
    }
}
