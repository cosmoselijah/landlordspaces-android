package com.landlordspaces.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Bathtub
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
private fun CircleAction(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Color.White, shadowElevation = 3.dp, modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun Fact(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(24.dp), tint = Brand.Primary)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
        Text(label, color = Brand.Muted, fontSize = 11.sp)
    }
}

@Composable
private fun Row2(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, color = Brand.Muted, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DetailScreen(repo: Repo, id: Int, onBack: () -> Unit, openListing: (Int) -> Unit, openChat: (Int) -> Unit, openWeb: (String, String) -> Unit) {
    StatusBar(darkBackground = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var d by remember(id) { mutableStateOf<Detail?>(null) }
    var loading by remember(id) { mutableStateOf(true) }
    var error by remember(id) { mutableStateOf("") }
    var tab by remember(id) { mutableStateOf(0) }
    var saved by remember(id) { mutableStateOf(false) }
    var enquiry by remember(id) { mutableStateOf(false) }
    var text by remember(id) { mutableStateOf("Hello, I am interested in this property. Is it still available?") }
    var sending by remember(id) { mutableStateOf(false) }
    var note by remember(id) { mutableStateOf("") }

    fun load() {
        scope.launch {
            loading = true; error = ""
            online { repo.detail(id) }.onSuccess { d = it; saved = it.base.saved }.onFailure { error = friendly(it) }
            loading = false
        }
    }
    LaunchedEffect(id) { load() }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Loadable(loading, error, { load() }) {
            val x = d ?: return@Loadable
            val photos = if (x.photos.isEmpty()) listOf("") else x.photos
            val pager = rememberPagerState(pageCount = { photos.size })
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Box(Modifier.fillMaxWidth().height(300.dp)) {
                        HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
                            if (photos[i].isNotEmpty()) AsyncImage(model = photos[i], contentDescription = x.base.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            else Box(Modifier.fillMaxSize().background(Brand.MintSoft))
                        }
                        Box(Modifier.fillMaxWidth().height(90.dp).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent))))
                        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            CircleAction(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", Modifier.size(20.dp), tint = Brand.Ink) }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircleAction({
                                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, x.base.title + " - " + SITE + x.url)
                                    context.startActivity(Intent.createChooser(send, "Share property"))
                                }) { Icon(Icons.Outlined.Share, "Share", Modifier.size(20.dp), tint = Brand.Ink) }
                                CircleAction({
                                    saved = !saved
                                    scope.launch { online { repo.toggleSave(id) }.onSuccess { saved = it } }
                                }) { Icon(if (saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Save", Modifier.size(20.dp), tint = if (saved) Brand.Danger else Brand.Ink) }
                            }
                        }
                        if (photos.size > 1) Surface(Modifier.align(Alignment.BottomEnd).padding(14.dp), shape = RoundedCornerShape(10.dp), color = Color(0x99000000)) {
                            Text("${pager.currentPage + 1}/${photos.size}", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                                Text(x.base.priceText, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = Brand.Primary)
                                if (x.base.periodText.isNotEmpty()) Text(x.base.periodText, color = Brand.Muted, modifier = Modifier.padding(start = 2.dp, bottom = 4.dp))
                            }
                            Surface(shape = RoundedCornerShape(10.dp), color = if (x.base.market == "rent") Brand.MintSoft else Color(0xFFECE6FB)) {
                                Text(x.base.badge, color = if (x.base.market == "rent") Brand.Primary else Brand.Violet, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                            }
                        }
                        Text(x.base.title, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 8.dp))
                        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, null, Modifier.size(16.dp), tint = Brand.Muted)
                            Text(x.address.ifEmpty { x.base.area }, color = Brand.Muted, fontSize = 14.sp, modifier = Modifier.padding(start = 3.dp))
                            if (x.base.verified) { Spacer(Modifier.width(10.dp)); Icon(Icons.Filled.Verified, "Verified", Modifier.size(16.dp), tint = Brand.Primary); Text(" Verified", color = Brand.Primary, fontSize = 12.sp) }
                        }
                        Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Fact(Icons.Outlined.Bed, (x.base.bedrooms ?: 0).toString(), "Beds", Modifier.weight(1f))
                            Fact(Icons.Outlined.Bathtub, (x.base.bathrooms ?: 0).toString(), "Baths", Modifier.weight(1f))
                            Fact(Icons.Outlined.SquareFoot, if (x.base.size > 0) x.base.size.toInt().toString() else "-", x.base.sizeUnit, Modifier.weight(1f))
                            Fact(Icons.Outlined.DirectionsCar, (x.base.parking ?: 0).toString(), "Parking", Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Overview", "Details", "Floor Plan", "Location").forEachIndexed { i, t -> Pill(t, tab == i, { tab = i }) }
                        }
                        Spacer(Modifier.height(16.dp))
                        when (tab) {
                            0 -> {
                                Text("Property Description", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(x.description.ifEmpty { "No description has been added yet." }, color = Brand.Ink.copy(alpha = 0.8f), modifier = Modifier.padding(top = 6.dp), lineHeight = 22.sp)
                                if (x.amenities.isNotEmpty()) {
                                    Text("Amenities", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(x.amenities) { a -> Surface(shape = RoundedCornerShape(10.dp), color = Brand.MintSoft) { Text(a, fontSize = 13.sp, color = Brand.PrimaryDark, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) } } }
                                }
                            }
                            1 -> Column {
                                Row2("Reference", x.ref)
                                Row2("Type", x.base.type)
                                Row2("Furnishing", x.furnishing)
                                Row2("Condition", x.condition)
                                Row2("Year built", x.yearBuilt?.toString() ?: "")
                                Row2("Toilets", x.toilets?.toString() ?: "")
                                Row2("Listed by", x.base.ownerName)
                                Row2("Views", x.views.toString())
                            }
                            2 -> Column {
                                if (x.hasFloorPlan) {
                                    Text("This property has an interactive floor plan with 2D and 3D views, room sizes and photos.", color = Brand.Muted)
                                    PrimaryButton("Open floor plan", { openWeb("Floor Plan", x.floorPlanUrl) }, Modifier.fillMaxWidth().padding(top = 12.dp), icon = Icons.Outlined.ViewInAr)
                                } else Text("The owner has not added a floor plan to this listing yet.", color = Brand.Muted)
                            }
                            else -> Column {
                                Text(x.address.ifEmpty { x.base.area }, fontWeight = FontWeight.SemiBold)
                                OutlineButton("Open in Maps", {
                                    val q = Uri.encode(x.address.ifEmpty { x.base.area })
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$q"))) } catch (e: Exception) { openInBrowser(context, "https://www.google.com/maps/search/?api=1&query=$q") }
                                }, Modifier.fillMaxWidth().padding(top = 12.dp))
                            }
                        }
                        if (x.similar.isNotEmpty()) {
                            Text("Similar properties", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                                items(x.similar, key = { it.id }) { s -> ListingCard(s, { openListing(s.id) }, {}, Modifier.width(240.dp), compact = true) }
                            }
                        }
                    }
                }
                Surface(shadowElevation = 12.dp, color = Color.White) {
                    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (x.own) PrimaryButton("Manage my listings", { openWeb("My Listings", "/account/listings") }, Modifier.weight(1f))
                        else PrimaryButton("Contact Agent", { enquiry = true }, Modifier.weight(1f))
                        if (x.phone.isNotEmpty()) Surface(
                            onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + x.phone))) }, shape = RoundedCornerShape(26.dp), color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Brand.Primary), modifier = Modifier.size(52.dp),
                        ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Call, "Call", tint = Brand.Primary) } }
                    }
                }
            }
        }
        if (loading || error.isNotEmpty()) Box(Modifier.statusBarsPadding().padding(12.dp)) { CircleAction(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", Modifier.size(20.dp), tint = Brand.Ink) } }
    }

    if (enquiry) {
        AlertDialog(
            onDismissRequest = { if (!sending) enquiry = false }, title = { Text("Message the owner") },
            text = { Column { LsField(text, { text = it }, "Your message", singleLine = false, minLines = 3); if (note.isNotEmpty()) Text(note, color = Brand.Danger, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) } },
            confirmButton = {
                TextButton(enabled = !sending && text.isNotBlank(), onClick = {
                    sending = true; note = ""
                    scope.launch {
                        online { repo.enquire(id, text.trim()) }
                            .onSuccess { cid -> enquiry = false; if (cid > 0) openChat(cid) }
                            .onFailure { note = friendly(it) }
                        sending = false
                    }
                }) { Text(if (sending) "Sending…" else "Send") }
            },
            dismissButton = { TextButton({ enquiry = false }, enabled = !sending) { Text("Cancel") } },
        )
    }
}
