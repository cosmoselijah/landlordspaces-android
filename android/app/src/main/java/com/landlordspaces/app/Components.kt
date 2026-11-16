package com.landlordspaces.app

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Bathtub
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.AsyncImage

@Composable
fun GreenHeader(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth().background(Brand.Header).statusBarsPadding()) { content() }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false, icon: ImageVector? = null) {
    Button(
        onClick = onClick, enabled = enabled && !loading, modifier = modifier.height(52.dp), shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Brand.Primary, contentColor = Color.White, disabledContainerColor = Brand.Primary.copy(alpha = 0.45f), disabledContentColor = Color.White),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        else {
            if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, dark: Boolean = false) {
    val c = if (dark) Color.White else Brand.Primary
    Surface(
        onClick = onClick, modifier = modifier.height(52.dp), shape = RoundedCornerShape(26.dp), color = Color.Transparent,
        border = BorderStroke(1.5.dp, c),
    ) { Box(contentAlignment = Alignment.Center) { Text(text, color = c, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) } }
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val bg = if (selected) Brand.Primary else if (onDark) Color.White.copy(alpha = 0.12f) else Color.White
    val fg = if (selected) Color.White else if (onDark) Color.White else Brand.Ink
    Surface(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp), color = bg,
        border = if (selected || onDark) null else BorderStroke(1.dp, Brand.Line),
    ) { Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
}

@Composable
fun LsField(
    value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, icon: ImageVector? = null,
    password: Boolean = false, keyboard: KeyboardOptions = KeyboardOptions.Default, trailing: (@Composable () -> Unit)? = null, singleLine: Boolean = true, minLines: Int = 1,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = modifier.fillMaxWidth(), singleLine = singleLine, minLines = minLines,
        placeholder = { Text(label, color = Brand.Muted) }, shape = RoundedCornerShape(14.dp),
        leadingIcon = if (icon != null) ({ Icon(icon, null, tint = Brand.Muted) }) else null, trailingIcon = trailing,
        visualTransformation = if (password) androidx.compose.ui.text.input.PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboard,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Brand.Primary, unfocusedBorderColor = Brand.Line, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
        ),
    )
}

@Composable
fun Avatar(url: String, name: String, size: Dp = 44.dp) {
    if (url.isNotEmpty()) {
        AsyncImage(model = url, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(CircleShape))
    } else {
        Box(Modifier.size(size).clip(CircleShape).background(Brand.MintSoft), contentAlignment = Alignment.Center) {
            Text(name.trim().take(1).uppercase().ifEmpty { "?" }, color = Brand.Primary, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.4f).sp)
        }
    }
}

@Composable
fun Spec(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = Brand.Muted)
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, color = Brand.Muted)
    }
}

@Composable
fun ListingCard(l: Listing, onClick: () -> Unit, onSave: () -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    Surface(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 2.dp,
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(if (compact) 120.dp else 190.dp)) {
                if (l.cover.isNotEmpty()) AsyncImage(model = l.cover, contentDescription = l.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Box(Modifier.fillMaxSize().background(Brand.MintSoft))
                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color(0x33000000)))))
                Surface(Modifier.padding(10.dp).align(Alignment.TopStart), shape = RoundedCornerShape(8.dp), color = if (l.market == "rent") Brand.Primary else Brand.Violet) {
                    Text(l.badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
                Surface(
                    onClick = onSave, modifier = Modifier.padding(10.dp).align(Alignment.TopEnd).size(34.dp), shape = CircleShape, color = Color.White,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (l.saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Save", Modifier.size(18.dp), tint = if (l.saved) Brand.Danger else Brand.Ink)
                    }
                }
                if (l.photos > 1) Surface(Modifier.padding(10.dp).align(Alignment.BottomEnd), shape = RoundedCornerShape(8.dp), color = Color(0x99000000)) {
                    Text("1/" + l.photos, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(l.priceText, fontWeight = FontWeight.ExtraBold, fontSize = if (compact) 15.sp else 19.sp, color = Brand.Ink)
                    if (l.periodText.isNotEmpty()) Text(l.periodText, fontSize = 12.sp, color = Brand.Muted, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
                    Spacer(Modifier.weight(1f))
                    if (l.verified) Icon(Icons.Filled.Verified, "Verified", Modifier.size(18.dp), tint = Brand.Primary)
                }
                Text(l.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                if (l.area.isNotEmpty()) Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, Modifier.size(15.dp), tint = Brand.Muted)
                    Text(l.area, fontSize = 12.sp, color = Brand.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp))
                }
                if (!compact) Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    l.bedrooms?.let { if (it > 0) Spec(Icons.Outlined.Bed, "$it bd") }
                    l.bathrooms?.let { if (it > 0) Spec(Icons.Outlined.Bathtub, "$it ba") }
                    if (l.size > 0) Spec(Icons.Outlined.SquareFoot, "${l.size.toInt()} ${l.sizeUnit}")
                    l.parking?.let { if (it > 0) Spec(Icons.Outlined.DirectionsCar, "$it") }
                }
            }
        }
    }
}

/** Loading spinner, error with retry, or the content. */
@Composable
fun Loadable(loading: Boolean, error: String, onRetry: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    when {
        loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Brand.Primary) }
        error.isNotEmpty() -> Column(modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(error, color = Brand.Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Try again", onRetry, Modifier.width(180.dp))
        }
        else -> content()
    }
}

@Composable
fun EmptyState(title: String, text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brand.Ink)
        Text(text, color = Brand.Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun SectionTitle(title: String, action: String = "", onAction: () -> Unit = {}, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brand.Ink, modifier = Modifier.weight(1f))
        if (action.isNotEmpty()) Text(action, color = Brand.Primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.clickable { onAction() })
    }
}
