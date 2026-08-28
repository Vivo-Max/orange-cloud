import { describe, it, expect } from "vitest";
import { constructEvent, StripeSignatureError } from "./webhook";

const SECRET = "whsec_test_secret";
const encoder = new TextEncoder();

// 用与被测代码相同的算法本地签出一个 v1，便于构造合法 / 非法用例。
async function sign(secret: string, payload: string): Promise<string> {
	const key = await crypto.subtle.importKey(
		"raw",
		encoder.encode(secret),
		{ name: "HMAC", hash: "SHA-256" },
		false,
		["sign"],
	);
	const mac = new Uint8Array(await crypto.subtle.sign("HMAC", key, encoder.encode(payload)));
	let h = "";
	for (const b of mac) h += b.toString(16).padStart(2, "0");
	return h;
}

function header(t: number, v1: string): string {
	return `t=${t},v1=${v1}`;
}

const body = JSON.stringify({ id: "evt_1", type: "checkout.session.completed", data: { object: { id: "cs_1" } } });

describe("constructEvent", () => {
	it("合法签名 -> 返回解析后的事件", async () => {
		const t = Math.floor(Date.now() / 1000);
		const sig = await sign(SECRET, `${t}.${body}`);
		const event = await constructEvent(body, header(t, sig), SECRET);
		expect(event.id).toBe("evt_1");
		expect(event.type).toBe("checkout.session.completed");
	});

	it("篡改 body -> 验签失败", async () => {
		const t = Math.floor(Date.now() / 1000);
		const sig = await sign(SECRET, `${t}.${body}`);
		await expect(constructEvent(body + "x", header(t, sig), SECRET)).rejects.toBeInstanceOf(
			StripeSignatureError,
		);
	});

	it("错误密钥 -> 验签失败", async () => {
		const t = Math.floor(Date.now() / 1000);
		const sig = await sign("whsec_wrong", `${t}.${body}`);
		await expect(constructEvent(body, header(t, sig), SECRET)).rejects.toBeInstanceOf(
			StripeSignatureError,
		);
	});

	it("时间戳超窗 -> 拒绝（挡重放）", async () => {
		const t = Math.floor(Date.now() / 1000) - 10_000;
		const sig = await sign(SECRET, `${t}.${body}`);
		await expect(constructEvent(body, header(t, sig), SECRET)).rejects.toBeInstanceOf(
			StripeSignatureError,
		);
	});

	it("缺 secret / 缺头 / 头畸形 -> 拒绝", async () => {
		const t = Math.floor(Date.now() / 1000);
		const sig = await sign(SECRET, `${t}.${body}`);
		await expect(constructEvent(body, header(t, sig), "")).rejects.toBeInstanceOf(StripeSignatureError);
		await expect(constructEvent(body, null, SECRET)).rejects.toBeInstanceOf(StripeSignatureError);
		await expect(constructEvent(body, "garbage", SECRET)).rejects.toBeInstanceOf(StripeSignatureError);
	});

	it("多个 v1 候选里有一个对就放行（Stripe 轮换密钥期）", async () => {
		const t = Math.floor(Date.now() / 1000);
		const good = await sign(SECRET, `${t}.${body}`);
		const sigHeader = `t=${t},v1=deadbeef,v1=${good}`;
		const event = await constructEvent(body, sigHeader, SECRET);
		expect(event.id).toBe("evt_1");
	});
});
