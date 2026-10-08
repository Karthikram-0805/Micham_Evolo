package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.CategoryEntity
import com.example.domain.sms.TransactionType
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SpentRed
import com.example.utils.CurrencyFormatter
import com.example.utils.DetectedSmsItem

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DetectedSmsReviewDialog(
    pendingItems: List<DetectedSmsItem>,
    categories: List<CategoryEntity>,
    currencySymbol: String = "₹",
    onConfirm: (item: DetectedSmsItem, description: String, categoryId: Long) -> Unit,
    onSkip: (item: DetectedSmsItem) -> Unit,
    onDismissAll: () -> Unit
) {
    if (pendingItems.isEmpty()) return

    val currentItem = pendingItems.first()
    val totalCount = pendingItems.size

    val initialDescription = remember(currentItem.uniqueHash) {
        val def = currentItem.defaultDescription
        if (def in listOf("UPI", "Debit Card", "Credit Card", "Net Banking", "Card", "Bank Deposit", "Merchant / Payee", "Sender / Source", "Other", "Unknown")) {
            ""
        } else {
            def
        }
    }

    var descriptionInput by remember(currentItem.uniqueHash) {
        mutableStateOf(initialDescription)
    }

    var selectedCategoryId by remember(currentItem.uniqueHash) {
        mutableLongStateOf(
            if (categories.any { it.id == currentItem.suggestedCategoryId }) {
                currentItem.suggestedCategoryId
            } else {
                categories.firstOrNull()?.id ?: 1L
            }
        )
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val quickDescriptionChips = if (currentItem.type == TransactionType.DEBIT) {
        listOf("Food", "Petrol", "Tea/Snacks", "Groceries", "Shopping", "Bills", "Rent", "Dinner", "Travel")
    } else {
        listOf("Salary", "Freelance", "Refund", "Cashback", "Bonus", "Transfer", "Deposit")
    }

    val selectedCategory = categories.find { it.id == selectedCategoryId }

    AlertDialog(
        onDismissRequest = onDismissAll,
        properties = DialogProperties(dismissOnClickOutside = false),
        modifier = Modifier.testTag("detected_sms_review_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New Bank SMS Detected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismissAll,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (totalCount > 1) {
                    Text(
                        text = "Reviewing 1 of $totalCount today's detected transactions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Amount & Type Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentItem.type == TransactionType.DEBIT) {
                            SpentRed.copy(alpha = 0.10f)
                        } else {
                            IncomeGreen.copy(alpha = 0.10f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (currentItem.type == TransactionType.DEBIT) SpentRed else IncomeGreen
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (currentItem.type == TransactionType.DEBIT) {
                                        Icons.Default.TrendingDown
                                    } else {
                                        Icons.Default.TrendingUp
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (currentItem.type == TransactionType.DEBIT) "DEBIT / SPENT" else "CREDIT / RECEIVED",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = (if (currentItem.type == TransactionType.DEBIT) "- " else "+ ") +
                                    CurrencyFormatter.formatIndian(currentItem.amount, currencySymbol),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = if (currentItem.type == TransactionType.DEBIT) SpentRed else IncomeGreen
                        )

                        Text(
                            text = "${currentItem.bankName} (${currentItem.accountInfo}) • ${currentItem.paymentMethod} • ${currentItem.time}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Description Input Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it },
                        placeholder = { Text("e.g. Food, Petrol, Shopping, Salary, Rent") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sms_description_input")
                    )
                }

                // Quick Description Suggestion Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickDescriptionChips.forEach { chipText ->
                        FilterChip(
                            selected = descriptionInput.equals(chipText, ignoreCase = true),
                            onClick = {
                                descriptionInput = chipText
                                // Auto-map category if matching
                                val matchedCat = categories.find { it.name.equals(chipText, ignoreCase = true) }
                                if (matchedCat != null) {
                                    selectedCategoryId = matchedCat.id
                                }
                            },
                            label = { Text(chipText, fontSize = 12.sp) },
                            modifier = Modifier.testTag("chip_$chipText")
                        )
                    }
                }

                // Category Selector (for Debit)
                if (currentItem.type == TransactionType.DEBIT && categories.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "Category",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCategoryId = cat.id
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDesc = descriptionInput.trim().ifEmpty {
                        selectedCategory?.name ?: if (currentItem.type == TransactionType.DEBIT) "Food" else "Salary"
                    }
                    onConfirm(currentItem, finalDesc, selectedCategoryId)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentItem.type == TransactionType.DEBIT) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        IncomeGreen
                    }
                ),
                modifier = Modifier.testTag("confirm_sms_button")
            ) {
                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (currentItem.type == TransactionType.DEBIT) {
                        "Deduct ₹${currentItem.amount.toLong()}"
                    } else {
                        "Add ₹${currentItem.amount.toLong()}"
                    }
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onSkip(currentItem) },
                modifier = Modifier.testTag("skip_sms_button")
            ) {
                Text("Skip")
            }
        }
    )
}
