import { useEffect, useId, useMemo, useRef, useState } from 'react';
import {
  CATEGORY_META,
  type CategoryBreakdownItem,
  type SavingsProjection,
  type TrendBucket,
} from '../types/ledger';
import { formatCents, formatCompactCents } from '../lib/currency';
import { SplitCurrencyText } from './IconsAndVisuals';

export function LiveSpendSparklineChart({
  projection,
  currencyCode,
}: {
  projection: SavingsProjection;
  currencyCode: string;
}) {
  const gradId = useId().replace(/:/g, '');
  const svgRef = useRef<SVGSVGElement | null>(null);
  const [scrubDayIdx, setScrubDayIdx] = useState<number | null>(null);

  const isOverBudget =
    projection.monthlyBudgetCents > 0 &&
    projection.spentToDateCents > projection.monthlyBudgetCents;
  const lineColor = isOverBudget ? 'var(--coral-tertiary)' : 'var(--mint-primary)';
  const projectedColor =
    projection.projectedMonthEndSpendCents <= projection.monthlyBudgetCents ||
    projection.monthlyBudgetCents <= 0
      ? 'var(--sky-secondary)'
      : 'var(--coral-tertiary)';

  const totalDays = Math.max(2, projection.daysInMonth);
  const currentIdx = Math.max(0, Math.min(totalDays - 1, projection.currentDayOfMonth - 1));
  const actualPoints = projection.actualCumulativeDailyCents;
  const projectedPoints = projection.projectedDailyCents;

  const maxScaleCents = useMemo(() => {
    return (
      Math.max(
        projection.spentToDateCents,
        projection.projectedMonthEndSpendCents,
        projection.monthlyBudgetCents,
        1000
      ) * 1.08
    );
  }, [projection]);

  const W = 560;
  const H = 120;
  const padTop = 12;
  const padBottom = 10;
  const plotH = H - padTop - padBottom;
  const stepX = W / Math.max(1, totalDays - 1);

  const yFor = (cents: number) => {
    const frac = Math.max(0, Math.min(1, cents / maxScaleCents));
    return padTop + plotH * (1 - frac);
  };

  const { actualLinePath, actualFillPath, lastX, lastY } = useMemo(() => {
    const calcY = (cents: number) => {
      const frac = Math.max(0, Math.min(1, cents / maxScaleCents));
      return padTop + plotH * (1 - frac);
    };
    if (actualPoints.length === 0) {
      return { actualLinePath: '', actualFillPath: '', lastX: 0, lastY: H - padBottom };
    }
    let lineD = '';
    let fillD = '';
    for (let i = 0; i < actualPoints.length; i += 1) {
      const x = i * stepX;
      const y = calcY(actualPoints[i]);
      if (i === 0) {
        lineD += `M ${x} ${y}`;
        fillD += `M ${x} ${H - padBottom} L ${x} ${y}`;
      } else {
        const prevX = (i - 1) * stepX;
        const prevY = calcY(actualPoints[i - 1]);
        const midX = (prevX + x) / 2;
        lineD += ` C ${midX} ${prevY}, ${midX} ${y}, ${x} ${y}`;
        fillD += ` C ${midX} ${prevY}, ${midX} ${y}, ${x} ${y}`;
      }
    }
    const lx = (actualPoints.length - 1) * stepX;
    const ly = calcY(actualPoints[actualPoints.length - 1]);
    fillD += ` L ${lx} ${H - padBottom} Z`;
    return { actualLinePath: lineD, actualFillPath: fillD, lastX: lx, lastY: ly };
  }, [actualPoints, stepX, maxScaleCents, plotH]);

  const projectedPath = useMemo(() => {
    const calcY = (cents: number) => {
      const frac = Math.max(0, Math.min(1, cents / maxScaleCents));
      return padTop + plotH * (1 - frac);
    };
    if (projectedPoints.length < totalDays || currentIdx >= totalDays - 1) return '';
    let d = '';
    for (let i = currentIdx; i < totalDays; i += 1) {
      const x = i * stepX;
      const y = calcY(projectedPoints[i]);
      if (i === currentIdx) {
        d += `M ${x} ${y}`;
      } else {
        const prevX = (i - 1) * stepX;
        const prevY = calcY(projectedPoints[i - 1]);
        const midX = (prevX + x) / 2;
        d += ` C ${midX} ${prevY}, ${midX} ${y}, ${x} ${y}`;
      }
    }
    return d;
  }, [projectedPoints, totalDays, currentIdx, stepX, maxScaleCents, plotH]);

  const handlePointer = (clientX: number) => {
    const svg = svgRef.current;
    if (!svg) return;
    const rect = svg.getBoundingClientRect();
    const rel = Math.max(0, Math.min(1, (clientX - rect.left) / Math.max(1, rect.width)));
    const idx = Math.max(0, Math.min(totalDays - 1, Math.round(rel * (totalDays - 1))));
    setScrubDayIdx(idx);
  };

  const inspectIdx = scrubDayIdx;
  const inspectIsFuture = inspectIdx !== null && inspectIdx > currentIdx;
  const inspectCents =
    inspectIdx !== null
      ? inspectIsFuture
        ? (projectedPoints[inspectIdx] ?? projection.projectedMonthEndSpendCents)
        : (actualPoints[inspectIdx] ?? projection.spentToDateCents)
      : null;

  return (
    <div className="w-full space-y-2" data-testid="live_spend_sparkline">
      <div className="flex items-center justify-between gap-2 min-h-[24px]">
        {inspectIdx !== null && inspectCents !== null ? (
          <div
            className="inline-flex items-center px-2.5 py-1 rounded-full bg-[var(--bg-surface-variant)] border text-[11.5px] font-bold tabular-nums text-[var(--ink-primary)]"
            style={{ borderColor: lineColor }}
          >
            {inspectIsFuture ? 'Proj. ' : ''}Day {inspectIdx + 1} ·{' '}
            {formatCents(inspectCents, currencyCode)}
          </div>
        ) : (
          <div className="flex items-center gap-3 text-[11px] text-[var(--ink-muted)]">
            <span className="inline-flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full" style={{ backgroundColor: lineColor }} />
              Actual
            </span>
            <span className="inline-flex items-center gap-1.5">
              <span
                className="w-2 h-2 rounded-full opacity-75"
                style={{ backgroundColor: projectedColor }}
              />
              Projected pace
            </span>
          </div>
        )}
        <span className="text-[11px] text-[var(--ink-muted)] opacity-75">
          Hover or tap curve to inspect
        </span>
      </div>

      <svg
        ref={svgRef}
        viewBox={`0 0 ${W} ${H}`}
        className="w-full h-[108px] touch-none select-none cursor-crosshair overflow-visible"
        onMouseMove={(e) => handlePointer(e.clientX)}
        onMouseLeave={() => setScrubDayIdx(null)}
        onClick={(e) => handlePointer(e.clientX)}
        onTouchMove={(e) => {
          if (e.touches[0]) handlePointer(e.touches[0].clientX);
        }}
        onTouchEnd={() => setScrubDayIdx(null)}
      >
        <defs>
          <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={lineColor} stopOpacity="0.28" />
            <stop offset="65%" stopColor={lineColor} stopOpacity="0.05" />
            <stop offset="100%" stopColor={lineColor} stopOpacity="0" />
          </linearGradient>
        </defs>

        {/* Budget cap dashed guide */}
        {projection.monthlyBudgetCents > 0 && (
          <line
            x1={0}
            x2={W}
            y1={yFor(projection.monthlyBudgetCents)}
            y2={yFor(projection.monthlyBudgetCents)}
            stroke="var(--ink-muted)"
            strokeOpacity="0.32"
            strokeWidth="1.2"
            strokeDasharray="6 6"
          />
        )}

        {/* Baseline */}
        <line
          x1={0}
          x2={W}
          y1={H - padBottom}
          y2={H - padBottom}
          stroke="var(--ink-muted)"
          strokeOpacity="0.24"
          strokeWidth="1"
        />

        {/* Projected curve */}
        {projectedPath && (
          <path
            d={projectedPath}
            fill="none"
            stroke={projectedColor}
            strokeOpacity="0.75"
            strokeWidth="2.2"
            strokeLinecap="round"
            strokeDasharray="8 6"
          />
        )}

        {/* Actual fill + spline */}
        {actualFillPath && <path d={actualFillPath} fill={`url(#${gradId})`} />}
        {actualLinePath && (
          <path
            d={actualLinePath}
            fill="none"
            stroke={lineColor}
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        )}

        {/* Current day node */}
        {actualPoints.length > 0 && (
          <>
            <circle cx={lastX} cy={lastY} r={9} fill={lineColor} fillOpacity="0.22" />
            <circle
              cx={lastX}
              cy={lastY}
              r={4.5}
              fill="var(--bg-surface)"
              stroke={lineColor}
              strokeWidth="2.4"
            />
          </>
        )}

        {/* Scrub crosshair */}
        {inspectIdx !== null && inspectCents !== null && (
          <>
            <line
              x1={inspectIdx * stepX}
              x2={inspectIdx * stepX}
              y1={padTop}
              y2={H - padBottom}
              stroke={inspectIsFuture ? projectedColor : lineColor}
              strokeOpacity="0.55"
              strokeWidth="1.4"
            />
            <circle
              cx={inspectIdx * stepX}
              cy={yFor(inspectCents)}
              r={8}
              fill={inspectIsFuture ? projectedColor : lineColor}
              fillOpacity="0.25"
            />
            <circle
              cx={inspectIdx * stepX}
              cy={yFor(inspectCents)}
              r={4.2}
              fill={inspectIsFuture ? projectedColor : lineColor}
            />
          </>
        )}
      </svg>

      <div className="flex items-center justify-between text-[10px] font-mono-data uppercase tracking-wider text-[var(--ink-muted)] opacity-80">
        <span>DAY 1</span>
        <span className="font-semibold" style={{ color: lineColor }}>
          TODAY · DAY {projection.currentDayOfMonth}
        </span>
        <span>DAY {totalDays}</span>
      </div>
    </div>
  );
}

