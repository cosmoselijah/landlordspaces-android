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
    LsLogo(size.dp)
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
