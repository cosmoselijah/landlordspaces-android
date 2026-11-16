package com.landlordspaces.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/** Shows a LandlordSpaces web page, already signed in as the same member. Photo pickers (camera + gallery), location and downloads work inside it. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPage(session: Session, path: String, modifier: Modifier, onTitle: (String) -> Unit = {}) {
    val context = LocalContext.current
    var token by remember(path) { mutableStateOf<String?>(null) }
    var error by remember(path) { mutableStateOf("") }
    var webView by remember(path) { mutableStateOf<WebView?>(null) }
    var canBack by remember(path) { mutableStateOf(false) }
    var progress by remember(path) { mutableStateOf(0) }
    var chooser by remember(path) { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var cameraUri by remember(path) { mutableStateOf<Uri?>(null) }
    var geoCallback by remember(path) { mutableStateOf<Pair<String, GeolocationPermissions.Callback>?>(null) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val cb = chooser
        chooser = null
        if (cb != null) {
            var out: Array<Uri>? = null
            if (r.resultCode == Activity.RESULT_OK) {
                out = WebChromeClient.FileChooserParams.parseResult(r.resultCode, r.data)
                if (out == null) cameraUri?.let { out = arrayOf(it) }
            }
            cb.onReceiveValue(out)
        }
    }
    val locationAsk = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val g = geoCallback
        geoCallback = null
        if (g != null) g.second.invoke(g.first, grants.values.any { it }, false)
    }

    LaunchedEffect(path) {
        online { session.freshAccess() }
            .onSuccess { token = it }
            .onFailure { error = friendly(it) }
    }

    BackHandler(enabled = canBack) { webView?.goBack() }

    val t = token
    if (t == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            if (error.isEmpty()) CircularProgressIndicator(color = Brand.Primary) else Text(error, color = Brand.Danger, modifier = Modifier.padding(24.dp))
        }
        return
    }

    Box(modifier) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setGeolocationEnabled(true)
                // The website recognises this tag and hides its own menus, so only the app's top bar and bottom menu show.
                settings.userAgentString = settings.userAgentString + " LandlordSpacesApp/" + BuildConfig.VERSION_NAME
                setDownloadListener { url, _, _, _, _ -> openInBrowser(ctx, url) }
                webChromeClient = object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView, title: String?) {
                        val t = (title ?: "").replace(Regex("\\s*[|·–—-]\\s*Landlordspaces.*$", RegexOption.IGNORE_CASE), "").trim()
                        if (t.isNotEmpty() && !t.startsWith("http")) onTitle(t)
                    }
                    override fun onProgressChanged(view: WebView, newProgress: Int) { progress = newProgress }

                    override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                        chooser?.onReceiveValue(null)
                        chooser = callback
                        val extras = ArrayList<Intent>()
                        try {
                            val file = File.createTempFile("photo_", ".jpg", ctx.cacheDir)
                            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", file)
                            cameraUri = uri
                            extras.add(
                                Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                    .putExtra(MediaStore.EXTRA_OUTPUT, uri)
                                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            )
                        } catch (e: Exception) {
                            cameraUri = null
                        }
                        return try {
                            val choose = Intent(Intent.ACTION_CHOOSER)
                                .putExtra(Intent.EXTRA_INTENT, params.createIntent())
                                .putExtra(Intent.EXTRA_TITLE, "Add photos")
                                .putExtra(Intent.EXTRA_INITIAL_INTENTS, extras.toTypedArray())
                            pick.launch(choose)
                            true
                        } catch (e: Exception) {
                            chooser = null
                            callback.onReceiveValue(null)
                            false
                        }
                    }

                    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (fine || coarse) callback.invoke(origin, true, false)
                        else {
                            geoCallback = Pair(origin, callback)
                            locationAsk.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    }
                }
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
    if (progress in 1..99) androidx.compose.material3.LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth(), color = Brand.Primary, trackColor = Color.Transparent)
    }
}
