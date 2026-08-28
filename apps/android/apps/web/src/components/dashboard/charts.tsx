import { EmptyState } from "@/components/dashboard/ui";
import { formatShortDate } from "@/lib/dashboard/format";
import type { StackedSeries } from "@/lib/dashboard/queries";

export const CHART_COLORS = [
	"var(--chart-1)",
	"var(--chart-2)",
	"var(--chart-3)",
	"var(--chart-4)",
	"var(--chart-5)",
	"var(--chart-6)",
	"var(--chart-7)",
	"var(--chart-8)",
];

export function colorByIndex(i: number): string {
	return CHART_COLORS[((i % CHART_COLORS.length) + CHART_COLORS.length) % CHART_COLORS.length];
}

/** Round a max value up to a clean axis bound. */
function niceCeil(v: number): number {
	if (v <= 5) return 5;
	const pow = Math.pow(10, Math.floor(Math.log10(v)));
	const step = pow / 2;
	return Math.ceil(v / step) * step;
}

// ---------------------------------------------------------------------------
// Stacked bar chart (daily trends)
// ---------------------------------------------------------------------------

export function StackedBarChart({
	series,
	colorFor,
	labelFor = (k) => k,
	height = 200,
}: {
	series: StackedSeries;
	colorFor: (key: string) => string;
	labelFor?: (key: string) => string;
	height?: number;
}) {
	const days = series.days;
	if (days.length === 0 || series.max === 0) {
		return <EmptyState label="该筛选下暂无趋势数据" />;
	}

	const W = 760;
	const H = height;
	const padL = 30;
	const padR = 12;
	const padT = 12;
	const padB = 22;
	const plotW = W - padL - padR;
	const plotH = H - padT - padB;
	const niceMax = niceCeil(series.max);
	const slot = plotW / days.length;
	const barW = Math.min(34, slot * 0.6);
	const y = (v: number) => padT + plotH - (v / niceMax) * plotH;
	const ticks = [0, 0.5, 1].map((t) => niceMax * t);
	const step = Math.max(1, Math.ceil(days.length / 8));

	return (
		<svg
			viewBox={`0 0 ${W} ${H}`}
			width="100%"
			height={H}
			preserveAspectRatio="xMidYMid meet"
			role="img"
			aria-label="每日趋势堆叠柱状图"
		>
			{ticks.map((v, idx) => (
				<g key={idx}>
					<line
						x1={padL}
						x2={W - padR}
						y1={y(v)}
						y2={y(v)}
						stroke="var(--grid)"
						strokeWidth={1}
					/>
					<text x={padL - 6} y={y(v) + 3} textAnchor="end" fontSize={9} fill="var(--muted)">
						{Math.round(v)}
					</text>
				</g>
			))}

			{days.map((d, i) => {
				const cx = padL + slot * i + slot / 2;
				let acc = 0;
				return (
					<g key={d.date}>
						{series.keys.map((k) => {
							const val = d.segments[k] ?? 0;
							if (!val) return null;
							const h = (val / niceMax) * plotH;
							const yy = y(acc + val);
							acc += val;
							return (
								<rect
									key={k}
									x={cx - barW / 2}
									y={yy}
									width={barW}
									height={Math.max(0, h)}
									rx={1.5}
									fill={colorFor(k)}
								>
									<title>{`${formatShortDate(d.date)} · ${labelFor(k)}: ${val}`}</title>
								</rect>
							);
						})}
						{i % step === 0 ? (
							<text x={cx} y={H - 6} textAnchor="middle" fontSize={9} fill="var(--muted)">
								{formatShortDate(d.date)}
							</text>
						) : null}
					</g>
				);
			})}
		</svg>
	);
}

// ---------------------------------------------------------------------------
// Donut chart
// ---------------------------------------------------------------------------

export interface Slice {
	name: string;
	value: number;
	color: string;
}

function polar(cx: number, cy: number, r: number, a: number): [number, number] {
	return [cx + r * Math.cos(a), cy + r * Math.sin(a)];
}

function donutArc(cx: number, cy: number, ro: number, ri: number, start: number, end: number): string {
	const large = end - start > Math.PI ? 1 : 0;
	const [x0, y0] = polar(cx, cy, ro, start);
	const [x1, y1] = polar(cx, cy, ro, end);
	const [x2, y2] = polar(cx, cy, ri, end);
	const [x3, y3] = polar(cx, cy, ri, start);
	return `M ${x0} ${y0} A ${ro} ${ro} 0 ${large} 1 ${x1} ${y1} L ${x2} ${y2} A ${ri} ${ri} 0 ${large} 0 ${x3} ${y3} Z`;
}

