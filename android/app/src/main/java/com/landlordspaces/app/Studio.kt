package com.landlordspaces.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private class Template(val name: String, val note: String, val market: String, val type: String, val beds: String, val baths: String, val period: String, val furnishing: String = "", val amenities: List<String> = emptyList())

private val TEMPLATES = listOf(
    Template("2-bedroom apartment for rent", "Rent per year, 2 beds, 2 baths", "rent", "Apartment", "2", "2", "year"),
    Template("3-bedroom flat for rent", "Rent per year, 3 beds, 3 baths", "rent", "Apartment", "3", "3", "year"),
    Template("Family house for sale", "4 beds, 4 baths, parking", "sell", "House", "4", "4", "", "", listOf("Parking")),
    Template("Furnished short-let", "Rent per night, furnished", "rent", "Apartment", "1", "1", "night", "furnished"),
    Template("Land plot for sale", "Plot of land", "sell", "Land", "", "", ""),
    Template("Commercial space for rent", "Office or shop, rent per year", "rent", "Commercial", "", "1", "year"),
)

private val LocalMenuClose = androidx.compose.runtime.compositionLocalOf<() -> Unit> { {} }

@Composable
private fun MenuItem(text: @Composable () -> Unit, onClick: () -> Unit) {
    val close = LocalMenuClose.current
    DropdownMenuItem(text, { close(); onClick() })
}

private val TABS = listOf("drafts" to "Drafts", "published" to "Published", "pending" to "Pending Sync", "archived" to "Archived")

/** Progress of a draft from what is really saved: photos, description, pricing, location. */
private class Progress(val photos: Int, val description: Boolean, val pricing: Boolean, val location: Boolean) {
    val percent: Int get() = (listOf(photos > 0, description, pricing, location).count { it } * 100) / 4
}

private fun progressOf(d: Draft): Progress {
    val f = d.fields
    return Progress(d.photos.size, f.optString("description").trim().length >= 30, f.optDouble("price", -1.0) >= 0 && f.has("price"), f.optString("city").isNotBlank())
}

private fun ago(t: Long): String {
    if (t <= 0) return "just now"
    val m = (System.currentTimeMillis() - t) / 60000
    if (m < 1) return "just now"
    if (m < 60) return "$m minute" + (if (m == 1L) "" else "s") + " ago"
    if (m < 1440) return (m / 60).toString() + " hour" + (if (m / 60 == 1L) "" else "s") + " ago"
    return (m / 1440).toString() + " day" + (if (m / 1440 == 1L) "" else "s") + " ago"
}

private fun JSONArray?.objs(): List<JSONObject> {
    val out = ArrayList<JSONObject>()
    if (this != null) for (i in 0 until length()) optJSONObject(i)?.let { out.add(it) }
    return out
}

private fun two(n: Int) = if (n in 0..9) "0$n" else n.toString()

