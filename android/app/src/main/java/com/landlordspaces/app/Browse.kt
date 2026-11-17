package com.landlordspaces.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val SORTS = listOf("relevance" to "Recommended", "newest" to "Newest", "price_asc" to "Price: low to high", "price_desc" to "Price: high to low", "views" to "Most viewed")

@Composable
fun BrowseScreen(repo: Repo, startMarket: String, startCat: String, startQuery: String, openListing: (Int) -> Unit, openWeb: (String, String) -> Unit) {
    StatusBar(darkBackground = true)
    val scope = rememberCoroutineScope()
    var segment by remember(startMarket, startCat, startQuery) { mutableStateOf(if (startCat == "commercial") "commercial" else if (startCat == "land") "land" else startMarket.ifEmpty { "all" }) }
    var query by remember(startMarket, startCat, startQuery) { mutableStateOf(startQuery) }
    var searching by remember(startMarket, startCat, startQuery) { mutableStateOf(startQuery.isNotEmpty()) }
    var sort by remember { mutableStateOf("relevance") }
    var city by remember { mutableStateOf("") }
    var beds by remember { mutableStateOf(0) }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var cities by remember { mutableStateOf<List<String>>(emptyList()) }
    var results by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var more by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var sortMenu by remember { mutableStateOf(false) }
    var filters by remember { mutableStateOf(false) }
    var near by remember { mutableStateOf("") }
    var nearNote by remember { mutableStateOf("") }
    val ctx = androidx.compose.ui.platform.LocalContext.current
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
        if (b == null) { nearNote = "Turn on location on your phone, then try again." }
        else { nearNote = ""; near = b.latitude.toString() + "," + b.longitude.toString() }
    }
    val askLocation = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { g ->
        if (g.values.any { it }) locate() else nearNote = "Location permission is needed to show properties near you."
    }
    val list = rememberLazyListState()

    fun extra(): String {
        val b = StringBuilder()
        if (near.isNotEmpty()) b.append("&near=").append(near)
        if (beds > 0) b.append("&bedrooms=").append(beds)
        if (minPrice.isNotBlank()) b.append("&min_price=").append(minPrice.filter { it.isDigit() })
        if (maxPrice.isNotBlank()) b.append("&max_price=").append(maxPrice.filter { it.isDigit() })
        return b.toString()
    }
    fun fetch(p: Int) {
        scope.launch {
            if (p == 1) { loading = true; error = "" } else more = true
            val market = if (segment == "rent" || segment == "sell") segment else ""
            val cat = if (segment == "commercial" || segment == "land") segment else ""
            online { repo.listings(query, market, cat, sort, city, p, extra()) }
                .onSuccess { (rows, j) ->
                    results = if (p == 1) rows else results + rows
                    total = j.optInt("total"); page = p; hasMore = !j.isNull("next") && j.optInt("next", 0) > 0
                }.onFailure { error = friendly(it) }
            loading = false; more = false
        }
    }
    LaunchedEffect(segment, sort, city, near, startMarket, startCat, startQuery) { fetch(1) }
    LaunchedEffect(Unit) { online { repo.meta() }.onSuccess { cities = it.first } }
    LaunchedEffect(list, results.size, hasMore) {
        snapshotFlow { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (hasMore && !more && !loading && last >= results.size - 2 && results.isNotEmpty()) fetch(page + 1)
        }
    }
    fun toggle(l: Listing) {
        l.saved = !l.saved; results = results.toList()
        scope.launch { online { repo.toggleSave(l.id) }.onSuccess { l.saved = it; results = results.toList() } }
    }

    Column(Modifier.fillMaxSize().background(Brand.Bg)) {
        GreenHeader {
            Column(Modifier.padding(bottom = 14.dp)) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Browse Properties", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    IconButton({ searching = !searching }) { Icon(Icons.Filled.Search, "Search", tint = Color.White) }
                }
                if (searching) {
                    Surface(Modifier.padding(horizontal = 20.dp).padding(bottom = 10.dp).fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = query, onValueChange = { query = it }, singleLine = true, modifier = Modifier.padding(16.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = Brand.Ink),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { fetch(1) }),
                            decorationBox = { inner -> if (query.isEmpty()) Text("Search location, title or LS-123…", color = Brand.Muted, fontSize = 14.sp); inner() },
                        )
                    }
                }
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("all" to "All", "rent" to "For Rent", "sell" to "For Sale", "commercial" to "Commercial", "land" to "Land")) { (k, label) ->
                        Pill(label, segment == k, { segment = k }, onDark = true)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("Filters", city.isNotEmpty() || beds > 0 || minPrice.isNotBlank() || maxPrice.isNotBlank(), { filters = true })
            Pill("Near me", near.isNotEmpty(), {
                if (near.isNotEmpty()) { near = "" }
                else {
                    val ok = androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                        androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (ok) locate() else askLocation.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            })
            Pill("Map", false, { openWeb("Map", "/search?view=map") })
            Box {
                Pill("Sort", false, { sortMenu = true })
                DropdownMenu(sortMenu, { sortMenu = false }) { SORTS.forEach { s -> DropdownMenuItem({ Text(s.second, fontWeight = if (sort == s.first) FontWeight.Bold else FontWeight.Normal) }, { sort = s.first; sortMenu = false }) } }
            }
        }
        if (nearNote.isNotEmpty()) Text(nearNote, color = Brand.Danger, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
        Text(if (loading) "Searching…" else "${java.text.NumberFormat.getIntegerInstance().format(total)} properties found", color = Brand.Muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
        Loadable(loading && results.isEmpty(), error, { fetch(1) }) {
            if (results.isEmpty()) EmptyState("No properties found", "Try a different search or clear your filters.")
            else LazyColumn(state = list, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(results, key = { it.id }) { l -> ListingCard(l, { openListing(l.id) }, { toggle(l) }, Modifier.fillMaxWidth()) }
                if (more) item { Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = Brand.Primary, strokeWidth = 2.dp) } }
            }
        }
    }

    if (filters) {
        var c by remember { mutableStateOf(city) }
        var b by remember { mutableStateOf(beds) }
        var lo by remember { mutableStateOf(minPrice) }
        var hi by remember { mutableStateOf(maxPrice) }
        AlertDialog(
            onDismissRequest = { filters = false }, title = { Text("Filters") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("City", fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { Pill("Any", c.isEmpty(), { c = "" }) }
                        items(cities) { x -> Pill(x, c == x, { c = x }) }
                    }
                    Text("Bedrooms", fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(0, 1, 2, 3, 4, 5)) { n -> Pill(if (n == 0) "Any" else "$n+", b == n, { b = n }) }
                    }
                    Text("Price range", fontWeight = FontWeight.SemiBold)
                    LsField(lo, { lo = it.filter { ch -> ch.isDigit() } }, "Minimum", keyboard = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                    LsField(hi, { hi = it.filter { ch -> ch.isDigit() } }, "Maximum", keyboard = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                }
            },
            confirmButton = { TextButton({ city = c; beds = b; minPrice = lo; maxPrice = hi; filters = false; fetch(1) }) { Text("Apply") } },
            dismissButton = { TextButton({ city = ""; beds = 0; minPrice = ""; maxPrice = ""; filters = false; fetch(1) }) { Text("Clear") } },
        )
    }
}
