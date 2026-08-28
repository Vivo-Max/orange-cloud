import { describe, expect, it } from "vitest";
import { timingSafeEqual, verifyAscSignature } from "./verify";

const SECRET = "test-webhook-secret";

async function hexMac(body: string, secret = SECRET): Promise<string> {
	const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
	const mac = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(body));
	return [...new Uint8Array(mac)].map((b) => b.toString(16).padStart(2, "0")).join("");
}
const header = async (body: string, secret = SECRET) => "hmacsha256=" + (await hexMac(body, secret));

describe("verifyAscSignature", () => {
	const body = JSON.stringify({ data: { type: "appStoreVersionAppVersionStateUpdated", attributes: { newValue: "READY_FOR_DISTRIBUTION" } } });

	it("接受正确签名", async () => {
		expect(await verifyAscSignature(body, await header(body), SECRET)).toBe(true);
	});

	it("拒绝篡改的 body", async () => {
		expect(await verifyAscSignature(body + "x", await header(body), SECRET)).toBe(false);
	});

	it("拒绝错误 secret 签出的头", async () => {
		expect(await verifyAscSignature(body, await header(body, "attacker"), SECRET)).toBe(false);
	});

	it("拒绝缺头 / 错前缀 / 空 secret", async () => {
		expect(await verifyAscSignature(body, null, SECRET)).toBe(false);
		expect(await verifyAscSignature(body, "sha256=deadbeef", SECRET)).toBe(false);
		expect(await verifyAscSignature(body, await header(body), "")).toBe(false);
	});

	it("hex 大小写不敏感（前缀保持小写）", async () => {
		const upper = "hmacsha256=" + (await hexMac(body)).toUpperCase();
		expect(await verifyAscSignature(body, upper, SECRET)).toBe(true);
	});
});

describe("timingSafeEqual", () => {
	it("等长相等 true，不等长 / 不等值 false", () => {
		expect(timingSafeEqual("abc123", "abc123")).toBe(true);
		expect(timingSafeEqual("abc123", "abc124")).toBe(false);
		expect(timingSafeEqual("abc", "abcd")).toBe(false);
	});
});
