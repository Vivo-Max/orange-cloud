import { describe, it, expect } from "vitest";
import { canSelfBindEmail } from "./bind";

describe("canSelfBindEmail", () => {
	it("码不存在 -> invalid", () => {
		expect(canSelfBindEmail(null).reason).toBe("invalid");
	});

	it("未绑定邮箱（null 或空串）+ 有效 -> 放行", () => {
		expect(canSelfBindEmail({ status: "active", buyer_email: null })).toMatchObject({ ok: true });
		expect(canSelfBindEmail({ status: "active", buyer_email: "  " })).toMatchObject({ ok: true });
	});

	it("已绑定邮箱 -> already_bound（须走人工改）", () => {
		expect(canSelfBindEmail({ status: "active", buyer_email: "a@b.com" }).reason).toBe("already_bound");
	});

	it("已撤销 -> revoked", () => {
		expect(canSelfBindEmail({ status: "revoked", buyer_email: null }).reason).toBe("revoked");
	});
});
