import { describe, it, expect } from "vitest";
import { canSelfReset, SELF_RESET_WINDOW_DAYS, type SelfResetRow } from "./selfreset";

const NOW = 1_700_000_000_000;
const base: SelfResetRow = { status: "active", buyer_email: "buyer@example.com", last_self_reset_at: null };

describe("canSelfReset", () => {
	it("码不存在 -> invalid（与邮箱不符同口径，防枚举）", () => {
		expect(canSelfReset(null, "buyer@example.com", NOW).reason).toBe("invalid");
	});

	it("邮箱匹配（忽略大小写/空格）+ 从未重置 -> 放行", () => {
		expect(canSelfReset(base, "  Buyer@Example.com ", NOW)).toMatchObject({ ok: true, reason: "ok" });
	});

	it("邮箱不符 -> invalid", () => {
		expect(canSelfReset(base, "someone@else.com", NOW).reason).toBe("invalid");
	});

	it("无登记邮箱 -> no_email（手动/赠码走人工）", () => {
		expect(canSelfReset({ ...base, buyer_email: null }, "x@y.com", NOW).reason).toBe("no_email");
	});

	it("已撤销 -> revoked", () => {
		expect(canSelfReset({ ...base, status: "revoked" }, "buyer@example.com", NOW).reason).toBe("revoked");
	});

	it("距上次重置不足 30 天 -> rate_limited，带 nextAt", () => {
		const last = NOW - 10 * 86_400_000; // 10 天前
		const d = canSelfReset({ ...base, last_self_reset_at: last }, "buyer@example.com", NOW);
		expect(d).toMatchObject({ ok: false, reason: "rate_limited" });
		expect(d.nextAt).toBe(last + SELF_RESET_WINDOW_DAYS * 86_400_000);
	});

	it("超过 30 天 -> 可再次重置", () => {
		const last = NOW - (SELF_RESET_WINDOW_DAYS + 1) * 86_400_000;
		expect(canSelfReset({ ...base, last_self_reset_at: last }, "buyer@example.com", NOW).ok).toBe(true);
	});
});
