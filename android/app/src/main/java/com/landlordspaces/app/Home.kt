package com.landlordspaces.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bathtub
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.KingBed
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.net.URLEncoder

private class Chip(val label: String, val extra: String)

private val CHIPS = listOf(
    Chip("All", "&featured=1"),
    Chip("For Sale", "&market=sell"),
    Chip("For Rent", "&market=rent"),
    Chip("New Homes", "&condition=new"),
    Chip("Verified", "&verified=1"),
    Chip("Under Construction", "&type=Development"),
)

private class Cat(val icon: ImageVector, val label: String, val go: (openBrowse: (String, String, String) -> Unit, openWeb: (String, String) -> Unit) -> Unit)

private val CATS = listOf(
    Cat(Icons.Outlined.Home, "Buy") { b, _ -> b("sell", "", "") },
    Cat(Icons.Outlined.Key, "Rent") { b, _ -> b("rent", "", "") },
    Cat(Icons.Outlined.Apartment, "Commercial") { b, _ -> b("", "commercial", "") },
    Cat(Icons.Outlined.Landscape, "Land") { b, _ -> b("", "land", "") },
    Cat(Icons.Outlined.CalendarMonth, "Short Let") { _, w -> w("Short Let", "/search?shortlet=1") },
    Cat(Icons.Outlined.MonetizationOn, "Invest") { _, w -> w("Invest", "/search?type=Development&type=Estate") },
    Cat(Icons.Outlined.Groups, "Agents") { _, w -> w("Agents", "/agents") },
    Cat(Icons.Outlined.GridView, "More") { b, _ -> b("", "", "") },
)

private val CITIES = listOf("Lagos", "Abuja", "London", "Dubai", "New York")

private class Tool(val icon: ImageVector, val label: String, val title: String, val path: String)

private val TOOLS = listOf(
    Tool(Icons.Outlined.Calculate, "Mortgage\nCalculator", "Mortgage Calculator", "/tools/mortgage-calculator"),
    Tool(Icons.Outlined.AccountBalanceWallet, "Rent Savings\n& Loans", "Rent Savings & Loans", "/rent-savings"),
    Tool(Icons.Outlined.Architecture, "Floor Plan\nStudio", "Floor Plan Studio", "/floor-plans"),
    Tool(Icons.Outlined.BarChart, "Property\nValuation", "Property Valuation", "/tools/valuation"),
    Tool(Icons.Outlined.TrendingUp, "Market\nInsights", "Market Insights", "/tools/investment-insights"),
    Tool(Icons.Outlined.Layers, "Listing\nPerformance", "Listing Performance", "/account/performance"),
)

@Composable
fun LsLogo(size: Dp) {
    Image(painterResource(R.drawable.ic_logo), "LandlordSpaces", Modifier.size(size))
}

@Composable
private fun CategoryTile(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.width(80.dp).height(80.dp), shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 2.dp) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, Modifier.size(26.dp), tint = Brand.Primary)
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Brand.Ink, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun Heading(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Brand.Ink, modifier = Modifier.weight(1f))
        if (action.isNotEmpty()) {
            Row(Modifier.clickable { onAction() }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(action, color = Brand.Primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Icon(Icons.Filled.ArrowForward, null, Modifier.padding(start = 4.dp).size(14.dp), tint = Brand.Primary)
            }
        }
    }
}

@Composable
private fun SpecItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(14.dp), tint = Brand.Muted)
        Text(text, fontSize = 10.sp, color = Brand.Ink, maxLines = 1, modifier = Modifier.padding(start = 3.dp))
    }
}

