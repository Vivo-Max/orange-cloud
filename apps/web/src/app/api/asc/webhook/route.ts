import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { verifyAscSignature } from "@/lib/asc/verify";
import { getReleaseState, putTrackState } from "@/lib/livestate/store";
import { bucketOfAppVersionState, reduceTrackState } from "@/lib/livestate/logic";
import { latestVersion } from "@orange-cloud/changelog";

// App Store Connect webhook 入口（只需在 ASC 勾选「App 版本状态」一项）。
// App 版本状态变更（appStoreVersionAppVersionStateUpdated）驱动整套状态机：
//   送审/审核中 → 官网把最新版本标「审核中」；READY_FOR_DISTRIBUTION → 去标注、正常展示；
//   被拒/撤回 → 撤下「审核中」。
//
// 安全边界 = HMAC-SHA256 验签（头 X-Apple-Signature，secret 在 ASC 后台设定 = Worker secret ASC_WEBHOOK_SECRET）。
// payload 不含版本「字符串」（只有版本资源 ID），故"哪个版本"用单一数据源解析（ios.json 最新版）——
// 前提：发版说明在送审前已提交。
//
// 配置：App Store Connect → Users and Access → Integrations → Webhooks，
//   事件 App Version State，URL https://oss.omail.us.kg/api/asc/webhook

export const dynamic = "force-dynamic";

interface AscEvent {
	data?: { type?: string; attributes?: { newValue?: string; oldValue?: string } };
}

export async function POST(request: NextRequest): Promise<NextResponse> {
	const { env } = getCloudflareContext();
	const secret = (env as CloudflareEnv & { ASC_WEBHOOK_SECRET?: string }).ASC_WEBHOOK_SECRET;
	if (!secret) return NextResponse.json({ error: "not configured" }, { status: 503 });

	const raw = await request.text();
	if (!(await verifyAscSignature(raw, request.headers.get("x-apple-signature"), secret))) {
		return NextResponse.json({ error: "invalid signature" }, { status: 401 });
	}

	let event: AscEvent;
	try {
		event = JSON.parse(raw) as AscEvent;
	} catch {
		return NextResponse.json({ error: "bad json" }, { status: 400 });
	}

	const newValue = event.data?.attributes?.newValue;
	if (event.data?.type !== "appStoreVersionAppVersionStateUpdated" || !newValue) {
		return NextResponse.json({ ok: true, ignored: true });
	}
	const bucket = bucketOfAppVersionState(newValue);
	const version = latestVersion("ios");
	if (!bucket || !version) return NextResponse.json({ ok: true, ignored: true, state: newValue });

	if (env.IAP_DB) {
		const state = await getReleaseState(env.IAP_DB);
		await putTrackState(env.IAP_DB, "ios", reduceTrackState(state.ios ?? {}, version, bucket));
	}
	return NextResponse.json({ ok: true, track: "ios", version, state: newValue, bucket });
}
