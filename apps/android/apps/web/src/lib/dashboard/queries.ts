import { getDb, queryAll, queryFirst } from "./db";
import { ENVIRONMENT, PLATFORM_ORDER, PRODUCT_ORDER, type Filters } from "./types";

// ---------------------------------------------------------------------------
// Filter helpers
// ---------------------------------------------------------------------------

interface FilterCols {
	/** environment column name (omit if the table has none). */
	env?: string;
	/** product_id column name (omit to ignore the product filter). */
	product?: string;
	/** epoch-ms column the `days` window applies to (omit to ignore). */
	date?: string;
	/** platform column name (omit to ignore the store filter). */
	platform?: string;
}

/** Build an ` AND ...` SQL fragment + bound params for the active filters. */
function applyFilters(f: Filters, cols: FilterCols): { sql: string; params: unknown[] } {
	const clauses: string[] = [];
	const params: unknown[] = [];

	// The dashboard is Production-only: any table with an environment column is
	// always constrained to Production, regardless of user filters.
	if (cols.env) {
		clauses.push(`${cols.env} = ?`);
		params.push(ENVIRONMENT);
	}
	if (cols.product && f.productId) {
		clauses.push(`${cols.product} = ?`);
		params.push(f.productId);
	}
	if (cols.date && f.days) {
		clauses.push(`${cols.date} >= ?`);
		params.push(Date.now() - f.days * 86_400_000);
	}
	if (cols.platform && f.platform) {
		clauses.push(`${cols.platform} = ?`);
		params.push(f.platform);
	}

	return { sql: clauses.length ? ` AND ${clauses.join(" AND ")}` : "", params };
}

// ---------------------------------------------------------------------------
// Result shapes
// ---------------------------------------------------------------------------

export interface CurrencyRevenue {
	currency: string;
	count: number;
	sumMillis: number;
}

/** 单个平台（App Store / Google Play）的横向对比口径。 */
export interface PlatformSummary {
	platform: string;
	activeTotal: number;
	transactions: number;
	refunds: number;
	revenueByCurrency: CurrencyRevenue[];
}

export interface Overview {
	activeLifetime: number;
	activeSubscription: number;
	activeTotal: number;
	transactions: number;
	refunds: number;
	refundRate: number;
	autoRenewOn: number;
	autoRenewTotal: number;
	autoRenewRate: number;
	revenueByCurrency: CurrencyRevenue[];
	/** 分平台明细（只含有数据的平台，按 PLATFORM_ORDER 排序）。 */
	platforms: PlatformSummary[];
}

export interface StackedDay {
	date: string;
	total: number;
	segments: Record<string, number>;
}

export interface StackedSeries {
	keys: string[];
	days: StackedDay[];
	max: number;
}

export interface NameValue {
	name: string;
	value: number;
}

/** The transaction a notification refers to (joined on transaction_id). */
export interface LinkedTxn {
	transaction_id: string;
	product_id: string | null;
	type: string | null;
	offer_type: number | null;
	price_millis: number | null;
	currency: string | null;
	purchase_date: number | null;
	expires_date: number | null;
	revocation_date: number | null;
	revocation_reason: number | null;
}

export interface NotificationRow {
	notification_uuid: string;
	notification_type: string;
	subtype: string | null;
	original_transaction_id: string | null;
	transaction_id: string | null;
	environment: string | null;
	received_at: number;
	platform: string | null;
	/** Associated transaction, or null when none is linked/found. */
	txn: LinkedTxn | null;
}

interface NotificationJoinRow {
	notification_uuid: string;
	notification_type: string;
	subtype: string | null;
	original_transaction_id: string | null;
	transaction_id: string | null;
	environment: string | null;
	received_at: number;
	platform: string | null;
	t_id: string | null;
	t_product_id: string | null;
	t_type: string | null;
	t_offer_type: number | null;
	t_price_millis: number | null;
	t_currency: string | null;
	t_purchase_date: number | null;
	t_expires_date: number | null;
	t_revocation_date: number | null;
	t_revocation_reason: number | null;
}

export interface TransactionRow {
	transaction_id: string;
	original_transaction_id: string;
	product_id: string | null;
	type: string | null;
	offer_type: number | null;
	offer_identifier: string | null;
	storefront: string | null;
	price_millis: number | null;
	currency: string | null;
	environment: string | null;
	purchase_date: number | null;
	revocation_date: number | null;
	notification_type: string | null;
	platform: string | null;
}

