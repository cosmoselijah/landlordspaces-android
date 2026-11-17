package com.landlordspaces.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume

/** Where a property is. Coordinates come from the property itself; if it has none, the address is looked up and never guessed. */
class MapTarget(val title: String, val address: String, val area: String, val cover: String, val lat: Double?, val lng: Double?) {
    val pin: Pair<Double, Double>?
        get() {
            val a = lat
            val b = lng
            return if (a != null && b != null && !(a == 0.0 && b == 0.0)) Pair(a, b) else null
        }
}

private class RouteInfo(val meters: Double, val seconds: Double, val via: String, val coords: String)
private class Fix(val point: Pair<Double, Double>?, val error: String)

// ---------- map page (OpenStreetMap tiles, drawn with Leaflet inside a WebView) ----------
private fun mapHtml(lat: Double, lng: Double, interactive: Boolean): String {
    val i = if (interactive) "true" else "false"
    return """
<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<style>
html,body,#m{height:100%;margin:0;background:#e8efe9}
.pin{position:relative;width:26px;height:26px;border-radius:50% 50% 50% 0;background:#d93025;transform:rotate(-45deg);border:2px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,.4)}
.pin:after{content:'';position:absolute;width:8px;height:8px;background:#fff;border-radius:50%;left:7px;top:7px}
.me{width:16px;height:16px;border-radius:50%;background:#1a73e8;border:3px solid #fff;box-shadow:0 0 0 3px rgba(26,115,232,.35)}
.leaflet-control-attribution{font-size:9px}
</style></head><body><div id="m"></div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script>
var P=[$lat,$lng],map=null,route=null,me=null;
function pinIcon(){return L.divIcon({className:'',html:'<div class="pin"></div>',iconSize:[26,26],iconAnchor:[13,26]});}
function init(){
  map=L.map('m',{zoomControl:false,dragging:$i,touchZoom:$i,doubleClickZoom:$i,scrollWheelZoom:false,boxZoom:false,keyboard:false,tap:false}).setView(P,16);
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; OpenStreetMap'}).addTo(map);
  L.marker(P,{icon:pinIcon()}).addTo(map);
  if(window.LS){LS.onReady();}
}
function setRoute(c){
  if(route){map.removeLayer(route);route=null;}
  route=L.polyline(c.map(function(p){return [p[1],p[0]];}),{color:'#0e7c56',weight:6,opacity:0.9}).addTo(map);
  fit();
}
function clearRoute(){if(route){map.removeLayer(route);route=null;}}
function setMe(a,b){
  if(me){map.removeLayer(me);}
  me=L.marker([a,b],{icon:L.divIcon({className:'',html:'<div class="me"></div>',iconSize:[16,16],iconAnchor:[8,8]})}).addTo(map);
}
function fit(){
  var b=L.latLngBounds([P]);
  var more=false;
  if(route){b.extend(route.getBounds());more=true;}
  if(me){b.extend(me.getLatLng());more=true;}
  if(more){map.fitBounds(b,{padding:[60,60]});}else{map.setView(P,16);}
}
function zoom(d){map.setZoom(map.getZoom()+d);}
function center(a,b){map.setView([a,b],16);}
if(typeof L==='undefined'){if(window.LS){LS.onFail();}}else{init();}
</script></body></html>
"""
}

class MapBridge(private val view: WebView, private val onReadyCb: () -> Unit, private val onFailCb: () -> Unit) {
    @JavascriptInterface
    fun onReady() { view.post { onReadyCb() } }

    @JavascriptInterface
    fun onFail() { view.post { onFailCb() } }
}

