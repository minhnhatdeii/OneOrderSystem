package com.example.oneorder.utils

import java.text.NumberFormat
import java.util.Locale

/**
 * Utility object for formatting currency values
 */
object CurrencyFormatter {
    
    /**
     * Format a Double amount to Vietnamese Dong (VNĐ)
     * Example: 150000.0 -> "150.000 VNĐ"
     */
    fun formatVND(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        formatter.maximumFractionDigits = 0 // VNĐ doesn't use decimals
        formatter.minimumFractionDigits = 0
        return "${formatter.format(amount)} VNĐ"
    }
    
    /**
     * Format a Double amount to a simple VNĐ string without thousands separator
     * Example: 150000.0 -> "150000 VNĐ"
     */
    fun formatVNDSimple(amount: Double): String {
        return "${amount.toInt()} VNĐ"
    }
}
