package com.landlordspaces.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

private val TYPES = listOf("buyer" to "Buyer / Renter", "tenant" to "Tenant", "owner" to "Property owner", "landlord" to "Landlord", "agent" to "Agent", "developer" to "Developer", "company" to "Company")
private val COUNTRIES = listOf(
    "Nigeria", "Ghana", "Kenya", "South Africa", "United Kingdom", "United States", "United Arab Emirates", "Canada", "Egypt", "Ethiopia", "Tanzania", "Uganda",
    "Rwanda", "Senegal", "Côte d'Ivoire", "Cameroon", "Zambia", "Zimbabwe", "Botswana", "Namibia", "Morocco", "Benin", "Togo", "Sierra Leone", "Liberia", "Gambia",
    "India", "Pakistan", "Saudi Arabia", "Qatar", "Australia", "Ireland", "Germany", "France", "Spain", "Netherlands",
)

private fun passwordProblem(p: String): String = when {
    p.length < 8 -> "Your password needs at least 8 characters."
    !p.any { it.isLetter() } -> "Your password needs at least one letter."
    !p.any { it.isDigit() } -> "Your password needs at least one number."
    else -> ""
}

private fun validEmail(e: String): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()

/** Emerald brand header with the official logo and the building artwork (shared by Create Account, Sign In and the fingerprint screen). */
@Composable
private fun AuthHeader(onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().background(Brand.Hero)) {
        Box(Modifier.matchParentSize()) {
            Image(
                painterResource(R.drawable.hero_building), null, contentScale = ContentScale.Crop, alpha = 0.5f,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(200.dp),
            )
        }
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color(0xFF063B30), Color(0xFF063B30), Color(0x00063B30)))))
        Column(Modifier.statusBarsPadding().height(168.dp).padding(horizontal = 12.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Row(Modifier.padding(start = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                LsLogo(46.dp)
                Column(Modifier.padding(start = 10.dp)) {
                    Text("LandlordSpaces", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("List • Rent • Buy • Invest • Manage", color = Brand.Gold, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun SelectField(label: String, value: String, icon: ImageVector, options: List<Pair<String, String>>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        Surface(
            onClick = { open = true }, shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Brand.Line),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Brand.Muted, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.Center) {
                    Text(label, fontSize = 11.sp, color = Brand.Muted)
                    Text(value, fontSize = 15.sp, color = Brand.Ink, fontWeight = FontWeight.Medium)
                }
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = Brand.Muted)
            }
        }
        DropdownMenu(open, { open = false }, modifier = Modifier.heightIn(max = 320.dp)) {
            options.forEach { o -> DropdownMenuItem({ Text(o.second) }, { onPick(o.first); open = false }) }
        }
    }
}

@Composable
fun AuthScreen(session: Session, startRegister: Boolean, onDone: () -> Unit, onBack: () -> Unit, notice: String = "") {
    StatusBar(darkBackground = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var register by remember { mutableStateOf(startRegister) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf("buyer") }
    var country by remember { mutableStateOf("Nigeria") }
    var terms by remember { mutableStateOf(false) }
    var keep by remember { mutableStateOf(true) }
    var code by remember { mutableStateOf("") }
    var challenge by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf(notice) }
    var offerBio by remember { mutableStateOf(false) }

    val bioReady = remember(register) { Bio.available(context) && session.bio && session.signedIn() }

    fun finish(json: JSONObject) {
        session.keep = keep
        session.save(json)
        // Offer fingerprint sign-in once, on phones that have one set up.
        if (keep && Bio.available(context) && !session.bio) offerBio = true else onDone()
    }

    fun submit() {
        error = ""; info = ""
        val c = challenge
        if (c == null) {
            if (register) {
                if (name.trim().length < 2) { error = "Please enter your full name."; return }
                if (!validEmail(email.trim())) { error = "Please enter a valid email address."; return }
                val pp = passwordProblem(password)
                if (pp.isNotEmpty()) { error = pp; return }
                if (confirm != password) { error = "The two passwords do not match."; return }
                if (!terms) { error = "Please accept the Terms of Use and Privacy Policy to continue."; return }
            } else {
                if (!validEmail(email.trim())) { error = "Please enter a valid email address."; return }
                if (password.isEmpty()) { error = "Please enter your password."; return }
            }
        }
        if (busy) return
        busy = true
        scope.launch {
            val r = when {
                c != null -> online { Api.request("POST", "/auth/2fa", JSONObject().put("challenge", c).put("code", code.trim())) }
                register -> online {
                    Api.request(
                        "POST", "/auth/register",
                        JSONObject().put("name", name.trim()).put("email", email.trim()).put("password", password).put("account_type", type)
                            .put("country", country).put("accept_terms", true).put("device", session.deviceJson()),
                    )
                }
                else -> online { Api.request("POST", "/auth/login", JSONObject().put("email", email.trim()).put("password", password).put("device", session.deviceJson())) }
            }
            busy = false
            r.onSuccess { j -> if (j.optBoolean("two_factor_required", false)) challenge = j.optString("challenge") else finish(j) }.onFailure { error = friendly(it) }
        }
    }

    fun forgot() {
        error = ""; info = ""
        if (!validEmail(email.trim())) { error = "Enter your email address above, then tap Forgot password."; return }
        scope.launch {
            online { Api.request("POST", "/auth/forgot", JSONObject().put("email", email.trim())) }
                .onSuccess { info = it.optString("message", "If that address has an account, a reset link is on its way to your email.") }
                .onFailure { error = friendly(it) }
        }
    }

    fun fingerprint() {
        error = ""
        Bio.prompt(
            context, "Sign in to LandlordSpaces", "Use your fingerprint", "Use password",
            { Launch.signedIn = true; onDone() },
            { kind, msg -> if (kind == 0 && msg.isNotEmpty()) error = msg },
        )
    }

    Column(Modifier.fillMaxSize().background(Brand.Deep)) {
        AuthHeader { if (challenge != null) { challenge = null; code = ""; error = "" } else onBack() }
        Column(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Brand.Bg)
                .imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (challenge != null) "Two-step check" else if (register) "Create your account" else "Welcome back",
                color = Brand.Ink, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold,
            )
            Text(
                if (challenge != null) "Enter the 6-digit code from your authenticator app, or a backup code." else if (register) "Join the global property marketplace" else "Sign in to your account",
                color = Brand.Muted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp),
            )
            if (challenge != null) {
                LsField(code, { code = it }, "Code", icon = Icons.Outlined.Lock, keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
            } else {
                if (register) LsField(name, { name = it }, "Full name", icon = Icons.Outlined.Person)
                LsField(email, { email = it.trim() }, "Email address", icon = Icons.Outlined.Email, keyboard = KeyboardOptions(keyboardType = KeyboardType.Email))
                LsField(
                    password, { password = it }, "Password", icon = Icons.Outlined.Lock, password = !show, keyboard = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailing = { IconButton({ show = !show }) { Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (show) "Hide password" else "Show password", tint = Brand.Muted) } },
                )
                if (register) {
                    LsField(
                        confirm, { confirm = it }, "Confirm password", icon = Icons.Outlined.Lock, password = !showConfirm, keyboard = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailing = { IconButton({ showConfirm = !showConfirm }) { Icon(if (showConfirm) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (showConfirm) "Hide password" else "Show password", tint = Brand.Muted) } },
                    )
                    SelectField("I am a", TYPES.first { it.first == type }.second, Icons.Outlined.Badge, TYPES) { type = it }
                    SelectField("Country", country, Icons.Outlined.Public, COUNTRIES.map { it to it }) { country = it }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(terms, { terms = it }, colors = CheckboxDefaults.colors(checkedColor = Brand.Primary))
                        Text("I agree to the ", fontSize = 13.sp)
                        Text("Terms of Use", color = Brand.Primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { openInBrowser(context, "$SITE/p/terms") })
                        Text(" and ", fontSize = 13.sp)
                        Text("Privacy Policy", color = Brand.Primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { openInBrowser(context, "$SITE/p/privacy") })
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(keep, { keep = it }, colors = CheckboxDefaults.colors(checkedColor = Brand.Primary))
                        Text("Keep me signed in", fontSize = 13.sp, modifier = Modifier.weight(1f).clickable { keep = !keep })
                        Text("Forgot password?", color = Brand.Primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.clickable { forgot() }.padding(vertical = 8.dp))
                    }
                }
            }
            if (error.isNotEmpty()) Text(error, color = Brand.Danger, fontSize = 14.sp)
            if (info.isNotEmpty()) Text(info, color = Brand.Primary, fontSize = 14.sp)
            PrimaryButton(
                if (challenge != null) "Verify" else if (register) "Create Account" else "Sign In",
                { submit() }, Modifier.fillMaxWidth().padding(top = 4.dp), loading = busy,
            )
            if (!register && challenge == null && bioReady) {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(1.dp).background(Brand.Line))
                    Text("  or  ", color = Brand.Muted, fontSize = 13.sp)
                    Box(Modifier.weight(1f).height(1.dp).background(Brand.Line))
                }
                Surface(onClick = { fingerprint() }, shape = RoundedCornerShape(16.dp), color = Brand.MintSoft, border = BorderStroke(1.dp, Brand.Mint.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth().height(62.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Fingerprint, null, tint = Brand.Primary, modifier = Modifier.size(34.dp))
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Sign in with Fingerprint", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Brand.Ink)
                            Text("Use your device biometrics for faster, secure access", fontSize = 12.sp, color = Brand.Muted)
                        }
                    }
                }
            }
            if (challenge == null) {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center) {
                    Text(if (register) "Already have an account?  " else "Don't have an account?  ", color = Brand.Muted, fontSize = 14.sp)
                    Text(
                        if (register) "Sign In" else "Sign Up", color = Brand.Primary, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.clickable { register = !register; error = ""; info = "" },
                    )
                }
                Text("Back to intro", color = Brand.Muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clickable { onBack() }.padding(8.dp))
            }
        }
    }

    if (offerBio) {
        AlertDialog(
            onDismissRequest = { offerBio = false; Launch.signedIn = true; onDone() },
            title = { Text("Sign in with your fingerprint?") },
            text = { Text("Next time you open LandlordSpaces you can unlock your account with your fingerprint instead of typing your password. Your fingerprint never leaves your phone.") },
            confirmButton = {
                TextButton({
                    offerBio = false
                    Bio.prompt(
                        context, "Turn on fingerprint sign-in", "Touch the fingerprint sensor", "Not now",
                        { session.bio = true; Launch.signedIn = true; onDone() },
                        { _, _ -> Launch.signedIn = true; onDone() },
                    )
                }) { Text("Turn on", color = Brand.Primary) }
            },
            dismissButton = { TextButton({ offerBio = false; Launch.signedIn = true; onDone() }) { Text("Not now") } },
        )
    }
}

