package com.landlordspaces.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.Dashboard
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun TopBar(title: String, onBack: () -> Unit, dark: Boolean = false, action: (@Composable () -> Unit)? = null) {
    val fg = if (dark) Color.White else Brand.Ink
    Row(
        Modifier.fillMaxWidth().background(if (dark) Brand.Forest else Color.White).statusBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = fg) }
        Text(title, color = fg, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
        if (action != null) action()
    }
}

@Composable
fun SavedScreen(repo: Repo, onBack: () -> Unit, openListing: (Int) -> Unit) {
    StatusBar(darkBackground = false)
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    fun load() { scope.launch { loading = true; error = ""; online { repo.saved() }.onSuccess { rows = it }.onFailure { error = friendly(it) }; loading = false } }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().background(Brand.Bg)) {
        TopBar("Saved Properties", onBack)
        Loadable(loading, error, { load() }) {
            if (rows.isEmpty()) EmptyState("Nothing saved yet", "Tap the heart on any property to keep it here.")
            else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(rows, key = { it.id }) { l ->
                    ListingCard(l, { openListing(l.id) }, {
                        scope.launch { online { repo.toggleSave(l.id) }.onSuccess { rows = rows.filter { x -> x.id != l.id } } }
                    }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(repo: Repo, onBack: () -> Unit, openWeb: (String, String) -> Unit) {
    StatusBar(darkBackground = false)
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<Notice>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    fun load() { scope.launch { loading = true; error = ""; online { repo.notifications() }.onSuccess { rows = it }.onFailure { error = friendly(it) }; loading = false } }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().background(Brand.Bg)) {
        TopBar("Notifications", onBack, action = {
            TextButton({ scope.launch { online { repo.markAllRead() }.onSuccess { rows = rows.map { n -> Notice(n.id, n.type, n.title, n.body, n.url, true, n.createdAt) } } } }) { Text("Mark all read", color = Brand.Primary) }
        })
        Loadable(loading, error, { load() }) {
            if (rows.isEmpty()) EmptyState("You're all caught up", "New alerts about enquiries, messages and listings show up here.")
            else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(rows, key = { it.id }) { n ->
                    Surface(
                        onClick = { if (n.url.startsWith("/")) openWeb(n.title, n.url) }, shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp,
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
                            Box(Modifier.padding(top = 6.dp).size(9.dp).clip(CircleShape).background(if (n.read) Color.Transparent else Brand.Primary))
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(n.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (n.body.isNotEmpty()) Text(n.body, color = Brand.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                                Text(shortTime(n.createdAt), color = Brand.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(22.dp), tint = tint) }
            Column(Modifier.padding(start = 10.dp)) {
                Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(label, color = Brand.Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
        Column(Modifier.padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(28.dp), tint = Brand.Primary)
            Text(label, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun WorkspaceScreen(repo: Repo, onBack: () -> Unit, openListing: (Int) -> Unit, openWeb: (String, String) -> Unit) {
    StatusBar(darkBackground = false)
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<JSONObject?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    fun load() { scope.launch { loading = true; error = ""; online { repo.workspace() }.onSuccess { data = it }.onFailure { error = friendly(it) }; loading = false } }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().background(Brand.Bg)) {
        TopBar("My Workspace", onBack)
        Loadable(loading, error, { load() }) {
            val st = data?.optJSONObject("stats") ?: JSONObject()
            val mine = data?.optJSONArray("listings").objects()
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar("", repo.session.name, 52.dp)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Welcome back,", color = Brand.Muted, fontSize = 13.sp)
                        Text(repo.session.name.ifEmpty { "Member" }, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Overview", tab == 0, { tab = 0 })
                    Pill("Properties", tab == 1, { tab = 1 })
                    Pill("Tenants", false, { openWeb("Tenant Management", "/tenant-management") })
                }
                if (tab == 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(Icons.Outlined.Apartment, "${st.optInt("active")}", "Active Listings", Brand.Primary, Modifier.weight(1f))
                        StatTile(Icons.Outlined.Notifications, "${st.optInt("enquiries")}", "Enquiries", Brand.Violet, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(Icons.Outlined.Groups, "${st.optInt("tenants")}", "Tenants", Brand.Primary, Modifier.weight(1f))
                        StatTile(Icons.Outlined.Payments, money(st.optDouble("rent_month", 0.0), "NGN"), "Rent (this month)", Brand.Gold, Modifier.weight(1f))
                    }
                    Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickAction(Icons.Filled.AddCircle, "List a Property", { openWeb("List a Property", "/account/listings/new") }, Modifier.weight(1f))
                        QuickAction(Icons.Outlined.Groups, "Manage Tenants", { openWeb("Tenant Management", "/tenant-management") }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickAction(Icons.Outlined.Savings, "Rent Savings", { openWeb("Rent Savings", "/account/rent-savings") }, Modifier.weight(1f))
                        QuickAction(Icons.Outlined.ViewInAr, "Floor Plan Studio", { openWeb("Floor Plan Studio", "/account/studio") }, Modifier.weight(1f))
                    }
                } else {
                    if (mine.isEmpty()) EmptyState("No listings yet", "List your first property and it will show here.")
                    mine.forEach { p ->
                        Surface(onClick = { if (p.str("status") == "published") openListing(p.optInt("id")) else openWeb("My Listings", "/account/listings") }, shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                coil.compose.AsyncImage(model = img(p.str("cover")), contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Brand.MintSoft))
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(p.str("title"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(money(p.optDouble("price", 0.0), p.str("currency").ifEmpty { "NGN" }), color = Brand.Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${p.optInt("views")} views", color = Brand.Muted, fontSize = 12.sp)
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = if (p.str("status") == "published") Brand.MintSoft else Color(0xFFFFF3D6)) {
                                    Text(p.str("status").replaceFirstChar { c -> c.uppercase() }, fontSize = 11.sp, color = Brand.Ink, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit, badge: Int = 0, danger: Boolean = false) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(22.dp), tint = if (danger) Brand.Danger else Brand.Ink)
        Text(label, color = if (danger) Brand.Danger else Brand.Ink, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f).padding(start = 16.dp))
        if (badge > 0) Box(Modifier.size(22.dp).clip(CircleShape).background(Brand.Danger), contentAlignment = Alignment.Center) { Text("$badge", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        if (!danger) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Brand.Muted)
    }
}

@Composable
fun AccountScreen(
    repo: Repo, openWeb: (String, String) -> Unit, openSaved: () -> Unit, openNotifications: () -> Unit, openWorkspace: () -> Unit, openAi: () -> Unit, onSignedOut: () -> Unit,
) {
    StatusBar(darkBackground = false)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<JSONObject?>(null) }
    var unread by remember { mutableStateOf(0) }
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        online { Pair(repo.me(), repo.home()) }.onSuccess { (u, h) -> user = u; unread = h.optJSONObject("kpi")?.optInt("unread_notifications") ?: 0 }
    }
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Text("Account", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, modifier = Modifier.padding(20.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(img(user?.str("avatar") ?: ""), user?.str("name") ?: repo.session.name, 64.dp)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user?.str("name") ?: repo.session.name, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (user?.optBoolean("verified") == true) Icon(Icons.Filled.Verified, "Verified", Modifier.padding(start = 6.dp).size(18.dp), tint = Brand.Primary)
                }
                Text(user?.str("email") ?: "", color = Brand.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(8.dp))
        MenuRow(Icons.Outlined.Dashboard, "My Workspace", openWorkspace)
        MenuRow(Icons.Outlined.Person, "My Profile", { openWeb("My Profile", "/account/profile") })
        MenuRow(Icons.Outlined.Apartment, "My Listings", { openWeb("My Listings", "/account/listings") })
        MenuRow(Icons.Outlined.FavoriteBorder, "Saved Properties", openSaved)
        MenuRow(Icons.Outlined.ViewInAr, "Floor Plan Studio", { openWeb("Floor Plan Studio", "/account/studio") })
        MenuRow(Icons.Outlined.Groups, "Tenant Management", { openWeb("Tenant Management", "/tenant-management") })
        MenuRow(Icons.Outlined.Savings, "Rent Savings", { openWeb("Rent Savings", "/account/rent-savings") })
        MenuRow(Icons.Outlined.Notifications, "Notifications", openNotifications, badge = unread)
        MenuRow(Icons.Outlined.Security, "Security & Privacy", { openWeb("Security & Privacy", "/account/security") })
        MenuRow(Icons.Outlined.AutoAwesome, "Ask AI", openAi)
        MenuRow(Icons.Outlined.Info, "Help & Support", { openWeb("Help & Support", "/help") })
        MenuRow(Icons.AutoMirrored.Filled.ExitToApp, "Sign Out", { confirm = true }, danger = true)
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Privacy", color = Brand.Muted, fontSize = 12.sp, modifier = Modifier.clickable { openInBrowser(context, "$SITE/p/privacy") })
            Text("Terms", color = Brand.Muted, fontSize = 12.sp, modifier = Modifier.clickable { openInBrowser(context, "$SITE/p/terms") })
            Text("Delete account", color = Brand.Muted, fontSize = 12.sp, modifier = Modifier.clickable { openInBrowser(context, "$SITE/delete-account") })
            Spacer(Modifier.weight(1f))
            Text("v" + BuildConfig.VERSION_NAME, color = Brand.Muted, fontSize = 12.sp)
        }
    }
    if (confirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirm = false }, title = { Text("Sign out?") }, text = { Text("You will need to sign in again to use your account.") },
            confirmButton = { TextButton({ confirm = false; scope.launch { online { repo.session.signOut() }; onSignedOut() } }) { Text("Sign out", color = Brand.Danger) } },
            dismissButton = { TextButton({ confirm = false }) { Text("Cancel") } },
        )
    }
}
