// 把一条 Play 通知转成 Bark 推送（与 lib/appstore/notify.ts 同款，标题带 ▶ 区分平台）。
// buildPlayBarkMessage 是纯函数（可单测）；notifyPlayEvent 负责发送（无 key 则跳过）。

import { type BarkPush, sendBark } from "../notify/bark";
import type { PlayLedgerRows } from "./logic";

// 「入账」类型：真正有钱进账的事件。SUBSCRIPTION_CANCELED（关自动续订）、
// SUBSCRIPTION_EXPIRED、VOIDED_PURCHASE 等都不算。
const REVENUE_TYPES = new Set<string>([
	"SUBSCRIPTION_PURCHASED",
	"SUBSCRIPTION_RENEWED",
	"SUBSCRIPTION_RECOVERED",
	"SUBSCRIPTION_RESTARTED",
	"ONE_TIME_PRODUCT_PURCHASED",
]);

const TYPE_LABEL: Record<string, string> = {
	SUBSCRIPTION_PURCHASED: "🎉 新订阅",
	SUBSCRIPTION_RENEWED: "🔁 订阅续期",
	SUBSCRIPTION_RECOVERED: "💚 账号保留期恢复",
	SUBSCRIPTION_RESTARTED: "↩️ 订阅重启",
	SUBSCRIPTION_CANCELED: "⚙️ 已关闭自动续订",
	SUBSCRIPTION_ON_HOLD: "⏸️ 进入账号保留期",
	SUBSCRIPTION_IN_GRACE_PERIOD: "⚠️ 进入宽限期",
	SUBSCRIPTION_PAUSED: "⏯️ 订阅已暂停",
	SUBSCRIPTION_PAUSE_SCHEDULE_CHANGED: "📅 暂停计划变更",
	SUBSCRIPTION_DEFERRED: "📅 续订已延长",
	SUBSCRIPTION_REVOKED: "🔕 权益撤销",
	SUBSCRIPTION_EXPIRED: "📕 订阅到期",
	SUBSCRIPTION_PRICE_CHANGE_CONFIRMED: "💱 涨价已确认",
	SUBSCRIPTION_PRICE_CHANGE_UPDATED: "💱 价格变更更新",
	SUBSCRIPTION_ITEMS_CHANGED: "🔀 套餐内容变更",
	SUBSCRIPTION_CANCELLATION_SCHEDULED: "🗓️ 已排期取消",
	SUBSCRIPTION_PENDING_PURCHASE_CANCELED: "🚫 待处理购买取消",
	ONE_TIME_PRODUCT_PURCHASED: "💰 买断购买",
	ONE_TIME_PRODUCT_CANCELED: "🚫 买断购买取消",
	VOIDED_PURCHASE: "↩️ 退款",
	PLAY_TEST: "🧪 测试通知",
};

const PRODUCT_LABEL: Record<string, string> = {
	"jiamin.chen.orange_cloud.pro.monthly": "Pro 月度",
	"jiamin.chen.orange_cloud.pro.yearly": "Pro 年度",
	"jiamin.chen.orange_cloud.pro.lifetime": "Pro 买断",
};

function formatPrice(millis: number | null, currency: string | null): string | null {
	if (millis == null || !currency) return null;
	return `${(millis / 1000).toFixed(2)} ${currency}`;
}

export interface BuiltPlayMessage {
	title: string;
	body: string;
	group: string;
	isSandbox: boolean;
	level: NonNullable<BarkPush["level"]>;
}

/** 账本行 -> Bark 标题/正文（纯函数）。 */
export function buildPlayBarkMessage(rows: PlayLedgerRows): BuiltPlayMessage {
	const { notification: nf, transaction: tx, subscription: sub } = rows;
	const isSandbox = nf.environment === "Sandbox";
	const label = TYPE_LABEL[nf.notificationType] ?? `📣 ${nf.notificationType}`;
	// ▶ = Play，与 Apple 侧同组推送并列时一眼分平台。
	const title = `${isSandbox ? "🧪 " : ""}▶ ${label}`;

	const productId = tx?.productId ?? sub?.productId ?? null;
	const priceMillis = tx?.priceMillis ?? sub?.priceMillis ?? null;
	const currency = tx?.currency ?? sub?.currency ?? null;

	const parts: string[] = [];
	if (nf.subtype) parts.push(nf.subtype);
	if (productId) parts.push(PRODUCT_LABEL[productId] ?? productId);
	const price = formatPrice(priceMillis, currency);
	if (price) parts.push(price);
	if (tx?.storefront) parts.push(tx.storefront);
	parts.push("Google Play");
	if (isSandbox) parts.push("Sandbox");

	const isPaidRevenue =
		REVENUE_TYPES.has(nf.notificationType) && priceMillis != null && priceMillis > 0;
	const level: BuiltPlayMessage["level"] = isSandbox
		? "passive"
		: isPaidRevenue
			? "timeSensitive"
			: "active";

	return {
		title,
		body: parts.join(" · "),
		// 与 Apple 侧同组，收入通知归拢在一起。
		group: isSandbox ? "[🧪]Orange Cloud IAP" : "Orange Cloud IAP",
		isSandbox,
		level,
	};
}

/** 构造并发送。无 deviceKey（未配置）则静默跳过；失败仅记日志、不抛。 */
export async function notifyPlayEvent(
	deviceKey: string | undefined,
	rows: PlayLedgerRows,
	server?: string,
): Promise<void> {
	if (!deviceKey) return;
	const msg = buildPlayBarkMessage(rows);
	const push: BarkPush = {
		title: msg.title,
		body: msg.body,
		group: msg.group,
		icon: "https://90dd.adsl8.workers.dev/icons/icon-64.png",
		level: msg.level,
		...(msg.level === "timeSensitive" ? { sound: "paymentsuccess" } : {}),
	};
	try {
		await sendBark(deviceKey, push, server);
	} catch (err) {
		console.error("[play-notifications] bark push failed", err);
	}
}