export function DonutChart({
	data,
	centerValue,
	centerLabel,
}: {
	data: Slice[];
	centerValue?: string;
	centerLabel?: string;
}) {
	const total = data.reduce((a, b) => a + b.value, 0);
	if (total === 0) return <EmptyState label="暂无数据" />;

	const cx = 70;
	const cy = 70;
	const ro = 60;
	const ri = 40;
	let angle = -Math.PI / 2;

	return (
		<div className="flex items-center gap-5">
			<svg viewBox="0 0 140 140" width={140} height={140} role="img" aria-label="占比环形图">
				{data.length === 1 ? (
					<circle
						cx={cx}
						cy={cy}
						r={(ro + ri) / 2}
						fill="none"
						stroke={data[0].color}
						strokeWidth={ro - ri}
					>
						<title>{`${data[0].name}: ${data[0].value} (100%)`}</title>
					</circle>
				) : (
					data.map((d, i) => {
						const frac = d.value / total;
						const start = angle;
						const end = angle + frac * 2 * Math.PI;
						angle = end;
						return (
							<path key={i} d={donutArc(cx, cy, ro, ri, start, end)} fill={d.color}>
								<title>{`${d.name}: ${d.value} (${(frac * 100).toFixed(0)}%)`}</title>
							</path>
						);
					})
				)}
				{centerValue ? (
					<text
						x={cx}
						y={cy - 1}
						textAnchor="middle"
						fontSize={20}
						fontWeight={600}
						fill="var(--foreground)"
					>
						{centerValue}
					</text>
				) : null}
				{centerLabel ? (
					<text x={cx} y={cy + 14} textAnchor="middle" fontSize={9} fill="var(--muted)">
						{centerLabel}
					</text>
				) : null}
			</svg>
			<ul className="flex min-w-0 flex-1 flex-col gap-1.5">
				{data.map((d, i) => (
					<li key={i} className="flex items-center gap-2 text-xs">
						<span
							className="h-2.5 w-2.5 shrink-0 rounded-[3px]"
							style={{ background: d.color }}
						/>
						<span className="truncate text-muted">{d.name}</span>
						<span className="ml-auto font-medium tabular-nums">{d.value}</span>
					</li>
				))}
			</ul>
		</div>
	);
}

// ---------------------------------------------------------------------------
// Horizontal bar list
// ---------------------------------------------------------------------------

export function BarList({ data }: { data: Slice[] }) {
	if (data.length === 0) return <EmptyState label="暂无数据" />;
	const max = Math.max(...data.map((d) => d.value), 1);
	return (
		<ul className="flex flex-col gap-3">
			{data.map((d, i) => (
				<li key={i} className="text-xs">
					<div className="mb-1 flex items-center justify-between">
						<span className="text-muted">{d.name}</span>
						<span className="font-medium tabular-nums">{d.value}</span>
					</div>
					<div className="h-2 overflow-hidden rounded-full bg-foreground/[0.06]">
						<div
							className="h-full rounded-full"
							style={{ width: `${(d.value / max) * 100}%`, background: d.color }}
						/>
					</div>
				</li>
			))}
		</ul>
	);
}

// ---------------------------------------------------------------------------
// Multi-series rank line chart (App Store 排名趋势)
// y 轴反转：名次 1 在顶部，数字越大越靠下。position 为 null 的日子断线。
// ---------------------------------------------------------------------------

interface RankLine {
	country: string;
	points: { date: string; position: number | null }[];
}

export function RankLineChart({
	dates,
	series,
	colorFor,
	labelFor = (c) => c,
	maxPosition,
	height = 220,
}: {
	dates: string[];
	series: RankLine[];
	colorFor: (country: string) => string;
	labelFor?: (country: string) => string;
	maxPosition: number;
	height?: number;
}) {
	if (dates.length === 0 || maxPosition === 0) {
		return <EmptyState label="暂无排名数据" />;
	}

	const W = 760;
	const H = height;
	const padL = 34;
	const padR = 14;
	const padT = 14;
	const padB = 24;
	const plotW = W - padL - padR;
	const plotH = H - padT - padB;
	const niceMax = Math.max(2, niceCeil(maxPosition));
	const denom = Math.max(1, niceMax - 1);

	const x = (i: number) => (dates.length === 1 ? padL + plotW / 2 : padL + (i / (dates.length - 1)) * plotW);
	// 名次 1 → 顶部，niceMax → 底部。
	const y = (p: number) => padT + ((p - 1) / denom) * plotH;

	const ticks = [...new Set([1, Math.round(niceMax / 2), niceMax])].filter((v) => v >= 1);
	const step = Math.max(1, Math.ceil(dates.length / 8));

	return (
		<svg
			viewBox={`0 0 ${W} ${H}`}
			width="100%"
			height={H}
			preserveAspectRatio="xMidYMid meet"
			role="img"
			aria-label="App Store 排名趋势折线图"
		>
			{ticks.map((v) => (
				<g key={v}>
					<line x1={padL} x2={W - padR} y1={y(v)} y2={y(v)} stroke="var(--grid)" strokeWidth={1} />
					<text x={padL - 6} y={y(v) + 3} textAnchor="end" fontSize={9} fill="var(--muted)">
						{`#${v}`}
					</text>
				</g>
			))}

			{series.map((s) => {
				const color = colorFor(s.country);
				// 把连续的非空点切成段（null 处断开）。
				const segments: { i: number; p: number }[][] = [];
				let current: { i: number; p: number }[] = [];
				s.points.forEach((pt, i) => {
					if (typeof pt.position === "number") {
						current.push({ i, p: pt.position });
					} else if (current.length) {
						segments.push(current);
						current = [];
					}
				});
				if (current.length) segments.push(current);

				return (
					<g key={s.country}>
						{segments.map((seg, si) =>
							seg.length >= 2 ? (
								<polyline
									key={si}
									points={seg.map((pt) => `${x(pt.i)},${y(pt.p)}`).join(" ")}
									fill="none"
									stroke={color}
									strokeWidth={2}
									strokeLinejoin="round"
									strokeLinecap="round"
								/>
							) : null,
						)}
						{s.points.map((pt, i) =>
							typeof pt.position === "number" ? (
								<circle key={i} cx={x(i)} cy={y(pt.position)} r={2.6} fill={color}>
									<title>{`${formatShortDate(pt.date)} · ${labelFor(s.country)}: #${pt.position}`}</title>
								</circle>
							) : null,
						)}
					</g>
				);
			})}

			{dates.map((d, i) =>
				i % step === 0 ? (
					<text key={d} x={x(i)} y={H - 6} textAnchor="middle" fontSize={9} fill="var(--muted)">
						{formatShortDate(d)}
					</text>
				) : null,
			)}
		</svg>
	);
}
