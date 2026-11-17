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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.AddBox
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
import androidx.compose.runtime.MutableState
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : androidx.fragment.app.FragmentActivity() {
    /** A link tapped outside the app (a property page on the website) waits here until the shell can open it. */
    private val pendingLink = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val session = Session(this)
        pendingLink.value = linkPath(intent)
        setContent { LsTheme { App(session, pendingLink) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        linkPath(intent)?.let { pendingLink.value = it }
    }

    private fun linkPath(i: Intent?): String? {
        val u = i?.data ?: return null
        val p = u.path ?: return null
        return if (p.startsWith("/property/")) p else null
    }
}

@Composable
fun App(session: Session, link: MutableState<String?>) {
    val context = LocalContext.current
    // A sign-in that was not marked "Keep me signed in" ends when the app is started again from scratch.
    remember {
        if (session.signedIn() && !session.keep && !Launch.signedIn) session.clear()
        0
    }
    var phase by remember {
        mutableStateOf(if (session.signedIn()) (if (session.bio && !Launch.signedIn && Bio.available(context)) "bio" else "main") else "intro")
    }
    var film by remember { mutableStateOf(!Launch.played && !reducedMotion(context)) }
    // If the server ends this session for good (password changed, signed out elsewhere), return to the sign-in screen.
    LaunchedEffect(phase) {
        while (phase == "main") {
            delay(3000)
            if (!session.signedIn()) { Launch.signedIn = false; Launch.notice = "Your session has ended. Please sign in again."; phase = "auth" }
        }
    }
    // Warm the first screen's data while the opening film plays, so the app is ready the moment it ends.
    LaunchedEffect(Unit) { online { Repo(session).listings("", "", "", "", "", 1) } }
    Box(Modifier.fillMaxSize()) {
        when (phase) {
            "intro" -> Onboarding { phase = "auth" }
            "bio" -> BiometricGate(onUnlocked = { Launch.signedIn = true; phase = "main" }, onPassword = { phase = "auth" })
            "auth" -> AuthScreen(session, startRegister = false, onDone = { Launch.signedIn = true; Launch.notice = ""; phase = "main" }, onBack = { phase = "intro" }, notice = Launch.notice)
            else -> MainShell(session, link) { Drafts.clearAll(context); Launch.signedIn = false; Launch.notice = ""; phase = "auth" }
        }
        if (film) IntroFilm { film = false; Launch.played = true }
    }
    if (!film) Launch.played = true
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
data class ListingScreen(val key: String) : Screen
data class ChatRoute(val id: Int) : Screen
data class WebRoute(val title: String, val path: String) : Screen
data object SavedRoute : Screen
data object NotificationsRoute : Screen
data object WorkspaceRoute : Screen
data object CompareRoute : Screen
data object TenantsRoute : Screen
data object ProfileRoute : Screen
data object SecurityRoute : Screen
data object HelpRoute : Screen
data object AiRoute : Screen
data class MapRoute(val target: MapTarget, val auto: Boolean) : Screen

/** One bottom bar (Home, Browse, List, Messages, Account) over the tab screens; details open on top of it. */
@Composable
fun MainShell(session: Session, link: MutableState<String?>, onSignedOut: () -> Unit) {
    val repo = remember { Repo(session) }
    var tab by remember { mutableIntStateOf(0) }
    val stack = remember { mutableStateListOf<Screen>() }
    var bMarket by remember { mutableStateOf("") }
    var bCat by remember { mutableStateOf("") }
    var bQuery by remember { mutableStateOf("") }
    val compare = remember { mutableStateListOf<Listing>() }
    val context = LocalContext.current
    var unreadMsgs by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            online { repo.home() }.onSuccess { unreadMsgs = it.optJSONObject("kpi")?.optInt("unread_messages") ?: 0 }
            delay(30000)
        }
    }
    // Actions made offline (messages, saved homes) are sent as soon as there is a connection.
    LaunchedEffect(Unit) {
        while (true) {
            if (session.pending() > 0 || Drafts.waiting(context) > 0) online { session.flush(); Drafts.sync(context, session) }
            delay(15000)
        }
    }
    // Keep the listing form's choices on the phone so the form opens with no connection.
    LaunchedEffect(Unit) { online { repo.listingForm() } }
    val askNotify = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        Alerts.setup(context)
        if (android.os.Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            askNotify.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun push(s: Screen) { stack.add(s) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    val openListing: (Int) -> Unit = { push(ListingScreen(it.toString())) }
    // Pages that have a native screen open natively; every other website page opens inside the signed-in web view.
    val openWeb: (String, String) -> Unit = { t, p ->
        when {
            p == "/account/listings" -> { stack.clear(); tab = 2 }
            p == "/tenant-management" && t == "Tenant Management" -> push(TenantsRoute)
            p == "/account/profile" && t == "My Profile" -> push(ProfileRoute)
            p == "/account/security" -> push(SecurityRoute)
            p == "/help" && t == "Help & Support" -> push(HelpRoute)
            else -> push(WebRoute(t, p))
        }
    }
    val toggleCompare: (Listing) -> Unit = { l ->
        val at = compare.indexOfFirst { it.id == l.id }
        if (at >= 0) compare.removeAt(at) else if (compare.size < 3) compare.add(l)
    }
    val pending = link.value
    LaunchedEffect(pending) {
        if (pending != null) {
            val slug = pending.removePrefix("/property/").substringBefore("/").substringBefore("?")
            if (slug.isNotEmpty()) { stack.clear(); push(ListingScreen(slug)) }
            link.value = null
        }
    }

    val openBrowse: (String, String, String) -> Unit = { m, c, q -> bMarket = m; bCat = c; bQuery = q; tab = 1 }

    BackHandler(enabled = stack.isNotEmpty()) { pop() }
    BackHandler(enabled = stack.isEmpty() && tab != 0) { tab = 0 }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Brand.Bg,
        bottomBar = { LsBottomBar(tab, unreadMsgs) { stack.clear(); tab = it } },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            Column(Modifier.fillMaxSize()) {
            if (repo.offline) OfflineBanner(waiting = session.pending() + Drafts.waiting(context))
            Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                0 -> HomeScreen(repo, openListing, openBrowse, openWeb, { push(NotificationsRoute) }, { push(WorkspaceRoute) }, { push(SavedRoute) }, { push(AiRoute) }, { stack.clear(); tab = 4 })
                1 -> BrowseScreen(repo, bMarket, bCat, bQuery, openListing, openWeb, { push(ChatRoute(it)) }, { push(NotificationsRoute) })
                2 -> ListScreen(session, repo, openWeb, { tab = 0 }, { push(NotificationsRoute) }, { push(AiRoute) }, { stack.clear(); tab = 4 }, { push(WorkspaceRoute) })
                3 -> MessagesScreen(repo, { push(ChatRoute(it)) }, openListing, { stack.clear(); tab = 1 }, { push(WorkspaceRoute) }, { push(AiRoute) }, { push(NotificationsRoute) }, { stack.clear(); tab = 4 })
                else -> AccountScreen(repo, openWeb, { push(SavedRoute) }, { push(NotificationsRoute) }, { push(WorkspaceRoute) }, { push(AiRoute) }, { session.signOut(); Alerts.stop(context); onSignedOut() })
            }
            }
            }
            if (compare.size >= 2 && stack.isEmpty()) CompareBar(compare.size, { push(CompareRoute) }, { compare.clear() }, Modifier.align(Alignment.BottomCenter))
        }
    }

    // Screens opened from the tabs sit on top of everything (hiding the bar) and keep the tab underneath as it was.
    stack.forEachIndexed { i, s ->
        Box(Modifier.fillMaxSize().background(Brand.Bg).pointerInput(Unit) { detectTapGestures { } }) {
            when (s) {
                is ListingScreen -> DetailScreen(repo, s.key, { pop() }, openListing, { push(ChatRoute(it)) }, openWeb, compare.any { c -> c.id.toString() == s.key || c.id == (s.key.toIntOrNull() ?: -1) }, toggleCompare, { tg, auto -> push(MapRoute(tg, auto)) })
                is ChatRoute -> ChatScreen(repo, s.id, { pop() }, openListing)
                is WebRoute -> Column(Modifier.fillMaxSize()) {
                    StatusBar(darkBackground = false)
                    var liveTitle by remember(i) { mutableStateOf(s.title) }
                    TopBar(liveTitle, { pop() })
                    WebPage(session, s.path, Modifier.weight(1f).navigationBarsPadding(), onTitle = { liveTitle = it })
                }
                SavedRoute -> SavedScreen(repo, { pop() }, openListing)
                NotificationsRoute -> NotificationsScreen(repo, { pop() }, openWeb)
                WorkspaceRoute -> WorkspaceScreen(repo, { pop() }, openListing, openWeb)
                CompareRoute -> CompareScreen(compare, { pop() }, { l -> compare.removeAll { it.id == l.id } }, openListing)
                TenantsRoute -> TenantsScreen(repo, { pop() }, { t, p -> push(WebRoute(t, p)) })
                ProfileRoute -> ProfileScreen(repo, { pop() }, { t, p -> push(WebRoute(t, p)) })
                SecurityRoute -> SecurityScreen(repo, { pop() }, { t, p -> push(WebRoute(t, p)) }, { Alerts.stop(context); onSignedOut() })
                HelpRoute -> HelpScreen(repo, { pop() }, { t, p -> push(WebRoute(t, p)) }, { push(AiRoute) })
                AiRoute -> AiScreen(repo, { pop() }, openWeb, openListing)
                is MapRoute -> MapScreen(s.target, s.auto, { pop() })
            }
        }
    }
}

