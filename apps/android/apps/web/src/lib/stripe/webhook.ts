// 校验 Stripe webhook 签名 —— 手写，不引 SDK（同 appstore/verify.ts 的纯 WebCrypto 风格）。
// Stripe-Signature 头形如 `t=1700000000,v1=abc...,v1=def...`。
// 期望签名 = HMAC-SHA256(secret, `${t}.${rawBody}`) 的十六进制；常数时间比对，并校验时间戳窗口。

const encoder = new TextEncoder();
const DEFAULT_TOLERANCE_S = 300; // 5 分钟，挡重放

function timingSafeEqual(a: Uint8Array, b: Uint8Array): boolean {
	if (a.length !== b.length) return false;
	let diff = 0;
	for (let i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
	return diff === 0;
}

function hex(bytes: Uint8Array): string {
	let s = "";
	for (const b of bytes) s += b.toString(16).padStart(2, "0");
	return s;
}

async function hmacSha256Hex(secret: string, msg: string): Promise<string> {
	const key = await crypto.subtle.importKey(
		"raw",
		encoder.encode(secret),
		{ name: "HMAC", hash: "SHA-256" },
		false,
		["sign"],
	);
	return hex(new Uint8Array(await crypto.subtle.sign("HMAC", key, encoder.encode(msg))));
}

function parseSigHeader(header: string): { t: number; v1: string[] } {
	let t = 0;
	const v1: string[] = [];
	for (const part of header.split(",")) {
		const idx = part.indexOf("=");
		if (idx < 0) continue;
		const k = part.slice(0, idx).trim();
		const v = part.slice(idx + 1).trim();
		if (k === "t") t = parseInt(v, 10);
		else if (k === "v1" && v) v1.push(v);
	}
	return { t, v1 };
}

export class StripeSignatureError extends Error {}

export interface StripeEvent {
	id: string;
	type: string;
	data: { object: Record<string, unknown> };
}

/**
 * 验签并返回解析后的事件。失败抛 StripeSignatureError。
 * rawBody 必须是原始未解析的请求体字符串（不能先 JSON.parse 再 stringify）。
 */
export async function constructEvent(
	rawBody: string,
	sigHeader: string | null,
	secret: string,
	toleranceS: number = DEFAULT_TOLERANCE_S,
	now: number = Date.now(),
): Promise<StripeEvent> {
	if (!secret) throw new StripeSignatureError("missing webhook secret");
	if (!sigHeader) throw new StripeSignatureError("missing signature header");

	const { t, v1 } = parseSigHeader(sigHeader);
	if (!t || v1.length === 0) throw new StripeSignatureError("malformed signature header");
	if (Math.abs(now / 1000 - t) > toleranceS)
		throw new StripeSignatureError("timestamp outside tolerance");

	const expected = encoder.encode(await hmacSha256Hex(secret, `${t}.${rawBody}`));
	const matched = v1.some((s) => timingSafeEqual(encoder.encode(s), expected));
	if (!matched) throw new StripeSignatureError("signature mismatch");

	return JSON.parse(rawBody) as StripeEvent;
}
