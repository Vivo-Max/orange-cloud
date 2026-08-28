import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { normalizeCode } from "@/lib/codes/code";
import { deactivate } from "@/lib/codes/store";

// App（direct 风味）自助反激活：释放本设备 (code, install_id) 的绑定名额。
// 凭据 = 知道 code + 自己的 install_id（同 /api/redeem），无需额外鉴权；幂等（删不到也回 ok）。
export const dynamic = "force-dynamic";

export async function POST(request: NextRequest): Promise<NextResponse> {
	let body: { code?: unknown; install_id?: unknown };
	try {
		body = (await request.json()) as typeof body;
	} catch {
		return NextResponse.json({ ok: false, reason: "bad_request" }, { status: 400 });
	}
	const installId = typeof body.install_id === "string" ? body.install_id.trim() : "";
	const core = typeof body.code === "string" ? normalizeCode(body.code) : null;
	if (!installId || !core) {
		return NextResponse.json({ ok: false, reason: "bad_request" }, { status: 400 });
	}

	const { env } = getCloudflareContext();
	const removed = await deactivate(env.IAP_DB, core, installId);
	return NextResponse.json({ ok: true, removed });
}
