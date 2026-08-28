// RTDN（+ Developer API 富化结果）-> 统一账本三表的行。纯函数，可单测。
//
// 字段对齐（左 Apple / 右 Play）：
//   notification_uuid       = Pub/Sub messageId
//   original_transaction_id = purchaseToken（订阅续期内不变，升降级会换发）
//   transaction_id          = orderId（每期续订一个新 orderId）
//   signed_date             = eventTimeMillis（同时是乱序保护水位）
//   price_millis            = 订单实付总额（含税），与 Apple 的 transaction.price 同口径

import {
	moneyToMillis,
	ONE_TIME_NOTIFICATION_TYPE,
	PLAY_TEST_NOTIFICATION_TYPE,
	rfc3339ToMillis,
	SUBSCRIPTION_NOTIFICATION_TYPE,
	toMillis,
	VOIDED_PURCHASE_TYPE,
	type DecodedPlayNotification,
	type PlayEnrichment,
} from "./types";

export interface PlayNotificationRow {
	notificationUuid: string;
	notificationType: string;
	subtype: string | null;
	purchaseToken: string | null;
	orderId: string | null;
	packageName: string | null;
	environment: string;
	eventTime: number;
	receivedAt: number;
	raw: string;
}

export interface PlayTransactionRow {
	orderId: string;
	purchaseToken: string;
	productId: string | null;
	/** 与 Apple 的 transactions.type 同列：自动续订 / 买断 */
	type: string | null;
	purchaseDate: number | null;
	expiresDate: number | null;
	priceMillis: number | null;
	devRevenueMillis: number | null;
	currency: string | null;
	storefront: string | null;
	offerIdentifier: string | null;
	revocationDate: number | null;
	environment: string;
}

export interface PlaySubscriptionRow {
	purchaseToken: string;
	productId: string | null;
	status: string;
	autoRenewStatus: number | null;
	linkedToken: string | null;
	environment: string;
	purchaseDate: number | null;
	expiresDate: number | null;
	isLifetime: boolean;
	priceMillis: number | null;
	currency: string | null;
}

export interface PlayLedgerRows {
	notification: PlayNotificationRow;
	transaction: PlayTransactionRow | null;
	subscription: PlaySubscriptionRow | null;
}

const TX_TYPE_SUBSCRIPTION = "Auto-Renewable Subscription";
const TX_TYPE_ONE_TIME = "Non-Consumable";

/** Play 订阅状态 -> 账本 status（与 Apple 侧同一套取值）。 */
export function mapSubscriptionState(state?: string, expiresDate?: number | null): string | null {
	switch (state) {
		case "SUBSCRIPTION_STATE_ACTIVE":
			return "active";
		case "SUBSCRIPTION_STATE_CANCELED":
			// Play 的 canceled = 已关自动续订但仍在有效期内，到期后才失去权益。
			return expiresDate != null && expiresDate <= Date.now() ? "expired" : "active";
		case "SUBSCRIPTION_STATE_IN_GRACE_PERIOD":
			return "grace";
		case "SUBSCRIPTION_STATE_ON_HOLD":
			return "billing_retry";
		case "SUBSCRIPTION_STATE_PAUSED":
			return "paused";
		case "SUBSCRIPTION_STATE_EXPIRED":
		case "SUBSCRIPTION_STATE_PENDING_PURCHASE_CANCELED":
			return "expired";
		case "SUBSCRIPTION_STATE_PENDING":
			return "pending";
		default:
			return null;
	}
}

