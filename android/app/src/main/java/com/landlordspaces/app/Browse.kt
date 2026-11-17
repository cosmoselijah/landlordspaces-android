package com.landlordspaces.app

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

private val SORTS = listOf("newest" to "Newest", "relevance" to "Recommended", "price_asc" to "Price: low to high", "price_desc" to "Price: high to low", "views" to "Most viewed")
private val SEGMENTS = listOf("all" to "All", "rent" to "For Rent", "sell" to "For Sale", "commercial" to "Commercial", "land" to "Land", "shortlet" to "Short Let", "new" to "New Homes")
private val TYPES = listOf("House", "Apartment", "Room", "Commercial", "Land", "Estate")
private val FURNISH = listOf("Furnished", "Semi-furnished", "Unfurnished")
private val REASONS = listOf("Wrong or misleading information", "Already rented or sold", "Suspected scam", "Duplicate listing", "Inappropriate content")

/** Browse Properties: search, quick segments, filters, saved searches and full-width property cards. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    repo: Repo, startMarket: String, startCat: String, startQuery: String,
    openListing: (Int) -> Unit, openWeb: (String, String) -> Unit, openChat: (Int) -> Unit = {}, openNotifications: () -> Unit = {},
) {
    StatusBar(darkBackground = true)
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var segment by remember(startMarket, startCat, startQuery) { mutableStateOf(if (startCat == "commercial") "commercial" else if (startCat == "land") "land" else startMarket.ifEmpty { "all" }) }
    var query by remember(startMarket, startCat, startQuery) { mutableStateOf(startQuery) }
    var applied by remember(startMarket, startCat, startQuery) { mutableStateOf(startQuery) }
    var sort by remember { mutableStateOf("newest") }
    var city by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var beds by remember { mutableStateOf(0) }
    var baths by remember { mutableStateOf(0) }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var furnishing by remember { mutableStateOf("") }
    var verifiedOnly by remember { mutableStateOf(false) }
    var recentOnly by remember { mutableStateOf(false) }
    var cities by remember { mutableStateOf<List<String>>(emptyList()) }
    var results by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var more by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var sortMenu by remember { mutableStateOf(false) }
    var cityMenu by remember { mutableStateOf(false) }
    var filters by remember { mutableStateOf(false) }
    var grid by remember { mutableStateOf(false) }
    var near by remember { mutableStateOf("") }
    var nearNote by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var recents by remember { mutableStateOf(readRecents(ctx)) }
    var reporting by remember { mutableStateOf<Listing?>(null) }
    var contacting by remember { mutableStateOf<Listing?>(null) }
    var requestId by remember { mutableStateOf(0) }

    fun locate() {
        val lm = ctx.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        var best: android.location.Location? = null
        try {
            for (p in lm.getProviders(true)) {
                val l = lm.getLastKnownLocation(p) ?: continue
                if (best == null || l.time > best!!.time) best = l
            }
        } catch (e: SecurityException) { }
        val b = best
        if (b == null) nearNote = "Turn on location on your phone, then try again."
        else { nearNote = ""; near = b.latitude.toString() + "," + b.longitude.toString() }
    }
    val askLocation = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { g ->
        if (g.values.any { it }) locate() else nearNote = "Location permission is needed to show properties near you."
    }
    val listState = rememberLazyListState()

    fun filterCount(): Int = listOf(type.isNotEmpty(), beds > 0, baths > 0, minPrice.isNotBlank(), maxPrice.isNotBlank(), furnishing.isNotEmpty(), verifiedOnly, recentOnly).count { it }

    fun extra(): String {
        val b = StringBuilder()
        if (near.isNotEmpty()) b.append("&near=").append(near)
        if (segment == "shortlet") b.append("&shortlet=1")
        if (segment == "new") b.append("&condition=new")
        if (type.isNotEmpty()) b.append("&type=").append(java.net.URLEncoder.encode(type, "UTF-8"))
        if (beds > 0) b.append("&bedrooms=").append(beds)
        if (baths > 0) b.append("&bathrooms=").append(baths)
        if (minPrice.isNotBlank()) b.append("&min_price=").append(minPrice.filter { it.isDigit() })
        if (maxPrice.isNotBlank()) b.append("&max_price=").append(maxPrice.filter { it.isDigit() })
        if (furnishing.isNotEmpty()) b.append("&furnishing=").append(java.net.URLEncoder.encode(furnishing, "UTF-8"))
        if (verifiedOnly) b.append("&verified=1")
        if (recentOnly) b.append("&new_days=30")
        return b.toString()
    }
    fun fetch(p: Int, pull: Boolean = false) {
        requestId += 1
        val mine = requestId
        scope.launch {
            if (p == 1) { if (pull) refreshing = true else loading = true; error = "" } else more = true
            val market = if (segment == "rent" || segment == "sell") segment else ""
            val cat = if (segment == "commercial" || segment == "land") segment else ""
            val r = online { repo.listings(applied, market, cat, sort, city, p, extra()) }
            if (mine == requestId) {
                r.onSuccess { (rows, j) ->
                    results = (if (p == 1) rows else results + rows).distinctBy { it.id }
                    total = j.optInt("total"); page = p; hasMore = !j.isNull("next") && j.optInt("next", 0) > 0
                }.onFailure { error = if (results.isEmpty() || p == 1) friendly(it) else "" }
                loading = false; more = false; refreshing = false
            }
        }
    }
    LaunchedEffect(segment, sort, city, near, applied, type, beds, baths, minPrice, maxPrice, furnishing, verifiedOnly, recentOnly, startMarket, startCat, startQuery) { fetch(1) }
    LaunchedEffect(Unit) { online { repo.meta() }.onSuccess { cities = it.first } }
    // search as you type, after a short pause
    LaunchedEffect(query) { if (query.trim() != applied.trim()) { delay(450); applied = query.trim() } }
    LaunchedEffect(listState, results.size, hasMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (hasMore && !more && !loading && last >= results.size - 2 && results.isNotEmpty()) fetch(page + 1)
        }
    }
    fun toggle(l: Listing) {
        l.saved = !l.saved; results = results.toList()
        scope.launch { online { repo.toggleSave(l.id) }.onSuccess { l.saved = it; results = results.toList() } }
    }
    fun searchJson(): JSONObject {
        val j = JSONObject()
        if (segment == "rent" || segment == "sell") j.put("market", segment)
        if (applied.isNotBlank()) j.put("q", applied)
        if (city.isNotEmpty()) j.put("city", city)
        if (type.isNotEmpty()) j.put("type", type)
        if (beds > 0) j.put("bedrooms", beds)
        if (baths > 0) j.put("bathrooms", baths)
        if (minPrice.isNotBlank()) j.put("min_price", minPrice)
        if (maxPrice.isNotBlank()) j.put("max_price", maxPrice)
        if (furnishing.isNotEmpty()) j.put("furnishing", furnishing)
        if (verifiedOnly) j.put("verified", 1)
        if (recentOnly) j.put("new_days", 30)
        return j
    }
    fun saveSearch(alerts: Boolean) {
        val seg = if (segment != "all") SEGMENTS.firstOrNull { it.first == segment }?.second else null
        val label = listOfNotNull(seg, city.ifEmpty { null }, applied.ifBlank { null }).joinToString(" · ").ifEmpty { "All properties" }
        scope.launch {
            online { repo.saveSearch(label, searchJson(), alerts) }
                .onSuccess { note = if (alerts) "Alerts are on for “$label”." else "Search saved." }
                .onFailure { note = friendly(it) }
        }
    }
    fun share(l: Listing, copy: Boolean) {
        val url = "https://landlordspaces.com/property/" + (l.slug.ifEmpty { l.id.toString() })
        if (copy) {
            val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("Property link", url)); note = "Link copied."
        } else {
            val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, l.title + "\n" + url)
            ctx.startActivity(Intent.createChooser(i, "Share property"))
        }
    }

    val location = if (city.isEmpty()) "All locations" else city
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = { fetch(1, true) }, modifier = Modifier.fillMaxSize().background(Brand.Bg)) {
        Column(Modifier.fillMaxSize()) {
            // ---- deep emerald header ----
            GreenHeader {
                Column(Modifier.padding(bottom = 28.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        LsLogo(30.dp)
                        Column(Modifier.padding(start = 8.dp).weight(1f)) {
                            Text("LandlordSpaces", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
                            Text("World Smartest Real Estate Marketplace", color = Color.White.copy(alpha = 0.75f), fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.Transparent, border = BorderStroke(1.dp, Brand.Gold), onClick = { openWeb("AI Assistant", "/ai") }) {
                            Text("✦ AI", color = Brand.Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                        IconButton(openNotifications) { Icon(Icons.Outlined.NotificationsNone, "Notifications", tint = Color.White) }
                    }
                    Column(Modifier.padding(horizontal = 16.dp).padding(top = 4.dp)) {
                        Text("Browse Properties", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                        Text("Find your perfect home, land or investment", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                        Box(Modifier.padding(top = 10.dp)) {
                            Surface(shape = RoundedCornerShape(20.dp), color = Color.Transparent, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), onClick = { cityMenu = true }) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.LocationOn, null, Modifier.size(16.dp), tint = Brand.Mint)
                                    Text(location, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 6.dp))
                                    Icon(Icons.Filled.ExpandMore, null, Modifier.size(18.dp), tint = Color.White)
                                }
                            }
                            DropdownMenu(cityMenu, { cityMenu = false }) {
                                DropdownMenuItem({ Text("All locations", fontWeight = if (city.isEmpty()) FontWeight.Bold else FontWeight.Normal) }, { city = ""; cityMenu = false })
                                cities.forEach { c -> DropdownMenuItem({ Text(c, fontWeight = if (city == c) FontWeight.Bold else FontWeight.Normal) }, { city = c; cityMenu = false }) }
                            }
                        }
                    }
                    LazyRow(Modifier.padding(top = 12.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SEGMENTS) { (k, label) -> Pill(label, segment == k, { segment = k }, onDark = true) }
                    }
                }
            }
            // ---- search bar overlapping the header ----
            Surface(
                Modifier.padding(horizontal = 16.dp).offset(y = (-22).dp).fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 6.dp,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, null, Modifier.padding(start = 16.dp).size(22.dp), tint = Brand.Muted)
                    androidx.compose.foundation.text.BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = Brand.Ink),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { applied = query.trim(); if (applied.isNotEmpty()) { saveRecent(ctx, applied); recents = readRecents(ctx) } }),
                        decorationBox = { inner -> if (query.isEmpty()) Text("Search by location, property or keyword", color = Brand.Muted, fontSize = 14.sp, maxLines = 1); inner() },
                    )
                    if (query.isNotEmpty()) IconButton({ query = ""; applied = "" }) { Icon(Icons.Filled.Close, "Clear", Modifier.size(18.dp), tint = Brand.Muted) }
                    Box(Modifier.size(1.dp, 28.dp).background(Brand.Line))
                    IconButton({ filters = true }) { Icon(Icons.Filled.Tune, "Filters", tint = Brand.Primary) }
                }
            }
            Column(Modifier.offset(y = (-14).dp)) {
                if (query.isEmpty() && recents.isNotEmpty()) {
                    LazyRow(Modifier.padding(bottom = 8.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recents) { s -> Pill(s, false, { query = s; applied = s }) }
                    }
                }
                // ---- quick actions ----
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ActionPill("Filters", Icons.Filled.Tune, filterCount() > 0, { filters = true }, dot = filterCount() > 0)
                    ActionPill("Near Me", Icons.Outlined.NearMe, near.isNotEmpty(), {
                        if (near.isNotEmpty()) near = ""
                        else {
                            val ok = androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                                androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (ok) locate() else askLocation.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    })
                    ActionPill("Map", Icons.Outlined.Map, false, { openWeb("Map", "/search?view=map") })
                    Box(Modifier.weight(1f)) {
                        ActionPill("Sort: " + (SORTS.firstOrNull { it.first == sort }?.second?.substringBefore(":") ?: "Newest"), Icons.Filled.ExpandMore, false, { sortMenu = true }, trailing = true)
                        DropdownMenu(sortMenu, { sortMenu = false }) {
                            SORTS.forEach { s -> DropdownMenuItem({ Text(s.second, fontWeight = if (sort == s.first) FontWeight.Bold else FontWeight.Normal) }, { sort = s.first; sortMenu = false }) }
                        }
                    }
                    Surface(onClick = { grid = !grid }, shape = RoundedCornerShape(12.dp), color = Brand.Primary, modifier = Modifier.size(38.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(if (grid) Icons.Filled.FormatListBulleted else Icons.Filled.GridView, if (grid) "List view" else "Grid view", Modifier.size(20.dp), tint = Color.White) }
                    }
                }
                if (nearNote.isNotEmpty()) Text(nearNote, color = Brand.Danger, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (loading) "Searching…" else java.text.NumberFormat.getIntegerInstance().format(total) + (if (total == 1) " property found" else " properties found"), color = Brand.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    SmallOutline("Save Search", Icons.Outlined.BookmarkBorder) { saveSearch(false) }
                    Spacer(Modifier.size(6.dp))
                    SmallOutline("Get Alerts", Icons.Outlined.NotificationsNone) { saveSearch(true) }
                }
                if (note.isNotEmpty()) Text(note, color = Brand.Primary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp))
            }
            Box(Modifier.weight(1f).offset(y = (-14).dp)) {
                Loadable(loading && results.isEmpty(), error, { fetch(1) }) {
                    if (results.isEmpty()) EmptyState("No properties found", "Try a different search or clear your filters.")
                    else if (grid) LazyVerticalGrid(
                        columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        gridItems(results, key = { it.id }) { l -> ListingCard(l, { openListing(l.id) }, { toggle(l) }, Modifier.fillMaxWidth(), compact = true) }
                    }
                    else LazyColumn(state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(results, key = { it.id }) { l ->
                            BrowseCard(l, { openListing(l.id) }, { toggle(l) }, { contacting = l }, { share(l, false) }, { share(l, true) }, { reporting = l })
                        }
                        if (more) item { Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = Brand.Primary, strokeWidth = 2.dp) } }
                    }
                }
            }
        }
    }

    if (filters) {
        var t by remember { mutableStateOf(type) }
        var b by remember { mutableStateOf(beds) }
        var ba by remember { mutableStateOf(baths) }
        var lo by remember { mutableStateOf(minPrice) }
        var hi by remember { mutableStateOf(maxPrice) }
        var f by remember { mutableStateOf(furnishing) }
        var v by remember { mutableStateOf(verifiedOnly) }
        var rc by remember { mutableStateOf(recentOnly) }
        AlertDialog(
            onDismissRequest = { filters = false }, title = { Text("Filters") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Property type", fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { Pill("Any", t.isEmpty(), { t = "" }) }
                        items(TYPES) { x -> Pill(x, t == x, { t = x }) }
                    }
                    Text("Bedrooms", fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(listOf(0, 1, 2, 3, 4, 5)) { n -> Pill(if (n == 0) "Any" else "$n+", b == n, { b = n }) } }
                    Text("Bathrooms", fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(listOf(0, 1, 2, 3, 4)) { n -> Pill(if (n == 0) "Any" else "$n+", ba == n, { ba = n }) } }
                    Text("Price range", fontWeight = FontWeight.SemiBold)
                    LsField(lo, { lo = it.filter { ch -> ch.isDigit() } }, "Minimum", keyboard = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                    LsField(hi, { hi = it.filter { ch -> ch.isDigit() } }, "Maximum", keyboard = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                    Text("Furnishing", fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { Pill("Any", f.isEmpty(), { f = "" }) }
                        items(FURNISH) { x -> Pill(x, f == x, { f = x }) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Verified listings only", Modifier.weight(1f)); Switch(v, { v = it }) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Listed in the last 30 days", Modifier.weight(1f)); Switch(rc, { rc = it }) }
                }
            },
            confirmButton = { TextButton({ type = t; beds = b; baths = ba; minPrice = lo; maxPrice = hi; furnishing = f; verifiedOnly = v; recentOnly = rc; filters = false }) { Text("Apply") } },
            dismissButton = { TextButton({ type = ""; beds = 0; baths = 0; minPrice = ""; maxPrice = ""; furnishing = ""; verifiedOnly = false; recentOnly = false; filters = false }) { Text("Clear all") } },
        )
    }
    reporting?.let { l ->
        AlertDialog(
            onDismissRequest = { reporting = null }, title = { Text("Report this property") },
            text = {
                Column {
                    REASONS.forEach { r ->
                        Text(r, Modifier.fillMaxWidth().clickable {
                            reporting = null
                            scope.launch { online { repo.report(l.id, r) }.onSuccess { note = it }.onFailure { note = friendly(it) } }
                        }.padding(vertical = 12.dp), fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {}, dismissButton = { TextButton({ reporting = null }) { Text("Cancel") } },
        )
    }
    contacting?.let { l ->
        var text by remember(l.id) { mutableStateOf("Hello, I am interested in “" + l.title + "”. Is it still available?") }
        var sending by remember(l.id) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!sending) contacting = null }, title = { Text("Contact the owner") },
            text = { LsField(text, { text = it }, "Message", singleLine = false, minLines = 3) },
            confirmButton = {
                TextButton({
                    sending = true
                    scope.launch {
                        online { repo.enquire(l.id, text) }
                            .onSuccess { contacting = null; if (it > 0) openChat(it) else note = "Message sent." }
                            .onFailure { note = friendly(it); contacting = null }
                        sending = false
                    }
                }, enabled = !sending && text.isNotBlank()) { Text(if (sending) "Sending…" else "Send") }
            },
            dismissButton = { TextButton({ contacting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ActionPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, on: Boolean, onClick: () -> Unit, dot: Boolean = false, trailing: Boolean = false) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = if (on) Brand.MintSoft else Color.White, border = BorderStroke(1.dp, if (on) Brand.Primary else Brand.Line), modifier = Modifier.height(38.dp)) {
        Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!trailing) Icon(icon, null, Modifier.size(16.dp), tint = Brand.Primary)
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Brand.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = if (trailing) 0.dp else 5.dp))
            if (dot) Box(Modifier.size(7.dp).clip(CircleShape).background(Brand.Primary))
            if (trailing) Icon(icon, null, Modifier.size(16.dp), tint = Brand.Ink)
        }
    }
}

@Composable
private fun SmallOutline(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), color = Color.White, border = BorderStroke(1.dp, Brand.Primary)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(14.dp), tint = Brand.Primary)
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Brand.Primary, modifier = Modifier.padding(start = 4.dp), maxLines = 1)
        }
    }
}

/** Full-width result card: photo with badges, price, title, place, specs, ⋮ menu and Contact. */
@Composable
private fun BrowseCard(l: Listing, onOpen: () -> Unit, onSave: () -> Unit, onContact: () -> Unit, onShare: () -> Unit, onCopy: () -> Unit, onReport: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Surface(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 2.dp) {
        Column {
            Box(Modifier.fillMaxWidth().height(200.dp)) {
                if (l.cover.isNotEmpty()) AsyncImage(model = l.cover, contentDescription = l.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Box(Modifier.fillMaxSize().background(Brand.MintSoft))
                Surface(Modifier.padding(10.dp).align(Alignment.TopStart), shape = RoundedCornerShape(8.dp), color = if (l.market == "rent") Brand.Primary else Brand.Violet) {
                    Text(l.badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
                Surface(onClick = onSave, modifier = Modifier.padding(10.dp).align(Alignment.TopEnd).size(36.dp), shape = CircleShape, color = Color.White) {
                    Box(contentAlignment = Alignment.Center) { Icon(if (l.saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Save", Modifier.size(19.dp), tint = if (l.saved) Brand.Danger else Brand.Ink) }
                }
                if (l.verified) Surface(Modifier.padding(10.dp).align(Alignment.BottomStart), shape = RoundedCornerShape(8.dp), color = Color.White) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Verified, null, Modifier.size(14.dp), tint = Brand.Primary)
                        Text("Verified", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Brand.Primary, modifier = Modifier.padding(start = 3.dp))
                    }
                }
                if (l.photos > 1) Surface(Modifier.padding(10.dp).align(Alignment.BottomEnd), shape = RoundedCornerShape(8.dp), color = Color(0x99000000)) {
                    Text("1/" + l.photos, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(l.priceText, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Brand.Ink)
                    if (l.periodText.isNotEmpty()) Text(l.periodText, fontSize = 12.sp, color = Brand.Muted, modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
                }
                Text(l.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                if (l.area.isNotEmpty()) Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, Modifier.size(15.dp), tint = Brand.Muted)
                    Text(l.area, fontSize = 12.sp, color = Brand.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp))
                }
                val specs = listOfNotNull(
                    l.bedrooms?.takeIf { it > 0 }?.let { "$it " + (if (it == 1) "Bedroom" else "Bedrooms") },
                    l.bathrooms?.takeIf { it > 0 }?.let { "$it " + (if (it == 1) "Bathroom" else "Bathrooms") },
                    l.size.takeIf { it > 0 }?.let { "${it.toInt()} ${l.sizeUnit}" },
                    l.parking?.takeIf { it > 0 }?.let { "$it Parking" },
                )
                if (specs.isNotEmpty()) Text(specs.joinToString("  |  "), fontSize = 12.sp, color = Brand.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Surface(onClick = { menu = true }, shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, Brand.Line), modifier = Modifier.size(42.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.MoreVert, "More", Modifier.size(20.dp), tint = Brand.Ink) }
                        }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem({ Text("Share") }, { menu = false; onShare() })
                            DropdownMenuItem({ Text("Copy link") }, { menu = false; onCopy() })
                            DropdownMenuItem({ Text("Report") }, { menu = false; onReport() })
                        }
                    }
                    Spacer(Modifier.size(10.dp))
                    Surface(onClick = onContact, shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.5.dp, Brand.Primary), modifier = Modifier.weight(1f).height(42.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("Contact", color = Brand.Primary, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                }
            }
        }
    }
}

private fun readRecents(c: android.content.Context): List<String> =
    c.getSharedPreferences("ls_browse", android.content.Context.MODE_PRIVATE).getString("recent", "").orEmpty().split("\n").filter { it.isNotBlank() }.take(6)

private fun saveRecent(c: android.content.Context, s: String) {
    val l = (listOf(s) + readRecents(c).filter { !it.equals(s, true) }).take(6)
    c.getSharedPreferences("ls_browse", android.content.Context.MODE_PRIVATE).edit().putString("recent", l.joinToString("\n")).apply()
}
