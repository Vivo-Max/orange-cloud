import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { isApiAuthed } from "@/lib/admin/auth";
import { formatCode } from "@/lib/codes/code";
import { createManualCodes } from "@/lib/codes/store";
import { sendInviteEmail, type EmailBinding } from "@/lib/notify/email";

// 后台手动生成激活码（管理员鉴权）：用于媒体 / 评测渠道分发。source=manual、无支付。
// 可选 note（渠道备注）、email（创建即绑定，便于日后自助重置）。count 限 1..50。
export const dynamic = "force-dynamic";

export async function POST(request: NextRequest): Promise<NextResponse> {
	const { env, ctx } = getCloudflareContext();
	const cfg = env as { ADMIN_PASSWORD?: string; EMAIL?: EmailBinding };
	if (!(await isApiAuthed(request, cfg.ADMIN_PASSWORD))) {
		return NextResponse.json({ error: "unauthorized" }, { status: 401 });
	}

	let body: { count?: unknown; note?: unknown; email?: unknown };
	try {
		body = (await request.json()) as typeof body;
	} catch {
		return NextResponse.json({ error: "bad_request" }, { status: 400 });
	}

	const count = Math.min(50, Math.max(1, Math.floor(Number(body.count) || 1)));
	const note = typeof body.note === "string" && body.note.trim() ? body.note.trim().slice(0, 200) : null;
	const email = typeof body.email === "string" && body.email.trim() ? body.email.trim() : null;

	const cores = await createManualCodes(env.IAP_DB, count, note, email);
	// 填了邮箱则发邀请邮件（媒体/评测可直接拿到码）；fire-and-forget。
	if (email && cores.length > 0) {
		ctx.waitUntil(sendInviteEmail(cfg.EMAIL, email, cores, note));
	}
	return NextResponse.json({ ok: true, codes: cores.map(formatCode) });
}