/** 拿不到订阅详情时的兜底：只按通知类型推状态。 */
export function statusFromNotificationType(type: string): string {
	switch (type) {
		case "SUBSCRIPTION_PURCHASED":
		case "SUBSCRIPTION_RENEWED":
		case "SUBSCRIPTION_RECOVERED":
		case "SUBSCRIPTION_RESTARTED":
		case "SUBSCRIPTION_DEFERRED":
		case "SUBSCRIPTION_CANCELED": // 仍在有效期内，只是关了续订
			return "active";
		case "SUBSCRIPTION_IN_GRACE_PERIOD":
			return "grace";
		case "SUBSCRIPTION_ON_HOLD":
			return "billing_retry";
		case "SUBSCRIPTION_PAUSED":
			return "paused";
		case "SUBSCRIPTION_EXPIRED":
		case "SUBSCRIPTION_PENDING_PURCHASE_CANCELED":
			return "expired";
		case "SUBSCRIPTION_REVOKED":
			return "revoked";
		default:
			return "active";
	}
}

/** 一条通知的名字（写进 notifications.notification_type，与 Apple 类型同列不冲突）。 */
export function notificationTypeName(decoded: DecodedPlayNotification): string {
	const n = decoded.notification;
	if (n.subscriptionNotification) {
		const t = n.subscriptionNotification.notificationType ?? 0;
		return SUBSCRIPTION_NOTIFICATION_TYPE[t] ?? `SUBSCRIPTION_${t}`;
	}
	if (n.oneTimeProductNotification) {
		const t = n.oneTimeProductNotification.notificationType ?? 0;
		return ONE_TIME_NOTIFICATION_TYPE[t] ?? `ONE_TIME_PRODUCT_${t}`;
	}
	if (n.voidedPurchaseNotification) return VOIDED_PURCHASE_TYPE;
	if (n.testNotification) return PLAY_TEST_NOTIFICATION_TYPE;
	return "PLAY_UNKNOWN";
}

/** 通知涉及的 purchaseToken（幂等 / 关联用）。 */
export function purchaseTokenOf(decoded: DecodedPlayNotification): string | null {
	const n = decoded.notification;
	return (
		n.subscriptionNotification?.purchaseToken ??
		n.oneTimeProductNotification?.purchaseToken ??
		n.voidedPurchaseNotification?.purchaseToken ??
		null
	);
}

const VOID_PRODUCT_TYPE: Record<number, string> = { 1: "SUBSCRIPTION", 2: "ONE_TIME" };
const VOID_REFUND_TYPE: Record<number, string> = { 1: "FULL_REFUND", 2: "PARTIAL_REFUND" };

function subtypeOf(decoded: DecodedPlayNotification): string | null {
	const v = decoded.notification.voidedPurchaseNotification;
	if (!v) return null;
	const parts = [
		v.productType != null ? VOID_PRODUCT_TYPE[v.productType] : null,
		v.refundType != null ? VOID_REFUND_TYPE[v.refundType] : null,
	].filter(Boolean);
	return parts.length ? parts.join("_") : null;
}

/**
 * 是否测试购买（license tester）—— 与 Apple 的 Sandbox 同义，看板默认排除。
 * products.get 的 purchaseType：0=Test、1=Promo（真实但免费）、2=Rewarded，
 * 只有 0 算测试。未富化时无从判断，一律按 Production 计。
 */
function environmentOf(e: PlayEnrichment): string {
	if (e.subscription?.testPurchase) return "Sandbox";
	if (e.product?.purchaseType === 0) return "Sandbox";
	return "Production";
}

