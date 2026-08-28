import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { isAuthed } from "@/lib/admin/auth";

// 财务流水导出 CSV（仅 Production，需登录）。topbar「导出 CSV」按钮指向这里。
export const dynamic = "force-dynamic";

interface Row {
	purchase_date: number | null;
	notification_type: string | null;
	type: string | null;
	product_id: string | null;
	transaction_id: string;
	original_transaction_id: string;
	currency: string | null;
	price_millis: number | null;
	revocation_date: number | null;
	environment: string | null;
	platform: string | null;
	dev_revenue_millis: number | null;
}

function cell(v: unknown): string {
	const s = v == null ? "" : String(v);
	return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
}
const iso = (ms: number | null) => (ms ? new Date(ms).toISOString() : "");

export async function GET(request: NextRequest): Promise<NextResponse> {
	const { env } = getCloudflareContext();
	if (!(await isAuthed(request, env.ADMIN_PASSWORD))) {
		return NextResponse.redirect(new URL("/admin/login", request.url));
	}

	const { results } = await env.IAP_DB.prepare(
		`SELECT purchase_date, notification_type, type, product_id, transaction_id, original_transaction_id,
		        currency, price_millis, revocation_date, environment, platform, dev_revenue_millis
		 FROM transactions
		 WHERE COALESCE(environment, '') <> 'Sandbox'
		 ORDER BY COALESCE(purchase_date, created_at) DESC
		 LIMIT 5000`,
	).all<Row>();

	// dev_revenue（开发者到手金额）目前只有 Play 侧有值，Apple 行留空。
	const header = [
		"purchase_date", "platform", "notification_type", "transaction_type", "product_id",
		"transaction_id", "original_transaction_id", "currency", "price", "dev_revenue",
		"refunded", "revocation_date",
	];
	const lines = [header.join(",")];
	for (const r of results ?? []) {
		lines.push(
			[
				iso(r.purchase_date), r.platform ?? "apple", r.notification_type, r.type, r.product_id,
				r.transaction_id, r.original_transaction_id, r.currency,
				r.price_millis == null ? "" : (r.price_millis / 1000).toFixed(2),
				r.dev_revenue_millis == null ? "" : (r.dev_revenue_millis / 1000).toFixed(2),
				r.revocation_date ? "yes" : "no", iso(r.revocation_date),
			]
				.map(cell)
				.join(","),
		);
	}

	const csv = "﻿" + lines.join("\n"); // BOM 便于 Excel 识别 UTF-8
	return new NextResponse(csv, {
		headers: {
			"content-type": "text/csv; charset=utf-8",
			"content-disposition": `attachment; filename="orange-cloud-ledger.csv"`,
			"cache-control": "no-store",
		},
	});
}