/** Listing Studio: the member's publishing workspace. Everything is written on the phone first and synchronised when a connection exists. */
@Composable
fun ListScreen(
    session: Session, repo: Repo, openWeb: (String, String) -> Unit, onBack: () -> Unit,
    openNotifications: () -> Unit = {}, openAi: () -> Unit = {}, openAccount: () -> Unit = {}, openMenu: () -> Unit = {},
) {
    StatusBar(darkBackground = true)
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var drafts by remember { mutableStateOf(Drafts.all(ctx)) }
    var editing by remember { mutableStateOf<Draft?>(null) }
    var creating by remember { mutableStateOf(false) }
    var form by remember { mutableStateOf<JSONObject?>(null) }
    var formError by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(if (Drafts.all(ctx).isEmpty()) "published" else "drafts") }
    var expanded by remember { mutableStateOf(false) }
    var counts by remember { mutableStateOf(readCache(ctx, "counts")?.let { JSONObject(it) } ?: JSONObject()) }
    var server by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var serverNote by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(session.name) }
    var unread by remember { mutableStateOf(0) }
    var helpOpen by remember { mutableStateOf(false) }
    var templatesOpen by remember { mutableStateOf(false) }
    var noDrafts by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Draft?>(null) }
    var renaming by remember { mutableStateOf<Draft?>(null) }
    var previewing by remember { mutableStateOf<Draft?>(null) }
    var message by remember { mutableStateOf("") }
    var syncing by remember { mutableStateOf(false) }

    fun loadServer() {
        scope.launch {
            online { repo.studio(if (tab == "drafts") "draft" else tab) }
                .onSuccess { j ->
                    counts = j.optJSONObject("counts") ?: counts
                    server = j.optJSONArray("items").objs()
                    writeCache(ctx, "counts", counts.toString())
                    writeCache(ctx, "items:$tab", (j.optJSONArray("items") ?: JSONArray()).toString())
                    serverNote = ""
                }
                .onFailure {
                    server = readCache(ctx, "items:$tab")?.let { c -> try { JSONArray(c).objs() } catch (e: Exception) { emptyList() } } ?: emptyList()
                    serverNote = "Offline. Showing what was last saved on this phone."
                }
        }
    }
    fun syncNow() {
        scope.launch {
            syncing = true
            online { Drafts.sync(ctx, session) }
                .onFailure { message = friendly(it) }
            drafts = Drafts.all(ctx); syncing = false; loadServer()
        }
    }
    fun refreshAll() { drafts = Drafts.all(ctx); loadServer() }

    LaunchedEffect(Unit) {
        online { repo.listingForm() }.onSuccess { form = it }.onFailure { formError = "Open this page once with internet so the form can be saved on your phone." }
        online { repo.home() }.onSuccess { h ->
            val u = h.optJSONObject("user")
            name = u?.str("name") ?: name
            avatar = img(u?.str("avatar") ?: "")
            unread = h.optJSONObject("kpi")?.optInt("unread_notifications") ?: 0
        }
    }
    LaunchedEffect(tab, editing, creating) {
        if (!creating && editing == null) while (true) {
            drafts = Drafts.all(ctx)
            delay(2000)
        }
    }
    LaunchedEffect(tab, creating, editing) { if (!creating && editing == null) loadServer() }

    if (creating || editing != null) {
        val f = form
        if (f == null) {
            Column(Modifier.fillMaxSize().background(Brand.Bg)) {
                StatusBar(darkBackground = false)
                TopBar("Create property", { creating = false; editing = null })
                EmptyState("Getting the form ready", if (formError.isNotEmpty()) formError else "One moment.")
            }
        } else {
            ListingEditor(session, f, editing, ctx, { creating = false; editing = null; refreshAll() })
        }
        return
    }

    // ---- numbers, all real ----
    val local = drafts
    val unsent = local.filter { it.state == "draft" || it.state == "failed" }
    val waiting = local.filter { it.state == "queued" || it.state == "sending" }
    val failed = local.filter { it.state == "failed" }
    val draftCount = unsent.size + counts.optInt("draft")
    val pendingSync = waiting.size + failed.size
    val published = counts.optInt("published")
    val views = counts.optInt("views")
    val recent = unsent.firstOrNull() ?: waiting.firstOrNull()

    fun duplicate(d: Draft) {
        val copy = Draft(java.util.UUID.randomUUID().toString(), JSONObject(d.fields.toString()).put("title", d.title + " (copy)"), mutableListOf(), "draft", 0, 0, "", 0L)
        d.photos.forEach { p ->
            try { val src = File(p); val dst = File(src.parentFile, java.util.UUID.randomUUID().toString() + ".jpg"); src.copyTo(dst); copy.photos.add(dst.absolutePath) } catch (e: Exception) { }
        }
        Drafts.save(ctx, copy); refreshAll(); message = "Copy saved as a separate draft."
    }
    fun startTemplate(t: Template) {
        val types = form?.optJSONArray("types")
        var type = t.type
        if (types != null) for (i in 0 until types.length()) if (types.optString(i).equals(t.type, true)) type = types.optString(i)
        val j = JSONObject().put("market", t.market).put("type", type).put("bedrooms", t.beds).put("bathrooms", t.baths).put("amenities", JSONArray(t.amenities))
        if (t.period.isNotEmpty()) j.put("price_period", t.period)
        if (t.furnishing.isNotEmpty()) j.put("furnishing", t.furnishing)
        templatesOpen = false
        editing = Draft(java.util.UUID.randomUUID().toString(), j, mutableListOf(), "draft", 0, 0, "", 0L)
    }
    fun statusChange(id: Int, status: String, done: String) {
        scope.launch {
            online { repo.setListingStatus(id, status) }.onSuccess { message = done; loadServer() }.onFailure { message = friendly(it) }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Brand.Bg)) {
        val twoCol = maxWidth >= 320.dp
        Column(Modifier.fillMaxSize()) {
            // ---- header ----
            GreenHeader {
                Column(Modifier.padding(bottom = 14.dp)) {
                    Row(Modifier.padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Menu, "Menu", Modifier.clickable { openMenu() }.padding(6.dp).size(24.dp), tint = Color.White)
                        LsLogo(40.dp)
                        Column(Modifier.padding(start = 8.dp).weight(1f)) {
                            Text("LandlordSpaces", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
                            Text("World Smartest Real Estate Marketplace", color = Color.White.copy(alpha = 0.9f), fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Surface(onClick = openAi, shape = RoundedCornerShape(20.dp), color = Color.Transparent, border = BorderStroke(1.5.dp, Brand.Gold), modifier = Modifier.height(40.dp)) {
                            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.AutoAwesome, "Ask AI", Modifier.size(16.dp), tint = Brand.Gold)
                                Text(" AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        Box(Modifier.padding(start = 6.dp).clickable { openNotifications() }.padding(6.dp)) {
                            Icon(Icons.Outlined.Notifications, "Notifications", Modifier.size(24.dp), tint = Color.White)
                            if (unread > 0) Box(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp).defaultMinSize(minWidth = 16.dp, minHeight = 16.dp).clip(CircleShape).background(Brand.Danger), contentAlignment = Alignment.Center) {
                                Text(if (unread > 99) "99+" else "$unread", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                        Box(Modifier.padding(start = 4.dp).size(42.dp).clip(CircleShape).background(Brand.Primary).clickable { openAccount() }, contentAlignment = Alignment.Center) {
                            if (avatar.isNotEmpty() && !avatar.endsWith(".svg")) AsyncImage(avatar, "Profile", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Text(name.trim().take(1).uppercase().ifEmpty { "L" }, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(Modifier.padding(horizontal = 16.dp).padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Property Studio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 27.sp)
                            Text("Create, manage and publish your properties.", color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp, lineHeight = 18.sp)
                        }
                        Surface(onClick = { helpOpen = true }, shape = RoundedCornerShape(22.dp), color = Color.Transparent, border = BorderStroke(1.5.dp, Color.White), modifier = Modifier.padding(start = 8.dp).height(42.dp)) {
                            Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Outlined.HelpOutline, null, Modifier.size(20.dp), tint = Color.White)
                                Text(" Help", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // ---- offline mode, driven by the real state ----
                item(key = "offline") {
                    val busy = waiting.isNotEmpty() || syncing
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Brand.MintSoft) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Outlined.CloudOff, null, Modifier.size(30.dp), tint = Brand.Primary)
                            Column(Modifier.padding(start = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Offline Property Mode", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Brand.Ink)
                                    Surface(Modifier.padding(start = 8.dp), shape = RoundedCornerShape(12.dp), color = Brand.Primary) {
                                        Text("Enabled", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                                    }
                                }
                                Text(
                                    if (repo.offline) "You are offline. Properties are saved on this phone and will sync when you are back online."
                                    else if (busy) "Syncing ${waiting.size} " + (if (waiting.size == 1) "property" else "properties") + "…"
                                    else "Create and save properties without internet. They'll automatically sync when you're back online.",
                                    fontSize = 13.sp, color = Brand.Ink.copy(alpha = 0.8f), lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
                // ---- main banner ----
                item(key = "banner") {
                    Box(Modifier.fillMaxWidth().height(184.dp).clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(listOf(Brand.Deep, Brand.Forest)))) {
                        Image(painterResource(R.drawable.studio_building), null, Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.62f), contentScale = ContentScale.Crop, alignment = Alignment.CenterEnd)
                        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Brand.Deep, 0.42f to Brand.Deep.copy(alpha = 0.94f), 0.72f to Brand.Forest.copy(alpha = 0.3f), 1f to Color.Transparent)))
                        Column(Modifier.fillMaxWidth(0.68f).padding(start = 18.dp, top = 14.dp)) {
                            Surface(shape = RoundedCornerShape(12.dp), color = Brand.Primary) {
                                Text("NEW PROPERTY", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                            }
                            Text("Bring your property to the market.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 8.dp))
                            Text("Create a professional property with photos, pricing, specifications and location.", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 3)
                        }
                        Surface(onClick = { creating = true }, shape = RoundedCornerShape(24.dp), color = Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 14.dp).height(48.dp).fillMaxWidth(0.6f)) {
                            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Add, null, Modifier.size(20.dp), tint = Brand.Primary)
                                Text(" Create New Property", color = Brand.Primary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                            }
                        }
                    }
                }
                // ---- shortcut cards ----
                item(key = "shortcuts") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Shortcut(Icons.Outlined.EditNote, "Continue Draft", "Resume an unfinished property.", Modifier.weight(1f)) {
                            if (recent != null) editing = recent else noDrafts = true
                        }
                        Shortcut(Icons.Outlined.Layers, "Use Template", "Start with a saved property template.", Modifier.weight(1f)) { templatesOpen = true }
                    }
                }
                // ---- overview ----
                item(key = "overhead") { Text("Your Property Overview", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brand.Ink, modifier = Modifier.padding(top = 4.dp)) }
                item(key = "stats1") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Stat(Icons.Outlined.EditNote, two(draftCount), "Draft properties", Modifier.weight(1f)) { tab = "drafts"; expanded = true }
                        Stat(Icons.Outlined.CheckCircleOutline, two(published), "Published properties", Modifier.weight(1f)) { tab = "published"; expanded = true }
                    }
                }
                item(key = "stats2") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Stat(Icons.Outlined.CloudUpload, two(pendingSync), "Pending sync", Modifier.weight(1f)) { tab = "pending"; expanded = true }
                        Stat(Icons.Outlined.Visibility, views.toString(), "Total property views", Modifier.weight(1f)) { openWeb("Property Performance", "/account/performance") }
                    }
                }
                // ---- management ----
                item(key = "mgmt") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (tab) { "published" -> "Published (" + published + ")"; "pending" -> "Pending Sync (" + pendingSync + ")"; "archived" -> "Archived (" + counts.optInt("archived") + ")"; else -> "My Drafts (" + draftCount + ")" },
                                fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brand.Ink, modifier = Modifier.weight(1f),
                            )
                            Row(Modifier.clickable { expanded = !expanded }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (expanded) "Show less" else "See All", color = Brand.Primary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Icon(Icons.Filled.ArrowForward, null, Modifier.padding(start = 4.dp).size(18.dp), tint = Brand.Primary)
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(TABS) { (k, label) -> Pill(label, tab == k, { tab = k; expanded = false }) }
                        }
                        if (serverNote.isNotEmpty()) Text(serverNote, color = Brand.Muted, fontSize = 12.sp)
                        if (message.isNotEmpty()) Text(message, color = Brand.Primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                when (tab) {
                    "drafts" -> {
                        val shown = unsent + waiting
                        val all = shown.size + server.size
                        if (all == 0) item(key = "empty") { EmptyState("No drafts yet", "Tap Create New Property. Photos and details are saved on this phone.") }
                        val limit = if (expanded) Int.MAX_VALUE else 3
                        items(shown.take(limit), key = { "l" + it.id }) { d ->
                            LocalDraftCard(d, { editing = d }, { editing = d }, { duplicate(d) }, { previewing = d }, { renaming = d }, { deleting = d }, {
                                d.state = "queued"; d.note = ""; Drafts.save(ctx, d); syncNow()
                            })
                        }
                        items(server.take(maxOf(0, limit - shown.size)), key = { "s" + it.optInt("id") }) { s ->
                            ServerCard(s, tab, { openWeb("Edit property", "/account/listings/" + s.optInt("id") + "/edit") }, openWeb, { st, done -> statusChange(s.optInt("id"), st, done) })
                        }
                    }
                    "pending" -> {
                        val q = waiting + failed
                        if (q.isEmpty()) item(key = "empty") { EmptyState("Nothing waiting", "Everything on this phone has been sent.") }
                        else item(key = "retry") {
                            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Brand.Line)) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (syncing) "Synchronising…" else "${q.size} " + (if (q.size == 1) "property" else "properties") + " not yet on the server", Modifier.weight(1f), fontSize = 13.sp, color = Brand.Ink)
                                    Pill(if (syncing) "Sending…" else "Retry all", true, { if (!syncing) { q.forEach { it.state = "queued"; it.note = ""; Drafts.save(ctx, it) }; syncNow() } })
                                }
                            }
                        }
                        items(q.take(if (expanded) Int.MAX_VALUE else 3), key = { "l" + it.id }) { d ->
                            LocalDraftCard(d, { editing = d }, { editing = d }, { duplicate(d) }, { previewing = d }, { renaming = d }, { deleting = d }, {
                                d.state = "queued"; d.note = ""; Drafts.save(ctx, d); syncNow()
                            })
                        }
                    }
                    else -> {
                        if (server.isEmpty()) item(key = "empty") { EmptyState(if (tab == "published") "No published properties" else "No archived properties", "Properties you " + (if (tab == "published") "publish from My Properties" else "archive") + " appear here.") }
                        items(server.take(if (expanded) Int.MAX_VALUE else 3), key = { "s" + it.optInt("id") }) { s ->
                            ServerCard(s, tab, { openWeb("Edit property", "/account/listings/" + s.optInt("id") + "/edit") }, openWeb, { st, done -> statusChange(s.optInt("id"), st, done) })
                        }
                    }
                }
            }
        }
    }

    // ---- dialogs ----
    if (helpOpen) AlertDialog(
        onDismissRequest = { helpOpen = false }, title = { Text("Property Studio help") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• Create New Property: write the property with photos. It is saved on your phone as you go.", fontSize = 14.sp)
                Text("• With no internet, tap Save and send when online. It goes by itself when the connection returns.", fontSize = 14.sp)
                Text("• Drafts reach your account as drafts. Publish them from My Properties after you preview.", fontSize = 14.sp)
                Text("• Photos should be at least about 1000 pixels wide.", fontSize = 14.sp)
            }
        },
        confirmButton = { TextButton({ helpOpen = false; openWeb("Help Centre", "/help") }) { Text("Open Help Centre") } },
        dismissButton = { TextButton({ helpOpen = false }) { Text("Close") } },
    )
    if (noDrafts) AlertDialog(
        onDismissRequest = { noDrafts = false }, title = { Text("No unfinished drafts") },
        text = { Text("You have no draft on this phone yet. Create a new property to begin.") },
        confirmButton = { TextButton({ noDrafts = false; creating = true }) { Text("Create New Property") } },
        dismissButton = { TextButton({ noDrafts = false }) { Text("Not now") } },
    )
    if (templatesOpen) AlertDialog(
        onDismissRequest = { templatesOpen = false }, title = { Text("Choose a template") },
        text = {
            Column {
                TEMPLATES.forEach { t ->
                    Column(Modifier.fillMaxWidth().clickable { startTemplate(t) }.padding(vertical = 10.dp)) {
                        Text(t.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(t.note, color = Brand.Muted, fontSize = 12.sp)
                    }
                }
                Text("A template only fills in the basics. You can change everything, and it is saved as a new draft.", color = Brand.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = {}, dismissButton = { TextButton({ templatesOpen = false }) { Text("Cancel") } },
    )
    deleting?.let { d ->
        AlertDialog(
            onDismissRequest = { deleting = null }, title = { Text("Delete this draft?") },
            text = { Text("“" + d.title + "” and its photos will be removed from this phone. This cannot be undone." + if (d.serverId > 0) " A copy already sent to your account stays there until you remove it from My Properties." else "") },
            confirmButton = { TextButton({ Drafts.remove(ctx, d.id); deleting = null; refreshAll(); message = "Draft deleted." }) { Text("Delete", color = Brand.Danger) } },
            dismissButton = { TextButton({ deleting = null }) { Text("Keep it") } },
        )
    }
    renaming?.let { d ->
        var t by remember(d.id) { mutableStateOf(d.fields.optString("title")) }
        AlertDialog(
            onDismissRequest = { renaming = null }, title = { Text("Rename draft") },
            text = { LsField(t, { t = it }, "Title") },
            confirmButton = { TextButton({ if (t.isNotBlank()) { d.fields.put("title", t.trim()); Drafts.save(ctx, d); refreshAll() }; renaming = null }) { Text("Save") } },
            dismissButton = { TextButton({ renaming = null }) { Text("Cancel") } },
        )
    }
    previewing?.let { d ->
        val f = d.fields
        AlertDialog(
            onDismissRequest = { previewing = null }, title = { Text(d.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (d.photos.isNotEmpty()) AsyncImage(File(d.photos.first()), null, Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                    val price = f.optDouble("price", 0.0)
                    if (f.has("price")) Text(money(price, f.optString("currency").ifEmpty { "NGN" }) + (if (f.optString("price_period").isNotEmpty()) "/" + f.optString("price_period") else ""), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(listOf(f.optString("type"), f.optString("city"), f.optString("state")).filter { it.isNotBlank() }.joinToString(" · "), color = Brand.Muted, fontSize = 13.sp)
                    val specs = listOf(f.optString("bedrooms").takeIf { it.isNotBlank() }?.let { "$it bed" }, f.optString("bathrooms").takeIf { it.isNotBlank() }?.let { "$it bath" }, f.optString("parking").takeIf { it.isNotBlank() }?.let { "$it parking" }).filterNotNull()
                    if (specs.isNotEmpty()) Text(specs.joinToString("  |  "), fontSize = 13.sp)
                    Text(f.optString("description").ifBlank { "No description yet." }, fontSize = 13.sp, maxLines = 8, overflow = TextOverflow.Ellipsis)
                    Text("${d.photos.size} photo(s) · " + statusText(d), color = Brand.Muted, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton({ val x = d; previewing = null; editing = x }) { Text("Edit") } },
            dismissButton = { TextButton({ previewing = null }) { Text("Close") } },
        )
    }
}

@Composable
private fun Shortcut(icon: ImageVector, title: String, text: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(94.dp), shape = RoundedCornerShape(16.dp), color = Brand.MintSoft) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(32.dp), tint = Brand.Primary)
            Column(Modifier.padding(horizontal = 10.dp).weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Brand.Ink, maxLines = 1)
                Text(text, fontSize = 12.sp, lineHeight = 15.sp, color = Brand.Ink.copy(alpha = 0.75f), maxLines = 3)
            }
            Icon(Icons.Filled.ChevronRight, null, Modifier.size(20.dp), tint = Brand.Ink)
        }
    }
}

@Composable
private fun Stat(icon: ImageVector, value: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(82.dp), shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
        Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Brand.MintSoft), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(26.dp), tint = Brand.Primary)
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Brand.Ink, maxLines = 1)
                Text(label, fontSize = 13.sp, color = Brand.Ink.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun Check(label: String, value: String, done: Boolean, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(if (done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, Modifier.size(18.dp), tint = if (done) Brand.Primary else Brand.Muted)
        Column(Modifier.padding(start = 4.dp)) {
            Text(label, fontSize = 10.sp, color = Brand.Ink, maxLines = 1)
            Text(value, fontSize = 10.sp, color = Brand.Muted, maxLines = 1)
        }
    }
}

@Composable
private fun DraftShell(
    title: String, badge: String, badgeColor: Color, edited: String, photoCount: Int, cover: @Composable () -> Unit,
    p: Progress, status: String, statusColor: Color, primary: String, onPrimary: () -> Unit, menu: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 2.dp) {
        Column {
            Row(Modifier.padding(12.dp)) {
                Box(Modifier.width(112.dp).height(116.dp).clip(RoundedCornerShape(12.dp)).background(Brand.MintSoft)) {
                    cover()
                    if (photoCount > 0) Surface(Modifier.align(Alignment.BottomEnd).padding(6.dp), shape = RoundedCornerShape(8.dp), color = Color(0x99000000)) {
                        Row(Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(13.dp), tint = Color.White)
                            Text(" $photoCount", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Brand.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(10.dp), color = badgeColor) { Text(badge, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Brand.Ink, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) }
                        Text(edited, fontSize = 11.sp, color = Brand.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp))
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Check("Photos", if (p.photos > 0) "${p.photos} added" else "None", p.photos > 0, Modifier)
                        Check("Description", if (p.description) "Done" else "Pending", p.description, Modifier)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Check("Pricing", if (p.pricing) "Done" else "Pending", p.pricing, Modifier)
                        Check("Location", if (p.location) "Done" else "Pending", p.location, Modifier)
                    }
                }
            }
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(progress = { p.percent / 100f }, Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)), color = Brand.Mint, trackColor = Brand.Line)
                Text(p.percent.toString() + "% complete", fontSize = 11.sp, color = Brand.Ink, modifier = Modifier.padding(start = 8.dp))
            }
            if (status.isNotEmpty()) Text(status, fontSize = 12.sp, color = statusColor, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(1.dp).background(Brand.Line))
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).fillMaxHeight().clickable { onPrimary() }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(primary, color = Brand.Primary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Icon(Icons.Filled.ArrowForward, null, Modifier.padding(start = 6.dp).size(18.dp), tint = Brand.Primary)
                }
                Box(Modifier.width(1.dp).height(24.dp).background(Brand.Line))
                Box {
                    Row(Modifier.clickable { open = true }.padding(horizontal = 18.dp).height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MoreVert, "More options", Modifier.size(22.dp), tint = Brand.Ink)
                        Text(" More options", fontSize = 14.sp, color = Brand.Ink)
                    }
                    DropdownMenu(open, { open = false }) { androidx.compose.runtime.CompositionLocalProvider(LocalMenuClose provides { open = false }) { menu() } }
                }
            }
        }
    }
}