export interface ExpiringRow {
	original_transaction_id: string;
	product_id: string | null;
	expires_date: number;
	auto_renew_status: number | null;
	environment: string | null;
	price_millis: number | null;
	currency: string | null;
	platform: string | null;
}

export interface Page<T> {
	rows: T[];
	total: number;
	page: number;
	pageSize: number;
	totalPages: number;
}

export interface ChartsData {
	purchaseTrend: StackedSeries;
	notificationTrend: StackedSeries;
	productMix: NameValue[];
	statusBreakdown: NameValue[];
}

// ---------------------------------------------------------------------------
// Pivot helper: turn (date, key, count) rows into a dense stacked series.
// ---------------------------------------------------------------------------

function pivotStacked(
	rows: { d: string; k: string; c: number }[],
	keyOrder?: readonly string[],
): StackedSeries {
	const byDate = new Map<string, Record<string, number>>();
	const seenKeys = new Set<string>();

	for (const { d, k, c } of rows) {
		seenKeys.add(k);
		const bucket = byDate.get(d) ?? {};
		bucket[k] = (bucket[k] ?? 0) + c;
		byDate.set(d, bucket);
	}

	// Stable key ordering: preferred order first, then any extras alphabetically.
	const extras = [...seenKeys].filter((k) => !keyOrder?.includes(k)).sort();
	const keys = [...(keyOrder?.filter((k) => seenKeys.has(k)) ?? []), ...extras];

	const dates = [...byDate.keys()].sort();
	const filledDates = fillDateRange(dates);

	let max = 0;
	const days: StackedDay[] = filledDates.map((date) => {
		const segments = byDate.get(date) ?? {};
		const total = Object.values(segments).reduce((a, b) => a + b, 0);
		if (total > max) max = total;
		return { date, total, segments };
	});

	return { keys, days, max };
}

/** Fill the gaps between the min and max date so the x-axis is continuous. */
function fillDateRange(dates: string[]): string[] {
	if (dates.length === 0) return [];
	const start = new Date(`${dates[0]}T00:00:00Z`).getTime();
	const end = new Date(`${dates[dates.length - 1]}T00:00:00Z`).getTime();
	const out: string[] = [];
	for (let t = start; t <= end; t += 86_400_000) {
		out.push(new Date(t).toISOString().slice(0, 10));
	}
	// Guard against pathological ranges (shouldn't happen with real data).
	return out.length > 0 && out.length <= 400 ? out : dates;
}

/** Clamp a requested page to a valid range and compute the SQL offset. */
function paginate(total: number, page: number, pageSize: number) {
	const totalPages = Math.max(1, Math.ceil(total / pageSize));
	const clamped = Math.min(Math.max(1, page), totalPages);
	return { page: clamped, totalPages, offset: (clamped - 1) * pageSize };
}

// ---------------------------------------------------------------------------
// Section queries
// ---------------------------------------------------------------------------

