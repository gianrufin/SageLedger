package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyFormatter
import com.example.data.model.CategoryBreakdownItem
import com.example.data.model.SavingsProjection
import com.example.data.model.TrendBucket
import com.example.ui.theme.LocalLedgerExtraColors
import com.example.ui.theme.TabularAmountStyle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private data class DonutSegmentGeometry(
    val index: Int,
    val item: CategoryBreakdownItem?,
    val startAngle: Float,
    val sweepAngle: Float,
    val midAngle: Float,
    val isRemainder: Boolean
)

@Composable
fun CategoryDonutChart(
    items: List<CategoryBreakdownItem>,
    totalSpentCents: Long,
    currencyCode: String,
    modifier: Modifier = Modifier,
    budgetCapCents: Long? = null,
    selectedCategoryIndex: Int? = null,
    onSelectCategoryIndex: (Int?) -> Unit = {}
) {
    val nonZero = remember(items) { items.filter { it.spentCents > 0L } }
    if (nonZero.isEmpty() || totalSpentCents <= 0L) return

    val remainderCents = remember(budgetCapCents, totalSpentCents) {
        val cap = budgetCapCents ?: 0L
        if (cap > totalSpentCents) cap - totalSpentCents else 0L
    }
    val hasRemainder = remainderCents > 0L
    val totalRingCents = totalSpentCents + remainderCents

    var revealed by remember(nonZero) { mutableStateOf(false) }
    LaunchedEffect(nonZero) { revealed = true }
    val sweepReveal by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.05f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
        label = "donutReveal"
    )

    val extraColors = LocalLedgerExtraColors.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val hatchBase = MaterialTheme.colorScheme.surfaceContainerHigh
    val hatchStripe = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val defaultIdx = 0
    val activeIdx = selectedCategoryIndex?.takeIf { it in nonZero.indices } ?: defaultIdx
    val activeItem = nonZero.getOrNull(activeIdx)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(268.dp),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val diameterDp = minOf(maxWidth, 236.dp)
        val diameterPx = with(density) { diameterDp.toPx() }
        val strokeWidthDp = 28.dp
        val strokeWidthPx = with(density) { strokeWidthDp.toPx() }
        val radiusPx = (diameterPx - strokeWidthPx) / 2f

        val segments = remember(nonZero, remainderCents, radiusPx, strokeWidthPx) {
            val totalSlices = nonZero.size + if (hasRemainder) 1 else 0
            val capMarginDeg = if (radiusPx > 0f) {
                ((strokeWidthPx * 1.18f) / radiusPx) * (180f / PI.toFloat())
            } else {
                12f
            }
            val gapPerSlice = if (totalSlices > 1) capMarginDeg.coerceIn(10f, 22f) else 0f
            val totalGap = gapPerSlice * totalSlices
            val availableSweep = (360f - totalGap).coerceAtLeast(180f)

            val built = mutableListOf<DonutSegmentGeometry>()
            var cursor = -84f // slight tilt just like the playful mockup
            nonZero.forEachIndexed { idx, cat ->
                val rawFrac = cat.spentCents.toFloat() / totalRingCents.toFloat()
                val sweep = (rawFrac * availableSweep).coerceAtLeast(2f)
                val mid = cursor + sweep / 2f
                built.add(
                    DonutSegmentGeometry(
                        index = idx,
                        item = cat,
                        startAngle = cursor,
                        sweepAngle = sweep,
                        midAngle = mid,
                        isRemainder = false
                    )
                )
                cursor += sweep + gapPerSlice
            }
            if (hasRemainder) {
                val remFrac = remainderCents.toFloat() / totalRingCents.toFloat()
                val sweep = (remFrac * availableSweep).coerceAtLeast(12f)
                built.add(
                    DonutSegmentGeometry(
                        index = -1,
                        item = null,
                        startAngle = cursor,
                        sweepAngle = sweep,
                        midAngle = cursor + sweep / 2f,
                        isRemainder = true
                    )
                )
            }
            built
        }

        val activeSeg = segments.firstOrNull { !it.isRemainder && it.index == activeIdx } ?: segments.first()
        val animatedCalloutAngle by animateFloatAsState(
            targetValue = activeSeg.midAngle,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
            label = "calloutAngle"
        )

        Box(
            modifier = Modifier.size(diameterDp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(segments) {
                        detectTapGestures { tap ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = tap.x - cx
                            val dy = tap.y - cy
                            val dist = hypot(dx, dy)
                            val innerR = radiusPx - strokeWidthPx * 0.85f
                            val outerR = radiusPx + strokeWidthPx * 0.85f
                            if (dist < innerR) {
                                onSelectCategoryIndex(null)
                                return@detectTapGestures
                            }
                            if (dist in innerR..outerR) {
                                var angle = (atan2(dy, dx) * (180f / PI.toFloat()))
                                while (angle < -84f) angle += 360f
                                while (angle >= 276f) angle -= 360f
                                val hit = segments.firstOrNull { seg ->
                                    var s = seg.startAngle
                                    while (s < -84f) s += 360f
                                    var a = angle
                                    if (a < s) a += 360f
                                    !seg.isRemainder && a in s..(s + seg.sweepAngle + 8f)
                                }
                                if (hit != null) {
                                    onSelectCategoryIndex(
                                        if (selectedCategoryIndex == hit.index) null else hit.index
                                    )
                                }
                            }
                        }
                    }
            ) {
                val inset = strokeWidthPx / 2f
                val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
                val arcTopLeft = Offset(inset, inset)

                // Subtle recessed ring underlayer
                drawCircle(
                    color = hatchBase.copy(alpha = 0.35f),
                    radius = radiusPx,
                    style = Stroke(width = strokeWidthPx)
                )

                segments.forEach { seg ->
                    val animatedSweep = (seg.sweepAngle * sweepReveal).coerceAtLeast(0.5f)
                    if (seg.isRemainder) {
                        // Draw diagonal hatched unspent budget arc
                        val outerR = radiusPx + strokeWidthPx / 2f
                        val innerR = radiusPx - strokeWidthPx / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val wedgePath = buildAnnularWedgePath(
                            center = center,
                            innerRadius = innerR,
                            outerRadius = outerR,
                            startAngleDeg = seg.startAngle,
                            sweepAngleDeg = animatedSweep
                        )
                        clipPath(wedgePath) {
                            drawRect(color = hatchBase.copy(alpha = 0.72f))
                            val step = 8.dp.toPx()
                            var x = -size.height
                            while (x < size.width + size.height) {
                                drawLine(
                                    color = hatchStripe,
                                    start = Offset(x, size.height),
                                    end = Offset(x + size.height, 0f),
                                    strokeWidth = 2.dp.toPx()
                                )
                                x += step
                            }
                        }
                    } else if (seg.item != null) {
                        val isSelected = seg.index == activeIdx
                        val extraStroke = if (isSelected && selectedCategoryIndex != null) 4.dp.toPx() else 0f
                        val sliceColor = if (selectedCategoryIndex != null && !isSelected) {
                            seg.item.category.chartColor.copy(alpha = 0.42f)
                        } else {
                            seg.item.category.chartColor
                        }
                        drawArc(
                            color = sliceColor,
                            startAngle = seg.startAngle,
                            sweepAngle = animatedSweep,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(
                                width = strokeWidthPx + extraStroke,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
            }

            // Center figure
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .padding(horizontal = 36.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelectCategoryIndex(null) }
            ) {
                val showingSelected = selectedCategoryIndex != null && activeItem != null
                Text(
                    text = if (showingSelected) activeItem.category.shortLabel else "Total Expenses",
                    style = MaterialTheme.typography.labelMedium,
                    color = onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                SplitCurrencyText(
                    amountCents = if (showingSelected) activeItem.spentCents else totalSpentCents,
                    currencyCode = currencyCode,
                    style = TabularAmountStyle.copy(fontSize = 23.sp),
                    color = onSurface
                )
            }

            // Floating dark % pill anchored along the active slice's outer arc
            if (activeItem != null) {
                val pct = (activeItem.shareFraction * 100f).roundToInt().coerceIn(1, 100)
                val badgeRadiusPx = radiusPx + strokeWidthPx * 0.56f
                val rad = Math.toRadians(animatedCalloutAngle.toDouble())
                val offsetX = (cos(rad) * badgeRadiusPx).roundToInt()
                val offsetY = (sin(rad) * badgeRadiusPx).roundToInt()

                Surface(
                    shape = RoundedCornerShape(50),
                    color = extraColors.contrastPillBg,
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(2.dp, surfaceColor),
                    modifier = Modifier.offset { IntOffset(offsetX, offsetY) }
                ) {
                    Text(
                        text = "$pct%",
                        style = TabularAmountStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = extraColors.contrastPillText
                        ),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private fun buildAnnularWedgePath(
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    startAngleDeg: Float,
    sweepAngleDeg: Float
): Path {
    val path = Path()
    val outerRect = Rect(
        left = center.x - outerRadius,
        top = center.y - outerRadius,
        right = center.x + outerRadius,
        bottom = center.y + outerRadius
    )
    val innerRect = Rect(
        left = center.x - innerRadius,
        top = center.y - innerRadius,
        right = center.x + innerRadius,
        bottom = center.y + innerRadius
    )
    path.arcTo(outerRect, startAngleDeg, sweepAngleDeg, forceMoveTo = true)
    path.arcTo(innerRect, startAngleDeg + sweepAngleDeg, -sweepAngleDeg, forceMoveTo = false)
    path.close()
    return path
}

/**
 * Signature capsule progress bar from the Goals / Plan mockup:
 * Vivid fill bar + crisp thumb dot + diagonal-hatched unfilled remainder track.
 */
@Composable
fun HatchedGoalProgressBar(
    fraction: Float,
    fillColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 14.dp
) {
    val clamped = fraction.coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = clamped,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
        label = "hatchedProgress"
    )
    val trackBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val hatchColor = fillColor.copy(alpha = 0.34f)
    val thumbCenterColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height + 6.dp)
    ) {
        val barH = height.toPx()
        val topY = (size.height - barH) / 2f
        val barRectPath = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 0f,
                    top = topY,
                    right = size.width,
                    bottom = topY + barH,
                    cornerRadius = CornerRadius(barH / 2f, barH / 2f)
                )
            )
        }

        // Hatched unfilled track
        clipPath(barRectPath) {
            drawRect(
                color = trackBg.copy(alpha = 0.65f),
                topLeft = Offset(0f, topY),
                size = Size(size.width, barH)
            )
            val stripeStep = 7.dp.toPx()
            var x = -barH
            while (x < size.width + barH) {
                drawLine(
                    color = hatchColor,
                    start = Offset(x, topY + barH),
                    end = Offset(x + barH, topY),
                    strokeWidth = 2.dp.toPx()
                )
                x += stripeStep
            }
        }

        // Active solid fill bar
        val fillW = (size.width * animatedFraction).coerceAtLeast(if (animatedFraction > 0f) barH else 0f)
        if (fillW > 0f) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(0f, topY),
                size = Size(fillW, barH),
                cornerRadius = CornerRadius(barH / 2f, barH / 2f)
            )

            val knobX = fillW.coerceIn(barH / 2f, size.width - barH / 2f)
            val center = Offset(knobX, size.height / 2f)
            drawCircle(
                color = thumbCenterColor,
                radius = barH * 0.56f,
                center = center
            )
            drawCircle(
                color = fillColor,
                radius = barH * 0.56f,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }
}

/**
 * Compact circular ring gauge ("55%", "25%", "65%") used on category budget rows.
 */
@Composable
fun CircularPercentRing(
    percentage: Int,
    ringColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val targetFrac = (percentage.coerceIn(0, 100)) / 100f
    val animFrac by animateFloatAsState(
        targetValue = targetFrac,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "ringSweep"
    )
    val trackColor = ringColor.copy(alpha = 0.16f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 4.5.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke)
            )
            if (animFrac > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animFrac,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = "${percentage.coerceAtMost(999)}%",
            style = TabularAmountStyle.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

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

    val extraColors = LocalLedgerExtraColors.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val projectedColor = if (projection.projectedSavingsCents >= 0L) {
        extraColors.positiveGreen
    } else {
        extraColors.expenseCoral
    }
    val budgetLineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp)
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

            listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                val y = padTop + chartH * frac
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

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
                        width = 2.75.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f))
                    )
                )
            }

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
                            primaryColor.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(
                        width = 3.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                drawCircle(
                    color = primaryColor.copy(alpha = 0.24f),
                    radius = 8.dp.toPx(),
                    center = Offset(lastX, lastY)
                )
                drawCircle(
                    color = thumbColorFix(surfaceColor = Color.White),
                    radius = 4.5.dp.toPx(),
                    center = Offset(lastX, lastY)
                )
                drawCircle(
                    color = primaryColor,
                    radius = 4.5.dp.toPx(),
                    center = Offset(lastX, lastY),
                    style = Stroke(width = 2.2.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = primaryColor, label = "Spent ${CurrencyFormatter.formatCompactCents(projection.spentToDateCents, currencyCode)}")
            LegendDot(color = projectedColor, label = "Proj. ${CurrencyFormatter.formatCompactCents(projection.projectedMonthEndSpendCents, currencyCode)}")
            LegendDot(color = budgetLineColor, label = "Limit ${CurrencyFormatter.formatCompactCents(projection.monthlyBudgetCents, currencyCode)}")
        }
    }
}

