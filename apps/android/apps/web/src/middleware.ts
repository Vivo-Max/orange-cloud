import createMiddleware from "next-intl/middleware";
import { NextResponse, type NextRequest } from "next/server";
import { routing } from "./i18n/routing";
import { GUIDE_LOCALE } from "./lib/guides/guides";

const handleI18n = createMiddleware(routing);

const GUIDES_PREFIX = "/guides";

export default function middleware(request: NextRequest) {
	const { pathname } = request.nextUrl;

	// 指南板块目前只有英文版：跳过语言协商，否则中文浏览器打开 /guides 会被
	// 重定向到 /zh-Hant/guides（不存在，落 404）。这里内部改写到 /en/…，
	// 对外 URL 保持无前缀——与 as-needed 下默认语言的行为一致。
	if (pathname === GUIDES_PREFIX || pathname.startsWith(`${GUIDES_PREFIX}/`)) {
		const url = request.nextUrl.clone();
		url.pathname = `/${GUIDE_LOCALE}${pathname}`;
		return NextResponse.rewrite(url);
	}

	return handleI18n(request);
}

export const config = {
	// 排除 OAuth 回调（/oauth/callback 必须原样直达 route handler）、
	// 后台账本（/admin 直出 HTML，不走 i18n）、
	// API、Next 内部路径与所有带扩展名的静态资源。
	matcher: ["/((?!api|oauth|admin|_next|_vercel|.*\\..*).*)"],
};
