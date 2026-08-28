// Play RTDN 的验签、解码与账本映射（纯逻辑，不碰 D1 / 网络）。

import { describe, expect, it } from "vitest";
import { generateKeyPair, SignJWT, type JWTVerifyGetKey } from "jose";
import { decodeEnvelope, PlayVerifyError, verifyPushRequest, verifyPushToken } from "./verify";
import { buildLedgerRows, mapSubscriptionState, notificationTypeName } from "./logic";
import { buildPlayBarkMessage } from "./notify";
import { moneyToMillis, type DeveloperNotification, type PlayEnrichment } from "./types";
import { isRetryableStatus, parseServiceAccount } from "./api";

const PACKAGE = "jiamin.chen.orangecloud";
const AUDIENCE = "https://oss.omail.us.kg/api/play/notifications";
const SA_EMAIL = "play-rtdn@example.iam.gserviceaccount.com";

function envelope(n: DeveloperNotification, messageId = "m1") {
	return {
		message: { data: btoa(JSON.stringify(n)), messageId, publishTime: "2026-08-01T00:00:00Z" },
		subscription: "projects/p/subscriptions/s",
	};
}

const SUB_PURCHASE: DeveloperNotification = {
	version: "1.0",
	packageName: PACKAGE,
	eventTimeMillis: "1754006400000",
	subscriptionNotification: { version: "1.0", notificationType: 4, purchaseToken: "tok-abc" },
};

// ---------------------------------------------------------------------------
// OIDC 验签
// ---------------------------------------------------------------------------

async function signedToken(claims: Record<string, unknown>, key: CryptoKey) {
	return new SignJWT(claims)
		.setProtectedHeader({ alg: "RS256" })
		.setIssuedAt()
		.setExpirationTime("5m")
		.sign(key);
}

describe("verifyPushToken", () => {
	it("接受 Google 签发、aud / email 都对的 token", async () => {
		const { privateKey, publicKey } = await generateKeyPair("RS256");
		const keys = (async () => publicKey) as unknown as JWTVerifyGetKey;
		const token = await signedToken(
			{ iss: "https://accounts.google.com", aud: AUDIENCE, email: SA_EMAIL, email_verified: true },
			privateKey,
		);
		await expect(
			verifyPushToken(`Bearer ${token}`, {
				audience: AUDIENCE,
				serviceAccountEmail: SA_EMAIL,
				packageName: PACKAGE,
				keys,
			}),
		).resolves.toBeUndefined();
	});

	it("拒绝 audience 不符的 token", async () => {
		const { privateKey, publicKey } = await generateKeyPair("RS256");
		const keys = (async () => publicKey) as unknown as JWTVerifyGetKey;
		const token = await signedToken(
			{ iss: "https://accounts.google.com", aud: "https://evil.example", email: SA_EMAIL },
			privateKey,
		);
		await expect(
			verifyPushToken(`Bearer ${token}`, { audience: AUDIENCE, packageName: PACKAGE, keys }),
		).rejects.toBeInstanceOf(PlayVerifyError);
	});

	it("拒绝别的服务账号签发的 token", async () => {
		const { privateKey, publicKey } = await generateKeyPair("RS256");
		const keys = (async () => publicKey) as unknown as JWTVerifyGetKey;
		const token = await signedToken(
			{
				iss: "https://accounts.google.com",
				aud: AUDIENCE,
				email: "someone-else@example.com",
				email_verified: true,
			},
			privateKey,
		);
		await expect(
			verifyPushToken(`Bearer ${token}`, {
				audience: AUDIENCE,
				serviceAccountEmail: SA_EMAIL,
				packageName: PACKAGE,
				keys,
			}),
		).rejects.toThrow(/unexpected token subject/);
	});

	it("拒绝篡改过的 token（换密钥签名）", async () => {
		const attacker = await generateKeyPair("RS256");
		const google = await generateKeyPair("RS256");
		const keys = (async () => google.publicKey) as unknown as JWTVerifyGetKey;
		const token = await signedToken(
			{ iss: "https://accounts.google.com", aud: AUDIENCE, email: SA_EMAIL },
			attacker.privateKey,
		);
		await expect(
			verifyPushToken(`Bearer ${token}`, { audience: AUDIENCE, packageName: PACKAGE, keys }),
		).rejects.toBeInstanceOf(PlayVerifyError);
	});

	it("缺 Authorization 头直接拒", async () => {
		await expect(
			verifyPushToken(null, { audience: AUDIENCE, packageName: PACKAGE }),
		).rejects.toThrow(/missing bearer token/);
	});
});

