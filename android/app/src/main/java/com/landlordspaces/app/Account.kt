package com.landlordspaces.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.json.JSONObject

/** The shared green top bar: menu, logo, AI shortcut, notifications with count, and the member's photo. */
@Composable
fun LsTopBar(avatar: String, name: String, unread: Int, onMenu: () -> Unit, onAi: () -> Unit, onBell: () -> Unit, onAvatar: () -> Unit, online: Boolean = true) {
    Row(Modifier.padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Menu, "Menu", Modifier.clickable { onMenu() }.padding(6.dp).size(24.dp), tint = Color.White)
        LsLogo(40.dp)
        Column(Modifier.padding(start = 8.dp).weight(1f)) {
            Text("LandlordSpaces", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
            Text("World Smartest Real Estate Marketplace", color = Color.White.copy(alpha = 0.9f), fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Surface(onClick = onAi, shape = RoundedCornerShape(20.dp), color = Color.Transparent, border = BorderStroke(1.5.dp, Brand.Gold), modifier = Modifier.height(40.dp)) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, "Ask AI", Modifier.size(16.dp), tint = Brand.Gold)
                Text(" AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        Box(Modifier.padding(start = 6.dp).clickable { onBell() }.padding(6.dp)) {
            Icon(Icons.Outlined.Notifications, "Notifications", Modifier.size(24.dp), tint = Color.White)
            if (unread > 0) Box(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp).defaultMinSize(minWidth = 16.dp, minHeight = 16.dp).clip(CircleShape).background(Brand.Danger), contentAlignment = Alignment.Center) {
                Text(if (unread > 99) "99+" else "$unread", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
        Box(Modifier.padding(start = 4.dp).size(42.dp)) {
            Box(Modifier.fillMaxSize().clip(CircleShape).background(Brand.Primary).clickable { onAvatar() }, contentAlignment = Alignment.Center) {
                if (avatar.isNotEmpty() && !avatar.endsWith(".svg")) AsyncImage(avatar, "Profile", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Text(name.trim().take(1).uppercase().ifEmpty { "L" }, color = Color.White, fontWeight = FontWeight.Bold)
            }
            if (online) Box(Modifier.align(Alignment.BottomEnd).size(11.dp).clip(CircleShape).background(Color(0xFF22C55E)).padding(1.dp))
        }
    }
}

@Composable
private fun AccountRow(icon: ImageVector, label: String, onClick: () -> Unit, badge: Int = 0) {
    Row(Modifier.fillMaxWidth().height(54.dp).clickable { onClick() }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(23.dp), tint = Brand.Forest) }
        Text(label, color = Brand.Ink, fontWeight = FontWeight.Medium, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
        if (badge > 0) Box(Modifier.padding(end = 8.dp).defaultMinSize(minWidth = 22.dp, minHeight = 22.dp).clip(CircleShape).background(Brand.Danger), contentAlignment = Alignment.Center) {
            Text(if (badge > 99) "99+" else "$badge", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp))
        }
        Icon(Icons.Filled.ChevronRight, null, Modifier.size(20.dp), tint = Brand.Ink)
    }
}

@Composable
private fun Section(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Brand.MintSoft.copy(alpha = 0.55f)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(Modifier.padding(start = 4.dp, bottom = 6.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(22.dp), tint = Brand.Forest)
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brand.Ink, modifier = Modifier.padding(start = 12.dp))
            }
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Color.White) {
                Column(Modifier.padding(horizontal = 10.dp)) { content() }
            }
        }
    }
}

@Composable
private fun Divider1() { Box(Modifier.fillMaxWidth().padding(start = 44.dp).height(1.dp).background(Brand.Line)) }

