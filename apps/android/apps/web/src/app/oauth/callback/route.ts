import { NextRequest, NextResponse } from "next/server";

// OAuth 回调中转：Cloudflare 只接受 https redirect_uri，
// 此路由将授权码 302 透传给 iOS App 的自定义 scheme。
// 不存 code、不换 Token、不验 state（验证在 iOS App 端做，PKCE 保证安全）。
const APP_CALLBACK = "orangecloud://oauth/callback";

export async function GET(request: NextRequest) {
	const { searchParams } = request.nextUrl;

	const code = searchParams.get("code");
	const state = searchParams.get("state");
	const error = searchParams.get("error");

	// Cloudflare 返回错误（用户拒绝授权等）
	if (error) {
		const errorDesc = searchParams.get("error_description") ?? error;
		return NextResponse.redirect(`${APP_CALLBACK}?error=${encodeURIComponent(errorDesc)}`, { status: 302 });
	}

	// 缺少必要参数
	if (!code || !state) {
		return NextResponse.redirect(`${APP_CALLBACK}?error=invalid_response`, { status: 302 });
	}

	const appCallbackUrl = new URL(APP_CALLBACK);
	appCallbackUrl.searchParams.set("code", code);
	appCallbackUrl.searchParams.set("state", state);

	return NextResponse.redirect(appCallbackUrl.toString(), { status: 302 });
}
