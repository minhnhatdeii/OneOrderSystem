package com.example.oneorder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.oneorder.ui.theme.OneTextColor
import com.example.oneorder.ui.theme.OrderTextColor

/**
 * Styled "OneOrder" text component with "One" in black and "Order" in blue
 * Matches branding with OneOrder_SM
 */
@Composable
fun StyledOneOrderText(
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "One",
            style = style,
            fontWeight = FontWeight.Bold,
            color = OneTextColor
        )
        Text(
            text = "Order",
            style = style,
            fontWeight = FontWeight.Bold,
            color = OrderTextColor
        )
    }
}
