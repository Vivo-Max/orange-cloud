import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { isApiAuthed } from "@/lib/admin/auth";
import { normalizeCode } from "@/lib/codes/code";
import { setBuyerEmail } from "@/lib/codes/store";

// 后台给某码绑定 / 修改邮箱（管理员鉴权）。手动码无 buyer_email，绑定后用户才能凭邮箱自助重置。
export const dynamic = "force-dynamic";

export async function POST(request: NextRequest): Promise<NextResponse> {
	const { env } = getCloudflareContext();
	const cfg = env as { ADMIN_PASSWORD?: string };
	if (!(await isApiAuthed(request, cfg.ADMIN_PASSWORD))) {
		return NextResponse.json({ error: "unauthorized" }, { status: 401 });
	}

	let body: { code?: unknown; email?: unknown };
	try {
		body = (await request.json()) as typeof body;
	} catch {
		return NextResponse.json({ error: "bad_request" }, { status: 400 });
	}
	const core = typeof body.code === "string" ? normalizeCode(body.code) : null;
	const email = typeof body.email === "string" ? body.email.trim() : "";
	if (!core || !email) {
		return NextResponse.json({ error: "bad_request" }, { status: 400 });
	}

	const updated = await setBuyerEmail(env.IAP_DB, core, email);
	if (!updated) return NextResponse.json({ error: "not_found" }, { status: 404 });
	return NextResponse.json({ ok: true });
}
