"use client";

import { type DisplayCurrency, usePrefs } from "@/components/dashboard/prefs";
import { convertMillis, type FxRates } from "@/lib/dashboard/fx";
import type { CurrencyRevenue } from "@/lib/dashboard/queries";

function formatTotal(amount: number, currency: DisplayCurrency): string {
	return new Intl.NumberFormat("en-US", {
		style: "currency",
		currency,
		maximumFractionDigits: 0,
	}).format(amount);
}

export function UnifiedRevenue({
	revenue,
	fx,
}: {
	revenue: CurrencyRevenue[];
	fx: FxRates | null;
}) {
	const { currency, setCurrency } = usePrefs();

	if (!fx) {
		return <p className="px-5 pt-4 text-xs text-muted">汇率暂不可用，仅按原币种展示。</p>;
	}

	let total = 0;
	const missing: string[] = [];
	for (const r of revenue) {
		const converted = convertMillis(r.sumMillis, r.currency, currency, fx.rates);
		if (converted == null) missing.push(r.currency);
		else total += converted;
	}

	const options: DisplayCurrency[] = ["USD", "CNY"];

	return (
		<div className="flex items-end justify-between gap-3 px-5 pt-4">
			<div className="min-w-0">
				<p className="text-xs text-muted">折合总营收</p>
				<p className="mt-1 text-2xl font-semibold tracking-tight tabular-nums">
					{formatTotal(total, currency)}
				</p>
				<p className="mt-0.5 text-[11px] text-muted">
					按汇率折算（{new Date(fx.updatedAt).toISOString().slice(0, 10)} 更新）
					{missing.length ? ` · ${missing.join("/")} 无汇率未计入` : ""}
				</p>
			</div>
			<div className="inline-flex shrink-0 items-center gap-1 rounded-lg border border-border bg-surface-2 p-0.5">
				{options.map((c) => (
					<button
						key={c}
						type="button"
						onClick={() => setCurrency(c)}
						aria-pressed={currency === c}
						className={`rounded-md px-2.5 py-1 text-xs font-medium transition-colors ${
							currency === c ? "bg-accent text-white" : "text-muted hover:text-foreground"
						}`}
					>
						{c}
					</button>
				))}
			</div>
		</div>
	);
}
