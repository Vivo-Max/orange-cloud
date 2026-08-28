import { NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { captureRanks } from "@/lib/ranks/capture";

// 开放路由：被调用即执行一次排名采集（与每日 cron 同一逻辑）。
// 无鉴权——方便手动 / 外部定时器（uptime 探活之类）触发。
// 60s 去抖：仅拦截极短时间内的重复 / 并发调用（避免连打 Apple），不影响正常按需触发。
export const dynamic = "force-dynamic";

const DEBOUNCE_MS = 60_000;

export async function GET(): Promise<NextResponse> {
	const { env } = getCloudflareContext();
	const headers = { "cache-control": "no-store" };
	if (!env.IAP_DB) {
		return NextResponse.json({ ok: false, error: "no database binding" }, { status: 503, headers });
	}

	const last = await env.IAP_DB.prepare("SELECT MAX(captured_at) AS last FROM app_store_ranks").first<{
		last: number | null;
	}>();
	const lastAt = last?.last ?? null;
	if (lastAt && Date.now() - lastAt < DEBOUNCE_MS) {
		return NextResponse.json({ ok: true, triggered: false, reason: "recently captured", lastCapturedAt: lastAt }, { headers });
	}

	// result.captured / result.skipped 是「地区码数组」（已采集 / 因不可用跳过）。
	const result = await captureRanks(env.IAP_DB);
	return NextResponse.json({ ok: true, triggered: true, ...result }, { headers });
}
