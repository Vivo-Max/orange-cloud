// App Store Connect webhook 验签。
// 安全边界 = HMAC-SHA256(rawBody, secret) 与请求头 `X-Apple-Signature` 比对。
// 头格式：`hmacsha256=<hex>`（hex 小写）。secret 在 ASC 后台创建 webhook 时设定。

const PREFIX = "hmacsha256=";

/** 验证 ASC webhook 签名。secret 缺失、头缺失/格式错、或不匹配均返回 false。 */
export async function verifyAscSignature(
	rawBody: string,
	header: string | null,
	secret: string,
): Promise<boolean> {
	if (!secret || !header || !header.startsWith(PREFIX)) return false;
	const provided = header.slice(PREFIX.length).trim().toLowerCase();

	const key = await crypto.subtle.importKey(
		"raw",
		new TextEncoder().encode(secret),
		{ name: "HMAC", hash: "SHA-256" },
		false,
		["sign"],
	);
	const mac = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(rawBody));
	const expected = [...new Uint8Array(mac)].map((b) => b.toString(16).padStart(2, "0")).join("");

	return timingSafeEqual(provided, expected);
}

/** 常数时间字符串比较（等长才比，避免长度/提前返回泄露信息）。 */
export function timingSafeEqual(a: string, b: string): boolean {
	if (a.length !== b.length) return false;
	let diff = 0;
	for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
	return diff === 0;
}
