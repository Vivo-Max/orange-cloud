import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { normalizeCode } from "@/lib/codes/code";
import { selfBindEmail } from "@/lib/codes/store";
import { EmailBinding, sendCodesBindEmail } from "@/lib/notify/email";

// 用户自助绑定邮箱：仅「尚未绑定邮箱」的有效码可绑（已绑定的须走人工，防劫持找回邮箱）。
export const dynamic = "force-dynamic";

export async function POST(request: NextRequest): Promise<NextResponse> {
	let body: { code?: unknown; email?: unknown };
	try {
		body = (await request.json()) as typeof body;
	} catch {
		return NextResponse.json({ ok: false, reason: "bad_request" }, { status: 400 });
	}
	const core = typeof body.code === "string" ? normalizeCode(body.code) : null;
	const email = typeof body.email === "string" ? body.email.trim() : "";
	if (!core || !email || !email.includes("@")) {
		return NextResponse.json({ ok: false, reason: "bad_request" }, { status: 400 });
	}

	const { env, ctx } = getCloudflareContext();
	const result = await selfBindEmail(env.IAP_DB, core, email);
	if (result.ok) {
		const cfg = env as { EMAIL?: EmailBinding };
		ctx.waitUntil(sendCodesBindEmail(cfg.EMAIL, email, [core]));
	}

	const status = result.ok
		? 200
		: result.reason === "already_bound"
			? 409
			: result.reason === "revoked"
				? 410
				: 404; // invalid
	return NextResponse.json({ ok: result.ok, reason: result.reason }, { status });
}