@Composable
fun LeafletMap(
    lat: Double, lng: Double, interactive: Boolean, modifier: Modifier,
    onWeb: (WebView) -> Unit = {}, onReady: () -> Unit = {}, onFailed: () -> Unit = {},
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(0xFFE8EFE9.toInt())
                // A map inside a scrolling page must keep the finger while it is being moved.
                if (interactive) setOnTouchListener { v, _ -> v.parent?.requestDisallowInterceptTouchEvent(true); false }
                addJavascriptInterface(MapBridge(this, onReady, onFailed), "LS")
                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                        if (request.isForMainFrame) onFailed()
                    }
                }
                onWeb(this)
                loadDataWithBaseURL("https://landlordspaces.com/", mapHtml(lat, lng, interactive), "text/html", "utf-8", null)
            }
        },
    )
}

// ---------- real services: routing (OpenStreetMap routing servers) and address search (Nominatim) ----------
private fun httpGet(url: String): String {
    val c = URL(url).openConnection() as HttpURLConnection
    try {
        c.connectTimeout = 15000
        c.readTimeout = 25000
        c.setRequestProperty("User-Agent", "LandlordSpacesApp/" + BuildConfig.VERSION_NAME + " (support@landlordspaces.com)")
        c.setRequestProperty("Accept", "application/json")
        val status = c.responseCode
        if (status !in 200..299) throw Exception("HTTP $status")
        return c.inputStream.bufferedReader().use { it.readText() }
    } finally {
        c.disconnect()
    }
}

private fun geocode(query: String): Pair<Double, Double>? {
    val a = JSONArray(httpGet("https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + URLEncoder.encode(query, "UTF-8")))
    if (a.length() == 0) return null
    val o = a.getJSONObject(0)
    return Pair(o.getString("lat").toDouble(), o.getString("lon").toDouble())
}

private fun fetchRoute(mode: String, from: Pair<Double, Double>, to: Pair<Double, Double>): RouteInfo {
    val profile = when (mode) { "walk" -> "routed-foot"; "bike" -> "routed-bike"; else -> "routed-car" }
    val url = "https://routing.openstreetmap.de/" + profile + "/route/v1/driving/" + from.second + "," + from.first + ";" + to.second + "," + to.first +
        "?overview=full&geometries=geojson&steps=true"
    val j = JSONObject(httpGet(url))
    if (j.optString("code") != "Ok") throw Exception("No route")
    val r = j.getJSONArray("routes").getJSONObject(0)
    val via = r.optJSONArray("legs")?.optJSONObject(0)?.optString("summary") ?: ""
    return RouteInfo(r.getDouble("distance"), r.getDouble("duration"), via, r.getJSONObject("geometry").getJSONArray("coordinates").toString())
}

private fun fmtDistance(m: Double): String = if (m < 1000) m.toInt().toString() + " m" else String.format(java.util.Locale.US, "%.1f km", m / 1000)

private fun fmtTime(s: Double): String {
    val min = Math.round(s / 60.0).toInt()
    return if (min < 60) "$min min" else (min / 60).toString() + " h " + (min % 60).toString() + " min"
}

private fun hasLocation(ctx: Context): Boolean =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun locate(ctx: Context, done: (Fix) -> Unit) {
    try {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { try { lm.isProviderEnabled(it) } catch (e: Exception) { false } }
        if (providers.isEmpty()) { done(Fix(null, "Location is switched off on this phone. Turn it on, or type a starting address below.")); return }
        var best: Location? = null
        for (p in providers) {
            val l = try { lm.getLastKnownLocation(p) } catch (e: SecurityException) { null }
            if (l != null && (best == null || l.time > best.time)) best = l
        }
        val last = best
        if (last != null && System.currentTimeMillis() - last.time < 120000) { done(Fix(Pair(last.latitude, last.longitude), "")); return }
        LocationManagerCompat.getCurrentLocation(
            lm, providers.last(), null, ContextCompat.getMainExecutor(ctx),
            androidx.core.util.Consumer<Location?> { loc ->
                if (loc != null) done(Fix(Pair(loc.latitude, loc.longitude), ""))
                else if (last != null) done(Fix(Pair(last.latitude, last.longitude), ""))
                else done(Fix(null, "Your location could not be found. Try again outdoors, or type a starting address."))
            },
        )
    } catch (e: SecurityException) {
        done(Fix(null, "Location permission is needed to find where you are."))
    }
}

private fun coordText(p: Pair<Double, Double>): String = p.first.toString() + "," + p.second.toString()

/** Opens Google Maps (or the browser version) with the real destination; mode is drive, walk, bike or transit. */
fun openDirections(ctx: Context, dest: Pair<Double, Double>?, address: String, mode: String, origin: Pair<Double, Double>?, turnByTurn: Boolean) {
    val d = if (dest != null) coordText(dest) else Uri.encode(address)
    if (turnByTurn && mode != "transit") {
        val m = when (mode) { "walk" -> "w"; "bike" -> "b"; else -> "d" }
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$d&mode=$m")).setPackage("com.google.android.apps.maps"))
            return
        } catch (e: Exception) {
            // Google Maps is not installed: the browser version below opens instead.
        }
    }
    val tm = when (mode) { "walk" -> "walking"; "bike" -> "bicycling"; "transit" -> "transit"; else -> "driving" }
    val from = if (origin != null) "&origin=" + coordText(origin) else ""
    openInBrowser(ctx, "https://www.google.com/maps/dir/?api=1&travelmode=$tm&destination=$d$from")
}

fun openInMaps(ctx: Context, dest: Pair<Double, Double>?, address: String, title: String) {
    val geo = if (dest != null) "geo:" + coordText(dest) + "?q=" + coordText(dest) + "(" + Uri.encode(title) + ")" else "geo:0,0?q=" + Uri.encode(address)
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(geo)))
    } catch (e: Exception) {
        openInBrowser(ctx, "https://www.google.com/maps/search/?api=1&query=" + (if (dest != null) coordText(dest) else Uri.encode(address)))
    }
}

