package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {

    fun getIcon(iconName: String): ImageVector {
        return when (iconName.lowercase()) {
            "restaurant", "food" -> Icons.Default.Restaurant
            "local_cafe", "tea", "coffee" -> Icons.Default.LocalCafe
            "directions_bus", "travel", "bus" -> Icons.Default.DirectionsBus
            "local_gas_station", "petrol", "fuel" -> Icons.Default.LocalGasStation
            "shopping_bag", "shopping" -> Icons.Default.ShoppingBag
            "receipt_long", "bills", "bill" -> Icons.Default.ReceiptLong
            "home", "rent" -> Icons.Default.Home
            "credit_card", "emi", "card" -> Icons.Default.CreditCard
            "subscriptions", "sub" -> Icons.Default.Subscriptions
            "movie", "entertainment" -> Icons.Default.Movie
            "medical_services", "health" -> Icons.Default.MedicalServices
            "shopping_cart", "groceries" -> Icons.Default.ShoppingCart
            "family_restroom", "family" -> Icons.Default.FamilyRestroom
            "groups", "friends" -> Icons.Default.Groups
            "school", "education" -> Icons.Default.School
            "inventory_2", "online shopping", "package" -> Icons.Default.Inventory2
            else -> Icons.Default.MoreHoriz
        }
    }

    fun parseColor(hex: String, fallback: Color = Color(0xFF64748B)): Color {
        return try {
            val cleanHex = hex.replace("#", "")
            val colorInt = when (cleanHex.length) {
                6 -> 0xFF000000.toInt() or cleanHex.toLong(16).toInt()
                8 -> cleanHex.toLong(16).toInt()
                else -> fallback.value.toLong().toInt()
            }
            Color(colorInt)
        } catch (e: Exception) {
            fallback
        }
    }
}
