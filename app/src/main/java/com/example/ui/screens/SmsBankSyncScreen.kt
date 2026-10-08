package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.entity.ProcessedSmsEntity
import com.example.domain.sms.BankSmsParseResult
import com.example.domain.sms.BankSmsProcessResult
import com.example.domain.sms.ParsedBankTransaction
import com.example.domain.sms.TransactionType
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SpentRed
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.CurrencyFormatter
import com.example.utils.SmsScanSummary

@Composable
fun SmsBankSyncScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val settings by viewModel.userSettings.collectAsState()
    val processedSmsList by viewModel.processedSmsList.collectAsState()

    // Permission check
    var hasReceiveSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECEIVE_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasReadSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasReceiveSmsPermission = permissions[Manifest.permission.RECEIVE_SMS] == true
        hasReadSmsPermission = permissions[Manifest.permission.READ_SMS] == true
        if (hasReceiveSmsPermission) {
            Toast.makeText(context, "SMS reading permission granted! 🚀", Toast.LENGTH_SHORT).show()
        }
    }

    // Inbox scan state
    var isScanningInbox by remember { mutableStateOf(false) }
    var scanSummaryDialog by remember { mutableStateOf<SmsScanSummary?>(null) }

    // Test Simulator state
    var testSenderInput by remember { mutableStateOf("VK-HDFCBK") }
    var testBodyInput by remember {
        mutableStateOf("Rs. 500.00 debited from A/c XX1234 on 06-10-24 via UPI to SWIGGY. Avl Bal Rs. 14,500.00.")
    }
    var testResult by remember { mutableStateOf<BankSmsParseResult?>(null) }
    var testProcessStatus by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("sms_screen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto Bank SMS Reader 📱",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Zero manual entry • 100% On-device privacy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 1. PERMISSION STATUS CARD
            item {
                val allGranted = hasReceiveSmsPermission && hasReadSmsPermission
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (allGranted) {
                            IncomeGreen.copy(alpha = 0.12f)
                        } else {
                            WarningOrange.copy(alpha = 0.12f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (allGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (allGranted) IncomeGreen else WarningOrange,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (allGranted) "Auto SMS Sync Active 🛡️" else "SMS Permission Needed ⚠️",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (allGranted) {
                                        "App automatically reads incoming bank SMS and logs expenses & incomes."
                                    } else {
                                        "Grant permission so Micham Evlo can read bank debit and credit SMS."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!allGranted) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.RECEIVE_SMS,
                                            Manifest.permission.READ_SMS
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("grant_sms_permission_button")
                            ) {
                                Text("Grant SMS Permissions")
                            }
                        }
                    }
                }
            }

            // 2. TOGGLES & INBOX SCAN CARD
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Sync Settings",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Toggle Auto SMS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-detect Bank SMS",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Automatically update balance when bank SMS arrives",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.autoSmsDetectionEnabled,
                                onCheckedChange = { viewModel.toggleAutoSmsDetection(it) },
                                modifier = Modifier.testTag("auto_sms_toggle")
                            )
                        }

                        // Toggle Notifications
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tamil Dialogue Alerts",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Show funny Tamil reaction when bank SMS is detected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.autoSmsNotificationEnabled,
                                onCheckedChange = { viewModel.toggleAutoSmsNotification(it) },
                                modifier = Modifier.testTag("auto_sms_notification_toggle")
                            )
                        }

                        // Last Scan Time Info
                        val lastScanText = if (settings.lastSmsScanTimestamp > 0) {
                            val instant = java.time.Instant.ofEpochMilli(settings.lastSmsScanTimestamp)
                            val dt = instant.atZone(java.time.ZoneId.systemDefault())
                            "Last Checked: ${dt.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a, dd MMM"))}"
                        } else {
                            "Last Checked: Not checked yet today"
                        }
                        Text(
                            text = lastScanText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Button 1: Check Today's Bank SMS
                        OutlinedButton(
                            onClick = {
                                if (!hasReadSmsPermission) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.RECEIVE_SMS,
                                            Manifest.permission.READ_SMS
                                        )
                                    )
                                } else {
                                    viewModel.checkAppOpenSms(forceRescanToday = true) { items ->
                                        if (items.isEmpty()) {
                                            Toast.makeText(context, "No new bank transactions found today! All up to date 👍", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Found ${items.size} new bank transactions today! Review dialog opened.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("check_today_sms_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check Today's Bank Transactions")
                        }

                        // Button 2: Scan SMS Inbox Button (Historical)
                        Button(
                            onClick = {
                                if (!hasReadSmsPermission) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.RECEIVE_SMS,
                                            Manifest.permission.READ_SMS
                                        )
                                    )
                                } else {
                                    isScanningInbox = true
                                    viewModel.scanSmsInbox { summary ->
                                        isScanningInbox = false
                                        scanSummaryDialog = summary
                                    }
                                }
                            },
                            enabled = !isScanningInbox,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scan_inbox_button")
                        ) {
                            if (isScanningInbox) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scanning Bank SMS...")
                            } else {
                                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan & Import All Past SMS")
                            }
                        }
                    }
                }
            }

            // 3. INTERACTIVE SMS TESTER & SIMULATOR
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SMS Parser Tester & Simulator",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "Test how Micham Evlo detects DEBIT, CREDIT, and rejects loans, OTPs, or ads.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Preset chips
                        Text(
                            text = "Quick Presets:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        testSenderInput = "VK-HDFCBK"
                                        testBodyInput = "Rs. 1,250 debited from A/c XX1234 via UPI"
                                        testResult = viewModel.testParseSms(testSenderInput, testBodyInput)
                                        testProcessStatus = null
                                    },
                                    label = { Text("₹1,250 UPI (Your Example)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.TrendingDown, null, tint = SpentRed, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        testSenderInput = "VK-HDFCBK"
                                        testBodyInput = "Rs. 500.00 debited from A/c XX1234 on 06-10-24 via UPI to SWIGGY. Avl Bal Rs. 14,500.00."
                                        testResult = viewModel.testParseSms(testSenderInput, testBodyInput)
                                        testProcessStatus = null
                                    },
                                    label = { Text("₹500 Debit (Swiggy)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.TrendingDown, null, tint = SpentRed, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        testSenderInput = "AD-SBIINB"
                                        testBodyInput = "Rs. 10,000.00 credited to A/c XX5678 on 06-10-24 by transfer. Avl Bal Rs. 60,000.00."
                                        testResult = viewModel.testParseSms(testSenderInput, testBodyInput)
                                        testProcessStatus = null
                                    },
                                    label = { Text("₹10,000 Credit") },
                                    leadingIcon = {
                                        Icon(Icons.Default.TrendingUp, null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        testSenderInput = "AX-ICICIB"
                                        testBodyInput = "INR 1,250.50 spent on ICICI Bank Card XX4321 at AMAZON on 06-10-24. Avl Limit: Rs. 85,000."
                                        testResult = viewModel.testParseSms(testSenderInput, testBodyInput)
                                        testProcessStatus = null
                                    },
                                    label = { Text("₹1,250.50 Card") },
                                    leadingIcon = {
                                        Icon(Icons.Default.TrendingDown, null, tint = SpentRed, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        testSenderInput = "BAJAJFIN"
                                        testBodyInput = "Congratulations! You are eligible for a pre-approved personal loan of Rs. 5,00,000 at 10.5% interest. Apply now."
                                        testResult = viewModel.testParseSms(testSenderInput, testBodyInput)
                                        testProcessStatus = null
                                    },
                                    label = { Text("Loan Ad (Ignored)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Block, null, tint = WarningOrange, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        testSenderInput = "VK-HDFCBK"
                                        testBodyInput = "938201 is your OTP for transaction at Flipkart. Do NOT share this OTP with anyone."
                                        testResult = viewModel.testParseSms(testSenderInput, testBodyInput)
                                        testProcessStatus = null
                                    },
                                    label = { Text("OTP (Ignored)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Block, null, tint = WarningOrange, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        testSenderInput = "VM-KOTAKB"
                                        testBodyInput = "Available balance in A/c XX8899 is Rs. 42,300.00 as on 06-10-24."
                                        testResult = viewModel.testParseSms(testSenderInput, testBodyInput)
                                        testProcessStatus = null
                                    },
                                    label = { Text("Balance Only (Ignored)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Block, null, tint = WarningOrange, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }

                        // Inputs
                        OutlinedTextField(
                            value = testSenderInput,
                            onValueChange = { testSenderInput = it },
                            label = { Text("SMS Sender (e.g. VK-HDFCBK)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = testBodyInput,
                            onValueChange = { testBodyInput = it },
                            label = { Text("SMS Body") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    viewModel.simulateBankSmsForReview(testSenderInput, testBodyInput) { status ->
                                        testProcessStatus = status
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("simulate_review_flow_button")
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simulate Detection & Review (Enter Description)")
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        testResult = viewModel.testParseSms(testSenderInput, testBodyInput)
                                        testProcessStatus = null
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Test Parsing")
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.processTestSms(testSenderInput, testBodyInput) { res ->
                                            when (res) {
                                                is BankSmsProcessResult.AddedAsExpense -> {
                                                    testProcessStatus = "✅ Successfully added ₹${res.amount} as DEBIT expense! ${res.reaction}"
                                                }
                                                is BankSmsProcessResult.AddedAsIncome -> {
                                                    testProcessStatus = "✅ Successfully added ₹${res.amount} as CREDIT income! ${res.reaction}"
                                                }
                                                is BankSmsProcessResult.Duplicate -> {
                                                    testProcessStatus = "⚠️ Duplicate: ${res.message}"
                                                }
                                                is BankSmsProcessResult.Ignored -> {
                                                    testProcessStatus = "🛑 Not added: ${res.reason}"
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Auto-Save")
                                }
                            }
                        }

                        // Test output display
                        if (testResult != null) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when (val res = testResult!!) {
                                        is BankSmsParseResult.Success -> {
                                            if (res.transaction.type == TransactionType.DEBIT) {
                                                SpentRed.copy(alpha = 0.08f)
                                            } else {
                                                IncomeGreen.copy(alpha = 0.08f)
                                            }
                                        }
                                        is BankSmsParseResult.Ignored -> WarningOrange.copy(alpha = 0.08f)
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    when (val res = testResult!!) {
                                        is BankSmsParseResult.Success -> {
                                            val t = res.transaction
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (t.type == TransactionType.DEBIT) SpentRed else IncomeGreen
                                                ) {
                                                    Icon(
                                                        imageVector = if (t.type == TransactionType.DEBIT) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.padding(4.dp).size(16.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "VALID BANK TRANSACTION: ${t.type.name}",
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (t.type == TransactionType.DEBIT) SpentRed else IncomeGreen
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Amount: ₹${t.amount} | Bank: ${t.bankName}",
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Account: ${t.accountInfo} | Method: ${t.paymentMethod}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            if (t.payeeOrMerchant.isNotBlank()) {
                                                Text(
                                                    text = "Payee/Merchant: ${t.payeeOrMerchant}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            Text(
                                                text = "Date: ${t.date} ${t.time}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        is BankSmsParseResult.Ignored -> {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Block,
                                                    contentDescription = null,
                                                    tint = WarningOrange,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "REJECTED (SAFEGUARD)",
                                                    fontWeight = FontWeight.Bold,
                                                    color = WarningOrange
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = res.reason,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (testProcessStatus != null) {
                            Text(
                                text = testProcessStatus!!,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }

            // 4. DETECTED TRANSACTION HISTORY
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detected SMS Bank History (${processedSmsList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (processedSmsList.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No bank SMS detected yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Incoming bank transaction SMS will appear here automatically and update your balance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(processedSmsList) { sms ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (sms.transactionType == "DEBIT") SpentRed.copy(alpha = 0.15f) else IncomeGreen.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (sms.transactionType == "DEBIT") Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = if (sms.transactionType == "DEBIT") SpentRed else IncomeGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${sms.bankName} (${sms.accountInfo})",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = (if (sms.transactionType == "DEBIT") "- " else "+ ") + CurrencyFormatter.format(sms.amount),
                                        fontWeight = FontWeight.Black,
                                        color = if (sms.transactionType == "DEBIT") SpentRed else IncomeGreen,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${sms.paymentMethod} • ${sms.date} ${sms.time}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    IconButton(
                                        onClick = { viewModel.deleteProcessedSms(sms.smsHash) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Inbox scan summary dialog
    if (scanSummaryDialog != null) {
        val s = scanSummaryDialog!!
        AlertDialog(
            onDismissRequest = { scanSummaryDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = IncomeGreen,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "SMS Scan Complete 🎉",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Total SMS messages scanned: ${s.totalScanned}")
                    Text(
                        "💸 Debits added to Expenses: ${s.debitsAdded} (${CurrencyFormatter.format(s.totalDebitAmount)})",
                        color = SpentRed,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "💰 Credits added to Income: ${s.creditsAdded} (${CurrencyFormatter.format(s.totalCreditAmount)})",
                        color = IncomeGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("🔄 Duplicates skipped: ${s.duplicatesSkipped}")
                    Text("🛡️ Non-transactions ignored: ${s.nonTransactionsIgnored} (Loan ads, OTPs, promotional)")
                }
            },
            confirmButton = {
                Button(onClick = { scanSummaryDialog = null }) {
                    Text("Awesome!")
                }
            }
        )
    }
}