describe("verifyPushRequest 包名校验", () => {
	it("拒绝别的 App 的通知", async () => {
		const { privateKey, publicKey } = await generateKeyPair("RS256");
		const keys = (async () => publicKey) as unknown as JWTVerifyGetKey;
		const token = await signedToken(
			{ iss: "https://accounts.google.com", aud: AUDIENCE, email: SA_EMAIL },
			privateKey,
		);
		await expect(
			verifyPushRequest(
				`Bearer ${token}`,
				envelope({ ...SUB_PURCHASE, packageName: "com.evil.app" }),
				{ audience: AUDIENCE, packageName: PACKAGE, keys },
			),
		).rejects.toThrow(/unexpected packageName/);
	});
});

// ---------------------------------------------------------------------------
// 信封解码
// ---------------------------------------------------------------------------

describe("decodeEnvelope", () => {
	it("解出 messageId / eventTime / 通知体", () => {
		const d = decodeEnvelope(envelope(SUB_PURCHASE, "msg-7"));
		expect(d.messageId).toBe("msg-7");
		expect(d.eventTimeMillis).toBe(1_754_006_400_000);
		expect(d.notification.subscriptionNotification?.purchaseToken).toBe("tok-abc");
	});

	it("data 不是 base64 JSON 时报错", () => {
		expect(() => decodeEnvelope({ message: { data: "!!!", messageId: "x" } })).toThrow(
			PlayVerifyError,
		);
	});

	it("缺 messageId 时报错（没有幂等键）", () => {
		expect(() => decodeEnvelope({ message: { data: btoa("{}") } })).toThrow(/message id/);
	});
});

// ---------------------------------------------------------------------------
// 金额与状态映射
// ---------------------------------------------------------------------------

describe("moneyToMillis", () => {
	it("units + nanos -> milliunits", () => {
		expect(moneyToMillis({ currencyCode: "USD", units: "19", nanos: 990_000_000 })).toBe(19_990);
		expect(moneyToMillis({ currencyCode: "JPY", units: 1200, nanos: 0 })).toBe(1_200_000);
		expect(moneyToMillis(null)).toBeNull();
	});
});

describe("mapSubscriptionState", () => {
	it("CANCELED 在有效期内仍算 active，过期后才 expired", () => {
		const future = Date.now() + 86_400_000;
		const past = Date.now() - 86_400_000;
		expect(mapSubscriptionState("SUBSCRIPTION_STATE_CANCELED", future)).toBe("active");
		expect(mapSubscriptionState("SUBSCRIPTION_STATE_CANCELED", past)).toBe("expired");
	});

	it("其余状态一一映射", () => {
		expect(mapSubscriptionState("SUBSCRIPTION_STATE_ACTIVE")).toBe("active");
		expect(mapSubscriptionState("SUBSCRIPTION_STATE_IN_GRACE_PERIOD")).toBe("grace");
		expect(mapSubscriptionState("SUBSCRIPTION_STATE_ON_HOLD")).toBe("billing_retry");
		expect(mapSubscriptionState("SUBSCRIPTION_STATE_PAUSED")).toBe("paused");
		expect(mapSubscriptionState("SUBSCRIPTION_STATE_EXPIRED")).toBe("expired");
		expect(mapSubscriptionState("WHAT_IS_THIS")).toBeNull();
	});
});

