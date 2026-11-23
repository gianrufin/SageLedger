package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalConvenienceStore
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyFormatter
import com.example.core.LedgerAnalytics
import com.example.data.model.PaymentSource
import com.example.data.model.SpendingCategory
import com.example.data.model.SpendingTransaction
import com.example.ui.theme.LocalLedgerExtraColors
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.TabularAmountStyle

fun SpendingCategory.icon(): ImageVector = when (this) {
    SpendingCategory.FOOD -> Icons.Rounded.Restaurant
    SpendingCategory.GROCERIES -> Icons.Rounded.LocalGroceryStore
    SpendingCategory.TRANSPORT_FUEL -> Icons.Rounded.DirectionsCar
    SpendingCategory.CONVENIENCE -> Icons.Rounded.LocalConvenienceStore
    SpendingCategory.GADGETS -> Icons.Rounded.Devices
    SpendingCategory.UTILITIES -> Icons.Rounded.Bolt
    SpendingCategory.SHOPPING -> Icons.Rounded.ShoppingBag
    SpendingCategory.OTHER -> Icons.AutoMirrored.Rounded.ReceiptLong
}

fun PaymentSource.icon(): ImageVector = when (this) {
    PaymentSource.GOOGLE_PAY -> Icons.Rounded.AccountBalanceWallet
    PaymentSource.E_WALLET -> Icons.Rounded.QrCode2
    PaymentSource.CREDIT_CARD -> Icons.Rounded.CreditCard
    PaymentSource.SMS_ALERT -> Icons.Rounded.Sms
    PaymentSource.CASH -> Icons.Rounded.Payments
    PaymentSource.MANUAL -> Icons.Rounded.Edit
}

fun Modifier.springPressScale(pressedScale: Float = 0.97f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                }
            )
        }
}

/**
 * Displays currency with editorial Outfit split-decimal contrast:
 * major digits ExtraBold in primary ink, fractional `.cents` softer Medium weight.
 */
@Composable
fun SplitCurrencyText(
    amountCents: Long,
    currencyCode: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TabularAmountStyle.copy(fontSize = 44.sp),
    color: Color = MaterialTheme.colorScheme.onSurface,
    fractionAlpha: Float = 0.45f,
    showPlusSign: Boolean = false,
    prefix: String = ""
) {
    val formatted = remember(amountCents, currencyCode, showPlusSign, prefix) {
        prefix + CurrencyFormatter.formatCents(amountCents, currencyCode, showPlusSign = showPlusSign)
    }
    val dotIdx = formatted.lastIndexOf('.')
    val annotated = remember(formatted, color, fractionAlpha, style) {
        buildAnnotatedString {
            if (dotIdx in 1 until formatted.length - 1) {
                withStyle(SpanStyle(color = color, fontWeight = FontWeight.ExtraBold)) {
                    append(formatted.substring(0, dotIdx))
                }
                withStyle(
                    SpanStyle(
                        color = color.copy(alpha = fractionAlpha),
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append(formatted.substring(dotIdx))
                }
            } else {
                withStyle(SpanStyle(color = color, fontWeight = FontWeight.ExtraBold)) {
                    append(formatted)
                }
            }
        }
    }
    Text(
        text = annotated,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun CrispCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = LocalLedgerExtraColors.current.hairlineBorder,
    borderWidth: Dp = 0.9.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = containerColor,
            border = BorderStroke(borderWidth, borderColor),
            modifier = modifier
        ) {
            Column(content = content)
        }
    } else {
        Surface(
            shape = shape,
            color = containerColor,
            border = BorderStroke(borderWidth, borderColor),
            modifier = modifier
        ) {
            Column(content = content)
        }
    }
}

/**
 * Clean ledger transaction row designed to live inside a unified daily sheet
 * with hairline dividers rather than floating individual card boxes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerTransactionRow(
    transaction: SpendingTransaction,
    currencyCode: String,
    onClick: () -> Unit,
    onSwipeConfirm: (SpendingTransaction) -> Unit,
    showBottomDivider: Boolean = true,
    modifier: Modifier = Modifier,
    dismissOutOnConfirm: Boolean = false
) {
    val extraColors = LocalLedgerExtraColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        if (!transaction.needsReview) {
            LedgerRowContent(
                transaction = transaction,
                currencyCode = currencyCode,
                onClick = onClick
            )
        } else {
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.StartToEnd) {
                        onSwipeConfirm(transaction)
                        dismissOutOnConfirm
                    } else {
                        false
                    }
                }
            )
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = true,
                enableDismissFromEndToStart = false,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(extraColors.positiveGreen)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Confirm alert",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Confirm purchase",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            ) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    LedgerRowContent(
                        transaction = transaction,
                        currencyCode = currencyCode,
                        onClick = onClick
                    )
                }
            }
        }

        if (showBottomDivider) {
            HorizontalDivider(
                thickness = 0.75.dp,
                color = extraColors.hairlineBorder,
                modifier = Modifier.padding(start = 66.dp, end = 16.dp)
            )
        }
    }
}

@Composable
private fun LedgerRowContent(
    transaction: SpendingTransaction,
    currencyCode: String,
    onClick: () -> Unit
) {
    val category = transaction.categoryEnum
    val source = transaction.paymentSourceEnum
    val extraColors = LocalLedgerExtraColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("transaction_row_${transaction.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(40.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(0.8.dp, extraColors.hairlineBorder),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = category.icon(),
                        contentDescription = category.displayName,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            if (transaction.needsReview) {
                Surface(
                    shape = CircleShape,
                    color = extraColors.expenseCoral,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(10.dp)
                ) {}
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = transaction.merchant,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val metaText = buildString {
                append(category.shortLabel)
                append(" · ")
                append(source.shortLabel)
                if (transaction.needsReview) append(" · Swipe to confirm")
            }
            Text(
                text = metaText,
                style = MaterialTheme.typography.bodySmall,
                color = if (transaction.needsReview) {
                    extraColors.expenseCoral
                } else if (source == PaymentSource.E_WALLET) {
                    extraColors.eWalletTint
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "-${CurrencyFormatter.formatCents(transaction.amountCents, currencyCode)}",
                style = TabularAmountStyle.copy(
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1
            )
            Text(
                text = LedgerAnalytics.formatDateTime(transaction.occurredAtMillis),
                style = MonoCodeStyle.copy(fontSize = 10.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTransactionRowCard(
    transaction: SpendingTransaction,
    currencyCode: String,
    onClick: () -> Unit,
    onSwipeConfirm: (SpendingTransaction) -> Unit,
    modifier: Modifier = Modifier,
    dismissOutOnConfirm: Boolean = false
) {
    CrispCard(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        LedgerTransactionRow(
            transaction = transaction,
            currencyCode = currencyCode,
            onClick = onClick,
            onSwipeConfirm = onSwipeConfirm,
            showBottomDivider = false,
            dismissOutOnConfirm = dismissOutOnConfirm
        )
    }
}