async function getOverview(db: D1Database, f: Filters): Promise<Overview> {
	const subActive = applyFilters(f, {
		env: "environment",
		product: "product_id",
		platform: "platform",
	});
	const tx = applyFilters(f, {
		env: "environment",
		product: "product_id",
		date: "purchase_date",
		platform: "platform",
	});

	const [lifetimeRows, txRow, autoRow, revenueRows, platformActive, platformTx] =
		await Promise.all([
		queryAll<{ is_lifetime: number; c: number }>(
			db,
			`SELECT is_lifetime, COUNT(*) c FROM subscriptions
			 WHERE status = 'active'${subActive.sql} GROUP BY is_lifetime`,
			subActive.params,
		),
		queryAll<{ total: number; refunds: number }>(
			db,
			`SELECT COUNT(*) total,
			        SUM(CASE WHEN revocation_date IS NOT NULL THEN 1 ELSE 0 END) refunds
			 FROM transactions WHERE 1 = 1${tx.sql}`,
			tx.params,
		),
		queryAll<{ on_count: number; total: number }>(
			db,
			`SELECT SUM(CASE WHEN auto_renew_status = 1 THEN 1 ELSE 0 END) on_count,
			        COUNT(*) total
			 FROM subscriptions
			 WHERE status = 'active' AND is_lifetime = 0${subActive.sql}`,
			subActive.params,
		),
		queryAll<{ currency: string; count: number; sum_millis: number }>(
			db,
			`SELECT currency, COUNT(*) count, SUM(price_millis) sum_millis
			 FROM transactions
			 WHERE revocation_date IS NULL${tx.sql}
			 GROUP BY currency
			 ORDER BY count DESC, currency ASC`,
			tx.params,
		),
		// 分平台明细：活跃权益来自 subscriptions，交易 / 退款 / 营收来自 transactions。
		queryAll<{ platform: string; c: number }>(
			db,
			`SELECT platform, COUNT(*) c FROM subscriptions
			 WHERE status = 'active'${subActive.sql} GROUP BY platform`,
			subActive.params,
		),
		queryAll<{
			platform: string;
			currency: string | null;
			total: number;
			paid: number;
			refunds: number;
			sum_millis: number;
		}>(
			db,
			`SELECT platform, currency, COUNT(*) total,
			        SUM(CASE WHEN revocation_date IS NULL THEN 1 ELSE 0 END) paid,
			        SUM(CASE WHEN revocation_date IS NOT NULL THEN 1 ELSE 0 END) refunds,
			        SUM(CASE WHEN revocation_date IS NULL THEN price_millis ELSE 0 END) sum_millis
			 FROM transactions WHERE 1 = 1${tx.sql}
			 GROUP BY platform, currency`,
			tx.params,
		),
	]);

	const activeLifetime = lifetimeRows.find((r) => r.is_lifetime === 1)?.c ?? 0;
	const activeSubscription = lifetimeRows.find((r) => r.is_lifetime === 0)?.c ?? 0;
	const txTotals = txRow[0] ?? { total: 0, refunds: 0 };
	const auto = autoRow[0] ?? { on_count: 0, total: 0 };

	// 把两组分平台行合并成 PlatformSummary[]（只保留真有数据的平台）。
	const summaries = new Map<string, PlatformSummary>();
	const summaryFor = (platform: string): PlatformSummary => {
		let s = summaries.get(platform);
		if (!s) {
			s = { platform, activeTotal: 0, transactions: 0, refunds: 0, revenueByCurrency: [] };
			summaries.set(platform, s);
		}
		return s;
	};
	for (const r of platformActive) summaryFor(r.platform ?? "apple").activeTotal += r.c;
	for (const r of platformTx) {
		const s = summaryFor(r.platform ?? "apple");
		s.transactions += r.total;
		s.refunds += r.refunds ?? 0;
		if ((r.paid ?? 0) > 0) {
			s.revenueByCurrency.push({
				currency: r.currency ?? "—",
				count: r.paid,
				sumMillis: r.sum_millis ?? 0,
			});
		}
	}
	const platforms = [...summaries.values()].sort(
		(a, b) =>
			PLATFORM_ORDER.indexOf(a.platform as (typeof PLATFORM_ORDER)[number]) -
			PLATFORM_ORDER.indexOf(b.platform as (typeof PLATFORM_ORDER)[number]),
	);

	return {
		platforms,
		activeLifetime,
		activeSubscription,
		activeTotal: activeLifetime + activeSubscription,
		transactions: txTotals.total ?? 0,
		refunds: txTotals.refunds ?? 0,
		refundRate: txTotals.total ? (txTotals.refunds ?? 0) / txTotals.total : 0,
		autoRenewOn: auto.on_count ?? 0,
		autoRenewTotal: auto.total ?? 0,
		autoRenewRate: auto.total ? (auto.on_count ?? 0) / auto.total : 0,
		revenueByCurrency: revenueRows.map((r) => ({
			currency: r.currency ?? "—",
			count: r.count,
			sumMillis: r.sum_millis ?? 0,
		})),
	};
}

