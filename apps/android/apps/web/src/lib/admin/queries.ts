// 后台账本的聚合查询，dashboard 页面与 JSON 接口共用同一套。
//
// 只算 Production：环境为 Sandbox（沙盒 / 审核测试）的行一律排除（查询层过滤，
// webhook 仍全量入库）。金额跨币种用 money.ts 的近似汇率归一到 USD（再给 CNY 估算）。

import { toUSD } from "./money";

const NOT_SANDBOX = "COALESCE(environment, '') <> 'Sandbox'";

/** product_id -> 展示名 */
export const PRODUCT_NAMES: Record<string, string> = {
	"jiamin.chen.orange_cloud.pro.monthly": "Pro 月度会员",
	"jiamin.chen.orange_cloud.pro.yearly": "Pro 年度会员",
	"jiamin.chen.orange_cloud.pro.lifetime": "终身买断",
};

export type Range = "day" | "month";

export interface Kpis {
	monthNetUsd: number;
	monthDeltaPct: number | null; // 对比上月
	todayNetUsd: number;
	todayDeltaPct: number | null; // 对比昨日
	activeSubs: number;
	newSubsThisMonth: number;
	lifetimeMonthUsd: number;
	lifetimeMonthCount: number;
	refundMonthUsd: number; // 正数，展示时加负号
	refundMonthCount: number;
	cumulativeNetUsd: number;
	totalSubs: number;
	totalNotifications: number;
}

export interface TrendPoint {
	label: string; // x 轴标签：6/17 或 6月
	netUsd: number;
	refundUsd: number;
}

export interface StatusSegment {
	key: string;
	label: string;
	value: number;
	color: string;
}

export interface TxnRow {
	purchase_date: number | null;
	notification_type: string | null;
	product_id: string | null;
	transaction_id: string;
	currency: string | null;
	price_millis: number | null;
	/** 'apple' | 'play'（历史行默认 apple） */
	platform: string | null;
	revoked: boolean;
}

export interface SubRow {
	original_transaction_id: string;
	product_id: string | null;
	status: string;
	auto_renew_status: number | null;
	is_lifetime: number;
	expires_date: number | null;
	price_millis: number | null;
	currency: string | null;
	purchase_date: number | null;
	platform: string | null;
}

export interface AdminStats {
	generatedAt: number;
	range: Range;
	periodLabel: string;
	kpis: Kpis;
	trend: TrendPoint[];
	trendNetUsd: number;
	statusBreakdown: StatusSegment[];
	transactions: TxnRow[];
	subscriptions: SubRow[];
	hasData: boolean;
}

const STATUS_META: Record<string, { label: string; color: string; order: number }> = {
	active: { label: "活跃 Active", color: "var(--sys-green)", order: 0 },
	grace: { label: "宽限期 Grace", color: "var(--oc-orange)", order: 1 },
	billing_retry: { label: "扣款重试 Retry", color: "var(--sys-yellow)", order: 2 },
	expired: { label: "已过期 Expired", color: "var(--sys-gray)", order: 3 },
	refunded: { label: "已退款 Refunded", color: "var(--sys-red)", order: 4 },
	revoked: { label: "已撤销 Revoked", color: "var(--sys-red)", order: 5 },
	// Google Play 特有的两种状态（暂停 / 待付款），Apple 侧不会出现
	paused: { label: "已暂停 Paused", color: "var(--sys-gray)", order: 6 },
	pending: { label: "待处理 Pending", color: "var(--sys-yellow)", order: 7 },
};

interface CurSum {
	currency: string | null;
	s: number | null;
}

/** 按币种 sum(price_millis) 折算并求和 USD */
function sumUsd(rows: CurSum[]): number {
	let usd = 0;
	for (const r of rows) {
		const u = toUSD(r.s, r.currency);
		if (u != null) usd += u;
	}
	return usd;
}

/** 一段 WHERE 内、按币种聚合的交易金额（用于各时间窗收入） */
function revenueQuery(db: D1Database, whereExtra: string, params: unknown[]) {
	return db
		.prepare(
			`SELECT currency, sum(price_millis) AS s FROM transactions
			 WHERE ${NOT_SANDBOX} AND ${whereExtra} GROUP BY currency`,
		)
		.bind(...params)
		.all<CurSum>();
}

function pctDelta(now: number, prev: number): number | null {
	if (prev <= 0) return null;
	return ((now - prev) / prev) * 100;
}

function utcMonthStart(year: number, month: number): number {
	return Date.UTC(year, month, 1);
}

