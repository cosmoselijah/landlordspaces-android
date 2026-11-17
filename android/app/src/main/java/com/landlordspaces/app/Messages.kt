package com.landlordspaces.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val CATEGORY_LABEL = mapOf("buyer" to "Buyer", "owner" to "Owner", "agent" to "Agent", "support" to "Support")

private fun badgeBg(cat: String): Color = when (cat) {
    "agent" -> Color(0xFFDDEBFF)
    "owner" -> Color(0xFFFFE9B8)
    "support" -> Color(0xFFEBDDFF)
    else -> Color(0xFFD5F2E3)
}

private fun badgeFg(cat: String): Color = when (cat) {
    "agent" -> Color(0xFF1E4FA8)
    "owner" -> Color(0xFF8A5A00)
    "support" -> Color(0xFF6B3FB5)
    else -> Color(0xFF0E7C56)
}

/** Messages: your enquiry conversations with owners, agents, buyers and support, from the real conversation list. */
@Composable
fun MessagesScreen(
    repo: Repo, openChat: (Int) -> Unit, openListing: (Int) -> Unit = {}, onNewMessage: () -> Unit = {},
    openMenu: () -> Unit = {}, openAi: () -> Unit = {}, openNotifications: () -> Unit = {}, openAccount: () -> Unit = {},
) {
    StatusBar(darkBackground = true)
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    var convos by remember { mutableStateOf<List<Convo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var query by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(repo.session.name) }
    var notices by remember { mutableStateOf(0) }

    fun load(quiet: Boolean = false) {
        scope.launch {
            if (!quiet) { loading = true; error = "" }
            online { repo.conversations() }.onSuccess { convos = it; error = "" }.onFailure { if (!quiet) error = friendly(it) }
            loading = false
        }
    }
    LaunchedEffect(Unit) {
        online { repo.home() }.onSuccess { h ->
            val u = h.optJSONObject("user")
            name = u?.str("name") ?: name
            avatar = img(u?.str("avatar") ?: "")
            notices = h.optJSONObject("kpi")?.optInt("unread_notifications") ?: 0
        }
    }
    LaunchedEffect(Unit) { load(); while (true) { delay(12000); load(true) } }

    val counts = mapOf(
        "all" to convos.size, "unread" to convos.count { it.unread > 0 },
        "buyer" to convos.count { it.category == "buyer" }, "owner" to convos.count { it.category == "owner" },
        "agent" to convos.count { it.category == "agent" }, "support" to convos.count { it.category == "support" },
    )
    val tabs = listOf("all" to "All", "unread" to "Unread", "buyer" to "Buyers", "owner" to "Owners", "agent" to "Agents", "support" to "Support")
        .filter { it.first == "all" || it.first == "unread" || (counts[it.first] ?: 0) > 0 }
    if (tabs.none { it.first == filter }) filter = "all"
    val q = query.trim()
    val shown = convos.filter { c ->
        (filter == "all" || (filter == "unread" && c.unread > 0) || c.category == filter) &&
            (q.isEmpty() || c.otherName.contains(q, true) || c.propertyTitle.contains(q, true) || c.last.contains(q, true))
    }

    Column(Modifier.fillMaxSize().background(Brand.Bg)) {
        GreenHeader { Column(Modifier.padding(bottom = 14.dp)) { LsTopBar(avatar, name, notices, openMenu, openAi, openNotifications, openAccount) } }
        Column(Modifier.fillMaxWidth().background(Brand.Bg)) {
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Messages", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = Brand.Ink)
                    Text("Your conversations with owners, agents and buyers.", color = Brand.Ink.copy(alpha = 0.75f), fontSize = 13.sp, lineHeight = 16.sp)
                }
                Surface(onClick = { try { focus.requestFocus() } catch (e: Exception) { } }, shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, Brand.Line), modifier = Modifier.padding(start = 6.dp).size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Search, "Search", Modifier.size(22.dp), tint = Brand.Ink) }
                }
                Surface(onClick = onNewMessage, shape = RoundedCornerShape(24.dp), color = Brand.Primary, modifier = Modifier.padding(start = 8.dp).height(44.dp)) {
                    Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp), tint = Color.White)
                        Text(" New", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                    }
                }
            }
            LazyRow(Modifier.padding(top = 12.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tabs, key = { it.first }) { t ->
                    val k = t.first
                    val on = filter == k
                    Surface(onClick = { filter = k }, shape = RoundedCornerShape(22.dp), color = if (on) Brand.Primary else Color.White, border = if (on) null else BorderStroke(1.dp, Brand.Line), modifier = Modifier.height(42.dp)) {
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(t.second + " (" + (counts[k] ?: 0) + ")", color = if (on) Color.White else Brand.Ink, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1)
                            if (k == "unread" && (counts["unread"] ?: 0) > 0 && !on) Box(Modifier.padding(start = 6.dp).size(8.dp).clip(CircleShape).background(Brand.Danger))
                        }
                    }
                }
            }
            Surface(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Brand.Line)) {
                Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, null, Modifier.size(21.dp), tint = Brand.Muted)
                    androidx.compose.foundation.text.BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true, modifier = Modifier.padding(horizontal = 10.dp).weight(1f).focusRequester(focus),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Brand.Ink),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                        decorationBox = { inner -> if (query.isEmpty()) Text("Search conversations, people or properties…", color = Brand.Muted, fontSize = 14.sp, maxLines = 1); inner() },
                    )
                    if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Filled.Close, "Clear search", Modifier.size(20.dp), tint = Brand.Muted) }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(Color.White)) {
            Loadable(loading, error, { load() }) {
                if (shown.isEmpty() && convos.isEmpty()) EmptyState("No conversations yet", "When you message an owner or agent about a property, the chat appears here.")
                else if (shown.isEmpty()) EmptyState("No matches", if (q.isNotEmpty()) "Nothing matches “$q”." else "Nothing in this tab right now.")
                else LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                    items(shown, key = { it.id }) { c ->
                        Row(Modifier.fillMaxWidth().clickable { openChat(c.id) }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
                            Avatar(c.otherAvatar, c.otherName, 52.dp)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(c.otherName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Brand.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                    if (c.verified) Icon(Icons.Filled.Verified, "Verified", Modifier.padding(start = 4.dp).size(16.dp), tint = Brand.Primary)
                                    Surface(Modifier.padding(start = 6.dp), shape = RoundedCornerShape(10.dp), color = badgeBg(c.category)) {
                                        Text(CATEGORY_LABEL[c.category] ?: "Buyer", color = badgeFg(c.category), fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), maxLines = 1)
                                    }
                                }
                                Text(
                                    c.last.ifEmpty { "No messages yet" }, color = if (c.unread > 0) Brand.Ink else Brand.Ink.copy(alpha = 0.75f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (c.unread > 0) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(top = 2.dp),
                                )
                                if (c.propertyTitle.isNotEmpty()) Row(Modifier.padding(top = 3.dp).clickable { if (c.propertyId > 0) openListing(c.propertyId) else openChat(c.id) }, verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Home, null, Modifier.size(15.dp), tint = Brand.Primary)
                                    Text(c.propertyTitle, color = Brand.Primary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(shortTime(c.lastAt), color = Brand.Ink.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1)
                                if (c.unread > 0) Box(Modifier.padding(top = 6.dp).defaultMinSize(minWidth = 22.dp, minHeight = 22.dp).clip(CircleShape).background(Brand.Primary), contentAlignment = Alignment.Center) {
                                    Text("${c.unread}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp))
                                }
                            }
                        }
                        Box(Modifier.fillMaxWidth().padding(start = 80.dp, end = 16.dp).height(1.dp).background(Brand.Line))
                    }
                    item(key = "banner") {
                        Surface(onClick = onNewMessage, modifier = Modifier.padding(16.dp).fillMaxWidth().height(92.dp), shape = RoundedCornerShape(16.dp), color = Brand.MintSoft, border = BorderStroke(1.dp, Brand.Primary.copy(alpha = 0.25f))) {
                            Box {
                                Image(painterResource(R.drawable.hero_building), null, Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.34f), contentScale = ContentScale.Crop, alignment = Alignment.CenterEnd)
                                Column(Modifier.fillMaxWidth(0.68f).padding(14.dp)) {
                                    Text("Property Enquiry Messages", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brand.Ink)
                                    Text("Start a conversation from any property: enquiries, inspections and deals in one place.", fontSize = 12.sp, lineHeight = 15.sp, color = Brand.Ink.copy(alpha = 0.75f), maxLines = 3)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val QUICK = listOf("Is this property still available?", "When can I come for an inspection?", "Is the price negotiable?", "Please share more photos.")

private fun dayLabel(raw: String): String {
    if (raw.length < 10) return ""
    val day = raw.take(10)
    return try {
        val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        f.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val d = f.parse(day) ?: return day
        val today = f.format(java.util.Date())
        val yest = f.format(java.util.Date(System.currentTimeMillis() - 86400000L))
        if (day == today) "Today" else if (day == yest) "Yesterday" else java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.US).format(d)
    } catch (e: Exception) { day }
}

@Composable
fun ChatScreen(repo: Repo, id: Int, onBack: () -> Unit, openListing: (Int) -> Unit = {}) {
    StatusBar(darkBackground = false)
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember(id) { mutableStateOf<Repo.ChatInfo?>(null) }
    var loading by remember(id) { mutableStateOf(true) }
    var error by remember(id) { mutableStateOf("") }
    var text by remember(id) { mutableStateOf("") }
    var sending by remember(id) { mutableStateOf(false) }
    var notice by remember(id) { mutableStateOf("") }
    var menu by remember(id) { mutableStateOf(false) }
    var attachMenu by remember(id) { mutableStateOf(false) }
    var blockAsk by remember(id) { mutableStateOf(false) }
    var reportAsk by remember(id) { mutableStateOf(false) }
    val list = rememberLazyListState()
    val msgs = info?.msgs ?: emptyList()

    fun load(quiet: Boolean = false) {
        scope.launch {
            online { repo.chat(id) }.onSuccess { info = it; error = "" }.onFailure { if (!quiet) error = friendly(it) }
            loading = false
        }
    }
    LaunchedEffect(id) { load(); while (true) { delay(5000); load(true) } }
    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) list.animateScrollToItem(msgs.size - 1) }

    fun sendFile(uri: android.net.Uri, image: Boolean) {
        scope.launch {
            sending = true; notice = ""
            val mime = ctx.contentResolver.getType(uri) ?: (if (image) "image/jpeg" else "application/octet-stream")
            val f: File? = withContext(Dispatchers.IO) {
                try {
                    if (image) {
                        val p = Drafts.addPhoto(ctx, uri)
                        if (p == null) null else File(p)
                    } else {
                        val clean = (uri.lastPathSegment ?: "file").substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_")
                        val out = File(ctx.cacheDir, "att_" + System.currentTimeMillis() + "_" + clean)
                        ctx.contentResolver.openInputStream(uri)?.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
                        if (out.length() > 12L * 1024 * 1024) { out.delete(); null } else out
                    }
                } catch (e: Exception) { null }
            }
            if (f == null) { notice = "That file could not be read, or is larger than 12 MB."; sending = false; return@launch }
            online { repo.sendFile(id, f, if (image) "image/jpeg" else mime, text.trim()) }
                .onSuccess { text = ""; load(true) }.onFailure { notice = friendly(it) }
            try { f.delete() } catch (e: Exception) { }
            sending = false
        }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u -> if (u != null) sendFile(u, true) }
    val pickDoc = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u -> if (u != null) sendFile(u, false) }

    val c = info
    Column(Modifier.fillMaxSize().background(Brand.Bg).statusBarsPadding().imePadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Avatar(c?.avatar ?: "", c?.name ?: "", 40.dp)
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text((c?.name ?: "").ifEmpty { "Conversation" }, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (c?.verified == true) Icon(Icons.Filled.Verified, "Verified", Modifier.padding(start = 4.dp).size(16.dp), tint = Brand.Primary)
                }
                if (c != null && c.propTitle.isNotEmpty()) Text(c.propTitle, color = Brand.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box {
                IconButton({ menu = true }) { Icon(Icons.Filled.MoreVert, "Chat options") }
                DropdownMenu(menu, { menu = false }) {
                    if (c != null && c.propId > 0) DropdownMenuItem({ Text("View property") }, { menu = false; openListing(c.propId) })
                    DropdownMenuItem({ Text("Report this person") }, { menu = false; reportAsk = true })
                    DropdownMenuItem({ Text("Block", color = Brand.Danger) }, { menu = false; blockAsk = true })
                }
            }
        }
        if (c != null && c.propId > 0) Surface(Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 1.dp) {
            Row(Modifier.clickable { openListing(c.propId) }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (c.propCover.isNotEmpty()) AsyncImage(c.propCover, null, Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                else Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Brand.MintSoft), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Home, null, tint = Brand.Primary) }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(c.propTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (c.propPrice.isNotEmpty()) Text(c.propPrice, color = Brand.Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Surface(shape = RoundedCornerShape(16.dp), color = Color.Transparent, border = BorderStroke(1.dp, Brand.Primary)) {
                    Text("View Property", color = Brand.Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
        Box(Modifier.weight(1f)) {
            Loadable(loading, error, { load() }) {
                LazyColumn(state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(msgs.size, key = { msgs[it].id }) { i ->
                        val m = msgs[i]
                        val day = dayLabel(m.createdAt)
                        Column {
                            if (i == 0 || day != dayLabel(msgs[i - 1].createdAt)) Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                Surface(shape = RoundedCornerShape(12.dp), color = Brand.MintSoft) { Text(day, fontSize = 11.sp, color = Brand.Ink, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)) }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.mine) Arrangement.End else Arrangement.Start) {
                                Surface(
                                    Modifier.widthIn(max = 290.dp), color = if (m.mine) Brand.Primary else Color.White,
                                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (m.mine) 18.dp else 4.dp, bottomEnd = if (m.mine) 4.dp else 18.dp), shadowElevation = 1.dp,
                                ) {
                                    Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                                        if (m.attachment.isNotEmpty() && m.attType == "image") AsyncImage(m.attachment, null, Modifier.widthIn(max = 230.dp).height(160.dp).clip(RoundedCornerShape(10.dp)).clickable { openInBrowser(ctx, m.attachment) }, contentScale = ContentScale.Crop)
                                        else if (m.attachment.isNotEmpty()) Text("Open attachment", color = if (m.mine) Color.White else Brand.Primary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { openInBrowser(ctx, m.attachment) }.padding(bottom = 2.dp))
                                        if (m.body.isNotEmpty()) Text(m.body, color = if (m.mine) Color.White else Brand.Ink, fontSize = 15.sp)
                                        Row(Modifier.align(Alignment.End).padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(shortTime(m.createdAt), color = if (m.mine) Color.White.copy(alpha = 0.7f) else Brand.Muted, fontSize = 10.sp)
                                            if (m.mine) Icon(if (m.read) Icons.Filled.DoneAll else Icons.Filled.Done, if (m.read) "Read" else "Delivered", Modifier.padding(start = 4.dp).size(14.dp), tint = if (m.read) Brand.Mint else Color.White.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (notice.isNotEmpty()) Text(notice, color = Brand.Danger, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 4.dp))
        if (text.isEmpty()) LazyRow(Modifier.fillMaxWidth().background(Color.White).padding(top = 6.dp), contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(QUICK) { qk -> Pill(qk, false, { text = qk }) }
        }
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                IconButton({ attachMenu = true }, enabled = !sending) { Icon(Icons.Filled.AttachFile, "Attach", tint = Brand.Primary) }
                DropdownMenu(attachMenu, { attachMenu = false }) {
                    DropdownMenuItem({ Text("Photo") }, { attachMenu = false; pickImage.launch("image/*") })
                    DropdownMenuItem({ Text("Document") }, { attachMenu = false; pickDoc.launch("*/*") })
                }
            }
            Surface(Modifier.weight(1f), shape = RoundedCornerShape(24.dp), color = Brand.Bg) {
                androidx.compose.foundation.text.BasicTextField(
                    value = text, onValueChange = { text = it }, maxLines = 4, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = Brand.Ink),
                    decorationBox = { inner -> if (text.isEmpty()) Text("Write a message…", color = Brand.Muted, fontSize = 15.sp); inner() },
                )
            }
            Surface(
                onClick = {
                    val t = text.trim()
                    if (t.isNotEmpty() && !sending) {
                        sending = true
                        scope.launch {
                            online { repo.send(id, t) }.onSuccess { text = ""; notice = ""; load(true) }.onFailure { notice = friendly(it); text = t }
                            sending = false
                        }
                    }
                },
                modifier = Modifier.padding(start = 8.dp).size(48.dp), shape = CircleShape, color = Brand.Primary,
            ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.Send, "Send", Modifier.size(22.dp), tint = Color.White) } }
        }
    }
    if (blockAsk) AlertDialog(
        onDismissRequest = { blockAsk = false }, title = { Text("Block this person?") }, text = { Text("They will no longer be able to message you, and you will not be able to message them.") },
        confirmButton = {
            TextButton({
                blockAsk = false
                val o = info?.otherId ?: 0
                scope.launch { online { repo.blockUser(o) }.onSuccess { notice = "Blocked." }.onFailure { notice = friendly(it) } }
            }) { Text("Block", color = Brand.Danger) }
        },
        dismissButton = { TextButton({ blockAsk = false }) { Text("Cancel") } },
    )
    if (reportAsk) AlertDialog(
        onDismissRequest = { reportAsk = false }, title = { Text("Report this person") },
        text = {
            Column {
                listOf("Suspected scam or fraud", "Harassment or abuse", "Asking for payment outside the platform", "Fake property or identity").forEach { r ->
                    Text(r, Modifier.fillMaxWidth().clickable {
                        reportAsk = false
                        val o = info?.otherId ?: 0
                        scope.launch { online { repo.reportUser(o, r) }.onSuccess { notice = it }.onFailure { notice = friendly(it) } }
                    }.padding(vertical = 12.dp), fontSize = 14.sp)
                }
            }
        },
        confirmButton = {}, dismissButton = { TextButton({ reportAsk = false }) { Text("Cancel") } },
    )
}
