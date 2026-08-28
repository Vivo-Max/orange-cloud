// 后台「激活码（安卓渠道）」只读查询。迁移 0005 未应用时各查询容错返回空，
// 不阻断既有 IAP 看板渲染。

import { formatCode } from "./code";

export interface CodeRevenue {
	currency: string;
	minor: number; // 货币最小单位合计（Stripe amount_total 口径）
	count: number;
}

export interface CodesStats {
	sold: number;
	active: number;
	revoked: number;
	pendingRefunds: number;
	revenue: CodeRevenue[];
}

export interface AdminCode {
	code: string; // 核心串
	display: string; // OC-XXXXX-XXXXX
	status: string;
	source: string;
	amountTotal: number | null;
	currency: string | null;
	buyerEmail: string | null;
	activations: number;
	refundStatus: string;
	note: string | null;
	createdAt: number;
}

export interface PendingRefund {
	code: string;
	display: string;
	buyerEmail: string | null;
	reason: string | null;
	requestedAt: number | null;
	createdAt: number;
	amountTotal: number | null;
	currency: string | null;
}

export async function getCodesStats(db: D1Database): Promise<CodesStats> {
	try {
		const counts = await db
			.prepare(
				`SELECT
				   COUNT(*) AS sold,
				   SUM(CASE WHEN status = 'active' THEN 1 ELSE 0 END) AS active,
				   SUM(CASE WHEN status = 'revoked' THEN 1 ELSE 0 END) AS revoked,
				   SUM(CASE WHEN refund_status = 'requested' THEN 1 ELSE 0 END) AS pending
				 FROM codes WHERE source = 'stripe'`,
			)
			.first<{ sold: number; active: number; revoked: number; pending: number }>();

		const rev = await db
			.prepare(
				`SELECT currency, SUM(amount_total) AS minor, COUNT(*) AS count
				 FROM codes
				 WHERE source = 'stripe' AND status = 'active' AND amount_total IS NOT NULL AND currency IS NOT NULL
				 GROUP BY currency ORDER BY minor DESC`,
			)
			.all<{ currency: string; minor: number; count: number }>();

		return {
			sold: counts?.sold ?? 0,
			active: counts?.active ?? 0,
			revoked: counts?.revoked ?? 0,
			pendingRefunds: counts?.pending ?? 0,
			revenue: rev.results ?? [],
		};
	} catch {
		return { sold: 0, active: 0, revoked: 0, pendingRefunds: 0, revenue: [] };
	}
}

export async function listRecentCodes(db: D1Database, limit = 50): Promise<AdminCode[]> {
	try {
		const rows = await db
			.prepare(
				`SELECT c.code, c.status, c.source, c.amount_total, c.currency, c.buyer_email,
				        c.refund_status, c.note, c.created_at,
				        (SELECT COUNT(*) FROM code_activations a WHERE a.code = c.code) AS activations
				 FROM codes c ORDER BY c.created_at DESC LIMIT ?`,
			)
			.bind(limit)
			.all<{
				code: string;
				status: string;
				source: string;
				amount_total: number | null;
				currency: string | null;
				buyer_email: string | null;
				refund_status: string;
				note: string | null;
				created_at: number;
				activations: number;
			}>();
		return (rows.results ?? []).map((r) => ({
			code: r.code,
			display: formatCode(r.code),
			status: r.status,
			source: r.source,
			amountTotal: r.amount_total,
			currency: r.currency,
			buyerEmail: r.buyer_email,
			activations: r.activations,
			refundStatus: r.refund_status,
			note: r.note,
			createdAt: r.created_at,
		}));
	} catch {
		return [];
	}
}

export async function listPendingRefunds(db: D1Database): Promise<PendingRefund[]> {
	try {
		const rows = await db
			.prepare(
				`SELECT code, buyer_email, refund_reason, refund_requested_at, created_at, amount_total, currency
				 FROM codes WHERE refund_status = 'requested' ORDER BY refund_requested_at ASC`,
			)
			.all<{
				code: string;
				buyer_email: string | null;
				refund_reason: string | null;
				refund_requested_at: number | null;
				created_at: number;
				amount_total: number | null;
				currency: string | null;
			}>();
		return (rows.results ?? []).map((r) => ({
			code: r.code,
			display: formatCode(r.code),
			buyerEmail: r.buyer_email,
			reason: r.refund_reason,
			requestedAt: r.refund_requested_at,
			createdAt: r.created_at,
			amountTotal: r.amount_total,
			currency: r.currency,
		}));
	} catch {
		return [];
	}
}
