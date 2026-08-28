import { describe, it, expect } from "vitest";
import { decideRedeem, MAX_ACTIVATIONS, type CodeRow } from "./redeem";

const active: CodeRow = { code: "ABCDE12345", product: "pro.lifetime", status: "active" };

describe("decideRedeem", () => {
	it("码不存在 -> not_found", () => {
		expect(decideRedeem(null, [], "dev-1")).toMatchObject({ ok: false, reason: "not_found" });
	});

	it("已撤销 -> revoked（带 product 便于客户端提示）", () => {
		const d = decideRedeem({ ...active, status: "revoked" }, ["dev-1"], "dev-1");
		expect(d).toMatchObject({ ok: false, reason: "revoked", product: "pro.lifetime" });
	});

	it("全新设备、名额未满 -> 放行且需绑定", () => {
		expect(decideRedeem(active, [], "dev-1")).toMatchObject({ ok: true, reason: "ok", bind: true });
	});

	it("已绑定设备复核 -> 幂等放行、不再绑定", () => {
		const d = decideRedeem(active, ["dev-1", "dev-2"], "dev-1");
		expect(d).toMatchObject({ ok: true, reason: "ok", bind: false });
	});

	it("新设备但达到上限 -> device_limit", () => {
		const bound = Array.from({ length: MAX_ACTIVATIONS }, (_, i) => `dev-${i}`);
		expect(decideRedeem(active, bound, "dev-new")).toMatchObject({ ok: false, reason: "device_limit" });
	});

	it("达到上限但本机已在名单内 -> 仍放行（复核优先于名额判断）", () => {
		const bound = Array.from({ length: MAX_ACTIVATIONS }, (_, i) => `dev-${i}`);
		expect(decideRedeem(active, bound, "dev-0")).toMatchObject({ ok: true, bind: false });
	});
});
