import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { normalizeCode } from "@/lib/codes/code";
import { redeemCode } from "@/lib/codes/store";

// App（Android direct 风味）调用：兑换激活码 + 周期复核。
// 请求体 { code, install_id }。幂等：首次绑定设备，之后复核（用于尊重退款撤销）。
// 客户端应缓存 ok 结果带 TTL（离线宽限），仅原生调用、无需 CORS。

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
	const decision = await redeemCode(env.IAP_DB, core, installId);

	const status = decision.ok
		? 200
		: decision.reason === "not_found"
			? 404
			: decision.reason === "device_limit"
				? 409
				: 403; // revoked
	return NextResponse.json(
		{ ok: decision.ok, reason: decision.reason, product: decision.product ?? null },
		{ status },
	);
}
