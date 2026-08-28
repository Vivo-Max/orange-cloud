import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { normalizeCode } from "@/lib/codes/code";
import { selfResetDevices } from "@/lib/codes/store";

// 用户自助重置设备绑定：激活码 + 购买邮箱验证，每码 30 天最多 1 次。
// 反枚举：码不存在 / 邮箱不符都回 404 invalid。
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
	if (!core || !email) {
		return NextResponse.json({ ok: false, reason: "bad_request" }, { status: 400 });
	}

	const { env } = getCloudflareContext();
	const result = await selfResetDevices(env.IAP_DB, core, email);

	const status = result.ok
		? 200
		: result.reason === "rate_limited"
			? 429
			: result.reason === "no_email"
				? 422
				: result.reason === "revoked"
					? 410
					: 404; // invalid
	return NextResponse.json(
		{ ok: result.ok, reason: result.reason, removed: result.removed ?? 0, nextAt: result.nextAt ?? null },
		{ status },
	);
}
