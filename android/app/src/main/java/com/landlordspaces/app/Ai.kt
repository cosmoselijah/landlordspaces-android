package com.landlordspaces.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class AiMsg(val mine: Boolean, val text: String, val actions: List<Pair<String, String>> = emptyList(), val suggest: List<String> = emptyList())

/** The Landlordspaces assistant inside the app: same brain as the website's "Ask AI", shown as a normal chat. */
@Composable
fun AiScreen(repo: Repo, onBack: () -> Unit, openWeb: (String, String) -> Unit, openListing: (Int) -> Unit) {
    StatusBar(darkBackground = false)
    val scope = rememberCoroutineScope()
    val msgs = remember { mutableStateListOf(AiMsg(false, "Hello! I am your Landlordspaces assistant. Ask me about finding, renting, buying or listing a property, or about using the app.", suggest = listOf("How do I list a property?", "How do I get verified?", "Is it safe to pay a deposit?"))) }
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    LaunchedEffect(msgs.size, busy) { if (msgs.isNotEmpty()) list.animateScrollToItem(msgs.size) }

    fun ask(q: String) {
        val question = q.trim()
        if (question.isEmpty() || busy) return
        val history = msgs.filter { it.text.isNotEmpty() }.map { (if (it.mine) "user" else "assistant") to it.text }
        msgs.add(AiMsg(true, question)); text = ""; busy = true
        scope.launch {
            online { repo.guide(question, history) }
                .onSuccess { j ->
                    val reply = j.str("text")
                    if (reply.isEmpty()) msgs.add(AiMsg(false, j.str("message").ifEmpty { "I could not answer that. Please try again, or contact support." }))
                    else msgs.add(AiMsg(false, reply, j.optJSONArray("actions").objects().map { it.str("label") to it.str("url") }, j.optJSONArray("suggest").strings().take(3)))
                }
                .onFailure { msgs.add(AiMsg(false, friendly(it))) }
            busy = false
        }
    }
    fun go(label: String, url: String) {
        val m = Regex("^/property/([^/?#]+)").find(url)
        if (url.startsWith("/")) { if (m != null) { val id = m.groupValues[1].toIntOrNull(); if (id != null) openListing(id) else openWeb(label, url) } else openWeb(label, url) }
    }

    Column(Modifier.fillMaxSize().background(Brand.Bg).imePadding()) {
        TopBar("Landlordspaces AI", onBack)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(msgs) { m ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (m.mine) Alignment.End else Alignment.Start) {
                    Surface(shape = RoundedCornerShape(18.dp), color = if (m.mine) Brand.Primary else Color.White, shadowElevation = if (m.mine) 0.dp else 1.dp, modifier = Modifier.widthIn(max = 320.dp)) {
                        Text(m.text.replace("**", ""), color = if (m.mine) Color.White else Brand.Ink, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                    }
                    m.actions.forEach { (label, url) ->
                        Surface(shape = RoundedCornerShape(14.dp), color = Brand.MintSoft, modifier = Modifier.padding(top = 6.dp).clickable { go(label, url) }) {
                            Text(label, color = Brand.PrimaryDark, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                        }
                    }
                    if (m === msgs.last()) m.suggest.forEach { q ->
                        Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Brand.Line), modifier = Modifier.padding(top = 6.dp).clickable { ask(q) }) {
                            Text(q, color = Brand.Primary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                        }
                    }
                }
            }
            if (busy) item { Text("Thinking…", color = Brand.Muted, fontSize = 13.sp) }
        }
        Surface(shadowElevation = 8.dp, color = Color.White) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    LsField(text, { text = it }, "Ask about property or the app", keyboard = KeyboardOptions(imeAction = ImeAction.Send))
                }
                IconButton({ ask(text) }, enabled = text.isNotBlank() && !busy) { Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = if (text.isNotBlank() && !busy) Brand.Primary else Brand.Muted) }
            }
        }
    }
}
