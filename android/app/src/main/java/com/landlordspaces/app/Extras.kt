package com.landlordspaces.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun OfflineBanner(modifier: Modifier = Modifier, waiting: Int = 0) {
    Row(modifier.fillMaxWidth().background(Brand.Ink).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.WifiOff, null, Modifier.size(14.dp), tint = Color.White)
        Text("  You are offline. Showing saved results." + (if (waiting > 0) " $waiting change(s) will send when you are back online." else ""), color = Color.White, fontSize = 12.sp)
    }
}

/** Floating bar that appears while two or more properties are picked for comparison. */
@Composable
fun CompareBar(count: Int, onOpen: () -> Unit, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.padding(16.dp), shape = RoundedCornerShape(28.dp), color = Brand.Forest, shadowElevation = 8.dp) {
        Row(Modifier.padding(start = 20.dp, end = 6.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Compare ($count)", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onOpen() }.padding(vertical = 10.dp))
            Spacer(Modifier.width(10.dp))
            IconButton(onClear) { Icon(Icons.Filled.Close, "Clear", tint = Color.White) }
        }
    }
}

@Composable
fun CompareScreen(items: List<Listing>, onBack: () -> Unit, onRemove: (Listing) -> Unit, openListing: (Int) -> Unit) {
    StatusBar(darkBackground = false)
    Column(Modifier.fillMaxSize().background(Brand.Bg)) {
        TopBar("Compare properties", onBack)
        if (items.size < 2) {
            EmptyState("Pick at least two", "Open a property and tap the compare icon to add it here.")
            return@Column
        }
        val rows = listOf<Pair<String, (Listing) -> String>>(
            "Price" to { l: Listing -> l.priceText + l.periodText },
            "Type" to { l: Listing -> l.type.ifEmpty { "-" } },
            "Bedrooms" to { l: Listing -> (l.bedrooms ?: 0).toString() },
            "Bathrooms" to { l: Listing -> (l.bathrooms ?: 0).toString() },
            "Size" to { l: Listing -> if (l.size > 0) l.size.toInt().toString() + " " + l.sizeUnit else "-" },
            "Parking" to { l: Listing -> (l.parking ?: 0).toString() },
            "Area" to { l: Listing -> l.area.ifEmpty { "-" } },
            "Listed by" to { l: Listing -> l.ownerName.ifEmpty { "-" } },
            "Verified" to { l: Listing -> if (l.verified) "Yes" else "No" },
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()).padding(16.dp)) {
            Row {
                Spacer(Modifier.width(92.dp))
                items.forEach { l ->
                    Column(Modifier.width(150.dp).padding(horizontal = 4.dp).clickable { openListing(l.id) }) {
                        if (l.cover.isNotEmpty()) AsyncImage(l.cover, l.title, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(90.dp).background(Brand.MintSoft, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)))
                        Text(l.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                        Text("Remove", color = Brand.Danger, fontSize = 12.sp, modifier = Modifier.clickable { onRemove(l) }.padding(vertical = 4.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            rows.forEachIndexed { i, (label, get) ->
                Row(Modifier.background(if (i % 2 == 0) Color.White else Brand.Bg).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = Brand.Muted, fontSize = 12.sp, modifier = Modifier.width(92.dp).padding(start = 6.dp))
                    items.forEach { l -> Text(get(l), fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.width(150.dp).padding(horizontal = 4.dp)) }
                }
            }
        }
    }
}


@Composable
fun TenantsScreen(repo: Repo, onBack: () -> Unit, openWeb: (String, String) -> Unit) {
    StatusBar(darkBackground = false)
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<JSONObject?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    fun load() { scope.launch { loading = true; error = ""; online { repo.tenants() }.onSuccess { data = it }.onFailure { error = friendly(it) }; loading = false } }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().background(Brand.Bg)) {
        TopBar("Tenant Management", onBack)
        Loadable(loading, error, { load() }) {
            val s = data?.optJSONObject("summary") ?: JSONObject()
            val units = data?.optJSONArray("units").objects()
            val cur = s.str("currency").ifEmpty { "NGN" }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Units" to s.optInt("units").toString(), "Occupied" to s.optInt("occupied").toString(), "Vacant" to s.optInt("vacant").toString()).forEach { (l, v) ->
                        Surface(Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
                            Column(Modifier.padding(14.dp)) { Text(v, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Brand.Primary); Text(l, color = Brand.Muted, fontSize = 12.sp) }
                        }
                    }
                }
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Brand.Forest) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Monthly rent roll", color = Color(0xCCFFFFFF), fontSize = 13.sp)
                        Text(money(s.optDouble("rent_month", 0.0), cur), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                    }
                }
                if (units.isEmpty()) EmptyState("No units yet", "Add your buildings and units to track rent and leases.")
                units.forEach { u ->
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(u.str("name"), fontWeight = FontWeight.Bold)
                                    Text(u.str("building"), color = Brand.Muted, fontSize = 12.sp)
                                }
                                val occ = u.str("status") == "occupied"
                                Surface(shape = RoundedCornerShape(10.dp), color = if (occ) Brand.MintSoft else Color(0xFFFFF3D6)) {
                                    Text(if (occ) "Occupied" else "Vacant", color = if (occ) Brand.Primary else Color(0xFF8A6100), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                }
                            }
                            if (u.str("tenant_name").isNotEmpty()) Text("Tenant: " + u.str("tenant_name"), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                            Text(money(u.optDouble("rent", 0.0), u.str("currency").ifEmpty { cur }) + " rent" + if (u.str("lease_end").isNotEmpty()) "  ·  lease ends " + u.str("lease_end").take(10) else "", color = Brand.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                PrimaryButton("Open full tenant tools", { openWeb("Tenant tools", "/tenant-management") }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun ProfileScreen(repo: Repo, onBack: () -> Unit, openWeb: (String, String) -> Unit) {
    StatusBar(darkBackground = false)
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var good by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        online { repo.me() }.onSuccess { u -> name = u.str("name"); phone = u.str("phone"); city = u.str("city"); email = u.str("email") }.onFailure { note = friendly(it) }
        loading = false
    }
    Column(Modifier.fillMaxSize().background(Brand.Bg)) {
        TopBar("My Profile", onBack)
        Loadable(loading, "", {}) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Full name", fontWeight = FontWeight.SemiBold)
                LsField(name, { name = it }, "Your name")
                Text("Email", fontWeight = FontWeight.SemiBold)
                Text(email, color = Brand.Muted)
                Text("Phone", fontWeight = FontWeight.SemiBold)
                LsField(phone, { phone = it }, "+234…", keyboard = KeyboardOptions(keyboardType = KeyboardType.Phone))
                Text("City", fontWeight = FontWeight.SemiBold)
                LsField(city, { city = it }, "Where you are based")
                if (note.isNotEmpty()) Text(note, color = if (good) Brand.Primary else Brand.Danger, fontSize = 13.sp)
                PrimaryButton("Save changes", {
                    saving = true; note = ""
                    scope.launch {
                        online { repo.saveProfile(name, phone, city) }
                            .onSuccess { good = true; note = "Saved."; repo.session.name = name }
                            .onFailure { good = false; note = friendly(it) }
                        saving = false
                    }
                }, Modifier.fillMaxWidth(), enabled = !saving, loading = saving)
                OutlineButton("Photo, verification and more", { openWeb("Account settings", "/account/profile") }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun SecurityScreen(repo: Repo, onBack: () -> Unit, openWeb: (String, String) -> Unit, onSignedOut: () -> Unit) {
    StatusBar(darkBackground = false)
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    fun load() { scope.launch { loading = true; error = ""; online { repo.sessions() }.onSuccess { list = it }.onFailure { error = friendly(it) }; loading = false } }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().background(Brand.Bg)) {
        TopBar("Security & Privacy", onBack)
        Loadable(loading, error, { load() }) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Devices signed in", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                list.forEach { s ->
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 1.dp) {
                        Column(Modifier.padding(14.dp)) {
                            Text(s.str("device_name").ifEmpty { "Android phone" }, fontWeight = FontWeight.SemiBold)
                            Text("App v" + s.str("app_version") + "  ·  last used " + s.str("last_used").take(16), color = Brand.Muted, fontSize = 12.sp)
                        }
                    }
                }
                if (list.isEmpty()) Text("No other devices.", color = Brand.Muted)
                OutlineButton("Sign out of all devices", { confirm = true }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Password and two-step sign-in", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                PrimaryButton("Manage password and 2-step", { openWeb("Account security", "/account/profile") }, Modifier.fillMaxWidth())
                val ctx = LocalContext.current
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Text("Privacy policy", color = Brand.Primary, modifier = Modifier.clickable { openInBrowser(ctx, "$SITE/p/privacy") })
                    Text("Delete account", color = Brand.Danger, modifier = Modifier.clickable { openInBrowser(ctx, "$SITE/delete-account") })
                }
            }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false }, title = { Text("Sign out everywhere?") }, text = { Text("Every phone and browser using the app will need to sign in again, including this one.") },
        confirmButton = { TextButton({ confirm = false; scope.launch { online { repo.revokeAll() }; repo.session.clear(); onSignedOut() } }) { Text("Sign out all", color = Brand.Danger) } },
        dismissButton = { TextButton({ confirm = false }) { Text("Cancel") } },
    )
}

@Composable
fun HelpScreen(repo: Repo, onBack: () -> Unit, openWeb: (String, String) -> Unit, openAi: () -> Unit) {
    StatusBar(darkBackground = false)
    val ctx = LocalContext.current
    var email by remember { mutableStateOf("support@landlordspaces.com") }
    LaunchedEffect(Unit) { online { repo.config() }.onSuccess { c -> c.str("support_email").takeIf { it.isNotEmpty() }?.let { email = it } } }
    val faq = listOf(
        "How do I contact an owner?" to "Open a property and tap Contact Agent. Your message appears under Messages.",
        "How do I list a property?" to "Tap List in the bottom bar and follow the steps. Photos can come from your camera or gallery.",
        "Why is a property marked Verified?" to "Verified listings have had the owner or agent checked by our team.",
        "How do I reset my password?" to "On the sign in screen tap Forgot password and follow the email link.",
    )
    var open by remember { mutableStateOf(-1) }
    Column(Modifier.fillMaxSize().background(Brand.Bg)) {
        TopBar("Help & Support", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Ask the AI assistant", openAi, Modifier.fillMaxWidth())
            faq.forEachIndexed { i, (q, a) ->
                Surface(Modifier.fillMaxWidth().clickable { open = if (open == i) -1 else i }, shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 1.dp) {
                    Column(Modifier.padding(16.dp)) {
                        Text(q, fontWeight = FontWeight.SemiBold)
                        if (open == i) Text(a, color = Brand.Muted, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            PrimaryButton("Email support", {
                try { ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).putExtra(Intent.EXTRA_SUBJECT, "LandlordSpaces app support")) } catch (e: Exception) { }
            }, Modifier.fillMaxWidth())
            Text(email, color = Brand.Muted, fontSize = 13.sp)
            OutlineButton("Help centre", { openWeb("Help centre", "/help") }, Modifier.fillMaxWidth())
        }
    }
}
