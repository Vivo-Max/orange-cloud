import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { normalizeCode, formatCode } from "@/lib/codes/code";
import { requestRefund } from "@/lib/codes/store";
import { sendBark } from "@/lib/notify/bark";

// 官网自助退款「申请」（非即时退款）：校验 30 天政策 + 状态 -> 置 requested -> Bark 通知作者审批。
// 真正的退款在后台 /admin 审批时才发起（/api/admin/refund）。
export const dynamic = "force-dynamic";

export async function POST(request: NextRequest): Promise<NextResponse> {
	let body: { code?: unknown; reason?: unknown };
	try {
		body = (await request.json()) as typeof body;
	} catch {
		return NextResponse.json({ ok: false, reason: "bad_request" }, { status: 400 });
	}

	const core = typeof body.code === "string" ? normalizeCode(body.code) : null;
	const reason = (typeof body.reason === "string" ? body.reason : "").trim().slice(0, 500);
	if (!core) {
		return NextResponse.json({ ok: false, reason: "bad_request" }, { status: 400 });
	}

	const { env, ctx } = getCloudflareContext();
	const result = await requestRefund(env.IAP_DB, core, reason);

	if (result.ok) {
		const cfg = env as { BARK_KEY?: string; BARK_SERVER?: string };
		if (cfg.BARK_KEY) {
			ctx.waitUntil(
				sendBark(
					cfg.BARK_KEY,
					{
						title: "新退款申请",
						body: `${formatCode(core)}${reason ? `\n${reason}` : ""}`,
						group: "Orange Cloud",
						level: "timeSensitive",
					},
					cfg.BARK_SERVER,
				).catch(() => {}),
			);
		}
	}

	const status = result.ok
		? 200
		: result.reason === "not_found"
			? 404
			: result.reason === "not_paid"
				? 403
			: result.reason === "window_expired"
				? 410
				: 409; // already_requested / already_refunded
	return NextResponse.json({ ok: result.ok, reason: result.reason }, { status });
}
