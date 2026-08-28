// 读 app_store_ranks 库表的查询（/api/ranks/badge 无参「最好名次」用；
// /me 与 badge 的指定地区分支已改走实时 fetchCountryRank，不经此处）。
// 「fresh」= 近 freshDays 天内抓到的记录；「已上榜」= position 非空。

export interface RankRow {
	country: string;
	position: number;
	genreName: string | null;
	capturedDate: string;
}

interface RankRowDB {
	country: string;
	position: number;
	genre_name: string | null;
	captured_date: string;
}

const SELECT = "SELECT country, position, genre_name, captured_date FROM app_store_ranks";

function cutoffDate(freshDays: number): string {
	return new Date(Date.now() - freshDays * 86_400_000).toISOString().slice(0, 10);
}

function toRow(r: RankRowDB | null): RankRow | null {
	return r
		? { country: r.country, position: r.position, genreName: r.genre_name, capturedDate: r.captured_date }
		: null;
}

/** 全地区近 freshDays 天内「最好的」（名次最小）已上榜记录（无则 null）。 */
export async function bestFreshRank(db: D1Database, freshDays: number): Promise<RankRow | null> {
	const r = await db
		.prepare(
			`${SELECT}
			 WHERE position IS NOT NULL AND captured_date >= ?
			 ORDER BY position ASC, captured_date DESC LIMIT 1`,
		)
		.bind(cutoffDate(freshDays))
		.first<RankRowDB>();
	return toRow(r);
}
