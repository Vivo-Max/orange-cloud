import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { formatCode } from "@/lib/codes/code";

// 成功页轮询：按 Stripe Checkout session_id 取激活码。
// 微信 / 支付宝是异步支付，webhook 可能尚未落库 -> ready:false，前端继续轮询 / 提示查邮箱。
// session_id 是 Stripe 不可猜的长串，充当取码凭据（同 Stripe 官方 success_url 模式）。
export const dynamic = "force-dynamic";

export async function GET(request: NextRequest): Promise<NextResponse> {
	const sessionId = request.nextUrl.searchParams.get("session_id");
	if (!sessionId) {
		return NextResponse.json({ ready: false, reason: "bad_request" }, { status: 400 });
	}

	const { env } = getCloudflareContext();
	const row = await env.IAP_DB.prepare(`SELECT code, status FROM codes WHERE stripe_session_id = ?`)
		.bind(sessionId)
		.first<{ code: string; status: string }>();

	if (!row) return NextResponse.json({ ready: false });
	return NextResponse.json({
		ready: true,
		code: formatCode(row.code),
		revoked: row.status === "revoked",
	});
}
