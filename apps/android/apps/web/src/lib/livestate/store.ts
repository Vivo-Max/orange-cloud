// 平台上架状态机的读写。官网更新历史据此门控与标注（见 page.tsx / decoratedReleases）。
// 写入方：/api/asc/webhook（iOS，ASC 全状态机）、/api/play/release（Android，fastlane 回调）。

import type { PendingState, TrackState } from "@orange-cloud/changelog";

export type Track = "ios" | "android";
export type ReleaseState = Partial<Record<Track, TrackState>>;
export type { PendingState, TrackState };

/** 读出各平台状态，缺则不含该 key。 */
export async function getReleaseState(db: D1Database): Promise<ReleaseState> {
	const { results } = await db
		.prepare("SELECT track, live_version, pending_version, pending_state FROM release_state")
		.all<{ track: string; live_version: string | null; pending_version: string | null; pending_state: string | null }>();
	const out: ReleaseState = {};
	for (const r of results ?? []) {
		if (r.track !== "ios" && r.track !== "android") continue;
		out[r.track] = {
			liveVersion: r.live_version ?? undefined,
			pendingVersion: r.pending_version ?? undefined,
			pendingState: (r.pending_state as PendingState | null) ?? undefined,
		};
	}
	return out;
}

/** 整行覆写某 track 的状态（调用方已用 reduceTrackState 算好 next）。 */
export async function putTrackState(
	db: D1Database,
	track: Track,
	s: TrackState,
	at: number = Date.now(),
): Promise<void> {
	await db
		.prepare(
			`INSERT INTO release_state (track, live_version, pending_version, pending_state, updated_at)
			 VALUES (?, ?, ?, ?, ?)
			 ON CONFLICT(track) DO UPDATE SET
			   live_version = excluded.live_version,
			   pending_version = excluded.pending_version,
			   pending_state = excluded.pending_state,
			   updated_at = excluded.updated_at`,
		)
		.bind(track, s.liveVersion ?? null, s.pendingVersion ?? null, s.pendingState ?? null, at)
		.run();
}
