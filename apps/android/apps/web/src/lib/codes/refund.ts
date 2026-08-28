// 退款资格判定（纯函数）。Stripe 不强制面向用户的退款期，各支付方式的技术窗口都 >30 天，
// 故自设 30 天政策。用户在官网自助「申请」，后台审批后才真正退款（非即时自动退）。

export type RefundReason =
	| "ok"
	| "not_found"
	| "not_paid"
	| "already_refunded"
	| "already_requested"
	| "window_expired";

export interface RefundableRow {
	source: string; // stripe | manual | ...
	stripe_payment_intent: string | null;
	status: string; // active | revoked
	refund_status: string; // none | requested | approved | rejected
	created_at: number; // ms epoch
}

export const REFUND_WINDOW_DAYS = 30;
const WINDOW_MS = REFUND_WINDOW_DAYS * 86_400_000;

export function canRequestRefund(
	row: RefundableRow | null,
	now: number = Date.now(),
	windowMs: number = WINDOW_MS,
): { ok: boolean; reason: RefundReason } {
	if (!row) return { ok: false, reason: "not_found" };
	if (row.source !== "stripe" || !row.stripe_payment_intent) {
		return { ok: false, reason: "not_paid" };
	}
	// 已退款 / 已撤销
	if (row.status === "revoked" || row.refund_status === "approved") {
		return { ok: false, reason: "already_refunded" };
	}
	// 已有在途 / 已被拒的申请：不重复受理，引导联系人工
	if (row.refund_status === "requested" || row.refund_status === "rejected") {
		return { ok: false, reason: "already_requested" };
	}
	if (now - row.created_at > windowMs) {
		return { ok: false, reason: "window_expired" };
	}
	return { ok: true, reason: "ok" };
}
