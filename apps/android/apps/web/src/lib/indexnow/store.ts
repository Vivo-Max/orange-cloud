// IndexNow 的推送台账：记住「哪个 URL 以哪个内容版本推过」，下次只推变了的。
// 表见 migrations/0008_indexnow.sql，与账本共用 IAP_DB。

export type SubmittedRow = { url: string; signature: string; submitted_at: number };

/** 读回已推送过的 URL → 内容版本。 */
export async function knownSignatures(db: D1Database): Promise<Map<string, string>> {
	const { results } = await db
		.prepare("SELECT url, signature FROM indexnow_urls")
		.all<{ url: string; signature: string }>();
	return new Map((results ?? []).map((row) => [row.url, row.signature]));
}

/** 记账：推送成功后把这批 URL 的内容版本写回（同 URL 覆盖）。 */
export async function recordSubmitted(
	db: D1Database,
	entries: { url: string; updated: string }[],
	submittedAt: number,
): Promise<void> {
	if (entries.length === 0) return;
	await db.batch(
		entries.map((entry) =>
			db
				.prepare(
					`INSERT INTO indexnow_urls (url, signature, submitted_at)
					 VALUES (?, ?, ?)
					 ON CONFLICT(url) DO UPDATE SET
					   signature = excluded.signature,
					   submitted_at = excluded.submitted_at`,
				)
				.bind(entry.url, entry.updated, submittedAt),
		),
	);
}
