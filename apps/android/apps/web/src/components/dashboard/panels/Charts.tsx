import {
	BarList,
	colorByIndex,
	DonutChart,
	RankLineChart,
	StackedBarChart,
	type Slice,
} from "@/components/dashboard/charts";
import { Card, CardHead, EmptyState, Legend } from "@/components/dashboard/ui";
import { formatNumber } from "@/lib/dashboard/format";
import type { NameValue, StackedSeries } from "@/lib/dashboard/queries";
import { type RankHistory, regionFlag, regionLabel } from "@/lib/dashboard/ranks";
import { notificationLabel, PRODUCT_ORDER, productLabel } from "@/lib/dashboard/types";
import { RANK_COUNTRIES } from "@/lib/ranks/capture";

export function PurchaseTrendCard({ series }: { series: StackedSeries }) {
	const colorFor = (key: string) => {
		const idx = (PRODUCT_ORDER as readonly string[]).indexOf(key);
		return colorByIndex(idx >= 0 ? idx : series.keys.indexOf(key));
	};
	const legend = series.keys.map((k) => ({ label: productLabel(k), color: colorFor(k) }));

	return (
		<Card>
			<CardHead title="每日购买趋势" hint="按交易笔数，按产品堆叠" />
			<div className="px-3 pt-3">
				<StackedBarChart series={series} colorFor={colorFor} labelFor={productLabel} />
			</div>
			<div className="px-5 pt-1 pb-4">
				<Legend items={legend} />
			</div>
		</Card>
	);
}

export function NotificationTrendCard({ series }: { series: StackedSeries }) {
	const colorFor = (key: string) => colorByIndex(series.keys.indexOf(key));
	const legend = series.keys.map((k) => ({ label: notificationLabel(k), color: colorFor(k) }));

	return (
		<Card>
			<CardHead title="每日通知量" hint="App Store 服务器通知，按类型堆叠" />
			<div className="px-3 pt-3">
				<StackedBarChart series={series} colorFor={colorFor} labelFor={notificationLabel} />
			</div>
			<div className="px-5 pt-1 pb-4">
				<Legend items={legend} />
			</div>
		</Card>
	);
}

export function ProductMixCard({ data }: { data: NameValue[] }) {
	const total = data.reduce((a, b) => a + b.value, 0);
	const slices: Slice[] = data.map((d) => {
		const idx = (PRODUCT_ORDER as readonly string[]).indexOf(d.name);
		return { name: productLabel(d.name), value: d.value, color: colorByIndex(idx >= 0 ? idx : 0) };
	});

	return (
		<Card>
			<CardHead title="活跃产品构成" hint="活跃权益按产品分布" />
			<div className="px-5 py-4">
				<DonutChart data={slices} centerValue={formatNumber(total)} centerLabel="活跃" />
			</div>
		</Card>
	);
}

const STATUS_META: Record<string, { label: string; color: string }> = {
	active: { label: "有效", color: "var(--positive)" },
	refunded: { label: "已退款", color: "var(--negative)" },
};

export function StatusBreakdownCard({ data }: { data: NameValue[] }) {
	const slices: Slice[] = data.map((d, i) => ({
		name: STATUS_META[d.name]?.label ?? d.name,
		value: d.value,
		color: STATUS_META[d.name]?.color ?? colorByIndex(i),
	}));

	return (
		<Card>
			<CardHead title="订阅状态" hint="按 subscriptions.status" />
			<div className="px-5 py-4">
				<BarList data={slices} />
			</div>
		</Card>
	);
}

const rankColorFor = (country: string) => {
	const idx = (RANK_COUNTRIES as readonly string[]).indexOf(country);
	return colorByIndex(idx >= 0 ? idx : 0);
};

export function RanksCard({ history }: { history: RankHistory }) {
	return (
		<Card>
			<CardHead title="App Store 排名" hint="各地区开发者工具榜每日名次（近 30 天，名次 1 在顶部）" />
			{history.hasData ? (
				<>
					<div className="px-3 pt-3">
						<RankLineChart
							dates={history.dates}
							series={history.series}
							colorFor={rankColorFor}
							labelFor={(c) => `${regionFlag(c)} ${regionLabel(c)}`}
							maxPosition={history.maxPosition}
						/>
					</div>
					<ul className="flex flex-wrap gap-x-4 gap-y-2 px-5 pt-1 pb-4">
						{history.series.map((s) => (
							<li key={s.country} className="flex items-center gap-1.5 text-xs">
								<span
									className="inline-block h-2.5 w-2.5 rounded-[3px]"
									style={{ background: rankColorFor(s.country) }}
								/>
								<span className="text-muted">
									{regionFlag(s.country)} {regionLabel(s.country)}
								</span>
								<span className="font-medium tabular-nums">
									{s.latest != null ? `#${s.latest}` : "未上榜"}
								</span>
							</li>
						))}
					</ul>
				</>
			) : (
				<div className="px-5 py-4">
					<EmptyState label="暂无排名数据（定时任务尚未抓取，或各地区均未上榜）" />
				</div>
			)}
		</Card>
	);
}