@Composable
private fun RoundMapButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Color.White, shadowElevation = 6.dp, modifier = Modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, tint = Brand.Ink, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
private fun ModeChip(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(24.dp), color = if (selected) Brand.Primary else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Brand.Primary else Brand.Line), modifier = modifier.height(48.dp),
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = if (selected) Color.White else Brand.Ink, modifier = Modifier.size(20.dp))
            Text(label, color = if (selected) Color.White else Brand.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp), maxLines = 1)
        }
    }
}

/** Location & Directions: the property on a live map, the route from you, distance and time, and navigation. */
@Composable
fun MapScreen(target: MapTarget, autoRoute: Boolean, onBack: () -> Unit) {
    StatusBar(darkBackground = false)
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var dest by remember { mutableStateOf(target.pin) }
    var finding by remember { mutableStateOf(target.pin == null) }
    var web by remember { mutableStateOf<WebView?>(null) }
    var ready by remember { mutableStateOf(false) }
    var mapFailed by remember { mutableStateOf(false) }
    var mapKey by remember { mutableIntStateOf(0) }
    var mode by remember { mutableStateOf("drive") }
    var origin by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var originLabel by remember { mutableStateOf("") }
    var route by remember { mutableStateOf<RouteInfo?>(null) }
    var routeError by remember { mutableStateOf("") }
    var routing by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var locating by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var rationale by remember { mutableStateOf(false) }
    var startText by remember { mutableStateOf("") }
    val d = dest

    // A property saved without coordinates is found from its address; nothing is invented.
    LaunchedEffect(Unit) {
        if (dest == null) {
            val q = target.address.ifBlank { target.area }
            if (q.isNotBlank()) dest = online { geocode(q) }.getOrNull()
            finding = false
        }
    }

    suspend fun getFix(): Fix = withTimeoutOrNull(25000L) {
        suspendCancellableCoroutine<Fix> { cont -> locate(ctx) { f -> if (cont.isActive) cont.resume(f) } }
    } ?: Fix(null, "Your location could not be found in time. Try again, or type a starting address.")

    fun useMyLocation() {
        if (locating) return
        locating = true
        notice = ""
        scope.launch {
            val f = getFix()
            locating = false
            val p = f.point
            if (p != null) { origin = p; originLabel = "Your location" } else notice = f.error
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.any { it }) useMyLocation()
        else notice = "Location was not allowed. You can still see the property, and type a starting address below."
    }

    fun askLocation() { if (hasLocation(ctx)) useMyLocation() else rationale = true }

    LaunchedEffect(Unit) { if (autoRoute) askLocation() }

    LaunchedEffect(origin, dest, mode, retry) {
        val o = origin
        val t = dest
        route = null
        routeError = ""
        if (o != null && t != null) {
            routing = true
            val m = mode
            val r = online { fetchRoute(m, o, t) }
            routing = false
            r.onSuccess { route = it }.onFailure { routeError = "No route could be loaded for this travel mode. Check your connection, try another mode, or open Google Maps." }
        }
    }
    LaunchedEffect(ready, web, route) {
        val w = web
        if (ready && w != null) {
            val r = route
            if (r != null) w.evaluateJavascript("setRoute(" + r.coords + ")", null) else w.evaluateJavascript("clearRoute()", null)
        }
    }
    LaunchedEffect(ready, web, origin) {
        val w = web
        val o = origin
        if (ready && w != null && o != null) w.evaluateJavascript("setMe(" + o.first + "," + o.second + ");fit();", null)
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to property", tint = Brand.Ink) }
            Text("Location & Directions", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brand.Ink, modifier = Modifier.weight(1f))
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (finding) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Brand.Primary) }
            } else if (d == null) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("We could not find this property on the map.", fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Text(target.address.ifBlank { target.area }, color = Brand.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
                    OutlineButton("Search in Maps", { openInMaps(ctx, null, target.address.ifBlank { target.area }, target.title) }, Modifier.fillMaxWidth())
                }
            } else if (mapFailed) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("The map could not load. Check your internet connection.", textAlign = TextAlign.Center)
                    PrimaryButton("Try again", { mapFailed = false; ready = false; mapKey += 1 }, Modifier.fillMaxWidth().padding(top = 16.dp))
                }
            } else {
                androidx.compose.runtime.key(mapKey) {
                    LeafletMap(d.first, d.second, true, Modifier.fillMaxSize(), onWeb = { web = it }, onReady = { ready = true }, onFailed = { mapFailed = true })
                }
                Surface(
                    shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 6.dp,
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (target.cover.isNotEmpty()) AsyncImage(model = target.cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)))
                        Column(Modifier.weight(1f).padding(start = if (target.cover.isNotEmpty()) 12.dp else 4.dp)) {
                            Text(target.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, color = Brand.Ink)
                            Text(target.address.ifBlank { target.area }, fontSize = 13.sp, color = Brand.Muted, maxLines = 1)
                        }
                    }
                }
                Column(Modifier.align(Alignment.CenterEnd).padding(end = 12.dp, top = 60.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoundMapButton(Icons.Filled.MyLocation, "Recenter on me") {
                        val o = origin
                        if (o == null) askLocation() else web?.evaluateJavascript("center(" + o.first + "," + o.second + ")", null)
                    }
                    RoundMapButton(Icons.Filled.Add, "Zoom in") { web?.evaluateJavascript("zoom(1)", null) }
                    RoundMapButton(Icons.Filled.Remove, "Zoom out") { web?.evaluateJavascript("zoom(-1)", null) }
                    RoundMapButton(Icons.Outlined.ZoomOutMap, "Route overview") { web?.evaluateJavascript("fit()", null) }
                }
            }
        }
        Surface(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = Color.White, shadowElevation = 16.dp) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 430.dp).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 12.dp).navigationBarsPadding().padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Brand.Line))
                Text("Directions", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brand.Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip(Icons.Filled.DirectionsCar, "Drive", mode == "drive", Modifier.weight(1f)) { mode = "drive" }
                    ModeChip(Icons.Filled.DirectionsWalk, "Walk", mode == "walk", Modifier.weight(1f)) { mode = "walk" }
                    ModeChip(Icons.Filled.DirectionsBike, "Cycle", mode == "bike", Modifier.weight(1f)) { mode = "bike" }
                    ModeChip(Icons.Filled.DirectionsTransit, "Transit", false, Modifier.weight(1f)) { openDirections(ctx, dest, target.address.ifBlank { target.area }, "transit", origin, false) }
                }
                val r = route
                if (r != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (mode == "walk") Icons.Filled.DirectionsWalk else if (mode == "bike") Icons.Filled.DirectionsBike else Icons.Filled.DirectionsCar, null, tint = Brand.Primary, modifier = Modifier.size(28.dp))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(fmtTime(r.seconds) + "  (" + fmtDistance(r.meters) + ")", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brand.Ink)
                            Text(
                                (if (r.via.isNotBlank()) "via " + r.via + " · " else "") + "from " + originLabel, fontSize = 13.sp, color = Brand.Muted, maxLines = 1,
                            )
                        }
                        TextButton({ origin = null; route = null; notice = "" }) { Text("Change", color = Brand.Primary) }
                    }
                } else if (routing || locating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = Brand.Primary, strokeWidth = 2.dp)
                        Text(if (locating) "  Finding your location…" else "  Finding the best route…", color = Brand.Muted, modifier = Modifier.padding(start = 8.dp))
                    }
                } else if (routeError.isNotEmpty()) {
                    Text(routeError, color = Brand.Danger, fontSize = 14.sp)
                    TextButton({ retry += 1 }) { Text("Try again", color = Brand.Primary) }
                } else {
                    Text("See the distance and travel time from where you are.", color = Brand.Muted, fontSize = 14.sp)
                    PrimaryButton("Use my location", { askLocation() }, Modifier.fillMaxWidth(), icon = Icons.Filled.MyLocation)
                    LsField(
                        startText, { startText = it }, "Or type a starting address", icon = Icons.Filled.Search,
                        trailing = {
                            TextButton({
                                val q = startText.trim()
                                if (q.length < 3) { notice = "Type a street, area or city to start from."; return@TextButton }
                                notice = ""
                                locating = true
                                scope.launch {
                                    val g = online { geocode(q) }
                                    locating = false
                                    val p = g.getOrNull()
                                    if (p != null) { origin = p; originLabel = q } else notice = if (g.isFailure) "The address search needs an internet connection." else "That address was not found. Try adding the city."
                                }
                            }) { Text("Go", color = Brand.Primary, fontWeight = FontWeight.Bold) }
                        },
                    )
                }
                if (notice.isNotEmpty()) Text(notice, color = Brand.Danger, fontSize = 13.sp)
                PrimaryButton(
                    "Start Navigation", { openDirections(ctx, dest, target.address.ifBlank { target.area }, mode, origin, true) },
                    Modifier.fillMaxWidth(), icon = Icons.Filled.Navigation,
                )
                OutlineButton("Open in Google Maps", { openInMaps(ctx, dest, target.address.ifBlank { target.area }, target.title) }, Modifier.fillMaxWidth())
            }
        }
    }

    if (rationale) {
        AlertDialog(
            onDismissRequest = { rationale = false },
            title = { Text("Use your location?") },
            text = { Text("LandlordSpaces uses your location only to draw the route from where you are to this property. It is not saved or shared.") },
            confirmButton = { TextButton({ rationale = false; permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) { Text("Allow", color = Brand.Primary) } },
            dismissButton = { TextButton({ rationale = false; notice = "No problem. You can type a starting address below instead." }) { Text("Not now") } },
        )
    }
}