async function buildTrend(db: D1Database, range: Range, now: Date): Promise<TrendPoint[]> {
	if (range === "month") {
		const start = Date.UTC(now.getUTCFullYear(), now.getUTCMonth() - 11, 1);
		const [net, refund] = await Promise.all([
			db
				.prepare(
					`SELECT strftime('%Y-%m', purchase_date/1000, 'unixepoch') AS k, currency, sum(price_millis) AS s
					 FROM transactions WHERE ${NOT_SANDBOX} AND purchase_date >= ? AND revocation_date IS NULL
					 GROUP BY k, currency`,
				)
				.bind(start)
				.all<{ k: string; currency: string | null; s: number | null }>(),
			db
				.prepare(
					`SELECT strftime('%Y-%m', revocation_date/1000, 'unixepoch') AS k, currency, sum(price_millis) AS s
					 FROM transactions WHERE ${NOT_SANDBOX} AND revocation_date >= ?
					 GROUP BY k, currency`,
				)
				.bind(start)
				.all<{ k: string; currency: string | null; s: number | null }>(),
		]);
		const netMap = new Map<string, CurSum[]>();
		const refMap = new Map<string, CurSum[]>();
		for (const r of net.results ?? []) (netMap.get(r.k) ?? netMap.set(r.k, []).get(r.k)!).push(r);
		for (const r of refund.results ?? []) (refMap.get(r.k) ?? refMap.set(r.k, []).get(r.k)!).push(r);
		const out: TrendPoint[] = [];
		for (let i = 11; i >= 0; i--) {
			const d = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() - i, 1));
			const key = `${d.getUTCFullYear()}-${String(d.getUTCMonth() + 1).padStart(2, "0")}`;
			out.push({
				label: `${d.getUTCMonth() + 1}月`,
				netUsd: sumUsd(netMap.get(key) ?? []),
				refundUsd: sumUsd(refMap.get(key) ?? []),
			});
		}
		return out;
	}

	// daily, last 30 days
	const start = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() - 29);
	const [net, refund] = await Promise.all([
		db
			.prepare(
				`SELECT date(purchase_date/1000, 'unixepoch') AS k, currency, sum(price_millis) AS s
				 FROM transactions WHERE ${NOT_SANDBOX} AND purchase_date >= ? AND revocation_date IS NULL
				 GROUP BY k, currency`,
			)
			.bind(start)
			.all<{ k: string; currency: string | null; s: number | null }>(),
		db
			.prepare(
				`SELECT date(revocation_date/1000, 'unixepoch') AS k, currency, sum(price_millis) AS s
				 FROM transactions WHERE ${NOT_SANDBOX} AND revocation_date >= ?
				 GROUP BY k, currency`,
			)
			.bind(start)
			.all<{ k: string; currency: string | null; s: number | null }>(),
	]);
	const netMap = new Map<string, CurSum[]>();
	const refMap = new Map<string, CurSum[]>();
	for (const r of net.results ?? []) (netMap.get(r.k) ?? netMap.set(r.k, []).get(r.k)!).push(r);
	for (const r of refund.results ?? []) (refMap.get(r.k) ?? refMap.set(r.k, []).get(r.k)!).push(r);
	const out: TrendPoint[] = [];
	for (let i = 29; i >= 0; i--) {
		const d = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() - i));
		const key = d.toISOString().slice(0, 10);
		out.push({
			label: `${d.getUTCMonth() + 1}/${d.getUTCDate()}`,
			netUsd: sumUsd(netMap.get(key) ?? []),
			refundUsd: sumUsd(refMap.get(key) ?? []),
		});
	}
	return out;
}

