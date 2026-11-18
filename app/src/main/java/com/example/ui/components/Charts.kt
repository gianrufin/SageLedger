package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyFormatter
import com.example.data.model.SavingsProjection
import com.example.data.model.TrendBucket
import com.example.ui.theme.TabularAmountStyle

@Composable
fun SavingsProjectionChart(
    projection: SavingsProjection,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(projection) { revealed = true }
    val progress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "projectionProgress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val projectedColor = if (projection.projectedSavingsCents >= 0L) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.error
    }
    val budgetLineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
        ) {
            val w = size.width
            val h = size.height
            val padTop = 10.dp.toPx()
            val padBottom = 10.dp.toPx()
            val chartH = (h - padTop - padBottom).coerceAtLeast(1f)

            val maxSpend = maxOf(
                projection.projectedMonthEndSpendCents,
                projection.monthlyBudgetCents,
                projection.monthlyIncomeCents,
                10_000L
            ).toFloat()

            fun yFor(cents: Long): Float {
                val frac = (cents.toFloat() / maxSpend).coerceIn(0f, 1.05f)
                return padTop + chartH * (1f - frac)
            }

            // Subtle horizontal reference lines
            listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                val y = padTop + chartH * frac
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Monthly budget target line
            if (projection.monthlyBudgetCents > 0L) {
                val budgetY = yFor(projection.monthlyBudgetCents)
                drawLine(
                    color = budgetLineColor,
                    start = Offset(0f, budgetY),
                    end = Offset(w, budgetY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }

            val totalDays = projection.daysInMonth.coerceAtLeast(2)
            val currentIdx = (projection.currentDayOfMonth - 1).coerceIn(0, totalDays - 1)
            val stepX = w / (totalDays - 1).toFloat()

            // Projected path (dashed from today -> end of month)
            if (projection.projectedDailyCents.size >= totalDays) {
                val projPath = Path()
                for (i in currentIdx until totalDays) {
                    val x = i * stepX
                    val y = yFor(projection.projectedDailyCents[i])
                    if (i == currentIdx) projPath.moveTo(x, y) else projPath.lineTo(x, y)
                }
                drawPath(
                    path = projPath,
                    color = projectedColor,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f))
                    )
                )
            }

            // Actual cumulative spend path + soft fill
            val actualPoints = projection.actualCumulativeDailyCents
            if (actualPoints.isNotEmpty()) {
                val visibleCount = ((actualPoints.size) * progress).toInt().coerceAtLeast(1)
                val linePath = Path()
                val fillPath = Path()
                for (i in 0 until visibleCount) {
                    val x = i * stepX
                    val y = yFor(actualPoints[i])
                    if (i == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, h - padBottom)
                        fillPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                val lastX = (visibleCount - 1) * stepX
                val lastY = yFor(actualPoints[visibleCount - 1])
                fillPath.lineTo(lastX, h - padBottom)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    )
                )
                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Today marker dot
                drawCircle(
                    color = primaryColor.copy(alpha = 0.24f),
                    radius = 7.dp.toPx(),
                    center = Offset(lastX, lastY)
                )
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = Offset(lastX, lastY)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = primaryColor, label = "Spent (${CurrencyFormatter.formatCompactCents(projection.spentToDateCents, currencyCode)})")
            LegendDot(color = projectedColor, label = "Proj. end (${CurrencyFormatter.formatCompactCents(projection.projectedMonthEndSpendCents, currencyCode)})")
            LegendDot(color = budgetLineColor, label = "Budget (${CurrencyFormatter.formatCompactCents(projection.monthlyBudgetCents, currencyCode)})")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(8.dp)
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SpendingTrendBarChart(
    buckets: List<TrendBucket>,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(buckets) {
        revealed = false
        revealed = true
    }
    val heightFactor by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "barGrowth"
    )

    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val maxCents = buckets.maxOfOrNull { it.amountCents }?.coerceAtLeast(100L) ?: 100L

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            if (buckets.isEmpty()) return@Canvas
            val count = buckets.size
            val slotWidth = size.width / count.toFloat()
            val barWidth = (slotWidth * 0.52f).coerceAtMost(28.dp.toPx())
            val maxBarH = size.height - 8.dp.toPx()

            buckets.forEachIndexed { idx, bucket ->
                val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                // Subtle background slot track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(x, 4.dp.toPx()),
                    size = Size(barWidth, maxBarH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                if (bucket.amountCents > 0L) {
                    val ratio = (bucket.amountCents.toFloat() / maxCents.toFloat()).coerceIn(0.06f, 1f)
                    val barH = maxBarH * ratio * heightFactor
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, 4.dp.toPx() + (maxBarH - barH)),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            buckets.forEach { bucket ->
                Text(
                    text = bucket.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val peak = buckets.maxByOrNull { it.amountCents }
        if (peak != null && peak.amountCents > 0L) {
            Text(
                text = "Peak (${peak.label}): ${CurrencyFormatter.formatCents(peak.amountCents, currencyCode)}",
                style = TabularAmountStyle.copy(
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
