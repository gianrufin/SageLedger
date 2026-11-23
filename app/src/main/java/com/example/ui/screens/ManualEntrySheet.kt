package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyFormatter
import com.example.core.LedgerAnalytics
import com.example.data.model.MerchantRule
import com.example.data.model.PaymentSource
import com.example.data.model.SpendingCategory
import com.example.data.model.SpendingTransaction
import com.example.ui.components.icon
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntrySheet(
    editingTransaction: SpendingTransaction?,
    currencyCode: String,
    merchantRules: List<MerchantRule>,
    onSuggestCategory: (merchant: String, snippet: String?, onResult: (SpendingCategory) -> Unit) -> Unit,
    onParseSnippet: (
        snippet: String,
        onParsed: (merchant: String, amountCents: Long, source: PaymentSource, category: SpendingCategory) -> Unit,
        onUnrecognized: () -> Unit
    ) -> Unit,
    onSave: (
        existingId: String?,
        merchant: String,
        amountCents: Long,
        category: SpendingCategory,
        paymentSource: PaymentSource,
        note: String,
        rawSnippet: String?,
        occurredAtMillis: Long,
        rememberMerchantRule: Boolean
    ) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialAmountStr = remember(editingTransaction) {
        editingTransaction?.let {
            String.format(java.util.Locale.US, "%.2f", it.amountCents / 100.0)
        }.orEmpty()
    }

    var merchant by remember(editingTransaction) { mutableStateOf(editingTransaction?.merchant.orEmpty()) }
    var amountText by remember(editingTransaction) { mutableStateOf(initialAmountStr) }
    var selectedCategory by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.categoryEnum ?: SpendingCategory.FOOD)
    }
    var selectedSource by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.paymentSourceEnum ?: PaymentSource.CASH)
    }
    var note by remember(editingTransaction) { mutableStateOf(editingTransaction?.note.orEmpty()) }
    var rawSnippet by remember(editingTransaction) { mutableStateOf(editingTransaction?.rawSnippet.orEmpty()) }
    var occurredAtMillis by remember(editingTransaction) {
        mutableLongStateOf(editingTransaction?.occurredAtMillis ?: System.currentTimeMillis())
    }
    var rememberRule by remember(editingTransaction) { mutableStateOf(false) }
    var showSmsPasteBox by remember { mutableStateOf(false) }
    var smsPasteInput by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTransaction == null) "Record Purchase or Cash" else "Review Purchase",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (editingTransaction == null) {
                        TextButton(
                            onClick = { showSmsPasteBox = !showSmsPasteBox },
                            modifier = Modifier.testTag("toggle_paste_alert_button")
                        ) {
                            Text(if (showSmsPasteBox) "Hide alert parser" else "Paste SMS/Alert")
                        }
                    } else {
                        IconButton(
                            onClick = {
                                onDelete(editingTransaction.id)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("delete_transaction_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete transaction",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showSmsPasteBox) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = smsPasteInput,
                        onValueChange = { smsPasteInput = it },
                        label = { Text("Bank, E-Wallet, or SMS text") },
                        placeholder = { Text("e.g. Paid \$14.80 via GCash at 7-Eleven") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("paste_sms_input")
                    )
                    OutlinedButton(
                        onClick = {
                            onParseSnippet(
                                smsPasteInput,
                                { m, cents, src, cat ->
                                    merchant = m
                                    amountText = String.format(java.util.Locale.US, "%.2f", cents / 100.0)
                                    selectedSource = src
                                    selectedCategory = cat
                                    rawSnippet = smsPasteInput
                                    validationError = null
                                    showSmsPasteBox = false
                                },
                                {
                                    validationError = "Could not detect an amount in that text."
                                }
                            )
                        },
                        enabled = smsPasteInput.isNotBlank(),
                        modifier = Modifier.testTag("parse_sms_button")
                    ) {
                        Text("Extract details")
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        amountText = filtered
                    }
                },
                label = { Text("Amount (${CurrencyFormatter.symbolFor(currencyCode)})") },
                placeholder = { Text("0.00") },
                prefix = { Text("${CurrencyFormatter.symbolFor(currencyCode)} ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input")
            )

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Store or merchant") },
                placeholder = { Text("e.g. Starbucks, Shell, 7-Eleven, Best Buy") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                trailingIcon = {
                    if (merchant.isNotBlank()) {
                        IconButton(
                            onClick = {
                                onSuggestCategory(merchant, rawSnippet.ifBlank { null }) { cat ->
                                    selectedCategory = cat
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoFixHigh,
                                contentDescription = "Match store category",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("merchant_input")
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Payment source",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentSource.entries.forEach { source ->
                        FilterChip(
                            selected = selectedSource == source,
                            onClick = { selectedSource = source },
                            label = { Text(source.displayName) },
                            leadingIcon = {
                                Icon(
                                    imageVector = source.icon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpendingCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.shortLabel) },
                            leadingIcon = {
                                Icon(
                                    imageVector = cat.icon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date: ${LedgerAnalytics.formatDateTime(occurredAtMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { showDatePicker = true }) {
                    Text("Change date")
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(300) },
                label = { Text("Note (optional)") },
                placeholder = { Text("Cash receipt, lunch split, etc.") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Remember store category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Auto-assign future alerts from this store",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = rememberRule,
                    onCheckedChange = { rememberRule = it }
                )
            }

            if (!validationError.isNullOrBlank()) {
                Text(
                    text = validationError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    val cleanMerchant = merchant.trim()
                    val parsedDouble = amountText.toDoubleOrNull()
                    val cents = parsedDouble?.let { (it * 100.0).roundToLong() } ?: 0L
                    when {
                        cleanMerchant.isEmpty() -> validationError = "Please enter a store or merchant name."
                        cents <= 0L -> validationError = "Please enter an amount greater than zero."
                        else -> {
                            onSave(
                                editingTransaction?.id,
                                cleanMerchant,
                                cents,
                                selectedCategory,
                                selectedSource,
                                note.trim(),
                                rawSnippet.trim().takeIf { it.isNotEmpty() },
                                occurredAtMillis,
                                rememberRule
                            )
                            onDismiss()
                        }
                    }
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_transaction_button")
            ) {
                Text(
                    text = if (editingTransaction == null) "Save Spending" else "Confirm & Update",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = occurredAtMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { picked ->
                            occurredAtMillis = picked + 12 * 3_600_000L
                        }
                        showDatePicker = false
                    }
                ) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun AddRecurringPresetDialog(
    currencyCode: String,
    onSave: (label: String, merchant: String, amountCents: Long, category: SpendingCategory, source: PaymentSource) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(SpendingCategory.FOOD) }
    var source by remember { mutableStateOf(PaymentSource.GOOGLE_PAY) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Quick-Add Expense") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(40) },
                    label = { Text("Button label") },
                    placeholder = { Text("e.g. Morning Coffee") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it.take(120) },
                    label = { Text("Store") },
                    placeholder = { Text("e.g. Starbucks") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) amountText = filtered
                    },
                    label = { Text("Amount (${CurrencyFormatter.symbolFor(currencyCode)})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpendingCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.shortLabel) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentSource.entries.forEach { src ->
                        FilterChip(
                            selected = source == src,
                            onClick = { source = src },
                            label = { Text(src.shortLabel) }
                        )
                    }
                }
                if (!errorText.isNullOrBlank()) {
                    Text(
                        text = errorText.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cents = amountText.toDoubleOrNull()?.let { (it * 100.0).roundToLong() } ?: 0L
                    when {
                        label.isBlank() -> errorText = "Enter a short button label."
                        merchant.isBlank() -> errorText = "Enter a store name."
                        cents <= 0L -> errorText = "Enter a valid amount."
                        else -> {
                            onSave(label.trim(), merchant.trim(), cents, category, source)
                            onDismiss()
                        }
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
