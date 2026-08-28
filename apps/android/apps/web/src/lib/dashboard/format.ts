// Formatting helpers. All timestamps in the DB are epoch milliseconds (UTC);
// `price_millis` is the price in the currency's major unit multiplied by 1000
// (e.g. 49990 USD = $49.99, 68000 CNY = ¥68, 1499000000 VND = ₫1,499,000).

/** Convert a `price_millis` value to its major-unit amount. */
export function millisToAmount(priceMillis: number | null | undefined): number {
	return (priceMillis ?? 0) / 1000;
}

/** Format a `price_millis` value as a localized currency string. */
export function formatMoney(priceMillis: number | null | undefined, currency: string): string {
	const amount = millisToAmount(priceMillis);
	try {
		return new Intl.NumberFormat("en-US", {
			style: "currency",
			currency,
			maximumFractionDigits: 2,
		}).format(amount);
	} catch {
		// Unknown/invalid ISO currency code — fall back to a plain number.
		return `${amount.toLocaleString("en-US")} ${currency}`;
	}
}

export function formatNumber(n: number): string {
	return new Intl.NumberFormat("en-US").format(n);
}

export function formatPercent(ratio: number, digits = 1): string {
	if (!Number.isFinite(ratio)) return "—";
	return `${(ratio * 100).toFixed(digits)}%`;
}

// Deterministic UTC formatters so server-rendered output never depends on the
// host timezone (avoids hydration drift and keeps dev/prod consistent).
const DATE_TIME_FMT = new Intl.DateTimeFormat("en-CA", {
	timeZone: "UTC",
	year: "numeric",
	month: "2-digit",
	day: "2-digit",
	hour: "2-digit",
	minute: "2-digit",
	hour12: false,
});

/** "2026-06-22 16:07" (UTC) or "—" for empty values. */
export function formatTimestamp(ms: number | null | undefined): string {
	if (!ms) return "—";
	return DATE_TIME_FMT.format(new Date(ms)).replace(",", "");
}

export type TimeZonePref = "UTC" | "Asia/Shanghai";

/** Format an epoch-ms value in the given timezone. */
export function formatTs(
	ms: number,
	timeZone: TimeZonePref,
	mode: "datetime" | "date" = "datetime",
): string {
	const opts: Intl.DateTimeFormatOptions =
		mode === "date"
			? { timeZone, year: "numeric", month: "2-digit", day: "2-digit" }
			: {
					timeZone,
					year: "numeric",
					month: "2-digit",
					day: "2-digit",
					hour: "2-digit",
					minute: "2-digit",
					hour12: false,
				};
	return new Intl.DateTimeFormat("en-CA", opts).format(new Date(ms)).replace(",", "");
}

/** "2026-06-22" (UTC). */
export function formatDate(ms: number): string {
	return new Date(ms).toISOString().slice(0, 10);
}

/** "Jun 22" style short label for chart axes (UTC). */
const SHORT_DATE_FMT = new Intl.DateTimeFormat("en-US", {
	timeZone: "UTC",
	month: "short",
	day: "numeric",
});
export function formatShortDate(isoDate: string): string {
	// isoDate is "YYYY-MM-DD" from SQLite date(); treat as UTC midnight.
	return SHORT_DATE_FMT.format(new Date(`${isoDate}T00:00:00Z`));
}

/** Relative-to-now label like "3 天后" / "5 小时后" / "已过期". */
export function formatRelativeFuture(ms: number, now = Date.now()): string {
	const diff = ms - now;
	if (diff <= 0) return "已过期";
	const days = Math.floor(diff / 86_400_000);
	if (days >= 1) return `${days} 天后`;
	const hours = Math.floor(diff / 3_600_000);
	if (hours >= 1) return `${hours} 小时后`;
	const mins = Math.max(1, Math.floor(diff / 60_000));
	return `${mins} 分钟后`;
}
