import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { isApiAuthed } from "@/lib/admin/auth";
import { normalizeCode } from "@/lib/codes/code";
import { resetActivations } from "@/lib/codes/store";

// 后台解除某码的全部设备绑定（管理员鉴权）。用于「换了所有设备被锁」的人工恢复。
export const dynamic = "force-dynamic";

export async function POST(request: NextRequest): Promise<NextResponse> {
	const { env } = getCloudflareContext();
	const cfg = env as { ADMIN_PASSWORD?: string };
	if (!(await isApiAuthed(request, cfg.ADMIN_PASSWORD))) {
		return NextResponse.json({ error: "unauthorized" }, { status: 401 });
	}

	let body: { code?: unknown };
	try {
		body = (await request.json()) as typeof body;
	} catch {
		return NextResponse.json({ error: "bad_request" }, { status: 400 });
	}
	const core = typeof body.code === "string" ? normalizeCode(body.code) : null;
	if (!core) return NextResponse.json({ error: "bad_request" }, { status: 400 });

	const removed = await resetActivations(env.IAP_DB, core);
	return NextResponse.json({ ok: true, removed });
}
