// 创建 Stripe Checkout Session —— 手写 form-POST，不引 stripe SDK，沿用本仓「最小依赖 + 手写」风格。
// 微信 / 支付宝是一次性支付方式：mode=payment；微信在 Checkout 必须给 client=web，否则不显示。

const STRIPE_API = "https://api.stripe.com/v1/checkout/sessions";

// 正式价（Stripe 香港账户）：HK$34.99 终身买断（港币原币收款，避免 CNY 跨币种转换费）。
// 测试模式用环境变量 STRIPE_PRICE_LIFETIME 覆盖（测试 / 正式的 price id 不同）。
const DEFAULT_PRICE = "price_1TlrEUL1CspNu8sawyh6Vdys";

export interface CheckoutOptions {
	secretKey: string;
	priceId?: string;
	successUrl: string;
	cancelUrl: string;
}

export async function createCheckoutSession(
	opts: CheckoutOptions,
): Promise<{ id: string; url: string }> {
	const form = new URLSearchParams();
	form.set("mode", "payment"); // 买断 = 一次性付款（非 subscription）
	form.set("line_items[0][price]", opts.priceId || DEFAULT_PRICE);
	form.set("line_items[0][quantity]", "1");
	form.set("payment_method_types[0]", "wechat_pay");
	form.set("payment_method_types[1]", "alipay");
	form.set("payment_method_types[2]", "card");
	form.set("payment_method_options[wechat_pay][client]", "web"); // ★ 微信 Checkout 必填
	form.set("success_url", opts.successUrl);
	form.set("cancel_url", opts.cancelUrl);
	form.set("locale", "zh");
	form.set("metadata[channel]", "android-cn-direct");

	const res = await fetch(STRIPE_API, {
		method: "POST",
		headers: {
			authorization: `Bearer ${opts.secretKey}`,
			"content-type": "application/x-www-form-urlencoded",
		},
		body: form.toString(),
	});
	if (!res.ok) {
		throw new Error(`stripe checkout create failed ${res.status}: ${await res.text()}`);
	}
	const json = (await res.json()) as { id: string; url: string };
	return { id: json.id, url: json.url };
}
