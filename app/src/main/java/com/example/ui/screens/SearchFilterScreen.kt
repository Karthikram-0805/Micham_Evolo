package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ExpenseEntity
import com.example.domain.model.ExpenseFilter
import com.example.ui.components.ExpenseItemCard
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit
) {
    val filter by viewModel.expenseFilter.collectAsState()
    val filteredResults by viewModel.filteredExpenses.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val settings by viewModel.userSettings.collectAsState()

    val totalSpent = filteredResults.sumOf { it.expense.amount }
    val paymentMethods = listOf("UPI", "Cash", "Debit Card", "Credit Card", "Net Banking")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search & Filters 🔍") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Search Input Field
                OutlinedTextField(
                    value = filter.searchQuery,
                    onValueChange = { viewModel.expenseFilter.value = filter.copy(searchQuery = it) },
                    label = { Text("Search by note, category, or amount...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (filter.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.expenseFilter.value = filter.copy(searchQuery = "") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_filter_input")
                )
            }

            // Category Filter Chips
            item {
                Text("Category Filter:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filter.categoryId == null,
                            onClick = { viewModel.expenseFilter.value = filter.copy(categoryId = null) },
                            label = { Text("All Categories") }
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = filter.categoryId == cat.id,
                            onClick = {
                                val newId = if (filter.categoryId == cat.id) null else cat.id
                                viewModel.expenseFilter.value = filter.copy(categoryId = newId)
                            },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }

            // Payment Method Filter Chips
            item {
                Text("Payment Method:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filter.paymentMethod == null,
                            onClick = { viewModel.expenseFilter.value = filter.copy(paymentMethod = null) },
                            label = { Text("All Methods") }
                        )
                    }
                    items(paymentMethods) { method ->
                        FilterChip(
                            selected = filter.paymentMethod == method,
                            onClick = {
                                val newMethod = if (filter.paymentMethod == method) null else method
                                viewModel.expenseFilter.value = filter.copy(paymentMethod = newMethod)
                            },
                            label = { Text(method) }
                        )
                    }
                }
            }

            // Results count and total banner
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Results: ${filteredResults.size} expenses",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total: ${CurrencyFormatter.formatIndian(totalSpent, settings.currencySymbol)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Results List
            if (filteredResults.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🔍", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No matching expenses found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredResults, key = { it.expense.id }) { item ->
                    ExpenseItemCard(
                        item = item,
                        currencySymbol = settings.currencySymbol,
                        onEdit = { onEditExpense(item.expense) },
                        onDelete = { viewModel.deleteExpense(item.expense.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
