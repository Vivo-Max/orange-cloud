import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { timingSafeEqual } from "@/lib/asc/verify";
import { getReleaseState, putTrackState } from "@/lib/livestate/store";
import { reduceTrackState, type Bucket } from "@/lib/livestate/logic";

// Android 发布信号入口。Google Play 无「应用已上架」webhook（RTDN 只管订阅），
// 故由 apps/android/fastlane 的 deploy lane 在 supply 到 production 后回调本端点。
//
// 鉴权 = 共享密钥头 X-Play-Secret（常数时间比对 PLAY_RELEASE_SECRET）。
// body: { "version": "<versionName>", "state"?: "live"|"in_review"|"pending_release"|"cleared" }
//   state 缺省 "live"（Play 审核快、无完成 webhook，故上传生产即视为上架；
//   想要「审核中」标注可传 state:"in_review"，过后再调一次 state:"live"）。

export const dynamic = "force-dynamic";

const BUCKETS = new Set<Bucket>(["live", "in_review", "pending_release", "cleared"]);

export async function POST(request: NextRequest): Promise<NextResponse> {
	const { env } = getCloudflareContext();
	const secret = (env as CloudflareEnv & { PLAY_RELEASE_SECRET?: string }).PLAY_RELEASE_SECRET;
	if (!secret) return NextResponse.json({ error: "not configured" }, { status: 503 });
	if (!timingSafeEqual(request.headers.get("x-play-secret") ?? "", secret)) {
		return NextResponse.json({ error: "unauthorized" }, { status: 401 });
	}

	let body: { version?: unknown; state?: unknown };
	try {
		body = (await request.json()) as { version?: unknown; state?: unknown };
	} catch {
		return NextResponse.json({ error: "bad json" }, { status: 400 });
	}
	const version = typeof body.version === "string" ? body.version.trim() : "";
	if (!version) return NextResponse.json({ error: "missing version" }, { status: 400 });
	const bucket: Bucket = typeof body.state === "string" && BUCKETS.has(body.state as Bucket) ? (body.state as Bucket) : "live";

	if (env.IAP_DB) {
		const state = await getReleaseState(env.IAP_DB);
		await putTrackState(env.IAP_DB, "android", reduceTrackState(state.android ?? {}, version, bucket));
	}
	return NextResponse.json({ ok: true, track: "android", version, bucket });
}
