import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { isApiAuthed } from "@/lib/admin/auth";
import { loadAdminStats } from "@/lib/admin/queries";

// 后台账本 JSON 接口。鉴权：会话 cookie 或 `Authorization: Bearer <管理口令>`。
export const dynamic = "force-dynamic";

export async function GET(request: NextRequest): Promise<NextResponse> {
	const { env } = getCloudflareContext();
	if (!(await isApiAuthed(request, env.ADMIN_PASSWORD))) {
		return NextResponse.json({ error: "unauthorized" }, { status: 401 });
	}
	const range = request.nextUrl.searchParams.get("range") === "month" ? "month" : "day";
	const stats = await loadAdminStats(env.IAP_DB, range);
	return NextResponse.json(stats, { headers: { "cache-control": "no-store" } });
}
