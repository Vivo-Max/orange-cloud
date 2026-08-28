import { describe, it, expect } from "vitest";
import { canRequestRefund, REFUND_WINDOW_DAYS, type RefundableRow } from "./refund";

const NOW = 1_700_000_000_000;
const fresh: RefundableRow = {
	source: "stripe",
	stripe_payment_intent: "pi_123",
	status: "active",
	refund_status: "none",
	created_at: NOW - 86_400_000,
}; // 1 天前

describe("canRequestRefund", () => {
	it("码不存在 -> not_found", () => {
		expect(canRequestRefund(null, NOW)).toMatchObject({ ok: false, reason: "not_found" });
	});

	it("非 Stripe / 缺 paymentIntent -> not_paid", () => {
		expect(canRequestRefund({ ...fresh, source: "manual" }, NOW).reason).toBe("not_paid");
		expect(canRequestRefund({ ...fresh, stripe_payment_intent: null }, NOW).reason).toBe("not_paid");
	});

	it("窗口内、未申请、未撤销 -> 放行", () => {
		expect(canRequestRefund(fresh, NOW)).toMatchObject({ ok: true, reason: "ok" });
	});

	it("已撤销 / 已批准退款 -> already_refunded", () => {
		expect(canRequestRefund({ ...fresh, status: "revoked" }, NOW).reason).toBe("already_refunded");
		expect(canRequestRefund({ ...fresh, refund_status: "approved" }, NOW).reason).toBe("already_refunded");
	});

	it("已申请 / 已被拒 -> already_requested（不重复受理）", () => {
		expect(canRequestRefund({ ...fresh, refund_status: "requested" }, NOW).reason).toBe("already_requested");
		expect(canRequestRefund({ ...fresh, refund_status: "rejected" }, NOW).reason).toBe("already_requested");
	});

	it("超出 30 天窗口 -> window_expired", () => {
		const old = { ...fresh, created_at: NOW - (REFUND_WINDOW_DAYS + 1) * 86_400_000 };
		expect(canRequestRefund(old, NOW).reason).toBe("window_expired");
	});

	it("恰好第 30 天边界内仍可申请", () => {
		const edge = { ...fresh, created_at: NOW - REFUND_WINDOW_DAYS * 86_400_000 + 1000 };
		expect(canRequestRefund(edge, NOW).ok).toBe(true);
	});
});
