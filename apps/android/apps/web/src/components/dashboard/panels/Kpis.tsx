import type { ReactNode } from "react";
import { Card, CardHead, EmptyState } from "@/components/dashboard/ui";
import type { FxRates } from "@/lib/dashboard/fx";
import { formatMoney, formatNumber, formatPercent } from "@/lib/dashboard/format";
import type { Overview } from "@/lib/dashboard/queries";
import { UnifiedRevenue } from "./UnifiedRevenue";

function Stat({ label, value, sub }: { label: string; value: ReactNode; sub?: ReactNode }) {
	return (
		<Card className="p-5">
			<p className="text-xs font-medium text-muted">{label}</p>
			<p className="mt-2 text-3xl font-semibold tracking-tight tabular-nums">{value}</p>
			<p className="mt-1 min-h-4 text-xs text-muted">{sub}</p>
		</Card>
	);
}

export function KpiStats({ overview }: { overview: Overview }) {
	const o = overview;
	return (
		<div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
			<Stat
				label="活跃权益"
				value={formatNumber(o.activeTotal)}
				sub={`终身 ${formatNumber(o.activeLifetime)} · 订阅 ${formatNumber(o.activeSubscription)}`}
			/>
			<Stat label="周期内交易" value={formatNumber(o.transactions)} sub="含首购与续订" />
			<Stat
				label="退款"
				value={formatNumber(o.refunds)}
				sub={`退款率 ${formatPercent(o.refundRate)}`}
			/>
			<Stat
				label="自动续订率"
				value={o.autoRenewTotal ? formatPercent(o.autoRenewRate) : "—"}
				sub={`${formatNumber(o.autoRenewOn)} / ${formatNumber(o.autoRenewTotal)} 个订阅开启`}
			/>
		</div>
	);
}

export function RevenueCard({ overview, fx }: { overview: Overview; fx: FxRates | null }) {
	const rows = overview.revenueByCurrency;
	return (
		<Card>
			<CardHead title="营收" hint="折合总额 + 各原币种明细，已扣除退款" />
			{rows.length === 0 ? (
				<EmptyState label="该筛选下暂无营收" />
			) : (
				<>
					<UnifiedRevenue revenue={rows} fx={fx} />
					<div className="mt-3 flex flex-wrap gap-2 border-t border-border px-5 pt-3 pb-5">
						{rows.map((c) => (
							<div
								key={c.currency}
								className="min-w-30 rounded-lg border border-border bg-surface-2 px-3 py-2"
							>
								<div className="flex items-center gap-1.5 text-[11px] text-muted">
									<span className="font-mono font-semibold">{c.currency}</span>
									<span>· {formatNumber(c.count)} 笔</span>
								</div>
								<div className="mt-0.5 text-lg font-semibold tabular-nums">
									{formatMoney(c.sumMillis, c.currency)}
								</div>
							</div>
						))}
					</div>
				</>
			)}
		</Card>
	);
}