function polarToCartesian(cx: number, cy: number, r: number, angleDeg: number) {
  const rad = (angleDeg * Math.PI) / 180;
  return {
    x: cx + r * Math.cos(rad),
    y: cy + r * Math.sin(rad),
  };
}

function describeArc(cx: number, cy: number, r: number, startAngle: number, sweepAngle: number) {
  const safeSweep = Math.min(359.5, Math.max(0.5, sweepAngle));
  const start = polarToCartesian(cx, cy, r, startAngle);
  const end = polarToCartesian(cx, cy, r, startAngle + safeSweep);
  const largeArcFlag = safeSweep > 180 ? 1 : 0;
  return `M ${start.x} ${start.y} A ${r} ${r} 0 ${largeArcFlag} 1 ${end.x} ${end.y}`;
}

export function CategoryDonutChart({
  items,
  totalSpentCents,
  currencyCode,
  budgetCapCents = null,
  selectedCategoryIndex,
  onSelectCategoryIndex,
}: {
  items: CategoryBreakdownItem[];
  totalSpentCents: number;
  currencyCode: string;
  budgetCapCents?: number | null;
  selectedCategoryIndex: number | null;
  onSelectCategoryIndex: (idx: number | null) => void;
}) {
  const hatchPatternId = useId().replace(/:/g, '');
  const nonZero = useMemo(() => items.filter((item) => item.spentCents > 0), [items]);

  const remainderCents = useMemo(() => {
    const cap = budgetCapCents ?? 0;
    return cap > totalSpentCents ? cap - totalSpentCents : 0;
  }, [budgetCapCents, totalSpentCents]);

  if (nonZero.length === 0 || totalSpentCents <= 0) return null;

  const hasRemainder = remainderCents > 0;
  const totalRingCents = totalSpentCents + remainderCents;
  const activeIdx =
    selectedCategoryIndex !== null && selectedCategoryIndex >= 0 && selectedCategoryIndex < nonZero.length
      ? selectedCategoryIndex
      : 0;
  const activeItem = nonZero[activeIdx];

  const size = 256;
  const cx = size / 2;
  const cy = size / 2;
  const strokeWidth = 26;
  const radius = 92;

  const totalSlices = nonZero.length + (hasRemainder ? 1 : 0);
  const capMarginDeg = ((strokeWidth * 1.18) / radius) * (180 / Math.PI);
  const gapPerSlice = totalSlices > 1 ? Math.max(10, Math.min(20, capMarginDeg)) : 0;
  const totalGap = gapPerSlice * totalSlices;
  const availableSweep = Math.max(180, 360 - totalGap);

  const segments: Array<{
    index: number;
    item: CategoryBreakdownItem | null;
    startAngle: number;
    sweepAngle: number;
    midAngle: number;
    isRemainder: boolean;
  }> = [];

  let cursor = -84;
  nonZero.forEach((cat, idx) => {
    const rawFrac = cat.spentCents / totalRingCents;
    const sweep = Math.max(2, rawFrac * availableSweep);
    segments.push({
      index: idx,
      item: cat,
      startAngle: cursor,
      sweepAngle: sweep,
      midAngle: cursor + sweep / 2,
      isRemainder: false,
    });
    cursor += sweep + gapPerSlice;
  });

  if (hasRemainder) {
    const remFrac = remainderCents / totalRingCents;
    const sweep = Math.max(12, remFrac * availableSweep);
    segments.push({
      index: -1,
      item: null,
      startAngle: cursor,
      sweepAngle: sweep,
      midAngle: cursor + sweep / 2,
      isRemainder: true,
    });
  }

  const activeSeg = segments.find((s) => !s.isRemainder && s.index === activeIdx) ?? segments[0];
  const badgePos = polarToCartesian(cx, cy, radius + strokeWidth * 0.62, activeSeg.midAngle);
  const pct = activeItem
    ? Math.max(1, Math.min(100, Math.round(activeItem.shareFraction * 100)))
    : 0;
  const showingSelected = selectedCategoryIndex !== null && Boolean(activeItem);

  return (
    <div className="relative flex items-center justify-center w-full py-2">
      <div className="relative w-[256px] h-[256px] flex items-center justify-center">
        <svg viewBox={`0 0 ${size} ${size}`} className="w-full h-full overflow-visible">
          <defs>
            <pattern
              id={hatchPatternId}
              width="8"
              height="8"
              patternTransform="rotate(45 0 0)"
              patternUnits="userSpaceOnUse"
            >
              <rect width="8" height="8" fill="var(--bg-track)" fillOpacity="0.65" />
              <line
                x1="0"
                y1="0"
                x2="0"
                y2="8"
                stroke="var(--ink-muted)"
                strokeOpacity="0.35"
                strokeWidth="3"
              />
            </pattern>
          </defs>

          <circle
            cx={cx}
            cy={cy}
            r={radius}
            fill="none"
            stroke="var(--bg-track)"
            strokeOpacity="0.35"
            strokeWidth={strokeWidth}
          />

          {segments.map((seg) => {
            if (seg.isRemainder) {
              return (
                <path
                  key="remainder"
                  d={describeArc(cx, cy, radius, seg.startAngle, seg.sweepAngle)}
                  fill="none"
                  stroke={`url(#${hatchPatternId})`}
                  strokeWidth={strokeWidth}
                  strokeLinecap="butt"
                />
              );
            }
            if (!seg.item) return null;
            const meta = CATEGORY_META[seg.item.category];
            const isSelected = seg.index === activeIdx;
            const dimmed = selectedCategoryIndex !== null && !isSelected;
            return (
              <path
                key={seg.item.category}
                d={describeArc(cx, cy, radius, seg.startAngle, seg.sweepAngle)}
                fill="none"
                stroke={meta.chartColor}
                strokeOpacity={dimmed ? 0.35 : 1}
                strokeWidth={
                  isSelected && selectedCategoryIndex !== null ? strokeWidth + 4 : strokeWidth
                }
                strokeLinecap="round"
                className="cursor-pointer transition-all duration-200"
                onClick={() =>
                  onSelectCategoryIndex(selectedCategoryIndex === seg.index ? null : seg.index)
                }
              />
            );
          })}
        </svg>

        {/* Center label */}
        <button
          type="button"
          onClick={() => onSelectCategoryIndex(null)}
          className="absolute inset-12 rounded-full flex flex-col items-center justify-center text-center px-3 cursor-pointer"
        >
          <span className="text-xs font-medium text-[var(--ink-muted)] truncate max-w-[130px]">
            {showingSelected && activeItem
              ? CATEGORY_META[activeItem.category].shortLabel
              : 'Total Expenses'}
          </span>
          <SplitCurrencyText
            amountCents={showingSelected && activeItem ? activeItem.spentCents : totalSpentCents}
            currencyCode={currencyCode}
            className="text-xl sm:text-2xl text-[var(--ink-primary)] mt-0.5"
          />
        </button>

        {/* Callout % badge */}
        {activeItem && (
          <div
            className="absolute -translate-x-1/2 -translate-y-1/2 px-2.5 py-0.5 rounded-full text-[11px] font-bold tabular-nums shadow-md border-2 border-[var(--bg-surface)] pointer-events-none transition-all duration-200"
            style={{
              left: `${(badgePos.x / size) * 100}%`,
              top: `${(badgePos.y / size) * 100}%`,
              backgroundColor: 'var(--contrast-pill-bg)',
              color: 'var(--contrast-pill-text)',
            }}
          >
            {pct}%
          </div>
        )}
      </div>
    </div>
  );
}

