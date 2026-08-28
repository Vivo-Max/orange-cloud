// 验签 + 解码 + 状态映射的单测（不联网、不依赖 Apple 真实证书）。
// 思路：现场用 @peculiar/x509 造一条「测试根 -> 叶子」证书链，用叶子私钥按
// App Store Server Notifications V2 的格式签出 signedPayload（含内层交易 / 续订 JWS），
// 再注入测试根校验解码。最后验证：篡改签名会拒、pin 到真 Apple 根会拒非 Apple 链。
//
// 存储层（幂等 / 乱序保护）走 wrangler d1 集成冒烟，不在此单测覆盖。

import { beforeAll, describe, expect, it } from "vitest";
import { CompactSign } from "jose";
import { cryptoProvider, X509Certificate, X509CertificateGenerator } from "@peculiar/x509";
import { deriveSubscription } from "./notification-logic";
import { NotificationVerifyError, verifyNotification } from "./verify";

const subtle = globalThis.crypto.subtle;
const NOW = Date.now();

let leafPrivateKey: CryptoKey;
let x5c: string[];
let testRoot: X509Certificate;

function bytesToBase64(buf: ArrayBuffer): string {
	const bytes = new Uint8Array(buf);
	let bin = "";
	for (let i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i]);
	return btoa(bin);
}

async function signJws(obj: unknown): Promise<string> {
	return new CompactSign(new TextEncoder().encode(JSON.stringify(obj)))
		.setProtectedHeader({ alg: "ES256", x5c })
		.sign(leafPrivateKey);
}

interface NotifInput {
	type: string;
	subtype?: string;
	bundleId?: string;
	environment?: string;
	transaction?: Record<string, unknown>;
	renewal?: Record<string, unknown>;
}

async function makeNotification(input: NotifInput): Promise<string> {
	const data: Record<string, unknown> = {
		bundleId: input.bundleId ?? "jiamin.chen.orange-cloud",
		environment: input.environment ?? "Sandbox",
	};
	if (input.transaction) data.signedTransactionInfo = await signJws(input.transaction);
	if (input.renewal) data.signedRenewalInfo = await signJws(input.renewal);
	return signJws({
		notificationType: input.type,
		subtype: input.subtype,
		notificationUUID: crypto.randomUUID(),
		version: "2.0",
		signedDate: NOW,
		data,
	});
}

beforeAll(async () => {
	cryptoProvider.set(globalThis.crypto as Crypto);
	const alg = { name: "ECDSA", namedCurve: "P-256" } as const;
	const sign = { name: "ECDSA", hash: "SHA-256" } as const;
	const notBefore = new Date(NOW - 86_400_000);
	const notAfter = new Date(NOW + 86_400_000);

	const rootKeys = await subtle.generateKey(alg, true, ["sign", "verify"]);
	testRoot = await X509CertificateGenerator.createSelfSigned({
		name: "CN=Orange Cloud Test Root",
		keys: rootKeys,
		notBefore,
		notAfter,
		signingAlgorithm: sign,
		serialNumber: "01",
	});

	const leafKeys = await subtle.generateKey(alg, true, ["sign", "verify"]);
	leafPrivateKey = leafKeys.privateKey;
	const leaf = await X509CertificateGenerator.create({
		subject: "CN=Orange Cloud Test Leaf",
		issuer: testRoot.subject,
		publicKey: leafKeys.publicKey,
		signingKey: rootKeys.privateKey,
		notBefore,
		notAfter,
		signingAlgorithm: sign,
		serialNumber: "02",
	});

	x5c = [bytesToBase64(leaf.rawData), bytesToBase64(testRoot.rawData)];
});

const baseTx = {
	transactionId: "2000000000000001",
	originalTransactionId: "2000000000000001",
	bundleId: "jiamin.chen.orange-cloud",
	productId: "jiamin.chen.orange_cloud.pro.yearly",
	type: "Auto-Renewable Subscription",
	inAppOwnershipType: "PURCHASED",
	purchaseDate: NOW,
	originalPurchaseDate: NOW,
	expiresDate: NOW + 365 * 86_400_000,
	price: 19_990,
	currency: "USD",
	signedDate: NOW,
	environment: "Sandbox",
};

const baseRenewal = {
	originalTransactionId: "2000000000000001",
	autoRenewProductId: "jiamin.chen.orange_cloud.pro.yearly",
	productId: "jiamin.chen.orange_cloud.pro.yearly",
	autoRenewStatus: 1,
	signedDate: NOW,
	environment: "Sandbox",
};

