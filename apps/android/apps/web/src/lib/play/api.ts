// Google Play Developer API 客户端（富化 RTDN 用）。
//
// RTDN 只给 purchaseToken：金额 / 到期 / 地区 / 退款状态都得回查这里。
// 鉴权是「服务账号自签 JWT -> access token」，因此必须保管一份私钥
// （Worker secret PLAY_SA_JSON = 服务账号 JSON 全文）。这是 Play 侧唯一可行的路子，
// 与 Apple 的「纯验签、零密钥」不同 —— 未配置时全部降级为「只记事件、不记金额」。
//
// 用到的三个端点：
//   purchases.subscriptionsv2.get  订阅状态 / 到期 / 续订开关 / 地区 / latestOrderId
//   purchases.products.get         一次性购买（买断）的购买状态与时间
//   orders.get                     订单金额：实付总额、税、开发者到手金额、买家国家

import { importPKCS8, SignJWT } from "jose";
import type { PlayOrder, ProductPurchaseV1, SubscriptionPurchaseV2 } from "./types";

const TOKEN_URL = "https://oauth2.googleapis.com/token";
const API_BASE = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications";
const SCOPE = "https://www.googleapis.com/auth/androidpublisher";

export class PlayApiError extends Error {
	constructor(
		message: string,
		readonly status: number,
		/** true = 值得让 Pub/Sub 重投递（限流 / 服务端故障 / 网络）。 */
		readonly retryable: boolean,
	) {
		super(message);
		this.name = "PlayApiError";
	}
}

interface ServiceAccount {
	client_email: string;
	private_key: string;
}

/** 解析 PLAY_SA_JSON；未配置或格式不对返回 null（调用方降级）。 */
export function parseServiceAccount(raw?: string | null): ServiceAccount | null {
	if (!raw?.trim()) return null;
	try {
		const sa = JSON.parse(raw) as Partial<ServiceAccount>;
		if (!sa.client_email || !sa.private_key) return null;
		// wrangler secret 经命令行传入时换行常被写成字面量 \n，这里统一还原。
		return { client_email: sa.client_email, private_key: sa.private_key.replace(/\\n/g, "\n") };
	} catch {
		return null;
	}
}

// access token 按 isolate 缓存（有效期 1h，提前 60s 过期）。
const tokenCache = new Map<string, { token: string; expiresAt: number }>();

async function getAccessToken(sa: ServiceAccount): Promise<string> {
	const cached = tokenCache.get(sa.client_email);
	if (cached && cached.expiresAt > Date.now()) return cached.token;

	const now = Math.floor(Date.now() / 1000);
	let assertion: string;
	try {
		const key = await importPKCS8(sa.private_key, "RS256");
		assertion = await new SignJWT({ scope: SCOPE })
			.setProtectedHeader({ alg: "RS256", typ: "JWT" })
			.setIssuer(sa.client_email)
			.setAudience(TOKEN_URL)
			.setIssuedAt(now)
			.setExpirationTime(now + 3600)
			.sign(key);
	} catch (err) {
		// 私钥坏了不是瞬时故障，重投递也修不好。
		throw new PlayApiError(`bad service account key: ${(err as Error).message}`, 0, false);
	}

	const res = await fetch(TOKEN_URL, {
		method: "POST",
		headers: { "content-type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({
			grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
			assertion,
		}),
	});
	if (!res.ok) {
		const body = await res.text();
		throw new PlayApiError(`token exchange failed: ${body.slice(0, 200)}`, res.status, res.status >= 500);
	}
	const json = (await res.json()) as { access_token?: string; expires_in?: number };
	if (!json.access_token) throw new PlayApiError("token exchange returned no token", 500, true);

	tokenCache.set(sa.client_email, {
		token: json.access_token,
		expiresAt: Date.now() + ((json.expires_in ?? 3600) - 60) * 1000,
	});
	return json.access_token;
}

/**
 * 哪些 HTTP 状态值得让 Pub/Sub 重投递。
 *
 * 429 / 5xx 是常规瞬时故障。401 / 403（insufficient permissions）也算——Play Console
 * 里给服务账号加「查看财务数据」/「管理订单和订阅」后，各 API 面的生效时间不同步，
 * 官方口径最长 24 小时。这个窗口里的真实购买若按「不可重试」处理，就会永久落成一笔
 * 没有金额的交易；对账本来说，那比重试噪音糟得多。权限一生效，重投递即自动补齐金额；
 * 真的配错了，也只是在 7 天投递窗口内退避重试后进死信，不会静默丢数据。
 */
export function isRetryableStatus(status: number): boolean {
	return status === 401 || status === 403 || status === 429 || status >= 500;
}

async function apiGet<T>(sa: ServiceAccount, path: string): Promise<T | null> {
	const token = await getAccessToken(sa);
	const res = await fetch(`${API_BASE}/${path}`, {
		headers: { authorization: `Bearer ${token}` },
	});
	if (res.status === 404 || res.status === 410) return null; // token/订单不存在：不重试
	if (!res.ok) {
		const body = await res.text();
		throw new PlayApiError(
			`GET ${path} -> ${res.status}: ${body.slice(0, 200)}`,
			res.status,
			isRetryableStatus(res.status),
		);
	}
	return (await res.json()) as T;
}

export class PlayApi {
	constructor(
		private readonly sa: ServiceAccount,
		private readonly packageName: string,
	) {}

	/** 未配置服务账号时返回 null，调用方据此降级。 */
	static from(rawSecret: string | undefined | null, packageName: string): PlayApi | null {
		const sa = parseServiceAccount(rawSecret);
		return sa ? new PlayApi(sa, packageName) : null;
	}

	getSubscription(purchaseToken: string): Promise<SubscriptionPurchaseV2 | null> {
		return apiGet<SubscriptionPurchaseV2>(
			this.sa,
			`${this.packageName}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`,
		);
	}

	getProduct(productId: string, purchaseToken: string): Promise<ProductPurchaseV1 | null> {
		return apiGet<ProductPurchaseV1>(
			this.sa,
			`${this.packageName}/purchases/products/${encodeURIComponent(productId)}/tokens/${encodeURIComponent(purchaseToken)}`,
		);
	}

	getOrder(orderId: string): Promise<PlayOrder | null> {
		return apiGet<PlayOrder>(this.sa, `${this.packageName}/orders/${encodeURIComponent(orderId)}`);
	}
}