export function SpendingTrendBarChart({
  buckets,
  currencyCode,
}: {
  buckets: TrendBucket[];
  currencyCode: string;
}) {
  const defaultIdx = useMemo(() => {
    if (buckets.length === 0) return 0;
    let bestIdx = 0;
    let bestVal = -1;
    buckets.forEach((b, i) => {
      if (b.amountCents > bestVal) {
        bestVal = b.amountCents;
        bestIdx = i;
      }
    });
    return bestIdx;
  }, [buckets]);

  const [selectedIdx, setSelectedIdx] = useState<number>(defaultIdx);

  useEffect(() => {
    setSelectedIdx(defaultIdx);
  }, [defaultIdx]);

  if (buckets.length === 0) return null;

  const maxCents = Math.max(100, ...buckets.map((b) => b.amountCents));
  const activeBucket = buckets[selectedIdx] ?? buckets[0];

  return (
    <div className="w-full space-y-3">
      <div className="flex items-end gap-2 h-[168px] pt-3">
        {buckets.map((bucket, idx) => {
          const isSelected = idx === selectedIdx;
          const pct = Math.max(bucket.amountCents > 0 ? 8 : 3, (bucket.amountCents / maxCents) * 100);
          return (
            <button
              key={`${bucket.label}-${idx}`}
              type="button"
              onClick={() => setSelectedIdx(idx)}
              className="flex-1 h-full flex flex-col items-center justify-end gap-1.5 group cursor-pointer"
            >
              <div className="w-full max-w-[34px] flex-1 rounded-full bg-[var(--bg-track)]/45 flex items-end overflow-hidden p-0.5">
                <div
                  className="w-full rounded-full transition-all duration-300"
                  style={{
                    height: `${pct}%`,
                    backgroundColor: 'var(--mint-primary)',
                    opacity: isSelected ? 1 : 0.42,
                  }}
                />
              </div>
              <span
                className={`text-[11px] font-medium ${
                  isSelected ? 'text-[var(--ink-primary)] font-bold' : 'text-[var(--ink-muted)]'
                }`}
              >
                {bucket.label}
              </span>
            </button>
          );
        })}
      </div>

      {activeBucket && (
        <div className="flex items-center justify-between text-xs px-1 pt-1 border-t border-[var(--hairline)]">
          <span className="text-[var(--ink-muted)]">
            Selected period: <strong className="text-[var(--ink-primary)]">{activeBucket.label}</strong>
          </span>
          <span className="font-bold tabular-nums text-[var(--mint-primary)]">
            {formatCents(activeBucket.amountCents, currencyCode)}
          </span>
        </div>
      )}
    </div>
  );
}

