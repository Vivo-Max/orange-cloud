// Shared types and constants for the Orange Cloud IAP dashboard.

export interface Filters {
	/** Single product scope, or null for all products. */
	productId: string | null;
	/** Trailing window in days, or null for all time. */
	days: number | null;
	/** Single store scope ("apple" / "play"), or null for both. */
	platform: string | null;
}

/** 账本里的平台维度（migrations/0007 起），按展示顺序。 */
export const PLATFORM_ORDER = ["apple", "play"] as const;

export const PLATFORM_LABEL: Record<string, string> = {
	apple: "App Store",
	play: "Google Play",
};

export function platformLabel(id?: string | null): string {
	if (!id) return "—";
	return PLATFORM_LABEL[id] ?? id;
}

/** The dashboard only ever shows Production data; Sandbox is filtered out. */
export const ENVIRONMENT = "Production";

/** Rows per page for the paginated notification and transaction lists. */
export const PAGE_SIZE = 20;

/** Product ids present in the orange-cloud-iap database, in display order. */
export const PRODUCT_ORDER = [
	"jiamin.chen.orange_cloud.pro.lifetime",
	"jiamin.chen.orange_cloud.pro.yearly",
	"jiamin.chen.orange_cloud.pro.monthly",
] as const;

/** Short, human-friendly product labels. */
export const PRODUCT_LABEL: Record<string, string> = {
	"jiamin.chen.orange_cloud.pro.lifetime": "终身买断",
	"jiamin.chen.orange_cloud.pro.yearly": "年订阅",
	"jiamin.chen.orange_cloud.pro.monthly": "月订阅",
};

export function productLabel(id?: string | null): string {
	if (!id) return "—";
	return PRODUCT_LABEL[id] ?? id;
}

/** Human labels for App Store Server Notification V2 types. */
export const NOTIFICATION_LABEL: Record<string, string> = {
	ONE_TIME_CHARGE: "一次性购买",
	DID_RENEW: "续订成功",
	CONSUMPTION_REQUEST: "消费请求",
	SUBSCRIBED: "订阅开始",
	DID_CHANGE_RENEWAL_STATUS: "续订状态变更",
	DID_CHANGE_RENEWAL_PREF: "续订方案变更",
	DID_FAIL_TO_RENEW: "续订失败",
	EXPIRED: "订阅过期",
	GRACE_PERIOD_EXPIRED: "宽限期结束",
	REFUND: "退款",
	REFUND_DECLINED: "退款被拒",
	REFUND_REVERSED: "退款撤销",
	REVOKE: "权益撤销",
	PRICE_INCREASE: "涨价",
	RENEWAL_EXTENDED: "续订延长",

	// Google Play RTDN（数字枚举已在入库时翻成串，见 lib/play/types.ts）
	SUBSCRIPTION_PURCHASED: "订阅开始",
	SUBSCRIPTION_RENEWED: "续订成功",
	SUBSCRIPTION_RECOVERED: "保留期恢复",
	SUBSCRIPTION_RESTARTED: "订阅重启",
	SUBSCRIPTION_CANCELED: "关闭自动续订",
	SUBSCRIPTION_ON_HOLD: "进入账号保留期",
	SUBSCRIPTION_IN_GRACE_PERIOD: "进入宽限期",
	SUBSCRIPTION_PAUSED: "订阅暂停",
	SUBSCRIPTION_PAUSE_SCHEDULE_CHANGED: "暂停计划变更",
	SUBSCRIPTION_DEFERRED: "续订延长",
	SUBSCRIPTION_REVOKED: "权益撤销",
	SUBSCRIPTION_EXPIRED: "订阅过期",
	SUBSCRIPTION_PRICE_CHANGE_CONFIRMED: "涨价已确认",
	SUBSCRIPTION_PRICE_CHANGE_UPDATED: "价格变更更新",
	SUBSCRIPTION_ITEMS_CHANGED: "套餐内容变更",
	SUBSCRIPTION_CANCELLATION_SCHEDULED: "已排期取消",
	SUBSCRIPTION_PENDING_PURCHASE_CANCELED: "待处理购买取消",
	SUBSCRIPTION_PRICE_STEP_UP_CONSENT_UPDATED: "涨价同意期变更",
	ONE_TIME_PRODUCT_PURCHASED: "一次性购买",
	ONE_TIME_PRODUCT_CANCELED: "一次性购买取消",
	VOIDED_PURCHASE: "退款",
	PLAY_TEST: "测试通知",
};

export function notificationLabel(type?: string | null): string {
	if (!type) return "—";
	return NOTIFICATION_LABEL[type] ?? type;
}