@Composable
private fun PropCard(l: Listing, onClick: () -> Unit, onSave: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 3.dp) {
        Column {
            Box(Modifier.fillMaxWidth().height(118.dp).background(Brand.Line)) {
                if (l.cover.isNotEmpty() && !l.cover.endsWith(".svg")) AsyncImage(l.cover, l.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Surface(Modifier.align(Alignment.TopStart).padding(8.dp), shape = RoundedCornerShape(8.dp), color = if (l.market == "rent") Brand.Primary else Brand.Violet) {
                    Text(l.badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Surface(onClick = onSave, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.95f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (l.saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, if (l.saved) "Remove from saved" else "Save", Modifier.size(18.dp), tint = if (l.saved) Brand.Danger else Brand.Ink)
                    }
                }
                if (l.verified) Surface(Modifier.align(Alignment.BottomStart).padding(8.dp), shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.95f)) {
                    Row(Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Verified, null, Modifier.size(14.dp), tint = Brand.Primary)
                        Text(" Verified", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Brand.Ink)
                    }
                }
                if (l.photos > 0) Surface(Modifier.align(Alignment.BottomEnd).padding(8.dp), shape = RoundedCornerShape(8.dp), color = Color(0x99000000)) {
                    Row(Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(12.dp), tint = Color.White)
                        Text(" 1/${l.photos}", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
            Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(l.priceText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Brand.Ink, maxLines = 1)
                    if (l.periodText.isNotEmpty()) Text(" " + l.periodText, fontSize = 11.sp, color = Brand.Muted, modifier = Modifier.padding(bottom = 1.dp))
                }
                Text(l.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Brand.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, Modifier.size(14.dp), tint = Brand.Muted)
                    Text(l.area.ifEmpty { "Location on request" }, fontSize = 11.sp, color = Brand.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 3.dp))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Brand.Line))
                Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (l.bedrooms != null) SpecItem(Icons.Outlined.KingBed, "${l.bedrooms} Beds")
                    if (l.bathrooms != null) SpecItem(Icons.Outlined.Bathtub, "${l.bathrooms} Baths")
                    if (l.size > 0) SpecItem(Icons.Outlined.SquareFoot, "${l.size.toInt()} " + (if (l.sizeUnit == "sqm") "m²" else l.sizeUnit))
                }
            }
        }
    }
}

