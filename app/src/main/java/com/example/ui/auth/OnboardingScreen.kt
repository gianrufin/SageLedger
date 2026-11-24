package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HeroBudgetGaugeRing
import com.example.ui.theme.LocalLedgerExtraColors
import kotlinx.coroutines.launch

private data class OnboardingStep(
    val stepLabel: String,
    val title: String,
    val description: String,
    val pillLabel: String,
    val cardTitle: String,
    val highlights: List<Pair<ImageVector, String>>,
    val showGaugePreview: Boolean = false
)

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalLedgerExtraColors.current
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val steps = listOf(
        OnboardingStep(
            stepLabel = "01 / 03",
            title = "Automatic phone\nnotification scan.",
            description = "SageLedger reads payment notifications from Google Wallet, GCash, Maya, GrabPay, PayPal, and bank SMS alerts automatically.",
            pillLabel = "Auto Capture",
            cardTitle = "Every payment alert, organized",
            highlights = listOf(
                Icons.Rounded.QrCode2 to "GCash, Maya & GrabPay e-wallets",
                Icons.Rounded.AccountBalanceWallet to "Google Wallet tap-to-pay receipts",
                Icons.Rounded.Sms to "Bank card & SMS transaction alerts"
            )
        ),
        OnboardingStep(
            stepLabel = "02 / 03",
            title = "Quick manual entry\nfor cash spending.",
            description = "Bought something with cash or split a bill? Record cash transactions in two taps, or paste an SMS snippet anytime.",
            pillLabel = "Cash & Shortcuts",
            cardTitle = "Built for everyday cash too",
            highlights = listOf(
                Icons.Rounded.Payments to "Fast cash & store category logging",
                Icons.Rounded.Bolt to "1-tap shortcuts for daily commutes & coffee",
                Icons.Rounded.Savings to "Custom merchant rules for instant sorting"
            )
        ),
        OnboardingStep(
            stepLabel = "03 / 03",
            title = "Clear monthly\nbudget clarity.",
            description = "Stay ahead of month-end with your live spending ring, safe daily allowance, and no-spend day streaks.",
            pillLabel = "Monthly Budget",
            cardTitle = "Your pace at a glance",
            highlights = listOf(
                Icons.Rounded.Savings to "Daily safe-to-spend allowance",
                Icons.Rounded.Bolt to "Projected month-end savings"
            ),
            showGaugePreview = true
        )
    )

    val ctaBg = if (extraColors.isDark) {
        MaterialTheme.colorScheme.primary
    } else {
        extraColors.contrastPillBg
    }
    val ctaContent = if (extraColors.isDark) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        extraColors.contrastPillText
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar: Step indicator + Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.8.dp, extraColors.hairlineBorder)
                ) {
                    Text(
                        text = steps[pagerState.currentPage].stepLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                TextButton(
                    onClick = onFinishOnboarding,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { idx ->
                val step = steps[idx]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 34.sp,
                                lineHeight = 40.sp,
                                letterSpacing = (-1.0).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Signature Charcoal Feature Card
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = extraColors.heroCardBg,
                        border = BorderStroke(0.8.dp, extraColors.heroCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.White
                                    ) {
                                        Text(
                                            text = step.pillLabel,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color(0xFF181C20),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }
                                    Text(
                                        text = step.cardTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = extraColors.heroCardInk
                                    )
                                }

                                if (step.showGaugePreview) {
                                    HeroBudgetGaugeRing(
                                        percentage = 68,
                                        isOverBudget = false,
                                        centerLabel = "On Track",
                                        size = 94.dp
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                step.highlights.forEach { (icon, text) ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = extraColors.heroCardChipBg,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = Color(0xFF4ADE80),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = extraColors.heroCardInk
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Bottom progress bars & CTA button
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { idx ->
                        val selected = pagerState.currentPage == idx
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(6.dp)
                                .width(if (selected) 26.dp else 8.dp)
                                .background(
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onFinishOnboarding()
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ctaBg,
                        contentColor = ctaContent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("onboarding_next_button")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (pagerState.currentPage < 2) "Continue" else "Get Started",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
