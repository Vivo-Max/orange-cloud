// 用 Play Developer API 把一条 RTDN 补全成「有金额、有状态」的事件。
//
// RTDN 本身只有 purchaseToken + 类型，所以每条通知按种类回查 1～2 个接口：
//   订阅   -> subscriptionsv2.get（状态/到期/续订开关/latestOrderId）+ orders.get（金额）
//   买断   -> products.get（购买状态/时间/orderId）+ orders.get（金额）
//   退款   -> orders.get（金额 + 退款状态）+ 视类型补订阅或买断详情
// 未配置服务账号（PlayApi 为 null）时全部跳过，返回空富化。

import type { PlayApi } from "./api";
import { purchaseTokenOf } from "./logic";
import type { DecodedPlayNotification, PlayEnrichment } from "./types";

export async function enrichNotification(
	api: PlayApi | null,
	decoded: DecodedPlayNotification,
): Promise<PlayEnrichment> {
	const token = purchaseTokenOf(decoded);
	if (!api || !token) return {};

	const n = decoded.notification;
	const out: PlayEnrichment = {};

	if (n.subscriptionNotification) {
		out.subscription = (await api.getSubscription(token)) ?? undefined;
		const orderId = out.subscription?.latestOrderId;
		if (orderId) out.order = (await api.getOrder(orderId)) ?? undefined;
		return out;
	}

	if (n.oneTimeProductNotification) {
		const sku = n.oneTimeProductNotification.sku;
		if (sku) out.product = (await api.getProduct(sku, token)) ?? undefined;
		const orderId = out.product?.orderId;
		if (orderId) out.order = (await api.getOrder(orderId)) ?? undefined;
		return out;
	}

	if (n.voidedPurchaseNotification) {
		const v = n.voidedPurchaseNotification;
		if (v.orderId) out.order = (await api.getOrder(v.orderId)) ?? undefined;
		if (v.productType === 1) {
			// 订阅退款：状态接口还能查到这条被撤销的订阅。
			out.subscription = (await api.getSubscription(token)) ?? undefined;
		} else if (v.productType === 2) {
			// 买断退款：RTDN 不带 sku，从订单行反查商品后再取购买详情。
			const productId = out.order?.lineItems?.[0]?.productId;
			if (productId) out.product = (await api.getProduct(productId, token)) ?? undefined;
		}
		return out;
	}

	return out;
}