@Composable
private fun CityCard(name: String, cover: String, count: Int, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.width(116.dp).height(104.dp), shape = RoundedCornerShape(12.dp), color = Brand.Forest) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Brand.Primary, Brand.Deep)))) {
            if (cover.isNotEmpty() && !cover.endsWith(".svg")) AsyncImage(cover, name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
            Column(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                if (count > 0) Text("$count ${if (count == 1) "Property" else "Properties"}", color = Color.White.copy(alpha = 0.9f), fontSize = 9.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ToolCard(t: Tool, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(86.dp), shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 2.dp) {
        Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(t.icon, null, Modifier.size(24.dp), tint = Brand.Primary)
            Text(t.label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Brand.Ink, textAlign = TextAlign.Center, lineHeight = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
fun HomeScreen(
    repo: Repo, openListing: (Int) -> Unit, openBrowse: (market: String, cat: String, query: String) -> Unit, openWeb: (String, String) -> Unit,
    openNotifications: () -> Unit, openWorkspace: () -> Unit, openSaved: () -> Unit, openAi: () -> Unit, openAccount: () -> Unit = {},
) {
    StatusBar(darkBackground = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(repo.session.name) }
    var avatar by remember { mutableStateOf("") }
    var unread by remember { mutableStateOf(0) }
    var cards by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var chip by remember { mutableStateOf(0) }
    var cardsLoading by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var update by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    val cityCover = remember { mutableStateMapOf<String, String>() }
    val cityCount = remember { mutableStateMapOf<String, Int>() }

    suspend fun loadCards(i: Int) {
        cardsLoading = true
        online {
            var r = repo.listings("", "", "", if (i == 0) "relevance" else "newest", "", 1, CHIPS[i].extra).first
            if (r.isEmpty() && i == 0) r = repo.listings("", "", "", "newest", "", 1).first
            r
        }.onSuccess { cards = it.take(6); error = "" }.onFailure { if (cards.isEmpty()) error = friendly(it) }
        cardsLoading = false
    }

    fun load() {
        scope.launch {
            loading = true; error = ""
            online { repo.home() }.onSuccess { h ->
                val u = h.optJSONObject("user")
                name = u?.str("name") ?: name
                avatar = img(u?.str("avatar") ?: "")
                unread = h.optJSONObject("kpi")?.optInt("unread_notifications") ?: 0
            }
            loadCards(chip)
            loading = false
            online { repo.config() }.onSuccess { update = it.optInt("latest_version_code") > BuildConfig.VERSION_CODE }
        }
    }
    LaunchedEffect(Unit) { load() }
    LaunchedEffect(Unit) {
        CITIES.forEach { c ->
            online { repo.session.open("GET", "/listings?per=1&city=" + URLEncoder.encode(c, "UTF-8")) }.onSuccess { j ->
                cityCount[c] = j.optInt("total")
                val first = j.optJSONArray("listings").objects().firstOrNull()
                if (first != null) cityCover[c] = img(first.str("cover"))
            }
        }
    }

    fun toggle(l: Listing) {
        l.saved = !l.saved
        cards = cards.toList()
        scope.launch { online { repo.toggleSave(l.id) }.onSuccess { l.saved = it; cards = cards.toList() } }
    }

    Column(Modifier.fillMaxSize().background(Brand.Bg)) {
        // ---- A. header ----
        GreenHeader {
            Row(Modifier.padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Menu, "Menu", Modifier.clickable { openWorkspace() }.padding(6.dp).size(24.dp), tint = Color.White)
                LsLogo(38.dp)
                Column(Modifier.padding(start = 8.dp).weight(1f)) {
                    Text("LandlordSpaces", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
                    Text("WORLD SMARTEST REAL ESTATE MARKETPLACE", color = Color.White.copy(alpha = 0.85f), fontSize = 7.sp, lineHeight = 9.sp, maxLines = 2, letterSpacing = 0.2.sp)
                }
                Surface(onClick = openAi, shape = RoundedCornerShape(18.dp), color = Color.Transparent, border = BorderStroke(1.5.dp, Brand.Gold)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AutoAwesome, "Ask AI", Modifier.size(16.dp), tint = Brand.Gold)
                        Text(" AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Box(Modifier.padding(start = 6.dp).clickable { openNotifications() }.padding(6.dp)) {
                    Icon(Icons.Outlined.Notifications, "Notifications", Modifier.size(24.dp), tint = Color.White)
                    if (unread > 0) Box(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp).defaultMinSize(minWidth = 16.dp, minHeight = 16.dp).clip(CircleShape).background(Brand.Danger), contentAlignment = Alignment.Center) {
                        Text(if (unread > 99) "99+" else "$unread", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
                Box(Modifier.padding(start = 4.dp).size(40.dp).clip(CircleShape).background(Brand.Primary).clickable { openAccount() }, contentAlignment = Alignment.Center) {
                    if (avatar.isNotEmpty() && !avatar.endsWith(".svg")) AsyncImage(avatar, "Profile", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Text(name.trim().take(1).uppercase().ifEmpty { "L" }, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        Loadable(loading, error, { load() }) {
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= 600.dp
                val cols = if (maxWidth >= 840.dp) 4 else if (maxWidth >= 600.dp) 3 else 2
                val toolCols = if (wide) 6 else 3
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize(), state = listState) {
                    // ---- hero + search ----
                    item(key = "hero") {
                        Column {
                            Box(Modifier.fillMaxWidth()) {
                                Box(Modifier.fillMaxWidth().height(164.dp).background(Brush.horizontalGradient(listOf(Brand.Deep, Brand.Forest)))) {
                                    Image(painterResource(R.drawable.hero_building), null, Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.66f), contentScale = ContentScale.Crop, alignment = Alignment.CenterEnd)
                                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Brand.Deep, 0.38f to Brand.Deep.copy(alpha = 0.92f), 0.7f to Brand.Forest.copy(alpha = 0.25f), 1f to Color.Transparent)))
                                    Column(Modifier.fillMaxWidth(0.7f).padding(start = 16.dp, top = 10.dp)) {
                                        Text("Discover, Buy, Rent, Invest and Manage Properties Worldwide.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 22.sp)
                                        Text("Homes • Commercial • Land • Investments", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                        Surface(onClick = { openBrowse("", "", "") }, modifier = Modifier.padding(top = 6.dp).height(40.dp), shape = RoundedCornerShape(21.dp), color = Brand.Primary) {
                                            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text("Explore Properties", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Icon(Icons.Filled.ArrowForward, null, Modifier.padding(start = 6.dp).size(16.dp), tint = Color.White)
                                            }
                                        }
                                    }
                                }
                                Surface(Modifier.align(Alignment.BottomCenter).offset(y = 40.dp).padding(horizontal = 16.dp).fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 8.dp) {
                                    Row(Modifier.padding(start = 16.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Search, "Search", Modifier.size(22.dp), tint = Brand.Primary)
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = search, onValueChange = { search = it }, singleLine = true, modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Brand.Ink),
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { openBrowse("", "", search) }),
                                            decorationBox = { inner -> if (search.isEmpty()) Text("Search location, property, agent or developer…", color = Brand.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); inner() },
                                        )
                                        Icon(Icons.Outlined.Tune, "Filters", Modifier.clickable { openBrowse("", "", search) }.padding(6.dp).size(24.dp), tint = Brand.Ink)
                                        Surface(onClick = { openWeb("Map", "/search?view=map") }, modifier = Modifier.padding(start = 4.dp).height(44.dp), shape = RoundedCornerShape(22.dp), color = Brand.Primary) {
                                            Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.LocationOn, null, Modifier.size(18.dp), tint = Color.White)
                                                Text(" Map", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(56.dp))
                        }
                    }
                    // ---- categories: all eight, one scrolling row ----
                    item(key = "cats") {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(CATS, key = { it.label }) { c -> CategoryTile(c.icon, c.label) { c.go(openBrowse, openWeb) } }
                        }
                    }
                    // ---- tenant management banner ----
                    item(key = "tenant") {
                        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).fillMaxWidth().height(140.dp).clip(RoundedCornerShape(18.dp)).background(Brush.horizontalGradient(listOf(Brand.Deep, Brand.Primary)))) {
                            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Home, null, Modifier.padding(start = 12.dp).size(32.dp), tint = Color.White)
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    Text("Tenant Management", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 19.sp)
                                    Text("Manage tenants, rent, payments and leases in one place.", color = Color.White.copy(alpha = 0.92f), fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 3.dp, bottom = 8.dp))
                                    Surface(onClick = { openWeb("Tenant Management", "/tenant-management") }, modifier = Modifier.height(34.dp), shape = RoundedCornerShape(17.dp), color = Color.White) {
                                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("Get Started", color = Brand.Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Icon(Icons.Filled.ArrowForward, null, Modifier.padding(start = 4.dp).size(14.dp), tint = Brand.Ink)
                                        }
                                    }
                                }
                                Surface(Modifier.width(58.dp).height(100.dp), shape = RoundedCornerShape(10.dp), color = Color.White, border = BorderStroke(3.dp, Brand.Ink), shadowElevation = 4.dp) {
                                    Column(Modifier.padding(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text("Tenant Mgmt", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Brand.Ink, maxLines = 1)
                                        repeat(3) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.size(9.dp).clip(CircleShape).background(Brand.Primary))
                                                Box(Modifier.padding(start = 3.dp).fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(Brand.Line))
                                            }
                                        }
                                    }
                                }
                                Box(Modifier.padding(start = 6.dp).width(104.dp).fillMaxHeight()) {
                                    Image(painterResource(R.drawable.tenant_woman), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.CenterEnd)
                                    Box(Modifier.fillMaxHeight().width(26.dp).background(Brush.horizontalGradient(listOf(Brand.Primary, Color.Transparent))))
                                }
                            }
                        }
                    }
                    // ---- featured: heading, six filters ----
                    item(key = "fhead") {
                        Column {
                            Spacer(Modifier.height(18.dp))
                            Heading("Featured Properties", "See All") { openBrowse("", "", "") }
                            Spacer(Modifier.height(10.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(CHIPS, key = { _, c -> c.label }) { i, c ->
                                    val on = chip == i
                                    Surface(onClick = { if (chip != i) { chip = i; scope.launch { loadCards(i) } } }, modifier = Modifier.height(36.dp), shape = RoundedCornerShape(18.dp), color = if (on) Brand.Primary else Color.White, border = if (on) null else BorderStroke(1.dp, Brand.Line)) {
                                        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (c.label == "Verified") Icon(Icons.Filled.CheckCircle, null, Modifier.padding(end = 5.dp).size(15.dp), tint = if (on) Color.White else Brand.Primary)
                                            Text(c.label, color = if (on) Color.White else Brand.Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    // ---- property grid: two columns on phones ----
                    if (cards.isEmpty()) {
                        item(key = "none") { EmptyState(if (cardsLoading) "Loading properties" else "No properties here yet", if (cardsLoading) "One moment." else "Try another filter or browse everything.") }
                    } else {
                        items(cards.chunked(cols), key = { row -> row.first().id }) { row ->
                            Row(Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { l -> PropCard(l, { openListing(l.id) }, { toggle(l) }, Modifier.weight(1f)) }
                                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    // ---- explore by location: six cards ----
                    item(key = "lhead") {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            Heading("Explore by Location", "View Global Market") { openBrowse("", "", "") }
                            Spacer(Modifier.height(10.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(CITIES, key = { it }) { c -> CityCard(c, cityCover[c] ?: "", cityCount[c] ?: 0) { openBrowse("", "", c) } }
                                item(key = "more") {
                                    Surface(onClick = { openBrowse("", "", "") }, modifier = Modifier.width(116.dp).height(104.dp), shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 2.dp) {
                                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Icon(Icons.Outlined.Public, null, Modifier.size(28.dp), tint = Brand.Primary)
                                            Text("More\nLocations", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Brand.Ink, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            Heading("Powerful Real Estate Tools", "") { }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                    // ---- tools: all six, equal widths ----
                    items(TOOLS.chunked(toolCols), key = { row -> row.first().title }) { row ->
                        Row(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { t -> ToolCard(t, Modifier.weight(1f)) { openWeb(t.title, t.path) } }
                            repeat(toolCols - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    item(key = "end") { Spacer(Modifier.height(24.dp)) }
                }
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