export function HatchedGoalProgressBar({
  fraction,
  fillColor,
}: {
  fraction: number;
  fillColor: string;
}) {
  const patternId = useId().replace(/:/g, '');
  const clamped = Math.max(0, Math.min(1, fraction));
  const pct = clamped * 100;

  return (
    <div className="relative w-full h-5 flex items-center">
      <svg className="w-full h-3.5 rounded-full overflow-hidden" viewBox="0 0 100 14" preserveAspectRatio="none">
        <defs>
          <pattern
            id={patternId}
            width="6"
            height="6"
            patternTransform="rotate(45 0 0)"
            patternUnits="userSpaceOnUse"
          >
            <rect width="6" height="6" fill="var(--bg-track)" fillOpacity="0.65" />
            <line x1="0" y1="0" x2="0" y2="6" stroke={fillColor} strokeOpacity="0.34" strokeWidth="2.5" />
          </pattern>
        </defs>
        <rect x="0" y="0" width="100" height="14" rx="7" fill={`url(#${patternId})`} />
        {pct > 0 && <rect x="0" y="0" width={Math.max(5, pct)} height="14" rx="7" fill={fillColor} />}
      </svg>
      {pct > 0 && (
        <div
          className="absolute w-4 h-4 rounded-full bg-[var(--bg-surface)] border-[2.5px] -translate-x-1/2 pointer-events-none transition-all duration-300"
          style={{
            left: `clamp(8px, ${pct}%, calc(100% - 8px))`,
            borderColor: fillColor,
          }}
        />
      )}
    </div>
  );
}