describe("notificationTypeName", () => {
	it("数字枚举翻成可读串", () => {
		expect(notificationTypeName(decodeEnvelopeOf(SUB_PURCHASE))).toBe("SUBSCRIPTION_PURCHASED");
		expect(
			notificationTypeName(
				decodeEnvelopeOf({
					packageName: PACKAGE,
					oneTimeProductNotification: { notificationType: 1, purchaseToken: "t", sku: "s" },
				}),
			),
		).toBe("ONE_TIME_PRODUCT_PURCHASED");
		expect(
			notificationTypeName(decodeEnvelopeOf({ packageName: PACKAGE, testNotification: {} })),
		).toBe("PLAY_TEST");
	});
});

function decodeEnvelopeOf(n: DeveloperNotification) {
	return decodeEnvelope(envelope(n));
}

// ---------------------------------------------------------------------------
// 账本映射
// ---------------------------------------------------------------------------

const SUB_ENRICHMENT: PlayEnrichment = {
	subscription: {
		subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
		latestOrderId: "GPA.1111-2222-3333-44444",
		regionCode: "US",
		startTime: "2026-08-01T00:00:00Z",
		lineItems: [
			{
				productId: "jiamin.chen.orange_cloud.pro.yearly",
				expiryTime: "2027-08-01T00:00:00Z",
				autoRenewingPlan: {
					autoRenewEnabled: true,
					recurringPrice: { currencyCode: "USD", units: "19", nanos: 990_000_000 },
				},
				offerDetails: { basePlanId: "yearly" },
			},
		],
	},
	order: {
		orderId: "GPA.1111-2222-3333-44444",
		state: "PROCESSED",
		createTime: "2026-08-01T00:00:00Z",
		total: { currencyCode: "USD", units: "19", nanos: 990_000_000 },
		developerRevenueInBuyerCurrency: { currencyCode: "USD", units: "16", nanos: 990_000_000 },
		buyerAddress: { buyerCountry: "US" },
	},
};

describe("buildLedgerRows", () => {
	it("订阅首购：交易 + 状态都齐，金额取订单实付", () => {
		const rows = buildLedgerRows(decodeEnvelopeOf(SUB_PURCHASE), SUB_ENRICHMENT, 1_754_006_500_000);
		expect(rows.notification.notificationType).toBe("SUBSCRIPTION_PURCHASED");
		expect(rows.notification.environment).toBe("Production");
		expect(rows.transaction).toMatchObject({
			orderId: "GPA.1111-2222-3333-44444",
			purchaseToken: "tok-abc",
			productId: "jiamin.chen.orange_cloud.pro.yearly",
			priceMillis: 19_990,
			devRevenueMillis: 16_990,
			currency: "USD",
			storefront: "US",
			type: "Auto-Renewable Subscription",
			revocationDate: null,
		});
		expect(rows.subscription).toMatchObject({
			purchaseToken: "tok-abc",
			status: "active",
			autoRenewStatus: 1,
			isLifetime: false,
		});
		expect(rows.subscription?.expiresDate).toBe(Date.parse("2027-08-01T00:00:00Z"));
	});

	it("测试购买标成 Sandbox（看板会排除）", () => {
		const rows = buildLedgerRows(decodeEnvelopeOf(SUB_PURCHASE), {
			...SUB_ENRICHMENT,
			subscription: { ...SUB_ENRICHMENT.subscription, testPurchase: {} },
		});
		expect(rows.notification.environment).toBe("Sandbox");
		expect(rows.transaction?.environment).toBe("Sandbox");
	});

	it("没配服务账号（无富化）：只记事件与状态，不造假流水", () => {
		const rows = buildLedgerRows(decodeEnvelopeOf(SUB_PURCHASE), {});
		expect(rows.transaction).toBeNull();
		expect(rows.subscription).toMatchObject({ status: "active", priceMillis: null });
	});

	it("买断：is_lifetime、无到期时间", () => {
		const n: DeveloperNotification = {
			packageName: PACKAGE,
			eventTimeMillis: "1754006400000",
			oneTimeProductNotification: {
				notificationType: 1,
				purchaseToken: "tok-life",
				sku: "jiamin.chen.orange_cloud.pro.lifetime",
			},
		};
		const rows = buildLedgerRows(decodeEnvelopeOf(n), {
			product: { orderId: "GPA.9", purchaseState: 0, purchaseTimeMillis: "1754006400000" },
			order: {
				orderId: "GPA.9",
				total: { currencyCode: "CNY", units: "198", nanos: 0 },
				buyerAddress: { buyerCountry: "CN" },
			},
		});
		expect(rows.transaction).toMatchObject({
			orderId: "GPA.9",
			type: "Non-Consumable",
			priceMillis: 198_000,
			currency: "CNY",
		});
		expect(rows.subscription).toMatchObject({ isLifetime: true, status: "active", expiresDate: null });
	});

	it("退款：状态 refunded、流水写撤销时间", () => {
		const n: DeveloperNotification = {
			packageName: PACKAGE,
			eventTimeMillis: "1754092800000",
			voidedPurchaseNotification: {
				purchaseToken: "tok-abc",
				orderId: "GPA.1111-2222-3333-44444",
				productType: 1,
				refundType: 1,
			},
		};
		const rows = buildLedgerRows(decodeEnvelopeOf(n), SUB_ENRICHMENT);
		expect(rows.notification.subtype).toBe("SUBSCRIPTION_FULL_REFUND");
		expect(rows.transaction?.revocationDate).toBe(1_754_092_800_000);
		expect(rows.subscription?.status).toBe("refunded");
	});

	it("测试通知不产生业务行", () => {
		const rows = buildLedgerRows(decodeEnvelopeOf({ packageName: PACKAGE, testNotification: {} }), {});
		expect(rows.transaction).toBeNull();
		expect(rows.subscription).toBeNull();
	});
});