describe("verifyNotification", () => {
	it("解码外层 + 内层交易 / 续订", async () => {
		const jws = await makeNotification({
			type: "SUBSCRIBED",
			subtype: "INITIAL_BUY",
			transaction: baseTx,
			renewal: baseRenewal,
		});
		const decoded = await verifyNotification(jws, { trustedRoot: testRoot });
		expect(decoded.payload.notificationType).toBe("SUBSCRIBED");
		expect(decoded.payload.data?.bundleId).toBe("jiamin.chen.orange-cloud");
		expect(decoded.transaction?.originalTransactionId).toBe("2000000000000001");
		expect(decoded.transaction?.price).toBe(19_990);
		expect(decoded.renewal?.autoRenewStatus).toBe(1);
	});

	it("篡改签名 -> 抛 NotificationVerifyError", async () => {
		const jws = await makeNotification({ type: "SUBSCRIBED", transaction: baseTx });
		const tampered = jws.slice(0, -3) + (jws.endsWith("AAA") ? "BBB" : "AAA");
		await expect(verifyNotification(tampered, { trustedRoot: testRoot })).rejects.toBeInstanceOf(
			NotificationVerifyError,
		);
	});

	it("pin 到真 Apple 根时拒绝非 Apple 证书链", async () => {
		const jws = await makeNotification({ type: "SUBSCRIBED", transaction: baseTx });
		// 不传 trustedRoot -> 默认 pin Apple Root CA G3，测试链锚不上
		await expect(verifyNotification(jws)).rejects.toBeInstanceOf(NotificationVerifyError);
	});
});

describe("deriveSubscription 状态映射", () => {
	async function derive(input: NotifInput) {
		const jws = await makeNotification(input);
		const decoded = await verifyNotification(jws, { trustedRoot: testRoot });
		return deriveSubscription(decoded);
	}

	it("SUBSCRIBED -> active, 非买断, 有到期", async () => {
		const d = await derive({ type: "SUBSCRIBED", transaction: baseTx, renewal: baseRenewal });
		expect(d?.status).toBe("active");
		expect(d?.isLifetime).toBe(false);
		expect(d?.expiresDate).toBe(baseTx.expiresDate);
		expect(d?.autoRenewStatus).toBe(1);
	});

	it("DID_FAIL_TO_RENEW + GRACE_PERIOD -> grace", async () => {
		const d = await derive({
			type: "DID_FAIL_TO_RENEW",
			subtype: "GRACE_PERIOD",
			transaction: baseTx,
			renewal: { ...baseRenewal, isInBillingRetryPeriod: true },
		});
		expect(d?.status).toBe("grace");
	});

	it("DID_FAIL_TO_RENEW (无 subtype) -> billing_retry", async () => {
		const d = await derive({ type: "DID_FAIL_TO_RENEW", transaction: baseTx, renewal: baseRenewal });
		expect(d?.status).toBe("billing_retry");
	});

	it("EXPIRED -> expired", async () => {
		const d = await derive({ type: "EXPIRED", subtype: "VOLUNTARY", transaction: baseTx, renewal: baseRenewal });
		expect(d?.status).toBe("expired");
	});

	it("REFUND -> refunded, 交易带撤销时间", async () => {
		const refundedTx = { ...baseTx, revocationDate: NOW, revocationReason: 1 };
		const jws = await makeNotification({ type: "REFUND", transaction: refundedTx });
		const decoded = await verifyNotification(jws, { trustedRoot: testRoot });
		const d = deriveSubscription(decoded);
		expect(d?.status).toBe("refunded");
		expect(decoded.transaction?.revocationDate).toBe(NOW);
	});

	it("ONE_TIME_CHARGE 非消耗 -> active, 买断, 无到期", async () => {
		const lifetimeTx = {
			...baseTx,
			productId: "jiamin.chen.orange_cloud.pro.lifetime",
			type: "Non-Consumable",
			expiresDate: undefined,
			price: 49_990,
		};
		const d = await derive({ type: "ONE_TIME_CHARGE", transaction: lifetimeTx });
		expect(d?.status).toBe("active");
		expect(d?.isLifetime).toBe(true);
		expect(d?.expiresDate).toBeUndefined();
		expect(d?.priceMillis).toBe(49_990);
	});

	it("TEST 通知无交易 -> 不入账（null）", async () => {
		const jws = await makeNotification({ type: "TEST" });
		const decoded = await verifyNotification(jws, { trustedRoot: testRoot });
		expect(deriveSubscription(decoded)).toBeNull();
	});
});
