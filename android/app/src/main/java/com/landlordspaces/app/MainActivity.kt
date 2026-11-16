package com.landlordspaces.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val Teal = Color(0xFF0F766E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = Session(this)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Teal, onPrimary = Color.White)) {
                App(session)
            }
        }
    }
}

@Composable
fun App(session: Session) {
    var signedIn by remember { mutableStateOf(session.signedIn()) }
    if (!signedIn) {
        LoginScreen(session) { signedIn = true }
    } else {
        MainScreen(session) { signedIn = false }
    }
}

/** Runs a network call off the main thread and gives back the result or the error message. */
suspend fun <T> online(block: () -> T): Result<T> = withContext(Dispatchers.IO) { runCatching(block) }

fun friendly(e: Throwable): String = when (e) {
    is ApiError -> e.message ?: "Something went wrong. Please try again."
    else -> "No connection. Check your internet and try again."
}

fun openInBrowser(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        // No browser installed: nothing more to do.
    }
}

// ---------------------------------------------------------------- sign in

@Composable
fun LoginScreen(session: Session, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var challenge by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }

    fun finish(json: JSONObject) {
        session.save(json)
        onDone()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("LandlordSpaces", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Teal)
        Text("Find, rent and sell property you can trust.", modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

        if (challenge == null) {
            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text("Email") },
                singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it }, label = { Text("Password") },
                singleLine = true, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text("Enter the 6-digit code from your authenticator app, or a backup code.", modifier = Modifier.padding(bottom = 12.dp))
            OutlinedTextField(
                value = code, onValueChange = { code = it }, label = { Text("Code") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (error.isNotEmpty()) Text(error, color = Color(0xFFB91C1C), modifier = Modifier.padding(top = 12.dp))
        if (info.isNotEmpty()) Text(info, color = Teal, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                error = ""; info = ""; busy = true
                scope.launch {
                    val c = challenge
                    val r = if (c == null) {
                        online {
                            Api.request("POST", "/auth/login", JSONObject()
                                .put("email", email.trim()).put("password", password).put("device", session.deviceJson()))
                        }
                    } else {
                        online { Api.request("POST", "/auth/2fa", JSONObject().put("challenge", c).put("code", code.trim())) }
                    }
                    busy = false
                    r.onSuccess { j ->
                        if (j.optBoolean("two_factor_required", false)) challenge = j.optString("challenge") else finish(j)
                    }.onFailure { error = friendly(it) }
                }
            },
            enabled = !busy && (if (challenge == null) email.isNotBlank() && password.isNotEmpty() else code.isNotBlank()),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (busy) CircularProgressIndicator(modifier = Modifier.height(18.dp).width(18.dp), color = Color.White, strokeWidth = 2.dp)
            else Text(if (challenge == null) "Sign in" else "Verify")
        }

        if (challenge == null) {
            TextButton(
                onClick = {
                    error = ""; info = ""
                    if (email.isBlank()) { error = "Enter your email above first."; return@TextButton }
                    scope.launch {
                        online { Api.request("POST", "/auth/forgot", JSONObject().put("email", email.trim())) }
                            .onSuccess { info = it.optString("message", "If that address has an account, a reset link is on its way.") }
                            .onFailure { error = friendly(it) }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Forgot password?") }
            OutlinedButton(onClick = { openInBrowser(context, "$SITE/register") }, modifier = Modifier.fillMaxWidth()) {
                Text("Create an account")
            }
        } else {
            TextButton(onClick = { challenge = null; code = ""; error = "" }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
    }
}

// ---------------------------------------------------------------- main shell

@Composable
fun MainScreen(session: Session, onSignedOut: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var webPath by remember { mutableStateOf<String?>(null) }

    val path = webPath
    if (path != null) {
        BackHandler { webPath = null }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { webPath = null }) { Text("Close") }
            }
            WebPage(session, path, Modifier.fillMaxSize())
        }
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Filled.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Filled.Search, null) }, label = { Text("Browse") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Filled.Email, null) }, label = { Text("Messages") })
                NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Icon(Icons.Filled.Person, null) }, label = { Text("Account") })
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(session) { webPath = it }
                1 -> WebPage(session, "/search", Modifier.fillMaxSize())
                2 -> WebPage(session, "/messages", Modifier.fillMaxSize())
                else -> AccountScreen(session, onOpen = { webPath = it }, onSignedOut = onSignedOut)
            }
        }
    }
}

// ---------------------------------------------------------------- home

