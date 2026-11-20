package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

fun Modifier.springPressScale(pressedScale: Float = 0.96f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
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
 * Displays a currency amount with editorial Outfit split-decimal contrast:
 * major digits ExtraBold in primary ink, fractional `.cents` softer Medium weight.
 */
@Composable
fun SplitCurrencyText(
    amountCents: Long,
    currencyCode: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TabularAmountStyle.copy(fontSize = 40.sp),
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
    shape: Shape = RoundedCornerShape(22.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = LocalLedgerExtraColors.current.hairlineBorder,
    borderWidth: Dp = 1.dp,
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
    if (!transaction.needsReview) {
        TransactionRowCard(
            transaction = transaction,
            currencyCode = currencyCode,
            onClick = onClick,
            modifier = modifier
        )
        return
    }

    val extraColors = LocalLedgerExtraColors.current
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
                    .clip(RoundedCornerShape(20.dp))
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
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Confirm purchase",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                }
            }
        },
        modifier = modifier
    ) {
        TransactionRowCard(
            transaction = transaction,
            currencyCode = currencyCode,
            onClick = onClick
        )
    }
}

@Composable
fun TransactionRowCard(
    transaction: SpendingTransaction,
    currencyCode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = transaction.categoryEnum
    val source = transaction.paymentSourceEnum
    val extraColors = LocalLedgerExtraColors.current

    CrispCard(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        borderColor = if (transaction.needsReview) {
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f)
        } else {
            extraColors.hairlineBorder
        },
        modifier = modifier
            .fillMaxWidth()
            .springPressScale(0.985f)
            .testTag("transaction_row_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = category.chartColor.copy(alpha = 0.13f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = category.icon(),
                        contentDescription = category.displayName,
                        tint = category.chartColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (transaction.needsReview) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "Swipe →",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = category.chartColor.copy(alpha = 0.11f)
                    ) {
                        Text(
                            text = category.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = category.chartColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = source.icon(),
                            contentDescription = source.displayName,
                            tint = if (source == PaymentSource.E_WALLET) {
                                extraColors.eWalletTint
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = source.shortLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "-${CurrencyFormatter.formatCents(transaction.amountCents, currencyCode)}",
                    style = TabularAmountStyle.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = extraColors.expenseCoral
                    )
                )
                Text(
                    text = LedgerAnalytics.formatDateTime(transaction.occurredAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
