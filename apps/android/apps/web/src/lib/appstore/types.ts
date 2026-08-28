// App Store Server Notifications V2 解码后的数据结构。
// 只声明本服务用到的字段；其余字段由 [key: string]: unknown 容纳，向前兼容
// （Apple 会持续往 payload 里加字段）。字段口径见 Apple 文档：
// App Store Server Notifications V2 / In-App Purchase（JWSTransaction / JWSRenewalInfo）。

export type Environment = "Sandbox" | "Production";

/** responseBodyV2DecodedPayload —— 外层通知体（已验签解码） */
export interface DecodedNotificationPayload {
	notificationType: string;
	subtype?: string;
	notificationUUID: string;
	version?: string;
	signedDate?: number; // ms epoch
	data?: NotificationData;
	summary?: NotificationSummary;
	externalPurchaseToken?: Record<string, unknown>;
	[key: string]: unknown;
}

export interface NotificationData {
	appAppleId?: number;
	bundleId?: string;
	bundleVersion?: string;
	environment?: Environment;
	/** 订阅状态码：1 active / 2 expired / 3 billing retry / 4 grace / 5 revoked */
	status?: number;
	signedTransactionInfo?: string; // 内层 JWS
	signedRenewalInfo?: string; // 内层 JWS
	[key: string]: unknown;
}

/** RENEWAL_EXTENSION / 部分类型用 summary 而非 data */
export interface NotificationSummary {
	requestIdentifier?: string;
	environment?: Environment;
	appAppleId?: number;
	bundleId?: string;
	productId?: string;
	[key: string]: unknown;
}

/** JWSTransactionDecodedPayload —— 单笔交易 */
export interface TransactionInfo {
	transactionId: string;
	originalTransactionId: string;
	bundleId?: string;
	productId?: string;
	subscriptionGroupIdentifier?: string;
	purchaseDate?: number;
	originalPurchaseDate?: number;
	expiresDate?: number;
	quantity?: number;
	type?: string; // "Auto-Renewable Subscription" | "Non-Consumable" | ...
	inAppOwnershipType?: string;
	signedDate?: number;
	environment?: Environment;
	offerType?: number;
	offerIdentifier?: string;
	revocationDate?: number;
	revocationReason?: number;
	isUpgraded?: boolean;
	price?: number; // 货币 milliunits（$19.99 -> 19990）
	currency?: string; // ISO 4217
	storefront?: string; // App Store 商店地区，ISO 3166-1 alpha-3（如 USA / CHN）
	storefrontId?: string; // Apple 数字 storefront id
	appAccountToken?: string;
	[key: string]: unknown;
}

/** JWSRenewalInfoDecodedPayload —— 自动续订状态 */
export interface RenewalInfo {
	originalTransactionId?: string;
	autoRenewProductId?: string;
	productId?: string;
	autoRenewStatus?: number; // 0 关 / 1 开
	expirationIntent?: number;
	gracePeriodExpiresDate?: number;
	isInBillingRetryPeriod?: boolean;
	offerType?: number;
	offerIdentifier?: string;
	signedDate?: number;
	environment?: Environment;
	recentSubscriptionStartDate?: number;
	renewalDate?: number;
	renewalPrice?: number; // milliunits
	currency?: string;
	[key: string]: unknown;
}

/** 一条通知完整解码结果（外层 + 内层都已验签解出） */
export interface DecodedNotification {
	payload: DecodedNotificationPayload;
	transaction?: TransactionInfo;
	renewal?: RenewalInfo;
}

// ---- 枚举（仅列本服务关心的取值）----

export const NotificationType = {
	subscribed: "SUBSCRIBED",
	didRenew: "DID_RENEW",
	didChangeRenewalStatus: "DID_CHANGE_RENEWAL_STATUS",
	didChangeRenewalPref: "DID_CHANGE_RENEWAL_PREF",
	didFailToRenew: "DID_FAIL_TO_RENEW",
	expired: "EXPIRED",
	gracePeriodExpired: "GRACE_PERIOD_EXPIRED",
	offerRedeemed: "OFFER_REDEEMED",
	priceIncrease: "PRICE_INCREASE",
	refund: "REFUND",
	refundDeclined: "REFUND_DECLINED",
	refundReversed: "REFUND_REVERSED",
	revoke: "REVOKE",
	consumptionRequest: "CONSUMPTION_REQUEST",
	renewalExtended: "RENEWAL_EXTENDED",
	renewalExtension: "RENEWAL_EXTENSION",
	oneTimeCharge: "ONE_TIME_CHARGE",
	test: "TEST",
} as const;

export const Subtype = {
	initialBuy: "INITIAL_BUY",
	resubscribe: "RESUBSCRIBE",
	autoRenewEnabled: "AUTO_RENEW_ENABLED",
	autoRenewDisabled: "AUTO_RENEW_DISABLED",
	voluntary: "VOLUNTARY",
	billingRetry: "BILLING_RETRY",
	gracePeriod: "GRACE_PERIOD",
	billingRecovery: "BILLING_RECOVERY",
	upgrade: "UPGRADE",
	downgrade: "DOWNGRADE",
	accepted: "ACCEPTED",
	pending: "PENDING",
	failure: "FAILURE",
	productNotForSale: "PRODUCT_NOT_FOR_SALE",
} as const;

/** subscriptions.status 取值 */
export type SubscriptionStatus =
	| "active"
	| "expired"
	| "grace"
	| "billing_retry"
	| "refunded"
	| "revoked";
