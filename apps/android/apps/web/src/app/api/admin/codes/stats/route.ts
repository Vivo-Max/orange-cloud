import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { isApiAuthed } from "@/lib/admin/auth";
import { getCodesStats } from "@/lib/codes/admin";

// 激活码（安卓 direct 渠道）只读统计 JSON 接口。鉴权同 IAP 看板：
// 会话 cookie 或 `Authorization: Bearer <管理口令>`（方便 curl / 树莓派拉取）。
// 与 /api/admin/stats（Apple IAP）互补：那个读三表 USD 口径，这个读 codes 表原币口径。
// getCodesStats 内部对未应用迁移 0005 的情况容错返回空，不会抛。
export const dynamic = "force-dynamic";

export async function GET(request: NextRequest): Promise<NextResponse> {
	const { env } = getCloudflareContext();
	if (!(await isApiAuthed(request, env.ADMIN_PASSWORD))) {
		return NextResponse.json({ error: "unauthorized" }, { status: 401 });
	}
	const stats = await getCodesStats(env.IAP_DB);
	return NextResponse.json(stats, { headers: { "cache-control": "no-store" } });
}
