import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { createCheckoutSession } from "@/lib/stripe/checkout";
import { routing } from "@/i18n/routing";

// 销售页「购买」按钮调用 -> 服务端创建 Checkout Session，返回 { url } 供前端跳转。
// 金额 / 商品一律服务端从固定 price 取，绝不信客户端传入。
// 回跳地址按购买页语言加前缀，但仅接受白名单内的 locale（防开放重定向）。
// STRIPE_SECRET_KEY（必）/ STRIPE_PRICE_LIFETIME（测试模式覆盖）经 wrangler secret / .dev.vars 注入。

export const dynamic = "force-dynamic";

const FALLBACK_ORIGIN = "https://90dd.adsl8.workers.dev";

export async function POST(request: NextRequest): Promise<NextResponse> {
	const { env } = getCloudflareContext();
	const cfg = env as { STRIPE_SECRET_KEY?: string; STRIPE_PRICE_LIFETIME?: string };
	if (!cfg.STRIPE_SECRET_KEY) {
		return NextResponse.json({ error: "not configured" }, { status: 503 });
	}

	// 回跳地址按购买页的语言加前缀（next-intl as-needed：默认语言无前缀）。
	let locale: string = routing.defaultLocale;
	try {
		const body = (await request.json()) as { locale?: unknown };
		if (typeof body.locale === "string" && (routing.locales as readonly string[]).includes(body.locale)) {
			locale = body.locale;
		}
	} catch {
		// 无 body / 非 JSON：用默认语言
	}
	const prefix = locale === routing.defaultLocale ? "" : `/${locale}`;

	try {
		const origin = new URL(request.url).origin || FALLBACK_ORIGIN;
		const session = await createCheckoutSession({
			secretKey: cfg.STRIPE_SECRET_KEY,
			priceId: cfg.STRIPE_PRICE_LIFETIME,
			successUrl: `${origin}${prefix}/buy/success?session_id={CHECKOUT_SESSION_ID}`,
			cancelUrl: `${origin}${prefix}/buy`,
		});
		return NextResponse.json({ url: session.url });
	} catch (err) {
		console.error("[checkout] create failed", err);
		return NextResponse.json({ error: "checkout failed" }, { status: 502 });
	}
}