export async function loadAdminStats(db: D1Database, range: Range = "day"): Promise<AdminStats> {
	const now = new Date();
	const monthStart = utcMonthStart(now.getUTCFullYear(), now.getUTCMonth());
	const prevMonthStart = utcMonthStart(now.getUTCFullYear(), now.getUTCMonth() - 1);
	const dayStart = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate());
	const yesterdayStart = dayStart - 86_400_000;

	const [
		monthRows,
		prevMonthRows,
		todayRows,
		yesterdayRows,
		lifetimeRows,
		refundRows,
		cumulativeRows,
		lifetimeCountRow,
		refundCountRow,
		activeRow,
		newSubsRow,
		totalSubsRow,
		notifCountRow,
		statusRows,
		txnRows,
		subRows,
		trend,
	] = await Promise.all([
		revenueQuery(db, "purchase_date >= ? AND revocation_date IS NULL", [monthStart]),
		revenueQuery(db, "purchase_date >= ? AND purchase_date < ? AND revocation_date IS NULL", [prevMonthStart, monthStart]),
		revenueQuery(db, "purchase_date >= ? AND revocation_date IS NULL", [dayStart]),
		revenueQuery(db, "purchase_date >= ? AND purchase_date < ? AND revocation_date IS NULL", [yesterdayStart, dayStart]),
		revenueQuery(db, "purchase_date >= ? AND revocation_date IS NULL AND type = 'Non-Consumable'", [monthStart]),
		revenueQuery(db, "revocation_date >= ?", [monthStart]),
		revenueQuery(db, "revocation_date IS NULL", []),
		db
			.prepare(`SELECT count(*) AS n FROM transactions WHERE ${NOT_SANDBOX} AND type = 'Non-Consumable' AND purchase_date >= ? AND revocation_date IS NULL`)
			.bind(monthStart)
			.first<{ n: number }>(),
		db
			.prepare(`SELECT count(*) AS n FROM transactions WHERE ${NOT_SANDBOX} AND revocation_date >= ?`)
			.bind(monthStart)
			.first<{ n: number }>(),
		db.prepare(`SELECT count(*) AS n FROM subscriptions WHERE ${NOT_SANDBOX} AND status = 'active'`).first<{ n: number }>(),
		db
			.prepare(`SELECT count(*) AS n FROM subscriptions WHERE ${NOT_SANDBOX} AND purchase_date >= ?`)
			.bind(monthStart)
			.first<{ n: number }>(),
		db.prepare(`SELECT count(*) AS n FROM subscriptions WHERE ${NOT_SANDBOX}`).first<{ n: number }>(),
		db.prepare(`SELECT count(*) AS n FROM notifications WHERE ${NOT_SANDBOX}`).first<{ n: number }>(),
		db.prepare(`SELECT status, count(*) AS n FROM subscriptions WHERE ${NOT_SANDBOX} GROUP BY status`).all<{ status: string; n: number }>(),
		db
			.prepare(
				`SELECT purchase_date, notification_type, product_id, transaction_id, currency, price_millis,
				        platform, (revocation_date IS NOT NULL) AS revoked
				 FROM transactions WHERE ${NOT_SANDBOX}
				 ORDER BY COALESCE(purchase_date, created_at) DESC LIMIT 14`,
			)
			.all<Omit<TxnRow, "revoked"> & { revoked: number }>(),
		db
			.prepare(
				`SELECT original_transaction_id, product_id, status, auto_renew_status, is_lifetime,
				        expires_date, price_millis, currency, purchase_date, platform
				 FROM subscriptions WHERE ${NOT_SANDBOX}
				 ORDER BY updated_at DESC LIMIT 50`,
			)
			.all<SubRow>(),
		buildTrend(db, range, now),
	]);

	const monthNetUsd = sumUsd(monthRows.results ?? []);
	const prevMonthNetUsd = sumUsd(prevMonthRows.results ?? []);
	const todayNetUsd = sumUsd(todayRows.results ?? []);
	const yesterdayNetUsd = sumUsd(yesterdayRows.results ?? []);

	const statusBreakdown: StatusSegment[] = (statusRows.results ?? [])
		.map((r) => ({
			key: r.status,
			label: STATUS_META[r.status]?.label ?? r.status,
			value: r.n,
			color: STATUS_META[r.status]?.color ?? "var(--sys-gray)",
			order: STATUS_META[r.status]?.order ?? 9,
		}))
		.sort((a, b) => a.order - b.order)
		.map((s) => ({ key: s.key, label: s.label, value: s.value, color: s.color }));

	const kpis: Kpis = {
		monthNetUsd,
		monthDeltaPct: pctDelta(monthNetUsd, prevMonthNetUsd),
		todayNetUsd,
		todayDeltaPct: pctDelta(todayNetUsd, yesterdayNetUsd),
		activeSubs: activeRow?.n ?? 0,
		newSubsThisMonth: newSubsRow?.n ?? 0,
		lifetimeMonthUsd: sumUsd(lifetimeRows.results ?? []),
		lifetimeMonthCount: lifetimeCountRow?.n ?? 0,
		refundMonthUsd: sumUsd(refundRows.results ?? []),
		refundMonthCount: refundCountRow?.n ?? 0,
		cumulativeNetUsd: sumUsd(cumulativeRows.results ?? []),
		totalSubs: totalSubsRow?.n ?? 0,
		totalNotifications: notifCountRow?.n ?? 0,
	};

	const transactions: TxnRow[] = (txnRows.results ?? []).map((r) => ({
		purchase_date: r.purchase_date,
		notification_type: r.notification_type,
		product_id: r.product_id,
		transaction_id: r.transaction_id,
		currency: r.currency,
		price_millis: r.price_millis,
		platform: r.platform ?? "apple",
		revoked: Boolean(r.revoked),
	}));

	const trendNetUsd = trend.reduce((a, p) => a + p.netUsd, 0);

	return {
		generatedAt: now.getTime(),
		range,
		periodLabel: `${now.getUTCFullYear()} 年 ${now.getUTCMonth() + 1} 月 · 截至 ${now.getUTCMonth() + 1}/${now.getUTCDate()}（UTC）`,
		kpis,
		trend,
		trendNetUsd,
		statusBreakdown,
		transactions,
		subscriptions: subRows.results ?? [],
		hasData: (totalSubsRow?.n ?? 0) > 0 || (notifCountRow?.n ?? 0) > 0,
	};
}