@Composable
private fun LocalDraftCard(
    d: Draft, onOpen: () -> Unit, onEdit: () -> Unit, onDuplicate: () -> Unit, onPreview: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit, onSend: () -> Unit,
) {
    val p = progressOf(d)
    val (badge, color) = when (d.state) {
        "queued" -> "Waiting to sync" to Color(0xFFDDEBFF)
        "sending" -> "Syncing" to Color(0xFFDDEBFF)
        "failed" -> "Failed" to Color(0xFFFFDADA)
        else -> "Draft" to Color(0xFFFFE3A3)
    }
    DraftShell(
        d.title, badge, color, "Last edited " + ago(d.updated), d.photos.size,
        { if (d.photos.isNotEmpty()) AsyncImage(File(d.photos.first()), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) },
        p, if (d.state == "draft") "Saved on this phone only." else statusText(d), if (d.state == "failed") Brand.Danger else Brand.Muted,
        if (d.state == "failed") "Fix and retry" else "Continue editing", onOpen,
    ) {
        if (d.state != "sending") MenuItem({ Text("Edit") }, { onEdit() })
        MenuItem({ Text("Preview") }, { onPreview() })
        MenuItem({ Text("Duplicate") }, { onDuplicate() })
        MenuItem({ Text("Rename") }, { onRename() })
        if (d.state == "draft" || d.state == "failed" || d.state == "queued") MenuItem({ Text(if (d.state == "queued") "Send now" else "Synchronise") }, { onSend() })
        if (d.state != "sending") MenuItem({ Text("Delete", color = Brand.Danger) }, { onDelete() })
    }
}