@Composable
fun HomeScreen(session: Session, open: (String) -> Unit) {
    val context = LocalContext.current
    var data by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var update by remember { mutableStateOf<JSONObject?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(reload) {
        loading = true; error = ""
        online { session.call("GET", "/home") }
            .onSuccess { data = it }
            .onFailure { error = friendly(it) }
        loading = false
        online { Api.request("GET", "/config") }.onSuccess { cfg ->
            if (cfg.optInt("latest_version_code", 0) > BuildConfig.VERSION_CODE) update = cfg
        }
    }

    val u = update
    if (u != null) {
        val forced = BuildConfig.VERSION_CODE < u.optInt("min_supported_code", 0)
        AlertDialog(
            onDismissRequest = { if (!forced) update = null },
            title = { Text("Update available") },
            text = { Text("A newer version of the LandlordSpaces app is ready to download.") },
            confirmButton = { TextButton(onClick = { openInBrowser(context, "$SITE/android") }) { Text("Update") } },
            dismissButton = { if (!forced) TextButton(onClick = { update = null }) { Text("Later") } }
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Hello, " + session.name.ifBlank { "there" }, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        val d = data
        if (d == null) {
            if (loading) CircularProgressIndicator()
            if (error.isNotEmpty()) {
                Text(error, color = Color(0xFFB91C1C))
                TextButton(onClick = { reload++ }) { Text("Try again") }
            }
        } else {
            val kpi = d.optJSONObject("kpi") ?: JSONObject()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("Messages", kpi.optInt("unread_messages"), Modifier.weight(1f)) { open("/messages") }
                Stat("Alerts", kpi.optInt("unread_notifications"), Modifier.weight(1f)) { open("/account/notifications") }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("Saved", kpi.optInt("saved"), Modifier.weight(1f)) { open("/account/saved") }
                Stat("Live listings", kpi.optInt("live_listings"), Modifier.weight(1f)) { open("/account/listings") }
            }

            val convs = d.optJSONArray("conversations")
            if (convs != null && convs.length() > 0) {
                Section("Recent conversations")
                for (i in 0 until convs.length()) {
                    val c = convs.getJSONObject(i)
                    Row(
                        Modifier.fillMaxWidth().clickable { open("/messages") }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(c.optString("other_name", ""), fontWeight = FontWeight.SemiBold)
                            Text(c.optString("property_title", ""), fontSize = 12.sp, color = Color.Gray)
                            Text(c.optString("last_body", ""), maxLines = 1)
                        }
                        val n = c.optInt("unread")
                        if (n > 0) Text("$n new", color = Teal, fontWeight = FontWeight.Bold)
                    }
                }
            }

            val notes = d.optJSONArray("recent_notifications")
            if (notes != null && notes.length() > 0) {
                Section("Recent notifications")
                for (i in 0 until notes.length()) {
                    val n = notes.getJSONObject(i)
                    val url = n.optString("url", "")
                    Column(
                        Modifier.fillMaxWidth().clickable { if (url.startsWith("/")) open(url) else open("/account/notifications") }.padding(vertical = 8.dp)
                    ) {
                        Text(n.optString("title", ""), fontWeight = if (n.optInt("read", 1) == 0) FontWeight.Bold else FontWeight.Normal)
                        Text(n.optString("body", ""), fontSize = 13.sp, color = Color.Gray, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
fun Section(title: String) {
    Spacer(Modifier.height(20.dp))
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Teal)
}

@Composable
fun Stat(label: String, value: Int, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp)) {
            Text(value.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Teal)
            Text(label, fontSize = 13.sp)
        }
    }
}

// ---------------------------------------------------------------- account

@Composable
fun AccountScreen(session: Session, onOpen: (String) -> Unit, onSignedOut: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(session.name.ifBlank { "Your account" }, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { onOpen("/account") }, modifier = Modifier.fillMaxWidth()) { Text("My dashboard") }
        OutlinedButton(onClick = { onOpen("/account/listings") }, modifier = Modifier.fillMaxWidth()) { Text("My listings") }
        OutlinedButton(onClick = { onOpen("/account/studio") }, modifier = Modifier.fillMaxWidth()) { Text("Floor Plan Studio") }
        OutlinedButton(onClick = { onOpen("/account/profile") }, modifier = Modifier.fillMaxWidth()) { Text("Profile and security") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { openInBrowser(context, "$SITE/p/privacy") }, modifier = Modifier.fillMaxWidth()) { Text("Privacy policy") }
        OutlinedButton(onClick = { openInBrowser(context, "$SITE/p/terms") }, modifier = Modifier.fillMaxWidth()) { Text("Terms of use") }
        OutlinedButton(onClick = { openInBrowser(context, "$SITE/delete-account") }, modifier = Modifier.fillMaxWidth()) { Text("Delete my account") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                busy = true; note = ""
                scope.launch {
                    online { session.call("POST", "/sessions/revoke-all") }
                        .onSuccess { session.clear(); onSignedOut() }
                        .onFailure { note = friendly(it) }
                    busy = false
                }
            },
            enabled = !busy, modifier = Modifier.fillMaxWidth()
        ) { Text("Sign out of all devices") }
        Button(
            onClick = {
                busy = true
                scope.launch {
                    online { session.signOut() }
                    busy = false
                    onSignedOut()
                }
            },
            enabled = !busy, modifier = Modifier.fillMaxWidth()
        ) { Text("Sign out") }
        if (note.isNotEmpty()) Text(note, color = Color(0xFFB91C1C))
        Text("Version " + BuildConfig.VERSION_NAME, fontSize = 12.sp, color = Color.Gray)
    }
}

// ---------------------------------------------------------------- website pages inside the app

/** Shows a LandlordSpaces web page, already signed in as the same member. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPage(session: Session, path: String, modifier: Modifier) {
    var token by remember(path) { mutableStateOf<String?>(null) }
    var error by remember(path) { mutableStateOf("") }
    var webView by remember(path) { mutableStateOf<WebView?>(null) }
    var canBack by remember(path) { mutableStateOf(false) }

    LaunchedEffect(path) {
        online { session.freshAccess() }
            .onSuccess { token = it }
            .onFailure { error = friendly(it) }
    }

    BackHandler(enabled = canBack) { webView?.goBack() }

    val t = token
    if (t == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            if (error.isEmpty()) CircularProgressIndicator() else Text(error, color = Color(0xFFB91C1C), modifier = Modifier.padding(24.dp))
        }
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                        canBack = view.canGoBack()
                    }
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val host = request.url.host ?: ""
                        return if (host == "landlordspaces.com" || host.endsWith(".landlordspaces.com")) {
                            false
                        } else {
                            openInBrowser(ctx, request.url.toString())
                            true
                        }
                    }
                }
                webView = this
                loadUrl(
                    "$SITE/api/mobile/v1/web-login?next=" + Uri.encode(path),
                    mapOf("Authorization" to "Bearer $t")
                )
            }
        }
    )
}
