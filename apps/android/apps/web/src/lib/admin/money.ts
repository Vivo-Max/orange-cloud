// 后台账本的金额归一与格式化。
//
// Apple 的每笔交易金额是「该交易币种的 milliunits」（$19.99 -> 19990）。
// 各国 storefront 币种不同，为了能汇总成一个总数 / 画趋势，这里用一张**近似汇率表**
// 把各币种折算到 USD，再 ×USD_CNY 给出 CNY 估算。
// 这是后台「估算口径」，不是 Apple 的实际结算金额；汇率需要时手动更新即可。

export const USD_CNY = 7.18; // USD -> CNY（与设计稿一致）

/** 各币种 -> USD 的近似汇率（2026 年中量级，估算用） */
export const FX_TO_USD: Record<string, number> = {
	USD: 1, CNY: 0.139, EUR: 1.08, GBP: 1.27, JPY: 0.0064, KRW: 0.00073,
	TWD: 0.031, HKD: 0.128, SGD: 0.74, AUD: 0.66, CAD: 0.73, NZD: 0.61,
	INR: 0.012, BRL: 0.18, MXN: 0.058, MYR: 0.21, THB: 0.028, IDR: 0.000062,
	PHP: 0.017, VND: 0.000039, TRY: 0.03, ZAR: 0.054, SEK: 0.094, NOK: 0.092,
	DKK: 0.145, CHF: 1.1, PLN: 0.25, CZK: 0.043, AED: 0.272, SAR: 0.267, ILS: 0.27,
};

const SYMBOL: Record<string, string> = {
	USD: "$", CNY: "¥", JPY: "¥", EUR: "€", GBP: "£", KRW: "₩",
	HKD: "HK$", TWD: "NT$", AUD: "A$", CAD: "C$", NZD: "NZ$", SGD: "S$",
	INR: "₹", BRL: "R$", MXN: "MX$", PHP: "₱", THB: "฿", CHF: "CHF ",
};
const ZERO_DECIMAL = new Set(["JPY", "KRW", "VND", "IDR", "CLP", "HUF"]);

/** 一笔（或一组）milliunits 折算到 USD；未知币种返回 null（聚合时跳过）。 */
export function toUSD(priceMillis: number | null | undefined, currency: string | null | undefined): number | null {
	if (priceMillis == null || !currency) return null;
	const rate = FX_TO_USD[currency];
	if (rate == null) return null;
	return (priceMillis / 1000) * rate;
}

export function fmtUSD(n: number, dp = 2): string {
	const sign = n < 0 ? "−" : "";
	return sign + "$" + Math.abs(n).toLocaleString("en-US", { minimumFractionDigits: dp, maximumFractionDigits: dp });
}

/** 入参是 USD 金额，按 USD_CNY 折算展示 CNY */
export function fmtCNY(usd: number): string {
	const sign = usd < 0 ? "−" : "";
	return sign + "¥" + Math.round(Math.abs(usd) * USD_CNY).toLocaleString("en-US");
}

export function fmtCNYk(usd: number): string {
	const v = Math.abs(usd) * USD_CNY;
	const sign = usd < 0 ? "−" : "";
	if (v >= 10000) return sign + "¥" + (v / 10000).toFixed(1) + "万";
	return sign + "¥" + Math.round(v).toLocaleString("en-US");
}

export function abbrUSD(n: number): string {
	if (Math.abs(n) >= 1000) return "$" + (n / 1000).toFixed(1) + "k";
	return "$" + Math.round(n);
}

/** 原币种金额（带符号），表格主列用 */
export function fmtNative(priceMillis: number | null | undefined, currency: string | null | undefined): string {
	if (priceMillis == null) return "—";
	const amount = priceMillis / 1000;
	const cur = currency ?? "";
	const dp = ZERO_DECIMAL.has(cur) ? 0 : 2;
	const body = Math.abs(amount).toLocaleString("en-US", { minimumFractionDigits: dp, maximumFractionDigits: dp });
	const sym = SYMBOL[cur];
	const sign = amount < 0 ? "−" : "";
	return sign + (sym ? sym + body : body + " " + cur);
}
