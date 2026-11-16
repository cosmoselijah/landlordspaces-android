package com.landlordspaces.app

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
private fun CategoryTile(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(26.dp), tint = Brand.Primary)
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
fun HomeScreen(
    repo: Repo, openListing: (Int) -> Unit, openBrowse: (market: String, cat: String, query: String) -> Unit, openWeb: (String, String) -> Unit,
    openNotifications: () -> Unit, openWorkspace: () -> Unit, openSaved: () -> Unit,
) {
    StatusBar(darkBackground = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(repo.session.name) }
    var unread by remember { mutableStateOf(0) }
    var savedCount by remember { mutableStateOf(0) }
    var featured by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var latest by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var update by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            loading = true; error = ""
            online {
                val h = repo.home()
                val f = repo.listings("", "", "", "relevance", "", 1, "&featured=1").first
                val l = repo.listings("", "", "", "newest", "", 1).first
                Triple(h, f, l)
            }.onSuccess { (h, f, l) ->
                name = h.optJSONObject("user")?.str("name") ?: name
                val k = h.optJSONObject("kpi")
                unread = k?.optInt("unread_notifications") ?: 0
                savedCount = k?.optInt("saved") ?: 0
                featured = if (f.isNotEmpty()) f else l.take(6)
                latest = l
            }.onFailure { error = friendly(it) }
            loading = false
            online { repo.config() }.onSuccess { update = it.optInt("latest_version_code") > BuildConfig.VERSION_CODE }
        }
    }
    LaunchedEffect(Unit) { load() }

    fun toggle(l: Listing) {
        l.saved = !l.saved
        featured = featured.toList(); latest = latest.toList()
        scope.launch { online { repo.toggleSave(l.id) }.onSuccess { l.saved = it; featured = featured.toList(); latest = latest.toList() } }
    }

    Column(Modifier.fillMaxSize().background(Brand.Bg)) {
        GreenHeader {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(openWorkspace) { Icon(Icons.Filled.Menu, "Workspace", tint = Color.White) }
                    LogoMark(30)
                    Text("LandlordSpaces", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp).weight(1f))
                    Box {
                        IconButton(openNotifications) { Icon(Icons.Outlined.Notifications, "Notifications", tint = Color.White) }
                        if (unread > 0) Box(Modifier.padding(top = 8.dp, end = 8.dp).size(10.dp).clip(CircleShape).background(Brand.Danger).align(Alignment.TopEnd))
                    }
                }
            }
        }
        Loadable(loading, error, { load() }) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(Modifier.fillMaxWidth().background(Brand.Forest).padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
                    Text("Hello, ${name.substringBefore(' ').ifEmpty { "there" }} 👋", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("Let's find your next property.", color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))
                    Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = search, onValueChange = { search = it }, singleLine = true, modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = Brand.Ink),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { openBrowse("", "", search) }),
                                decorationBox = { inner -> if (search.isEmpty()) Text("Search location, property or agent…", color = Brand.Muted, fontSize = 14.sp); inner() },
                            )
                            Surface(onClick = { openBrowse("", "", search) }, shape = RoundedCornerShape(12.dp), color = Brand.Primary) {
                                Icon(Icons.Filled.Search, "Search", Modifier.padding(10.dp).size(22.dp), tint = Color.White)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CategoryTile(Icons.Outlined.Home, "Buy", { openBrowse("sell", "", "") }, Modifier.weight(1f))
                    CategoryTile(Icons.Outlined.Key, "Rent", { openBrowse("rent", "", "") }, Modifier.weight(1f))
                    CategoryTile(Icons.Outlined.Apartment, "Commercial", { openBrowse("", "commercial", "") }, Modifier.weight(1f))
                    CategoryTile(Icons.Outlined.Landscape, "Land", { openBrowse("", "land", "") }, Modifier.weight(1f))
                }
                Surface(
                    onClick = { openWeb("Tenant Management", "/tenant-management") }, modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp), color = Color.Transparent,
                ) {
                    Column(Modifier.background(Brand.Cta).padding(20.dp)) {
                        Text("Tenant Management", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Text("Manage tenants, rent, payments and leases with ease.", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                        Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.2f)) {
                            Text("Try Now  →", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                SectionTitle("Featured Properties", "See All", { openBrowse("", "", "") })
                Spacer(Modifier.height(10.dp))
                if (featured.isEmpty()) EmptyState("No listings yet", "New properties will appear here.")
                else LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(featured, key = { it.id }) { l -> ListingCard(l, { openListing(l.id) }, { toggle(l) }, Modifier.width(250.dp), compact = true) }
                }
                Spacer(Modifier.height(20.dp))
                if (savedCount > 0) {
                    Surface(onClick = openSaved, modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("❤  Saved properties", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("$savedCount", color = Brand.Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                SectionTitle("Just listed", "Browse", { openBrowse("", "", "") })
                Spacer(Modifier.height(10.dp))
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    latest.take(4).forEach { l -> ListingCard(l, { openListing(l.id) }, { toggle(l) }, Modifier.fillMaxWidth()) }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (update && !dismissed) {
        AlertDialog(
            onDismissRequest = { dismissed = true }, title = { Text("Update available") }, text = { Text("A newer version of the LandlordSpaces app is ready to download.") },
            confirmButton = { TextButton({ dismissed = true; openInBrowser(context, "$SITE/android") }) { Text("Get it") } },
            dismissButton = { TextButton({ dismissed = true }) { Text("Later") } },
        )
    }
}
