// 纯函数：App 版本状态 → 展示桶，以及把一次状态事件归并进 TrackState。
// 无 I/O，便于单测。被 /api/asc/webhook 与 /api/play/release 复用。

import type { PendingState, TrackState } from "@orange-cloud/changelog";

export type Bucket = "live" | PendingState | "cleared";

// 与 @orange-cloud/changelog 的 isNewer 同口径（内联以让本模块零运行时依赖、便于单测）。
const isNewer = (a: string, b: string) => a.localeCompare(b, undefined, { numeric: true }) > 0;

// App Store Connect 的 appVersionState 枚举 → 展示桶。
const LIVE = new Set(["READY_FOR_DISTRIBUTION", "READY_FOR_SALE"]);
const IN_REVIEW = new Set([
	"WAITING_FOR_REVIEW",
	"IN_REVIEW",
	"PENDING_APPLE_RELEASE",
	"PROCESSING_FOR_DISTRIBUTION",
	"WAITING_FOR_EXPORT_COMPLIANCE",
]);
const PENDING_RELEASE = new Set(["PENDING_DEVELOPER_RELEASE", "ACCEPTED"]);
// 这些状态把在审条目「撤回」（官网不再标审核中，回到仅展示已上架）。
const CLEAR = new Set([
	"PREPARE_FOR_SUBMISSION",
	"REJECTED",
	"DEVELOPER_REJECTED",
	"METADATA_REJECTED",
	"INVALID_BINARY",
	"DEVELOPER_REMOVED_FROM_SALE",
	"REMOVED_FROM_SALE",
	"REPLACED_WITH_NEW_VERSION",
]);

/** App 版本状态字符串 → 展示桶；未知返回 null（忽略该事件）。 */
export function bucketOfAppVersionState(state: string): Bucket | null {
	if (LIVE.has(state)) return "live";
	if (IN_REVIEW.has(state)) return "in_review";
	if (PENDING_RELEASE.has(state)) return "pending_release";
	if (CLEAR.has(state)) return "cleared";
	return null;
}

/** 把一次「version 进入 bucket」的事件归并进当前 TrackState。 */
export function reduceTrackState(current: TrackState, version: string, bucket: Bucket): TrackState {
	switch (bucket) {
		case "live": {
			// liveVersion 单调前进；本次上架若覆盖了在审版本，则清掉 pending。
			const liveVersion = current.liveVersion && !isNewer(version, current.liveVersion) ? current.liveVersion : version;
			const keepPending = current.pendingVersion && isNewer(current.pendingVersion, liveVersion) ? current.pendingVersion : undefined;
			return { liveVersion, pendingVersion: keepPending, pendingState: keepPending ? current.pendingState : undefined };
		}
		case "in_review":
		case "pending_release": {
			// 仅当该版本比已上架版本新才标 pending（否则它已上架，无需标注）。
			if (current.liveVersion && !isNewer(version, current.liveVersion)) return current;
			return { ...current, pendingVersion: version, pendingState: bucket };
		}
		case "cleared": {
			// 仅撤回「正是这个在审版本」；已上架版本不受影响。
			if (current.pendingVersion === version) return { ...current, pendingVersion: undefined, pendingState: undefined };
			return current;
		}
	}
}