private fun thumbColorFix(surfaceColor: Color): Color = surfaceColor

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
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
    var selectedIdx by remember(buckets) {
        mutableIntStateOf(
            buckets.indices.maxByOrNull { buckets[it].amountCents } ?: 0
        )
    }
    LaunchedEffect(buckets) {
        revealed = false
        revealed = true
    }
    val heightFactor by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "barGrowth"
    )

    val primaryBar = MaterialTheme.colorScheme.primary
    val secondaryBar = MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
    val maxCents = buckets.maxOfOrNull { it.amountCents }?.coerceAtLeast(100L) ?: 100L

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .pointerInput(buckets) {
                    detectTapGestures { offset ->
                        if (buckets.isNotEmpty()) {
                            val idx = ((offset.x / size.width) * buckets.size)
                                .toInt()
                                .coerceIn(0, buckets.lastIndex)
                            selectedIdx = idx
                        }
                    }
                }
        ) {
            if (buckets.isEmpty()) return@Canvas
            val count = buckets.size
            val slotWidth = size.width / count.toFloat()
            val barWidth = (slotWidth * 0.54f).coerceAtMost(30.dp.toPx())
            val maxBarH = size.height - 8.dp.toPx()

            buckets.forEachIndexed { idx, bucket ->
                val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(x, 4.dp.toPx()),
                    size = Size(barWidth, maxBarH),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
                if (bucket.amountCents > 0L) {
                    val ratio = (bucket.amountCents.toFloat() / maxCents.toFloat()).coerceIn(0.08f, 1f)
                    val barH = maxBarH * ratio * heightFactor
                    drawRoundRect(
                        color = if (idx == selectedIdx) primaryBar else secondaryBar,
                        topLeft = Offset(x, 4.dp.toPx() + (maxBarH - barH)),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            buckets.forEachIndexed { idx, bucket ->
                val isSelected = idx == selectedIdx
                Text(
                    text = bucket.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) primaryBar else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val focused = buckets.getOrNull(selectedIdx) ?: buckets.maxByOrNull { it.amountCents }
        if (focused != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "${focused.label}: ${CurrencyFormatter.formatCents(focused.amountCents, currencyCode)}",
                    style = TabularAmountStyle.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
