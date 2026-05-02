package com.example.oneorder_sm.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.oneorder_sm.R
import com.example.oneorder_sm.ui.theme.*

data class SidebarMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badge: Int? = null // For showing count like pending orders
)

@Composable
fun AppSidebar(
    restaurantName: String,
    userRole: String,
    selectedItemId: String,
    onItemSelected: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val menuItems = buildList {
        if (userRole == "manager") {
            add(SidebarMenuItem("dashboard", "Tổng quan", Icons.Outlined.Dashboard, Icons.Filled.Dashboard))
        }
        add(SidebarMenuItem("orders", "Đơn hàng", Icons.Outlined.Receipt, Icons.Filled.Receipt))
        add(SidebarMenuItem("tables", "Quản lý bàn", Icons.Outlined.TableBar, Icons.Filled.TableBar))
        if (userRole == "manager") {
            add(SidebarMenuItem("menu", "Quản lý Menu", Icons.Outlined.RestaurantMenu, Icons.Filled.RestaurantMenu))
            add(SidebarMenuItem("food_promotion", "Nhà hàng", Icons.Outlined.Storefront, Icons.Filled.Storefront))
            add(SidebarMenuItem("staff", "Quản lý nhân viên", Icons.Outlined.People, Icons.Filled.People))
        } else {
            add(SidebarMenuItem("staff", "Chấm công", Icons.Outlined.AccessTime, Icons.Filled.AccessTime))
        }
        // Profile available for all users
        add(SidebarMenuItem("profile", "Hồ sơ cá nhân", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle))
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(SidebarBackground)
            .padding(top = 48.dp, bottom = 16.dp, start = 0.dp, end = 0.dp)
    ) {
        // Header with restaurant name
        SidebarHeader(
            restaurantName = restaurantName,
            userRole = userRole
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = SidebarItemHover
        )

        // Menu Items
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            menuItems.forEach { item ->
                SidebarItem(
                    item = item,
                    isSelected = selectedItemId == item.id,
                    onClick = { onItemSelected(item.id) }
                )
            }
        }

        // Logout button at bottom
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = SidebarItemHover
        )

        SidebarItem(
            item = SidebarMenuItem("logout", "Đăng xuất", Icons.Outlined.Logout),
            isSelected = false,
            onClick = onLogout,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SidebarHeader(
    restaurantName: String,
    userRole: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // App Logo
        Image(
            painter = painterResource(id = R.drawable.logo_oneorder),
            contentDescription = "OneOrder Logo",
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = restaurantName,
            style = MaterialTheme.typography.bodyLarge,
            color = SidebarTextMuted
        )

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            color = if (userRole == "manager") Primary else Secondary,
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = if (userRole == "manager") "Quản lý" else "Nhân viên",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun SidebarItem(
    item: SidebarMenuItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> SidebarItemSelected.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    val iconTint = when {
        isSelected -> Primary
        else -> SidebarTextMuted
    }

    val textColor = when {
        isSelected -> SidebarText
        else -> SidebarTextMuted
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.icon,
            contentDescription = item.title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        // Badge for counts
        item.badge?.let { count ->
            if (count > 0) {
                Surface(
                    color = Error,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (count > 99) "99+" else count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Selected indicator
        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Primary)
            )
        }
    }
}
