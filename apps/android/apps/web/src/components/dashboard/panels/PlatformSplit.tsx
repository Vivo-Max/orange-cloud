"use client";

import { usePrefs, type DisplayCurrency } from "@/components/dashboard/prefs";
import { Card, CardHead, PlatformBadge } from "@/components/dashboard/ui";
import { convertMillis, type FxRates } from "@/lib/dashboard/fx";
import { formatNumber } from "@/lib/dashboard/format";
import type { PlatformSummary } from "@/lib/dashboard/queries";

function formatTotal(amount: number, currency: DisplayCurrency): string {
	return new Intl.NumberFormat("en-US", {
		style: "currency",
		currency,
		maximumFractionDigits: 0,
	}).format(amount);
}

/**
 * App Store / Google Play 横向对比。只有一个平台有数据时不渲染
 * （那时上面的 KPI 行已经说明了全部）。
 */
export function PlatformSplitCard({
	platforms,
	fx,
}: {
	platforms: PlatformSummary[];
	fx: FxRates | null;
}) {
	const { currency } = usePrefs();
	if (platforms.length < 2) return null;

	return (
		<Card>
			<CardHead title="分平台" hint="同一筛选下各商店的权益、交易与折合营收" />
			<div className="grid grid-cols-1 gap-3 px-5 pt-4 pb-5 sm:grid-cols-2">
				{platforms.map((p) => {
					let revenue: number | null = fx ? 0 : null;
					if (fx) {
						for (const r of p.revenueByCurrency) {
							const converted = convertMillis(r.sumMillis, r.currency, currency, fx.rates);
							// 缺汇率的币种不计入（与「折合总营收」口径一致）
							if (converted != null) revenue = (revenue ?? 0) + converted;
						}
					}
					return (
						<div key={p.platform} className="rounded-lg border border-border bg-surface-2 p-4">
							<PlatformBadge platform={p.platform} />
							<p className="mt-2 text-2xl font-semibold tracking-tight tabular-nums">
								{revenue == null ? "—" : formatTotal(revenue, currency)}
							</p>
							<p className="mt-1 text-xs text-muted">
								活跃权益 {formatNumber(p.activeTotal)} · 交易 {formatNumber(p.transactions)} · 退款{" "}
								{formatNumber(p.refunds)}
							</p>
						</div>
					);
				})}
			</div>
		</Card>
	);
}
