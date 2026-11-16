package com.landlordspaces.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MessagesScreen(repo: Repo, openChat: (Int) -> Unit) {
    StatusBar(darkBackground = false)
    val scope = rememberCoroutineScope()
    var convos by remember { mutableStateOf<List<Convo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var query by remember { mutableStateOf("") }

    fun load(quiet: Boolean = false) {
        scope.launch {
            if (!quiet) { loading = true; error = "" }
            online { repo.conversations() }.onSuccess { convos = it }.onFailure { if (!quiet) error = friendly(it) }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load(); while (true) { delay(15000); load(true) } }

    val shown = convos.filter { (filter == "all" || it.unread > 0) && (query.isBlank() || it.otherName.contains(query, true) || it.propertyTitle.contains(query, true) || it.last.contains(query, true)) }

    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text("Messages", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            Text("Your conversations with owners, agents and buyers.", color = Brand.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("All (${convos.size})", filter == "all", { filter = "all" })
                Pill("Unread (${convos.count { it.unread > 0 }})", filter == "unread", { filter = "unread" })
            }
            Surface(Modifier.padding(top = 12.dp).fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Brand.Bg) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, null, Modifier.size(20.dp), tint = Brand.Muted)
                    androidx.compose.foundation.text.BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true, modifier = Modifier.padding(start = 10.dp).weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Brand.Ink),
                        decorationBox = { inner -> if (query.isEmpty()) Text("Search conversations…", color = Brand.Muted, fontSize = 14.sp); inner() },
                    )
                }
            }
        }
        Loadable(loading, error, { load() }) {
            if (shown.isEmpty()) EmptyState("No conversations yet", "When you message an owner or agent, the chat appears here.")
            else LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(shown, key = { it.id }) { c ->
                    Row(Modifier.fillMaxWidth().clickable { openChat(c.id) }.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(c.otherAvatar.ifEmpty { c.cover }, c.otherName, 52.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(c.otherName, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (c.propertyTitle.isNotEmpty()) Text(c.propertyTitle, color = Brand.Primary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(c.last, color = if (c.unread > 0) Brand.Ink else Brand.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (c.unread > 0) FontWeight.SemiBold else FontWeight.Normal)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(shortTime(c.lastAt), color = Brand.Muted, fontSize = 11.sp)
                            if (c.unread > 0) Box(Modifier.padding(top = 6.dp).size(20.dp).clip(CircleShape).background(Brand.Primary), contentAlignment = Alignment.Center) {
                                Text("${c.unread}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(repo: Repo, id: Int, onBack: () -> Unit) {
    StatusBar(darkBackground = false)
    val scope = rememberCoroutineScope()
    var who by remember(id) { mutableStateOf("") }
    var about by remember(id) { mutableStateOf("") }
    var msgs by remember(id) { mutableStateOf<List<Msg>>(emptyList()) }
    var loading by remember(id) { mutableStateOf(true) }
    var error by remember(id) { mutableStateOf("") }
    var text by remember(id) { mutableStateOf("") }
    var sending by remember(id) { mutableStateOf(false) }
    var notice by remember(id) { mutableStateOf("") }
    val list = rememberLazyListState()

    fun load(quiet: Boolean = false) {
        scope.launch {
            online { repo.thread(id) }.onSuccess { (n, t, m) -> who = n; about = t; msgs = m; error = "" }.onFailure { if (!quiet) error = friendly(it) }
            loading = false
        }
    }
    LaunchedEffect(id) { load(); while (true) { delay(6000); load(true) } }
    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) list.animateScrollToItem(msgs.size - 1) }

    Column(Modifier.fillMaxSize().background(Brand.Bg).statusBarsPadding().imePadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Avatar("", who, 38.dp)
            Column(Modifier.padding(start = 10.dp)) {
                Text(who.ifEmpty { "Conversation" }, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                if (about.isNotEmpty()) Text(about, color = Brand.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(Modifier.weight(1f)) {
            Loadable(loading, error, { load() }) {
                LazyColumn(state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(msgs, key = { it.id }) { m ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.mine) Arrangement.End else Arrangement.Start) {
                            Surface(
                                Modifier.widthIn(max = 290.dp), color = if (m.mine) Brand.Primary else Color.White,
                                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (m.mine) 18.dp else 4.dp, bottomEnd = if (m.mine) 4.dp else 18.dp), shadowElevation = 1.dp,
                            ) {
                                Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                                    if (m.body.isNotEmpty()) Text(m.body, color = if (m.mine) Color.White else Brand.Ink, fontSize = 15.sp)
                                    if (m.attachment.isNotEmpty()) Text("Attachment (open on the website)", color = if (m.mine) Color.White else Brand.Primary, fontSize = 12.sp)
                                    Text(shortTime(m.createdAt), color = if (m.mine) Color.White.copy(alpha = 0.7f) else Brand.Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.End).padding(top = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        if (notice.isNotEmpty()) Text(notice, color = Brand.Danger, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 4.dp))
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
}
