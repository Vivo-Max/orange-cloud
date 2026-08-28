import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { isApiAuthed } from "@/lib/admin/auth";
import { normalizeCode } from "@/lib/codes/code";
import { getCodeForRefund, markRefundApproved, markRefundRejected } from "@/lib/codes/store";
import { createRefund } from "@/lib/stripe/refund";

// 后台审批退款申请（需管理员鉴权）。
//   approve -> 发起 Stripe 退款（charge.refunded webhook 会撤销码）+ 立即标记 approved/revoked（幂等）
//   reject  -> 仅标记 rejected
export const dynamic = "force-dynamic";

export async function POST(request: NextRequest): Promise<NextResponse> {
	const { env } = getCloudflareContext();
	const cfg = env as { ADMIN_PASSWORD?: string; STRIPE_SECRET_KEY?: string };
	if (!(await isApiAuthed(request, cfg.ADMIN_PASSWORD))) {
		return NextResponse.json({ error: "unauthorized" }, { status: 401 });
	}

	let body: { code?: unknown; action?: unknown };
	try {
		body = (await request.json()) as typeof body;
	} catch {
		return NextResponse.json({ error: "bad_request" }, { status: 400 });
	}
	const core = typeof body.code === "string" ? normalizeCode(body.code) : null;
	const action = body.action;
	if (!core || (action !== "approve" && action !== "reject")) {
		return NextResponse.json({ error: "bad_request" }, { status: 400 });
	}

	if (action === "reject") {
		await markRefundRejected(env.IAP_DB, core);
		return NextResponse.json({ ok: true });
	}

	// approve
	const row = await getCodeForRefund(env.IAP_DB, core);
	if (!row) return NextResponse.json({ error: "not_found" }, { status: 404 });
	if (!row.paymentIntent) {
		return NextResponse.json({ error: "cannot_refund" }, { status: 422 });
	}
	// 已退款 / 已撤销：幂等返回
	if (row.refundStatus === "approved" || row.status === "revoked") {
		return NextResponse.json({ ok: true, already: true });
	}
	if (!cfg.STRIPE_SECRET_KEY) {
		return NextResponse.json({ error: "cannot_refund" }, { status: 422 });
	}

	try {
		await createRefund(cfg.STRIPE_SECRET_KEY, row.paymentIntent);
	} catch (err) {
		console.error("[admin-refund] stripe refund failed", err);
		return NextResponse.json({ error: "stripe_error" }, { status: 502 });
	}
	await markRefundApproved(env.IAP_DB, core);
	return NextResponse.json({ ok: true });
}
