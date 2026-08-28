// Foreign-exchange rates from open.er-api.com (ExchangeRate-API free tier — no
// API key required, ~160 currencies, updated daily). Rates are USD-based:
// rates[X] = units of X per 1 USD.

const ENDPOINT = "https://open.er-api.com/v6/latest/USD";
const TTL_MS = 60 * 60 * 1000; // refetch at most hourly

export interface FxRates {
	/** units of currency per 1 USD */
	rates: Record<string, number>;
	/** when the provider last refreshed the rates (epoch ms) */
	updatedAt: number;
}

// In-memory cache, best-effort within a warm isolate. FX moves slowly, so a
// stale value is fine and far better than fetching on every render.
let cache: (FxRates & { fetchedAt: number }) | null = null;

export async function getUsdRates(): Promise<FxRates | null> {
	if (cache && Date.now() - cache.fetchedAt < TTL_MS) {
		return { rates: cache.rates, updatedAt: cache.updatedAt };
	}
	try {
		const res = await fetch(ENDPOINT, { signal: AbortSignal.timeout(5000) });
		if (!res.ok) return cache ?? null;
		const data = (await res.json()) as {
			result?: string;
			rates?: Record<string, number>;
			time_last_update_unix?: number;
		};
		if (data.result !== "success" || !data.rates) return cache ?? null;
		cache = {
			rates: data.rates,
			updatedAt: (data.time_last_update_unix ?? Math.floor(Date.now() / 1000)) * 1000,
			fetchedAt: Date.now(),
		};
		return { rates: cache.rates, updatedAt: cache.updatedAt };
	} catch {
		// Network/timeout — serve the last good value if we have one.
		return cache ?? null;
	}
}

/**
 * Convert a `price_millis` value from one currency to another via USD.
 * Returns the amount in `to`'s major unit, or null if a rate is missing.
 */
export function convertMillis(
	priceMillis: number,
	from: string,
	to: string,
	rates: Record<string, number>,
): number | null {
	const rateFrom = rates[from];
	const rateTo = rates[to];
	if (!rateFrom || !rateTo) return null;
	return (priceMillis / 1000 / rateFrom) * rateTo;
}
