import { RANK_COUNTRIES } from "@/lib/ranks/capture";
import { getDb, queryAll } from "./db";

// App Store 榜单排名历史（读 app_store_ranks，供 /admin 折线图与当前名次图例）。

export interface RankPoint {
	date: string;
	position: number | null;
}

export interface RankCountrySeries {
	country: string;
	points: RankPoint[];
	/** 最近一次抓取的名次（该次未上榜则为 null）。 */
	latest: number | null;
	latestDate: string | null;
}

export interface RankHistory {
	/** 稠密日期轴（YYYY-MM-DD，从最早到最新有数据的一天）。 */
	dates: string[];
	series: RankCountrySeries[];
	/** 窗口内观测到的最差名次，用于 y 轴上界。 */
	maxPosition: number;
	hasData: boolean;
}

// ---- 地区展示（admin 为 zh-CN）----

const REGION_NAMES_ZH: Record<string, string> = {
	cn: "中国",
	tw: "台湾",
	us: "美国",
	my: "马来西亚",
	ca: "加拿大",
	ng: "尼日利亚",
	tr: "土耳其",
};

/** 国家码 → 地区名（缺省回退大写码）。 */
export function regionLabel(code: string): string {
	return REGION_NAMES_ZH[code] ?? code.toUpperCase();
}

/** 两位国家码 → 旗帜 emoji（regional indicator）。 */
export function regionFlag(code: string): string {
	const cc = code.toUpperCase();
	if (!/^[A-Z]{2}$/.test(cc)) return "🏳️";
	return String.fromCodePoint(...[...cc].map((c) => 0x1f1e6 + c.charCodeAt(0) - 65));
}

/** 稳定的国家展示顺序：先按抓取集合顺序，未知码追加在后。 */
function orderCountries(present: Set<string>): string[] {
	const known = (RANK_COUNTRIES as readonly string[]).filter((c) => present.has(c));
	const extras = [...present].filter((c) => !(RANK_COUNTRIES as readonly string[]).includes(c)).sort();
	return [...known, ...extras];
}

/** 把 [min,max] 之间的日期补齐成连续轴（最多 400 天兜底）。 */
function denseDates(dates: string[]): string[] {
	if (dates.length === 0) return [];
	const sorted = [...dates].sort();
	const start = new Date(`${sorted[0]}T00:00:00Z`).getTime();
	const end = new Date(`${sorted[sorted.length - 1]}T00:00:00Z`).getTime();
	const out: string[] = [];
	for (let t = start; t <= end; t += 86_400_000) out.push(new Date(t).toISOString().slice(0, 10));
	return out.length > 0 && out.length <= 400 ? out : sorted;
}

async function getRankHistory(db: D1Database, days: number): Promise<RankHistory> {
	const cutoff = new Date(Date.now() - days * 86_400_000).toISOString().slice(0, 10);
	const rows = await queryAll<{ captured_date: string; country: string; position: number | null }>(
		db,
		`SELECT captured_date, country, position FROM app_store_ranks
		 WHERE captured_date >= ?
		 ORDER BY captured_date ASC`,
		[cutoff],
	);

	if (rows.length === 0) {
		return { dates: [], series: [], maxPosition: 0, hasData: false };
	}

	const present = new Set<string>();
	const datesSeen = new Set<string>();
	// country -> date -> position
	const byCountry = new Map<string, Map<string, number | null>>();
	let maxPosition = 0;

	for (const r of rows) {
		present.add(r.country);
		datesSeen.add(r.captured_date);
		if (!byCountry.has(r.country)) byCountry.set(r.country, new Map());
		byCountry.get(r.country)!.set(r.captured_date, r.position);
		if (typeof r.position === "number" && r.position > maxPosition) maxPosition = r.position;
	}

	const dates = denseDates([...datesSeen]);
	const series: RankCountrySeries[] = orderCountries(present).map((country) => {
		const perDate = byCountry.get(country)!;
		const points: RankPoint[] = dates.map((date) => ({
			date,
			position: perDate.has(date) ? (perDate.get(date) ?? null) : null,
		}));
		// 最近一次抓取（该国有行的最大日期）的名次。
		const capturedDates = [...perDate.keys()].sort();
		const latestDate = capturedDates[capturedDates.length - 1] ?? null;
		const latest = latestDate ? (perDate.get(latestDate) ?? null) : null;
		return { country, points, latest, latestDate };
	});

	return { dates, series, maxPosition, hasData: maxPosition > 0 };
}

export async function fetchRankHistory(days = 30): Promise<RankHistory> {
	return getRankHistory(await getDb(), days);
}
