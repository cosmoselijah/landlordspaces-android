package com.landlordspaces.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The one bottom bar: Home, Browse, a raised List Property action, Messages (with unread count) and Account. */
@Composable
fun LsBottomBar(tab: Int, unread: Int, onTab: (Int) -> Unit) {
    Surface(color = Color.White, shadowElevation = 12.dp) {
        Row(Modifier.navigationBarsPadding().fillMaxWidth().height(68.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            NavSlot(Modifier.weight(1f), tab == 0, "Home", Icons.Filled.Home, Icons.Outlined.Home, 0) { onTab(0) }
            NavSlot(Modifier.weight(1f), tab == 1, "Browse", Icons.Filled.Search, Icons.Outlined.Search, 0) { onTab(1) }
            Column(Modifier.weight(1f).clickable { onTab(2) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.offset(y = (-6).dp).size(46.dp).clip(CircleShape).background(if (tab == 2) Brand.PrimaryDark else Brand.Primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Add, "List Property", Modifier.size(28.dp), tint = Color.White)
                }
                Text("List Property", fontSize = 11.sp, color = if (tab == 2) Brand.Primary else Brand.Muted, fontWeight = if (tab == 2) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, modifier = Modifier.offset(y = (-4).dp))
            }
            NavSlot(Modifier.weight(1f), tab == 3, "Messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, unread) { onTab(3) }
            NavSlot(Modifier.weight(1f), tab == 4, "Account", Icons.Filled.Person, Icons.Outlined.Person, 0) { onTab(4) }
        }
    }
}

@Composable
private fun NavSlot(modifier: Modifier, active: Boolean, label: String, on: ImageVector, off: ImageVector, badge: Int, onClick: () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            Modifier.clip(RoundedCornerShape(22.dp)).background(if (active) Brand.MintSoft else Color.Transparent).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box {
                Icon(if (active) on else off, label, Modifier.size(24.dp), tint = if (active) Brand.Primary else Brand.Muted)
                if (badge > 0) Box(Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp).defaultMinSize(minWidth = 16.dp, minHeight = 16.dp).clip(CircleShape).background(Brand.Danger), contentAlignment = Alignment.Center) {
                    Text(if (badge > 99) "99+" else "$badge", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
            Text(label, fontSize = 11.sp, color = if (active) Brand.Primary else Brand.Muted, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
        }
    }
}