/** Human labels for notification subtypes (e.g. INITIAL_BUY, AUTO_RENEW_DISABLED). */
export const SUBTYPE_LABEL: Record<string, string> = {
	INITIAL_BUY: "首次购买",
	RESUBSCRIBE: "重新订阅",
	UPGRADE: "升级",
	DOWNGRADE: "降级",
	AUTO_RENEW_ENABLED: "开启自动续订",
	AUTO_RENEW_DISABLED: "关闭自动续订",
	VOLUNTARY: "自愿到期",
	BILLING_RETRY: "计费重试",
	PRICE_INCREASE: "涨价生效",
	GRACE_PERIOD: "宽限期",
	BILLING_RECOVERY: "计费恢复",
	PRODUCT_NOT_FOR_SALE: "商品已下架",
	SUMMARY: "汇总",
	FAILURE: "失败",
	ACCEPTED: "已接受",
	PENDING: "待处理",
	UNREPORTED: "未上报",

	// Google Play 退款通知的 subtype（productType_refundType 组合）
	SUBSCRIPTION_FULL_REFUND: "订阅全额退款",
	SUBSCRIPTION_PARTIAL_REFUND: "订阅部分退款",
	ONE_TIME_FULL_REFUND: "买断全额退款",
	ONE_TIME_PARTIAL_REFUND: "买断部分退款",
};

/** Returns a labeled subtype, or null when there is no subtype. */
export function subtypeLabel(subtype?: string | null): string | null {
	if (!subtype) return null;
	return SUBTYPE_LABEL[subtype] ?? subtype;
}

/** Human labels for transaction product types. */
export const TX_TYPE_LABEL: Record<string, string> = {
	"Non-Consumable": "买断",
	Consumable: "消耗型",
	"Auto-Renewable Subscription": "自动续订",
	"Non-Renewing Subscription": "非续订订阅",
};

export function txTypeLabel(type?: string | null): string {
	if (!type) return "—";
	return TX_TYPE_LABEL[type] ?? type;
}

/** Human labels for Apple offer types (introductory / promotional / code / win-back). */
export const OFFER_TYPE_LABEL: Record<number, string> = {
	1: "试用优惠",
	2: "促销优惠",
	3: "兑换码",
	4: "赢回优惠",
};

/** Returns a labeled offer type, or null when there is no offer. */
export function offerTypeLabel(offerType?: number | null): string | null {
	if (offerType == null) return null;
	return OFFER_TYPE_LABEL[offerType] ?? `优惠 ${offerType}`;
}

/** Apple refund reasons (App Store Server API `revocationReason`). */
export const REVOCATION_REASON_LABEL: Record<number, string> = {
	0: "其他原因（如误购）",
	1: "App 内问题",
};

export function revocationReasonLabel(reason?: number | null): string | null {
	if (reason == null) return null;
	return REVOCATION_REASON_LABEL[reason] ?? `原因 ${reason}`;
}

export type BadgeTone = "muted" | "accent" | "positive" | "negative" | "info";

const POSITIVE_TYPES = new Set([
	// Apple
	"SUBSCRIBED",
	"DID_RENEW",
	"ONE_TIME_CHARGE",
	// Play
	"SUBSCRIPTION_PURCHASED",
	"SUBSCRIPTION_RENEWED",
	"SUBSCRIPTION_RECOVERED",
	"SUBSCRIPTION_RESTARTED",
	"ONE_TIME_PRODUCT_PURCHASED",
]);

const NEGATIVE_TYPES = new Set([
	// Apple
	"REVOKE",
	"EXPIRED",
	"DID_FAIL_TO_RENEW",
	// Play
	"VOIDED_PURCHASE",
	"SUBSCRIPTION_REVOKED",
	"SUBSCRIPTION_EXPIRED",
	"SUBSCRIPTION_ON_HOLD",
	"ONE_TIME_PRODUCT_CANCELED",
	"SUBSCRIPTION_PENDING_PURCHASE_CANCELED",
]);

/** Shared tone for a notification type, used by tables and the detail modal. */
export function notificationTone(type: string): BadgeTone {
	if (type === "REFUND_DECLINED" || type === "REFUND_REVERSED") return "info";
	if (type.startsWith("REFUND") || NEGATIVE_TYPES.has(type)) return "negative";
	if (POSITIVE_TYPES.has(type)) return "positive";
	return "info";
}

export const RANGE_OPTIONS: { value: string; days: number | null; label: string }[] = [
	{ value: "all", days: null, label: "全部" },
	{ value: "7", days: 7, label: "近 7 天" },
	{ value: "30", days: 30, label: "近 30 天" },
	{ value: "90", days: 90, label: "近 90 天" },
];

type RawSearchParams = Record<string, string | string[] | undefined>;

function getParam(sp: RawSearchParams, key: string): string | undefined {
	const v = sp[key];
	return Array.isArray(v) ? v[0] : v;
}

/** Parse URL search params into a validated Filters object. */
export function parseFilters(sp: RawSearchParams): Filters {
	const product = getParam(sp, "product");
	const productId = product && PRODUCT_LABEL[product] ? product : null;

	const daysRaw = getParam(sp, "days");
	const match = RANGE_OPTIONS.find((o) => o.value === daysRaw);
	const days = match ? match.days : null;

	const platformRaw = getParam(sp, "platform");
	const platform =
		platformRaw && (PLATFORM_ORDER as readonly string[]).includes(platformRaw)
			? platformRaw
			: null;

	return { productId, days, platform };
}

/** Parse a 1-based page number from a search param (defaults to 1). */
export function parsePage(sp: RawSearchParams, key: string): number {
	const n = Number(getParam(sp, key));
	return Number.isInteger(n) && n > 1 ? n : 1;
}
