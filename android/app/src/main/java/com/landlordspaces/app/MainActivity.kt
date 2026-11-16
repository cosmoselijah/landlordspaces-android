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
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val session = Session(this)
        setContent { LsTheme { App(session) } }
    }
}

@Composable
fun App(session: Session) {
    var phase by remember { mutableStateOf("splash") }
    when (phase) {
        "splash" -> Splash { phase = if (session.signedIn()) "main" else "intro" }
        "intro" -> Onboarding { phase = "auth" }
        "auth" -> AuthScreen(session, startRegister = false, onDone = { phase = "main" }, onBack = { phase = "intro" })
        else -> MainShell(session) { phase = "auth" }
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

sealed interface Screen
data class ListingScreen(val id: Int) : Screen
data class ChatRoute(val id: Int) : Screen
data class WebRoute(val title: String, val path: String) : Screen
data object SavedRoute : Screen
data object NotificationsRoute : Screen
data object WorkspaceRoute : Screen

/** One bottom bar (Home, Browse, List, Messages, Account) over the tab screens; details open on top of it. */
@Composable
fun MainShell(session: Session, onSignedOut: () -> Unit) {
    val repo = remember { Repo(session) }
    var tab by remember { mutableIntStateOf(0) }
    val stack = remember { mutableStateListOf<Screen>() }
    var bMarket by remember { mutableStateOf("") }
    var bCat by remember { mutableStateOf("") }
    var bQuery by remember { mutableStateOf("") }
    val top = stack.lastOrNull()

    fun push(s: Screen) { stack.add(s) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    val openListing: (Int) -> Unit = { push(ListingScreen(it)) }
    val openWeb: (String, String) -> Unit = { t, p -> push(WebRoute(t, p)) }
    val openBrowse: (String, String, String) -> Unit = { m, c, q -> bMarket = m; bCat = c; bQuery = q; tab = 1 }

    BackHandler(enabled = stack.isNotEmpty()) { pop() }
    BackHandler(enabled = stack.isEmpty() && tab != 0) { tab = 0 }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Brand.Bg,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val colors = NavigationBarItemDefaults.colors(selectedIconColor = Brand.Primary, selectedTextColor = Brand.Primary, indicatorColor = Brand.MintSoft, unselectedIconColor = Brand.Muted, unselectedTextColor = Brand.Muted)
                NavigationBarItem(tab == 0, { stack.clear(); tab = 0 }, { Icon(if (tab == 0) Icons.Filled.Home else Icons.Outlined.Home, "Home") }, label = { Text("Home") }, colors = colors)
                NavigationBarItem(tab == 1, { stack.clear(); tab = 1 }, { Icon(if (tab == 1) Icons.Filled.Search else Icons.Outlined.Search, "Browse") }, label = { Text("Browse") }, colors = colors)
                NavigationBarItem(false, { stack.clear(); push(WebRoute("List a Property", "/account/listings/new")) }, { Icon(Icons.Outlined.AddBox, "List") }, label = { Text("List") }, colors = colors)
                NavigationBarItem(tab == 3, { stack.clear(); tab = 3 }, { Icon(if (tab == 3) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline, "Messages") }, label = { Text("Messages") }, colors = colors)
                NavigationBarItem(tab == 4, { stack.clear(); tab = 4 }, { Icon(if (tab == 4) Icons.Filled.Person else Icons.Outlined.Person, "Account") }, label = { Text("Account") }, colors = colors)
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                0 -> HomeScreen(repo, openListing, openBrowse, openWeb, { push(NotificationsRoute) }, { push(WorkspaceRoute) }, { push(SavedRoute) })
                1 -> BrowseScreen(repo, bMarket, bCat, bQuery, openListing, openWeb)
                3 -> MessagesScreen(repo) { push(ChatRoute(it)) }
                else -> AccountScreen(repo, openWeb, { push(SavedRoute) }, { push(NotificationsRoute) }, { push(WorkspaceRoute) }, { session.clear(); onSignedOut() })
            }
        }
    }

    // Screens opened from the tabs sit on top of everything (hiding the bar) and keep the tab underneath as it was.
    stack.forEachIndexed { i, s ->
        Box(Modifier.fillMaxSize().background(Brand.Bg).pointerInput(Unit) { detectTapGestures { } }) {
            when (s) {
                is ListingScreen -> DetailScreen(repo, s.id, { pop() }, openListing, { push(ChatRoute(it)) }, openWeb)
                is ChatRoute -> ChatScreen(repo, s.id) { pop() }
                is WebRoute -> Column(Modifier.fillMaxSize()) {
                    StatusBar(darkBackground = false)
                    TopBar(s.title, { pop() })
                    WebPage(session, s.path, Modifier.weight(1f).navigationBarsPadding())
                }
                SavedRoute -> SavedScreen(repo, { pop() }, openListing)
                NotificationsRoute -> NotificationsScreen(repo, { pop() }, openWeb)
                WorkspaceRoute -> WorkspaceScreen(repo, { pop() }, openListing, openWeb)
            }
        }
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
