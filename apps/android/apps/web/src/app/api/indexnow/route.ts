import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { isApiAuthed } from "@/lib/admin/auth";
import { previewPending, syncIndexNow } from "@/lib/indexnow/sync";

// IndexNow 手动推送接口。鉴权同后台：会话 cookie 或 `Authorization: Bearer <管理口令>`。
//   GET  /api/indexnow          → 预览「本次会推哪些 URL」，不发请求
//   POST /api/indexnow          → 推送变更的 URL
//   POST /api/indexnow?force=1  → 无视台账全量重推（换域名 / 台账丢了时用）
// 平时不用管：cron 每天自动跑一趟（custom-worker.ts）。发完新指南想立刻通知才打这里。
export const dynamic = "force-dynamic";

const NO_STORE = { "cache-control": "no-store" };

export async function GET(request: NextRequest): Promise<NextResponse> {
	const { env } = getCloudflareContext();
	if (!(await isApiAuthed(request, env.ADMIN_PASSWORD))) {
		return NextResponse.json({ error: "unauthorized" }, { status: 401 });
	}
	const force = request.nextUrl.searchParams.get("force") === "1";
	const preview = await previewPending(env.IAP_DB, force);
	return NextResponse.json(
		{ total: preview.total, skipped: preview.skipped, pending: preview.pending.map((e) => e.url) },
		{ headers: NO_STORE },
	);
}

export async function POST(request: NextRequest): Promise<NextResponse> {
	const { env } = getCloudflareContext();
	if (!(await isApiAuthed(request, env.ADMIN_PASSWORD))) {
		return NextResponse.json({ error: "unauthorized" }, { status: 401 });
	}
	const force = request.nextUrl.searchParams.get("force") === "1";
	const result = await syncIndexNow(env.IAP_DB, { force });
	return NextResponse.json(result, { status: result.ok ? 200 : 502, headers: NO_STORE });
}
