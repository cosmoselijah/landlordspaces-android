package com.landlordspaces.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun LogoMark(size: Int = 84) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size / 4).dp)).background(Brand.Cta),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Filled.Home, null, Modifier.size((size * 0.56f).dp), tint = Color.White) }
}

@Composable
fun Splash(onDone: () -> Unit) {
    StatusBar(darkBackground = true)
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        a.animateTo(1f, tween(700))
        delay(900)
        onDone()
    }
    Box(Modifier.fillMaxSize().background(Brand.Hero), contentAlignment = Alignment.Center) {
        Column(Modifier.alpha(a.value), horizontalAlignment = Alignment.CenterHorizontally) {
            LogoMark(92)
            Text("LandlordSpaces", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 18.dp))
            Text("Buy  •  Rent  •  Invest  •  Manage", color = Brand.Mint, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            Text("Africa's Smart Real Estate Marketplace", color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, modifier = Modifier.padding(top = 28.dp))
        }
    }
}

private class Slide(val title: String, val text: String)

@Composable
fun Onboarding(onFinish: () -> Unit) {
    StatusBar(darkBackground = true)
    val slides = listOf(
        Slide("Find homes you can trust", "Search verified apartments, houses, land and commercial space across Nigeria and beyond, with real photos and clear prices."),
        Slide("Search, list and manage", "Message owners and agents, save favourites, list your own property and manage tenants, all in one place."),
        Slide("Built for how you live", "Track rent, plan your savings, tour floor plans in 3D and get alerts the moment something changes."),
    )
    val pager = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Brand.Hero).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) { LogoMark(34); Text("LandlordSpaces", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp)) }
            Text("Skip", color = Color.White.copy(alpha = 0.8f), modifier = Modifier.clickable { onFinish() }.padding(8.dp))
        }
        HorizontalPager(pager, Modifier.weight(1f)) { i ->
            Column(Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(180.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(120.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) { LogoMark(72) }
                }
                Text(slides[i].title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 36.dp))
                Text(slides[i].text, color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp, modifier = Modifier.padding(top = 12.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.Center) {
            repeat(slides.size) { i ->
                Box(Modifier.padding(4.dp).size(if (pager.currentPage == i) 22.dp else 8.dp, 8.dp).clip(CircleShape).background(if (pager.currentPage == i) Brand.Mint else Color.White.copy(alpha = 0.3f)))
            }
        }
        PrimaryButton(
            if (pager.currentPage == slides.size - 1) "Get started" else "Next",
            { if (pager.currentPage == slides.size - 1) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Spacer(Modifier.height(16.dp))
    }
}

private val TYPES = listOf("buyer" to "Buyer / Renter", "tenant" to "Tenant", "owner" to "Property owner", "landlord" to "Landlord", "agent" to "Agent", "developer" to "Developer", "company" to "Company")
private val COUNTRIES = listOf("Nigeria", "Ghana", "Kenya", "South Africa", "United Kingdom", "United States", "Canada", "United Arab Emirates", "Rwanda", "Uganda", "Tanzania", "Cameroon", "Senegal", "Egypt", "India")

@Composable
fun AuthScreen(session: Session, startRegister: Boolean, onDone: () -> Unit, onBack: () -> Unit) {
    StatusBar(darkBackground = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var register by remember { mutableStateOf(startRegister) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf("buyer") }
    var country by remember { mutableStateOf("Nigeria") }
    var terms by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var challenge by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    var typeMenu by remember { mutableStateOf(false) }
    var countryMenu by remember { mutableStateOf(false) }

    fun finish(json: JSONObject) { session.save(json); onDone() }

    Column(Modifier.fillMaxSize().background(Brand.Deep)) {
        Column(Modifier.fillMaxWidth().background(Brand.Hero).statusBarsPadding().padding(horizontal = 24.dp, vertical = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LogoMark(38)
                Text("LandlordSpaces", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
            }
            Text(if (challenge != null) "Two-step check" else if (register) "Create your account" else "Welcome back", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 18.dp))
            Text(if (register) "Join Africa's smart property marketplace" else "Sign in to your account", color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Brand.Bg)
                .imePadding().verticalScroll(rememberScrollState()).padding(24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (challenge != null) {
                Text("Enter the 6-digit code from your authenticator app, or a backup code.", color = Brand.Muted)
                LsField(code, { code = it }, "Code", icon = Icons.Outlined.Lock, keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
            } else {
                if (register) LsField(name, { name = it }, "Full name", icon = Icons.Outlined.Person)
                LsField(email, { email = it.trim() }, "Email address", icon = Icons.Outlined.Email, keyboard = KeyboardOptions(keyboardType = KeyboardType.Email))
                LsField(
                    password, { password = it }, "Password", icon = Icons.Outlined.Lock, password = !show, keyboard = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailing = { IconButton({ show = !show }) { Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Show password", tint = Brand.Muted) } },
                )
                if (register) {
                    Box {
                        Pill(TYPES.first { it.first == type }.second + "  ▾", false, { typeMenu = true })
                        DropdownMenu(typeMenu, { typeMenu = false }) { TYPES.forEach { t -> DropdownMenuItem({ Text(t.second) }, { type = t.first; typeMenu = false }) } }
                    }
                    Box {
                        Pill("$country  ▾", false, { countryMenu = true })
                        DropdownMenu(countryMenu, { countryMenu = false }) { COUNTRIES.forEach { c -> DropdownMenuItem({ Text(c) }, { country = c; countryMenu = false }) } }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(terms, { terms = it }, colors = CheckboxDefaults.colors(checkedColor = Brand.Primary))
                        Text("I agree to the ", fontSize = 13.sp)
                        Text("Terms of Use", color = Brand.Primary, fontSize = 13.sp, modifier = Modifier.clickable { openInBrowser(context, "$SITE/p/terms") })
                        Text(" and ", fontSize = 13.sp)
                        Text("Privacy Policy", color = Brand.Primary, fontSize = 13.sp, modifier = Modifier.clickable { openInBrowser(context, "$SITE/p/privacy") })
                    }
                } else {
                    Text(
                        "Forgot password?", color = Brand.Primary, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().clickable {
                            error = ""; info = ""
                            if (email.isBlank()) { error = "Enter your email above first."; return@clickable }
                            scope.launch {
                                online { Api.request("POST", "/auth/forgot", JSONObject().put("email", email)) }
                                    .onSuccess { info = it.optString("message", "If that address has an account, a reset link is on its way.") }
                                    .onFailure { error = friendly(it) }
                            }
                        },
                    )
                }
            }
            if (error.isNotEmpty()) Text(error, color = Brand.Danger, fontSize = 14.sp)
            if (info.isNotEmpty()) Text(info, color = Brand.Primary, fontSize = 14.sp)
            PrimaryButton(
                if (challenge != null) "Verify" else if (register) "Create account" else "Sign in",
                {
                    error = ""; info = ""; busy = true
                    scope.launch {
                        val c = challenge
                        val r = when {
                            c != null -> online { Api.request("POST", "/auth/2fa", JSONObject().put("challenge", c).put("code", code.trim())) }
                            register -> online {
                                Api.request("POST", "/auth/register", JSONObject().put("name", name.trim()).put("email", email.trim()).put("password", password)
                                    .put("account_type", type).put("country", country).put("accept_terms", terms).put("device", session.deviceJson()))
                            }
                            else -> online { Api.request("POST", "/auth/login", JSONObject().put("email", email.trim()).put("password", password).put("device", session.deviceJson())) }
                        }
                        busy = false
                        r.onSuccess { j -> if (j.optBoolean("two_factor_required", false)) challenge = j.optString("challenge") else finish(j) }.onFailure { error = friendly(it) }
                    }
                },
                Modifier.fillMaxWidth().padding(top = 4.dp), loading = busy,
                enabled = if (challenge != null) code.isNotBlank() else if (register) name.isNotBlank() && email.isNotBlank() && password.length >= 8 && terms else email.isNotBlank() && password.isNotEmpty(),
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                if (challenge != null) {
                    Text("Back", color = Brand.Primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { challenge = null; code = ""; error = "" })
                } else {
                    Text(if (register) "Already have an account?  " else "Don't have an account?  ", color = Brand.Muted)
                    Text(if (register) "Sign in" else "Sign up", color = Brand.Primary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { register = !register; error = ""; info = "" })
                }
            }
            if (challenge == null) Text("Back to intro", color = Brand.Muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clickable { onBack() }.padding(8.dp))
        }
    }
}