/** Screen 3: asks for the fingerprint before opening a saved sign-in. "Use password instead" goes to the normal sign-in. */
@Composable
fun BiometricGate(onUnlocked: () -> Unit, onPassword: () -> Unit) {
    StatusBar(darkBackground = true)
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }

    fun ask() {
        if (running) return
        val problem = Bio.problem(context)
        if (problem.isNotEmpty()) { message = problem; return }
        running = true
        message = ""
        Bio.prompt(
            context, "Verify your identity", "Use your fingerprint to sign in securely to LandlordSpaces.", "Use password instead",
            { running = false; onUnlocked() },
            { kind, msg -> running = false; if (kind == 2) onPassword() else if (kind == 0 && msg.isNotEmpty()) message = msg },
        )
    }
    LaunchedEffect(Unit) { delay(350); ask() }

    Box(Modifier.fillMaxSize().background(Brand.Hero)) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LsLogo(64.dp)
            Text("LandlordSpaces", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text("List • Rent • Buy • Invest • Manage", color = Brand.Gold, fontSize = 11.sp)
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp).navigationBarsPadding(), shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 12.dp,
        ) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onPassword, modifier = Modifier.size(48.dp)) { Icon(Icons.Filled.Close, "Close", tint = Brand.Ink) }
                }
                Icon(Icons.Filled.Fingerprint, null, tint = Brand.Primary, modifier = Modifier.size(44.dp))
                Text("Verify your identity", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Brand.Ink, modifier = Modifier.padding(top = 8.dp))
                Text("Use your fingerprint to sign in securely to LandlordSpaces.", fontSize = 14.sp, color = Brand.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                Box(
                    Modifier.padding(top = 22.dp).size(104.dp).clip(CircleShape).background(Brand.MintSoft).clickable { ask() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Fingerprint, "Scan fingerprint", tint = Brand.Primary, modifier = Modifier.size(72.dp)) }
                Text(
                    if (message.isNotEmpty()) message else "Touch the fingerprint sensor",
                    fontSize = 14.sp, color = if (message.isNotEmpty()) Brand.Danger else Brand.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp),
                )
                Text("Use password instead", color = Brand.Primary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.clickable { onPassword() }.padding(vertical = 16.dp))
            }
        }
    }
}