/** 富化后的完整映射。enrichment 为空时只产出通知行 + 尽可能的状态行。 */
export function buildLedgerRows(
	decoded: DecodedPlayNotification,
	enrichment: PlayEnrichment,
	receivedAt: number = Date.now(),
): PlayLedgerRows {
	const n = decoded.notification;
	const type = notificationTypeName(decoded);
	const token = purchaseTokenOf(decoded);
	const environment = environmentOf(enrichment);
	const eventTime = decoded.eventTimeMillis;

	const order = enrichment.order;
	const orderId =
		n.voidedPurchaseNotification?.orderId ??
		order?.orderId ??
		enrichment.subscription?.latestOrderId ??
		enrichment.product?.orderId ??
		null;

	const notification: PlayNotificationRow = {
		notificationUuid: decoded.messageId,
		notificationType: type,
		subtype: subtypeOf(decoded),
		purchaseToken: token,
		orderId,
		packageName: n.packageName ?? null,
		environment,
		eventTime,
		receivedAt,
		// 审计存档：原始通知 + 富化结果，便于日后回填（与 Apple 侧 raw_payload 同用途）。
		raw: JSON.stringify({ notification: n, enrichment }),
	};

	const money = order?.total ?? enrichment.subscription?.lineItems?.[0]?.autoRenewingPlan?.recurringPrice;
	const currency = money?.currencyCode ?? null;
	const priceMillis = moneyToMillis(money);
	const devRevenueMillis = moneyToMillis(order?.developerRevenueInBuyerCurrency);
	const storefront =
		order?.buyerAddress?.buyerCountry ??
		enrichment.subscription?.regionCode ??
		enrichment.product?.regionCode ??
		null;

	const isVoided = Boolean(n.voidedPurchaseNotification);
	const isOneTime =
		Boolean(n.oneTimeProductNotification) ||
		n.voidedPurchaseNotification?.productType === 2 ||
		(!n.subscriptionNotification && Boolean(enrichment.product));

	const line = enrichment.subscription?.lineItems?.[0];
	const productId =
		line?.productId ??
		n.oneTimeProductNotification?.sku ??
		enrichment.product?.productId ??
		n.subscriptionNotification?.subscriptionId ??
		order?.lineItems?.[0]?.productId ??
		null;

	const expiresDate = rfc3339ToMillis(line?.expiryTime);
	const purchaseDate =
		rfc3339ToMillis(order?.createTime) ??
		toMillis(enrichment.product?.purchaseTimeMillis) ??
		rfc3339ToMillis(enrichment.subscription?.startTime) ??
		(isVoided ? null : eventTime);

	// 财务流水行：必须有 orderId 才能落库（它是主键）。拿不到（没配服务账号 /
	// 订阅通知未富化）就只更新状态，不造假流水。
	const transaction: PlayTransactionRow | null =
		orderId && token
			? {
					orderId,
					purchaseToken: token,
					productId,
					type: isOneTime ? TX_TYPE_ONE_TIME : TX_TYPE_SUBSCRIPTION,
					purchaseDate,
					expiresDate,
					priceMillis,
					devRevenueMillis,
					currency,
					storefront,
					offerIdentifier: line?.offerDetails?.offerId ?? line?.offerDetails?.basePlanId ?? null,
					revocationDate: isVoided ? eventTime : null,
					environment,
				}
			: null;

	// 权益状态行
	let subscription: PlaySubscriptionRow | null = null;
	if (token && !n.testNotification) {
		let status: string;
		if (isVoided) {
			status = "refunded";
		} else if (isOneTime) {
			const state = enrichment.product?.purchaseState;
			status =
				type === "ONE_TIME_PRODUCT_CANCELED" || state === 1
					? "expired"
					: state === 2
						? "pending"
						: "active";
		} else {
			status =
				mapSubscriptionState(enrichment.subscription?.subscriptionState, expiresDate) ??
				statusFromNotificationType(type);
		}

		const autoRenew = line?.autoRenewingPlan
			? line.autoRenewingPlan.autoRenewEnabled === true
				? 1
				: 0
			: type === "SUBSCRIPTION_CANCELED"
				? 0
				: null;

		subscription = {
			purchaseToken: token,
			productId,
			status,
			autoRenewStatus: isOneTime ? null : autoRenew,
			linkedToken: enrichment.subscription?.linkedPurchaseToken ?? null,
			environment,
			purchaseDate,
			expiresDate: isOneTime ? null : expiresDate,
			isLifetime: isOneTime,
			priceMillis,
			currency,
		};
	}

	return { notification, transaction, subscription };
}