@Composable
private fun Counter(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    Surface(modifier.height(92.dp), shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Brand.Line)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(Brand.MintSoft), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(18.dp), tint = Brand.Primary) }
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brand.Ink, modifier = Modifier.padding(top = 3.dp), maxLines = 1)
            Text(label, fontSize = 10.sp, lineHeight = 12.sp, color = Brand.Ink.copy(alpha = 0.75f), maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

/** Account: a control centre built from the member's real profile, counters and the same destinations as before, in five groups. */
@Composable
fun AccountScreen(
    repo: Repo, openWeb: (String, String) -> Unit, openSaved: () -> Unit, openNotifications: () -> Unit, openWorkspace: () -> Unit, openAi: () -> Unit, onSignedOut: () -> Unit,
) {
    StatusBar(darkBackground = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<JSONObject?>(null) }
    var completion by remember { mutableStateOf(0) }
    var missing by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(0) }
    var saved by remember { mutableStateOf(0) }
    var unread by remember { mutableStateOf(0) }
    var views by remember { mutableStateOf(0) }
    var confirm by remember { mutableStateOf(false) }
    var deleteAsk by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        online { repo.account() }.onSuccess { j ->
            user = j.optJSONObject("user")
            completion = j.optInt("completion")
            val m = j.optJSONArray("missing")
            missing = if (m != null && m.length() > 0) "Still needed: " + (0 until m.length()).joinToString(", ") { m.optString(it) } else ""
            val s = j.optJSONObject("summary")
            active = s?.optInt("active_listings") ?: 0; saved = s?.optInt("saved") ?: 0
            unread = s?.optInt("unread_notifications") ?: 0; views = s?.optInt("listing_views") ?: 0
        }
    }
    val name = user?.str("name") ?: repo.session.name
    val avatar = img(user?.str("avatar") ?: "")
    val verified = user?.optBoolean("verified") == true
    val type = (user?.str("account_type") ?: "").replaceFirstChar { it.uppercase() }
    val editProfile = { openWeb("My Profile", "/account/profile") }

    Column(Modifier.fillMaxSize().background(Brand.Bg)) {
        GreenHeader {
            Column(Modifier.padding(bottom = 12.dp)) {
                LsTopBar(avatar, name, unread, openWorkspace, openAi, openNotifications, editProfile)
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                    Text("Manage your profile, listings, properties and settings.", color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp)
                }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // ---- profile card ----
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Brand.MintSoft.copy(alpha = 0.7f)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.clickable { editProfile() }) { Avatar(avatar, name, 68.dp) }
                        Column(Modifier.padding(start = 14.dp).weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Brand.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                if (verified) Icon(Icons.Filled.Verified, "Verified", Modifier.padding(start = 6.dp).size(19.dp), tint = Brand.Primary)
                            }
                            Text(user?.str("email") ?: "", color = Brand.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (verified) Surface(shape = RoundedCornerShape(12.dp), color = Brand.MintSoft, border = BorderStroke(1.dp, Brand.Primary.copy(alpha = 0.3f))) {
                                    Text("Verified Account", color = Brand.Primary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                                }
                                if (type.isNotEmpty()) {
                                    Icon(Icons.Outlined.WorkOutline, null, Modifier.padding(start = if (verified) 8.dp else 0.dp).size(16.dp), tint = Brand.Muted)
                                    Text(" $type", color = Brand.Ink, fontSize = 12.sp)
                                }
                            }
                        }
                        Surface(onClick = editProfile, shape = RoundedCornerShape(20.dp), color = Color.Transparent, border = BorderStroke(1.dp, Brand.Primary.copy(alpha = 0.6f)), modifier = Modifier.height(40.dp)) {
                            Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp), tint = Brand.Primary)
                                Text(" Edit", color = Brand.Primary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            }
                        }
                    }
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Profile Completion", fontSize = 13.sp, color = Brand.Ink, modifier = Modifier.weight(1f))
                        Text("$completion%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Brand.Ink)
                    }
                    LinearProgressIndicator(progress = { completion / 100f }, Modifier.fillMaxWidth().padding(top = 6.dp).height(8.dp).clip(RoundedCornerShape(4.dp)), color = Brand.Primary, trackColor = Brand.Line)
                    if (missing.isNotEmpty() && completion < 100) Text(missing, fontSize = 11.sp, color = Brand.Muted, modifier = Modifier.padding(top = 6.dp).clickable { editProfile() })
                }
            }
            // ---- four real counters ----
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Counter(Icons.Outlined.Home, active.toString(), "Active Listings", Modifier.weight(1f).clickable { openWeb("My Listings", "/account/listings") })
                Counter(Icons.Outlined.FavoriteBorder, saved.toString(), "Saved Properties", Modifier.weight(1f).clickable { openSaved() })
                Counter(Icons.Outlined.Notifications, unread.toString(), "Unread Notifications", Modifier.weight(1f).clickable { openNotifications() })
                Counter(Icons.Outlined.Visibility, java.text.NumberFormat.getIntegerInstance().format(views), "Listing Views", Modifier.weight(1f).clickable { openWeb("Listing Performance", "/account/performance") })
            }
            Section(Icons.Outlined.WorkOutline, "Workspace") {
                AccountRow(Icons.Outlined.Dashboard, "My Workspace", openWorkspace); Divider1()
                AccountRow(Icons.Outlined.Apartment, "My Listings", { openWeb("My Listings", "/account/listings") })
            }
            Section(Icons.Outlined.Person, "Personal & Property") {
                AccountRow(Icons.Outlined.Person, "My Profile", editProfile); Divider1()
                AccountRow(Icons.Outlined.FavoriteBorder, "Saved Properties", openSaved); Divider1()
                AccountRow(Icons.Outlined.ViewInAr, "Floor Plan Studio", { openWeb("Floor Plan Studio", "/account/studio") })
            }
            Section(Icons.Outlined.Groups, "Property Management") {
                AccountRow(Icons.Outlined.Groups, "Tenant Management", { openWeb("Tenant Management", "/tenant-management") }); Divider1()
                AccountRow(Icons.Outlined.Savings, "Rent Savings", { openWeb("Rent Savings", "/account/rent-savings") })
            }
            Section(Icons.Outlined.AutoAwesome, "Communication & Intelligence") {
                AccountRow(Icons.Outlined.Notifications, "Notifications", openNotifications, badge = unread); Divider1()
                AccountRow(Icons.Outlined.AutoAwesome, "Ask AI", openAi)
            }
            Section(Icons.Outlined.Security, "Security & Assistance") {
                AccountRow(Icons.Outlined.Security, "Security & Privacy", { openWeb("Security & Privacy", "/account/security") }); Divider1()
                AccountRow(Icons.Outlined.Info, "Help & Support", { openWeb("Help & Support", "/help") })
            }
            Row(Modifier.fillMaxWidth().height(48.dp).clickable { confirm = true }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null, Modifier.size(22.dp), tint = Brand.Danger)
                Text("Sign Out", color = Brand.Danger, fontWeight = FontWeight.Medium, fontSize = 15.sp, modifier = Modifier.padding(start = 14.dp))
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Privacy", color = Brand.Muted, fontSize = 12.sp, modifier = Modifier.clickable { openInBrowser(context, "$SITE/p/privacy") }.padding(end = 14.dp))
                Text("Terms", color = Brand.Muted, fontSize = 12.sp, modifier = Modifier.clickable { openInBrowser(context, "$SITE/p/terms") }.padding(end = 14.dp))
                Text("Delete Account", color = Brand.Muted, fontSize = 12.sp, modifier = Modifier.clickable { deleteAsk = true })
                Spacer(Modifier.weight(1f))
                Text("v" + BuildConfig.VERSION_NAME, color = Brand.Muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false }, title = { Text("Sign out?") }, text = { Text("You will need to sign in again to use your account. Drafts saved on this phone are kept until you sign out, then removed from this phone.") },
        confirmButton = { TextButton({ confirm = false; scope.launch { online { repo.session.signOut() }; onSignedOut() } }) { Text("Sign out", color = Brand.Danger) } },
        dismissButton = { TextButton({ confirm = false }) { Text("Cancel") } },
    )
    if (deleteAsk) AlertDialog(
        onDismissRequest = { deleteAsk = false }, title = { Text("Delete your account?") },
        text = { Text("Deleting your account removes your profile, listings and messages and cannot be undone. You will be asked to confirm with your password on the website before anything is deleted.") },
        confirmButton = { TextButton({ deleteAsk = false; openInBrowser(context, "$SITE/delete-account") }) { Text("Continue", color = Brand.Danger) } },
        dismissButton = { TextButton({ deleteAsk = false }) { Text("Keep my account") } },
    )
}
