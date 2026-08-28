// 变更推送的编排：清单（src/lib/site/urls.ts）↔ 台账（D1）比对出「变了的 URL」，推给 IndexNow，成功才记账。
//
// 触发点两处：
//   1. custom-worker.ts 的 scheduled()：每天 UTC 08:00 与榜单抓取同一趟跑，新指南上线后一天内自动推；
//   2. POST /api/indexnow（管理口令）：发完新内容想立刻推就打这个，别等 cron。

import { indexableUrls } from "../site/urls";
import { type Attempt, submitUrls } from "./client";
import { knownSignatures, recordSubmitted } from "./store";

export type SyncResult = {
	/** 本次推送的 URL */
	submitted: string[];
	/** 内容版本没变、跳过的条数 */
	skipped: number;
	/** 各端点的尝试结果（按顺序），排障时看这个 */
	attempts: Attempt[];
	ok: boolean;
	message?: string;
};

/** 纯函数：清单 ∖ 台账 = 需要推送的条目（新 URL，或内容版本变了的 URL）。 */
export function pendingEntries(
	entries: { url: string; updated: string }[],
	known: Map<string, string>,
	force = false,
): { url: string; updated: string }[] {
	if (force) return [...entries];
	return entries.filter((entry) => known.get(entry.url) !== entry.updated);
}

/** 待推送预览（不发请求），给 GET /api/indexnow 用。 */
export async function previewPending(db: D1Database, force = false) {
	const entries = indexableUrls();
	const known = await knownSignatures(db);
	const pending = pendingEntries(entries, known, force);
	return { total: entries.length, pending, skipped: entries.length - pending.length };
}

/**
 * 比对 → 推送 → 记账。推送失败不记账，下一趟自然重试。
 * 无变更时直接返回，不产生任何外发请求。
 */
export async function syncIndexNow(
	db: D1Database,
	options: { force?: boolean; now?: number } = {},
): Promise<SyncResult> {
	const { force = false, now = Date.now() } = options;
	const entries = indexableUrls();
	const known = await knownSignatures(db);
	const pending = pendingEntries(entries, known, force);
	const skipped = entries.length - pending.length;

	if (pending.length === 0) {
		console.log(`[indexnow] nothing changed (${entries.length} urls)`);
		return { submitted: [], skipped, attempts: [], ok: true };
	}

	const result = await submitUrls(pending.map((entry) => entry.url));
	if (!result.ok) {
		console.error(`[indexnow] submit failed: ${result.message} (${pending.length} urls)`);
		return { submitted: [], skipped, attempts: result.attempts, ok: false, message: result.message };
	}

	await recordSubmitted(db, pending, now);
	console.log(`[indexnow] submitted ${pending.length} urls, skipped ${skipped} (${result.message})`);
	return {
		submitted: pending.map((entry) => entry.url),
		skipped,
		attempts: result.attempts,
		ok: true,
		message: result.message,
	};
}