export function CircularPercentRing({
  percentage,
  ringColor,
  size = 44,
}: {
  percentage: number;
  ringColor: string;
  size?: number;
}) {
  const clamped = Math.max(0, Math.min(100, percentage));
  const stroke = 4.5;
  const radius = (size - stroke) / 2;
  const circumference = 2 * Math.PI * radius;
  const dashOffset = circumference * (1 - clamped / 100);

  return (
    <div
      className="relative flex items-center justify-center shrink-0"
      style={{ width: size, height: size }}
    >
      <svg width={size} height={size} className="-rotate-90">
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={ringColor}
          strokeOpacity="0.18"
          strokeWidth={stroke}
        />
        {clamped > 0 && (
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="none"
            stroke={ringColor}
            strokeWidth={stroke}
            strokeLinecap="round"
            strokeDasharray={circumference}
            strokeDashoffset={dashOffset}
          />
        )}
      </svg>
      <span className="absolute text-[10.5px] font-bold tabular-nums text-[var(--ink-primary)]">
        {Math.min(999, percentage)}%
      </span>
    </div>
  );
}

export function SavingsProjectionChart({
  projection,
  currencyCode,
}: {
  projection: SavingsProjection;
  currencyCode: string;
}) {
  const gradId = useId().replace(/:/g, '');
  const primaryColor = 'var(--mint-primary)';
  const projectedColor =
    projection.projectedSavingsCents >= 0 ? 'var(--mint-primary)' : 'var(--coral-tertiary)';
  const budgetLineColor = 'var(--coral-tertiary)';

  const W = 560;
  const H = 152;
  const padTop = 10;
  const padBottom = 10;
  const chartH = H - padTop - padBottom;

  const maxSpend = Math.max(
    projection.projectedMonthEndSpendCents,
    projection.monthlyBudgetCents,
    projection.monthlyIncomeCents,
    10_000
  );

  const yFor = (cents: number) => {
    const frac = Math.max(0, Math.min(1.05, cents / maxSpend));
    return padTop + chartH * (1 - frac);
  };

  const totalDays = Math.max(2, projection.daysInMonth);
  const currentIdx = Math.max(0, Math.min(totalDays - 1, projection.currentDayOfMonth - 1));
  const stepX = W / (totalDays - 1);

  const actualPoints = projection.actualCumulativeDailyCents;
  const projectedPoints = projection.projectedDailyCents;

  let actualLine = '';
  let actualFill = '';
  let lastX = 0;
  let lastY = H - padBottom;
  if (actualPoints.length > 0) {
    actualPoints.forEach((pt, i) => {
      const x = i * stepX;
      const y = yFor(pt);
      if (i === 0) {
        actualLine = `M ${x} ${y}`;
        actualFill = `M ${x} ${H - padBottom} L ${x} ${y}`;
      } else {
        actualLine += ` L ${x} ${y}`;
        actualFill += ` L ${x} ${y}`;
      }
      lastX = x;
      lastY = y;
    });
    actualFill += ` L ${lastX} ${H - padBottom} Z`;
  }

  let projLine = '';
  if (projectedPoints.length >= totalDays && currentIdx < totalDays - 1) {
    for (let i = currentIdx; i < totalDays; i += 1) {
      const x = i * stepX;
      const y = yFor(projectedPoints[i]);
      projLine += i === currentIdx ? `M ${x} ${y}` : ` L ${x} ${y}`;
    }
  }

  return (
    <div className="w-full space-y-3">
      <svg viewBox={`0 0 ${W} ${H}`} className="w-full h-[152px] overflow-visible">
        <defs>
          <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={primaryColor} stopOpacity="0.25" />
            <stop offset="100%" stopColor={primaryColor} stopOpacity="0" />
          </linearGradient>
        </defs>

        {[0.25, 0.5, 0.75].map((frac) => (
          <line
            key={frac}
            x1={0}
            x2={W}
            y1={padTop + chartH * frac}
            y2={padTop + chartH * frac}
            stroke="var(--hairline)"
            strokeWidth="1"
          />
        ))}

        {projection.monthlyBudgetCents > 0 && (
          <line
            x1={0}
            x2={W}
            y1={yFor(projection.monthlyBudgetCents)}
            y2={yFor(projection.monthlyBudgetCents)}
            stroke={budgetLineColor}
            strokeOpacity="0.8"
            strokeWidth="1.5"
            strokeDasharray="10 8"
          />
        )}

        {projLine && (
          <path
            d={projLine}
            fill="none"
            stroke={projectedColor}
            strokeWidth="2.75"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeDasharray="12 9"
          />
        )}

        {actualFill && <path d={actualFill} fill={`url(#${gradId})`} />}
        {actualLine && (
          <path
            d={actualLine}
            fill="none"
            stroke={primaryColor}
            strokeWidth="3.2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        )}

        {actualPoints.length > 0 && (
          <>
            <circle cx={lastX} cy={lastY} r={8} fill={primaryColor} fillOpacity="0.24" />
            <circle
              cx={lastX}
              cy={lastY}
              r={4.5}
              fill="var(--bg-surface)"
              stroke={primaryColor}
              strokeWidth="2.2"
            />
          </>
        )}
      </svg>

      <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-[var(--ink-muted)] tabular-nums">
        <span className="inline-flex items-center gap-1.5">
          <span className="w-2 h-2 rounded-full" style={{ backgroundColor: primaryColor }} />
          Spent {formatCompactCents(projection.spentToDateCents, currencyCode)}
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="w-2 h-2 rounded-full" style={{ backgroundColor: projectedColor }} />
          Proj. {formatCompactCents(projection.projectedMonthEndSpendCents, currencyCode)}
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span
            className="w-2 h-2 rounded-full opacity-80"
            style={{ backgroundColor: budgetLineColor }}
          />
          Limit {formatCompactCents(projection.monthlyBudgetCents, currencyCode)}
        </span>
      </div>
    </div>
  );
}