async function getPurchaseTrend(db: D1Database, f: Filters): Promise<StackedSeries> {
	const { sql, params } = applyFilters(f, {
		env: "environment",
		product: "product_id",
		date: "purchase_date",
		platform: "platform",
	});
	const rows = await queryAll<{ d: string; product_id: string | null; c: number }>(
		db,
		`SELECT date(purchase_date / 1000, 'unixepoch') d, product_id, COUNT(*) c
		 FROM transactions
		 WHERE purchase_date IS NOT NULL${sql}
		 GROUP BY d, product_id
		 ORDER BY d`,
		params,
	);
	return pivotStacked(
		rows.map((r) => ({ d: r.d, k: r.product_id ?? "其他", c: r.c })),
		PRODUCT_ORDER,
	);
}

async function getNotificationTrend(db: D1Database, f: Filters): Promise<StackedSeries> {
	// notifications 表无 product 列：产品筛选经关联交易（transaction_id -> transactions.product_id），
	// 与下方通知列表口径一致。
	const { sql, params } = applyFilters(f, {
		env: "n.environment",
		product: "t.product_id",
		date: "n.received_at",
		platform: "n.platform",
	});
	const rows = await queryAll<{ d: string; notification_type: string; c: number }>(
		db,
		`SELECT date(n.received_at / 1000, 'unixepoch') d, n.notification_type, COUNT(*) c
		 FROM notifications n
		 LEFT JOIN transactions t ON t.transaction_id = n.transaction_id
		 WHERE 1 = 1${sql}
		 GROUP BY d, n.notification_type
		 ORDER BY d`,
		params,
	);
	return pivotStacked(rows.map((r) => ({ d: r.d, k: r.notification_type, c: r.c })));
}

async function getProductMix(db: D1Database, f: Filters): Promise<NameValue[]> {
	const { sql, params } = applyFilters(f, {
		env: "environment",
		product: "product_id",
		platform: "platform",
	});
	const rows = await queryAll<{ product_id: string | null; c: number }>(
		db,
		`SELECT product_id, COUNT(*) c FROM subscriptions
		 WHERE status = 'active'${sql} GROUP BY product_id ORDER BY c DESC`,
		params,
	);
	return rows.map((r) => ({ name: r.product_id ?? "—", value: r.c }));
}

async function getStatusBreakdown(db: D1Database, f: Filters): Promise<NameValue[]> {
	const { sql, params } = applyFilters(f, {
		env: "environment",
		product: "product_id",
		platform: "platform",
	});
	const rows = await queryAll<{ status: string | null; c: number }>(
		db,
		`SELECT status, COUNT(*) c FROM subscriptions
		 WHERE 1 = 1${sql} GROUP BY status ORDER BY c DESC`,
		params,
	);
	return rows.map((r) => ({ name: r.status ?? "unknown", value: r.c }));
}

async function getNotificationsPage(
	db: D1Database,
	f: Filters,
	page: number,
	pageSize: number,
): Promise<Page<NotificationRow>> {
	// notifications 表无 product 列：产品筛选经关联交易（transaction_id -> transactions.product_id）。
	// 故计数与取数都 LEFT JOIN transactions，env/日期按 n、产品按 t；两处共用同一套条件。
	const filter = applyFilters(f, {
		env: "n.environment",
		product: "t.product_id",
		date: "n.received_at",
		platform: "n.platform",
	});
	const totalRow = await queryFirst<{ c: number }>(
		db,
		`SELECT COUNT(*) c FROM notifications n
		 LEFT JOIN transactions t ON t.transaction_id = n.transaction_id
		 WHERE 1 = 1${filter.sql}`,
		filter.params,
	);
	const total = totalRow?.c ?? 0;
	const { page: p, totalPages, offset } = paginate(total, page, pageSize);

	// Join the referenced transaction so the UI can show full details (e.g.
	// product, amount, and refund date/reason) when a notification is clicked.
	const raw = await queryAll<NotificationJoinRow>(
		db,
		`SELECT n.notification_uuid, n.notification_type, n.subtype,
		        n.original_transaction_id, n.transaction_id, n.environment, n.received_at, n.platform,
		        t.transaction_id AS t_id, t.product_id AS t_product_id, t.type AS t_type,
		        t.offer_type AS t_offer_type, t.price_millis AS t_price_millis,
		        t.currency AS t_currency, t.purchase_date AS t_purchase_date,
		        t.expires_date AS t_expires_date, t.revocation_date AS t_revocation_date,
		        t.revocation_reason AS t_revocation_reason
		 FROM notifications n
		 LEFT JOIN transactions t ON t.transaction_id = n.transaction_id
		 WHERE 1 = 1${filter.sql}
		 ORDER BY n.received_at DESC LIMIT ? OFFSET ?`,
		[...filter.params, pageSize, offset],
	);

	const rows: NotificationRow[] = raw.map((r) => ({
		notification_uuid: r.notification_uuid,
		notification_type: r.notification_type,
		subtype: r.subtype,
		original_transaction_id: r.original_transaction_id,
		transaction_id: r.transaction_id,
		environment: r.environment,
		received_at: r.received_at,
		platform: r.platform,
		txn: r.t_id
			? {
					transaction_id: r.t_id,
					product_id: r.t_product_id,
					type: r.t_type,
					offer_type: r.t_offer_type,
					price_millis: r.t_price_millis,
					currency: r.t_currency,
					purchase_date: r.t_purchase_date,
					expires_date: r.t_expires_date,
					revocation_date: r.t_revocation_date,
					revocation_reason: r.t_revocation_reason,
				}
			: null,
	}));
	return { rows, total, page: p, pageSize, totalPages };
}