@Composable
private fun ServerCard(s: JSONObject, tab: String, onEdit: () -> Unit, openWeb: (String, String) -> Unit, change: (String, String) -> Unit) {
    val ctx = LocalContext.current
    val p = Progress(s.optInt("photos"), s.optBoolean("has_description"), s.optBoolean("has_price"), s.optBoolean("has_location"))
    val cover = img(s.optString("cover"))
    val slug = s.optString("slug")
    val status = s.optString("status")
    val (badge, color) = when (status) {
        "published" -> "Published" to Color(0xFFD5F2E3)
        "pending" -> "In review" to Color(0xFFDDEBFF)
        "archived" -> "Archived" to Color(0xFFE6E6E6)
        else -> "Draft" to Color(0xFFFFE3A3)
    }
    val line = money(s.optDouble("price", 0.0), s.optString("currency").ifEmpty { "NGN" })
    DraftShell(
        s.optString("title").ifBlank { "Untitled property" }, badge, color, line + " · " + s.optInt("views") + " views", s.optInt("photos"),
        { if (cover.isNotEmpty()) AsyncImage(cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) },
        p, "", Brand.Muted, if (status == "published") "View property" else "Continue editing",
        { if (status == "published" && slug.isNotEmpty()) openWeb("Property", "/property/$slug") else onEdit() },
    ) {
        MenuItem({ Text("Edit") }, { onEdit() })
        MenuItem({ Text("Preview") }, { openWeb("Preview", "/account/listings/" + s.optInt("id") + "/preview") })
        if (status == "published") {
            MenuItem({ Text("Share") }, {
                val url = "https://landlordspaces.com/property/$slug"
                ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, s.optString("title") + "\n" + url), "Share property"))
            })
            MenuItem({ Text("View performance") }, { openWeb("Property Performance", "/account/performance") })
            MenuItem({ Text(if (s.optString("market") == "rent") "Mark as rented" else "Mark as sold") }, { change(if (s.optString("market") == "rent") "rented" else "sold", "Marked as " + (if (s.optString("market") == "rent") "rented." else "sold.")) })
            MenuItem({ Text("Pause (back to draft)") }, { change("draft", "Property paused and moved to drafts.") })
        }
        if (status == "archived") MenuItem({ Text("Restore as draft") }, { change("draft", "Restored as a draft.") })
        else MenuItem({ Text("Archive") }, { change("archived", "Property archived.") })
    }
}

private fun readCache(c: android.content.Context, key: String): String? =
    c.applicationContext.getSharedPreferences("ls_studio", android.content.Context.MODE_PRIVATE).getString(key, null)

private fun writeCache(c: android.content.Context, key: String, v: String) {
    c.applicationContext.getSharedPreferences("ls_studio", android.content.Context.MODE_PRIVATE).edit().putString(key, v).apply()
}