// ---------------------------------------------------------------------------
// Bark 文案 / 服务账号解析
// ---------------------------------------------------------------------------

describe("buildPlayBarkMessage", () => {
	it("入账事件穿透专注模式，正文带商品与金额", () => {
		const rows = buildLedgerRows(decodeEnvelopeOf(SUB_PURCHASE), SUB_ENRICHMENT);
		const msg = buildPlayBarkMessage(rows);
		expect(msg.level).toBe("timeSensitive");
		expect(msg.title).toContain("新订阅");
		expect(msg.body).toContain("Pro 年度");
		expect(msg.body).toContain("19.99 USD");
		expect(msg.body).toContain("Google Play");
	});

	it("沙盒事件静默入列", () => {
		const rows = buildLedgerRows(decodeEnvelopeOf(SUB_PURCHASE), {
			...SUB_ENRICHMENT,
			subscription: { ...SUB_ENRICHMENT.subscription, testPurchase: {} },
		});
		expect(buildPlayBarkMessage(rows).level).toBe("passive");
	});
});

describe("isRetryableStatus", () => {
	it("401 / 403 算可重试：Play Console 权限最长 24h 才生效，别把真实购买永久记成无金额", () => {
		expect(isRetryableStatus(401)).toBe(true);
		expect(isRetryableStatus(403)).toBe(true);
	});

	it("限流与服务端故障可重试", () => {
		expect(isRetryableStatus(429)).toBe(true);
		expect(isRetryableStatus(500)).toBe(true);
		expect(isRetryableStatus(503)).toBe(true);
	});

	it("参数错这类不可重试（重投递也修不好）", () => {
		expect(isRetryableStatus(400)).toBe(false);
		expect(isRetryableStatus(404)).toBe(false);
	});
});

describe("parseServiceAccount", () => {
	it("未配置 / 残缺 JSON 一律返回 null（调用方降级）", () => {
		expect(parseServiceAccount(undefined)).toBeNull();
		expect(parseServiceAccount("")).toBeNull();
		expect(parseServiceAccount("{}")).toBeNull();
		expect(parseServiceAccount("not json")).toBeNull();
	});

	it("还原被转义的换行", () => {
		const sa = parseServiceAccount(
			JSON.stringify({ client_email: "a@b.com", private_key: "-----BEGIN-----\\nX\\n-----END-----" }),
		);
		expect(sa?.private_key).toBe("-----BEGIN-----\nX\n-----END-----");
	});
});