async function getTransactionsPage(
	db: D1Database,
	f: Filters,
	page: number,
	pageSize: number,
): Promise<Page<TransactionRow>> {
	const { sql, params } = applyFilters(f, {
		env: "environment",
		product: "product_id",
		date: "purchase_date",
		platform: "platform",
	});
	const totalRow = await queryFirst<{ c: number }>(
		db,
		`SELECT COUNT(*) c FROM transactions WHERE 1 = 1${sql}`,
		params,
	);
	const total = totalRow?.c ?? 0;
	const { page: p, totalPages, offset } = paginate(total, page, pageSize);
	const rows = await queryAll<TransactionRow>(
		db,
		`SELECT transaction_id, original_transaction_id, product_id, type, offer_type,
		        offer_identifier, storefront, price_millis, currency, environment, purchase_date,
		        revocation_date, notification_type, platform
		 FROM transactions
		 WHERE 1 = 1${sql}
		 ORDER BY created_at DESC LIMIT ? OFFSET ?`,
		[...params, pageSize, offset],
	);
	return { rows, total, page: p, pageSize, totalPages };
}

async function getExpiringSoon(db: D1Database, f: Filters): Promise<ExpiringRow[]> {
	const { sql, params } = applyFilters(f, {
		env: "environment",
		product: "product_id",
		platform: "platform",
	});
	return queryAll<ExpiringRow>(
		db,
		`SELECT original_transaction_id, product_id, expires_date,
		        auto_renew_status, environment, price_millis, currency, platform
		 FROM subscriptions
		 WHERE status = 'active' AND is_lifetime = 0
		   AND expires_date IS NOT NULL AND expires_date > ?${sql}
		 ORDER BY expires_date ASC LIMIT 50`,
		[Date.now(), ...params],
	);
}

// ---------------------------------------------------------------------------
// Aggregate: everything the dashboard page needs, fetched concurrently.
// ---------------------------------------------------------------------------

// Each section fetches independently so the page can stream it behind its own
// Suspense boundary. getDb()/getCloudflareContext is request-cached, so calling
// it per section is cheap, and sibling sections still run concurrently.

export async function fetchOverview(f: Filters): Promise<Overview> {
	return getOverview(await getDb(), f);
}

export async function fetchCharts(f: Filters): Promise<ChartsData> {
	const db = await getDb();
	const [purchaseTrend, notificationTrend, productMix, statusBreakdown] = await Promise.all([
		getPurchaseTrend(db, f),
		getNotificationTrend(db, f),
		getProductMix(db, f),
		getStatusBreakdown(db, f),
	]);
	return { purchaseTrend, notificationTrend, productMix, statusBreakdown };
}

export async function fetchTransactions(
	f: Filters,
	page: number,
	pageSize: number,
): Promise<Page<TransactionRow>> {
	return getTransactionsPage(await getDb(), f, page, pageSize);
}

export async function fetchNotifications(
	f: Filters,
	page: number,
	pageSize: number,
): Promise<Page<NotificationRow>> {
	return getNotificationsPage(await getDb(), f, page, pageSize);
}

export async function fetchExpiring(f: Filters): Promise<ExpiringRow[]> {
	return getExpiringSoon(await getDb(), f);
}
