// Google Play Real-time Developer Notifications（RTDN）与 Play Developer API 的类型。
//
// RTDN 走 Cloud Pub/Sub：Play 把 DeveloperNotification 发到我们的 topic，
// topic 配一个 push 订阅 POST 到 /api/play/notifications。请求体是 Pub/Sub 信封，
// message.data 是 base64 的 DeveloperNotification JSON。
//
// 关键差异（与 Apple 对照）：RTDN **只给 purchaseToken + 类型**，没有金额、
// 没有到期时间、没有地区。要拿这些必须回调 Play Developer API（见 api.ts）。

/** Pub/Sub push 信封。 */
export interface PubSubEnvelope {
	message?: {
		data?: string;
		messageId?: string;
		message_id?: string;
		publishTime?: string;
		publish_time?: string;
		attributes?: Record<string, string>;
	};
	subscription?: string;
}

export interface SubscriptionNotification {
	version?: string;
	notificationType?: number;
	purchaseToken?: string;
	subscriptionId?: string;
}

export interface OneTimeProductNotification {
	version?: string;
	notificationType?: number;
	purchaseToken?: string;
	sku?: string;
}

export interface VoidedPurchaseNotification {
	purchaseToken?: string;
	orderId?: string;
	/** 1 = 订阅，2 = 一次性购买 */
	productType?: number;
	/** 1 = 全额退款，2 = 按数量部分退款 */
	refundType?: number;
}

export interface DeveloperNotification {
	version?: string;
	packageName?: string;
	eventTimeMillis?: string | number;
	subscriptionNotification?: SubscriptionNotification;
	oneTimeProductNotification?: OneTimeProductNotification;
	voidedPurchaseNotification?: VoidedPurchaseNotification;
	testNotification?: { version?: string };
}

/** 验签 + 解码后交给业务层的一条通知。 */
export interface DecodedPlayNotification {
	/** Pub/Sub messageId —— 幂等键（重投递复用同一个 id）。 */
	messageId: string;
	/** 事件发生时刻（ms epoch），用作乱序保护的水位。 */
	eventTimeMillis: number;
	notification: DeveloperNotification;
}

// ---------------------------------------------------------------------------
// 通知类型枚举 —— RTDN 传数字，账本里存可读串（与 Apple 侧的字符串类型同列）
// ---------------------------------------------------------------------------

export const SUBSCRIPTION_NOTIFICATION_TYPE: Record<number, string> = {
	1: "SUBSCRIPTION_RECOVERED",
	2: "SUBSCRIPTION_RENEWED",
	3: "SUBSCRIPTION_CANCELED",
	4: "SUBSCRIPTION_PURCHASED",
	5: "SUBSCRIPTION_ON_HOLD",
	6: "SUBSCRIPTION_IN_GRACE_PERIOD",
	7: "SUBSCRIPTION_RESTARTED",
	8: "SUBSCRIPTION_PRICE_CHANGE_CONFIRMED",
	9: "SUBSCRIPTION_DEFERRED",
	10: "SUBSCRIPTION_PAUSED",
	11: "SUBSCRIPTION_PAUSE_SCHEDULE_CHANGED",
	12: "SUBSCRIPTION_REVOKED",
	13: "SUBSCRIPTION_EXPIRED",
	17: "SUBSCRIPTION_ITEMS_CHANGED",
	18: "SUBSCRIPTION_CANCELLATION_SCHEDULED",
	19: "SUBSCRIPTION_PRICE_CHANGE_UPDATED",
	20: "SUBSCRIPTION_PENDING_PURCHASE_CANCELED",
	22: "SUBSCRIPTION_PRICE_STEP_UP_CONSENT_UPDATED",
};

export const ONE_TIME_NOTIFICATION_TYPE: Record<number, string> = {
	1: "ONE_TIME_PRODUCT_PURCHASED",
	2: "ONE_TIME_PRODUCT_CANCELED",
};

export const VOIDED_PURCHASE_TYPE = "VOIDED_PURCHASE";
export const PLAY_TEST_NOTIFICATION_TYPE = "PLAY_TEST";

// ---------------------------------------------------------------------------
// Play Developer API 响应（只声明我们用到的字段）
// ---------------------------------------------------------------------------

/** google.type.Money —— units 是「整数部分」字符串，nanos 是十亿分之一。 */
export interface Money {
	currencyCode?: string;
	units?: string | number;
	nanos?: number;
}

export interface SubscriptionPurchaseV2 {
	subscriptionState?: string;
	latestOrderId?: string;
	regionCode?: string;
	startTime?: string;
	linkedPurchaseToken?: string;
	testPurchase?: Record<string, unknown>;
	canceledStateContext?: Record<string, unknown>;
	lineItems?: {
		productId?: string;
		expiryTime?: string;
		autoRenewingPlan?: {
			autoRenewEnabled?: boolean;
			recurringPrice?: Money;
		};
		prepaidPlan?: Record<string, unknown>;
		offerDetails?: { basePlanId?: string; offerId?: string };
	}[];
}

export interface ProductPurchaseV1 {
	orderId?: string;
	purchaseTimeMillis?: string | number;
	/** 0 已购买 / 1 已取消 / 2 待处理 */
	purchaseState?: number;
	/** 0 未确认 / 1 已确认 */
	acknowledgementState?: number;
	/** 0 正式 / 1 测试（license tester、内部测试） */
	purchaseType?: number;
	productId?: string;
	regionCode?: string;
	quantity?: number;
}

export interface PlayOrder {
	orderId?: string;
	purchaseToken?: string;
	state?: string;
	createTime?: string;
	lastEventTime?: string;
	total?: Money;
	tax?: Money;
	developerRevenueInBuyerCurrency?: Money;
	buyerAddress?: { buyerCountry?: string; buyerState?: string; buyerPostcode?: string };
	lineItems?: {
		productId?: string;
		productTitle?: string;
		listingPrice?: Money;
		total?: Money;
		tax?: Money;
	}[];
}

/** 富化结果：能拿到多少算多少，缺 Service Account 时全部为空。 */
export interface PlayEnrichment {
	subscription?: SubscriptionPurchaseV2;
	product?: ProductPurchaseV1;
	order?: PlayOrder;
}

// ---------------------------------------------------------------------------
// 小工具
// ---------------------------------------------------------------------------

/** google.type.Money -> milliunits（与 Apple 的 price 口径一致：$19.99 -> 19990）。 */
export function moneyToMillis(m?: Money | null): number | null {
	if (!m) return null;
	const units = typeof m.units === "string" ? Number(m.units) : (m.units ?? 0);
	const nanos = m.nanos ?? 0;
	if (!Number.isFinite(units)) return null;
	return Math.round(units * 1000 + nanos / 1_000_000);
}

/** RFC3339 时间串 -> ms epoch（无效返回 null）。 */
export function rfc3339ToMillis(s?: string | null): number | null {
	if (!s) return null;
	const t = Date.parse(s);
	return Number.isFinite(t) ? t : null;
}

/** 字符串 / 数字型 epoch 毫秒 -> number（无效返回 null）。 */
export function toMillis(v?: string | number | null): number | null {
	if (v == null) return null;
	const n = typeof v === "string" ? Number(v) : v;
	return Number.isFinite(n) ? n : null;
}
