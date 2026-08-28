// App Store 榜单排名抓取：每天 UTC 08:00 由 custom-worker.ts 的 scheduled() 调用。
//
// 数据源是 App Store 网页端目录接口（无需鉴权）：
//   https://apps.apple.com/api/apps/v1/catalog/{country}/apps/{id}?platform=iphone
// 返回 data[0].attributes.chartPositions.appStore 即榜单名次。
//   - 不可用地区（接口 404，如 cn 当前未上架）跳过、不入库。
//   - 已上架但当时未上榜（无 chartPositions）记 position=NULL。

export const RANK_APP_ID = "6779323783";

// 抓取的地区集合（App Store 商店区码，小写）。顺序即遍历顺序。
export const RANK_COUNTRIES = ["cn", "tw", "us", "my", "ca", "ng", "tr"] as const;
export type RankCountry = (typeof RANK_COUNTRIES)[number];

const REQUEST_UA =
	"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15";

export interface RankParsed {
	/** 榜单名次；上架但未上榜为 null。 */
	position: number | null;
	chart: string | null;
	genre: number | null;
	genreName: string | null;
}

export interface CaptureResult {
	capturedDate: string;
	captured: string[];
	skipped: string[];
}

function catalogUrl(country: string): string {
	return `https://apps.apple.com/api/apps/v1/catalog/${country}/apps/${RANK_APP_ID}?platform=iphone`;
}

/**
 * 从目录接口 JSON 解出榜单名次。
 *   - 无 data[0]（空数组 / 结构异常）→ null（视作不可用、跳过）。
 *   - 有 data[0] 但无 chartPositions → position=null（上架未上榜）。
 */
export function parseRankPayload(json: unknown): RankParsed | null {
	if (!json || typeof json !== "object") return null;
	const data = (json as { data?: unknown }).data;
	if (!Array.isArray(data) || data.length === 0) return null;

	const attrs = (data[0] as { attributes?: unknown })?.attributes;
	const appStore = (attrs as { chartPositions?: { appStore?: unknown } })?.chartPositions?.appStore as
		| { position?: unknown; chart?: unknown; genre?: unknown; genreName?: unknown }
		| undefined;

	return {
		position: typeof appStore?.position === "number" ? appStore.position : null,
		chart: typeof appStore?.chart === "string" ? appStore.chart : null,
		genre: typeof appStore?.genre === "number" ? appStore.genre : null,
		genreName: typeof appStore?.genreName === "string" ? appStore.genreName : null,
	};
}

/** 抓单个地区。接口非 200（含 404 未上架）或解析失败 → null（跳过）。 */
export async function fetchCountryRank(country: string): Promise<RankParsed | null> {
	const res = await fetch(catalogUrl(country), {
		headers: { "User-Agent": REQUEST_UA, Accept: "application/json" },
		signal: AbortSignal.timeout(10_000),
	});
	if (!res.ok) return null;
	const json = await res.json().catch(() => null);
	return parseRankPayload(json);
}

/**
 * 抓全部地区并把当日快照写入 app_store_ranks。
 * 顺序遍历（一天一次、对 Apple 友好）；单地区失败不影响其余；
 * 不可用地区跳过；按 (captured_date, country) upsert（同日重跑覆盖）。
 */
export async function captureRanks(db: D1Database, capturedAt: number = Date.now()): Promise<CaptureResult> {
	const capturedDate = new Date(capturedAt).toISOString().slice(0, 10);
	const captured: string[] = [];
	const skipped: string[] = [];
	const statements: D1PreparedStatement[] = [];

	for (const country of RANK_COUNTRIES) {
		let parsed: RankParsed | null = null;
		try {
			parsed = await fetchCountryRank(country);
		} catch (err) {
			console.error(`[ranks] fetch failed for ${country}:`, err);
		}
		if (!parsed) {
			skipped.push(country);
			continue;
		}
		captured.push(country);
		statements.push(
			db
				.prepare(
					`INSERT INTO app_store_ranks
					 (captured_date, country, position, chart, genre, genre_name, captured_at)
					 VALUES (?, ?, ?, ?, ?, ?, ?)
					 ON CONFLICT(captured_date, country) DO UPDATE SET
					   position = excluded.position,
					   chart = excluded.chart,
					   genre = excluded.genre,
					   genre_name = excluded.genre_name,
					   captured_at = excluded.captured_at`,
				)
				.bind(
					capturedDate,
					country,
					parsed.position,
					parsed.chart,
					parsed.genre,
					parsed.genreName,
					capturedAt,
				),
		);
	}

	if (statements.length > 0) await db.batch(statements);
	console.log(`[ranks] ${capturedDate} captured=[${captured.join(",")}] skipped=[${skipped.join(",")}]`);
	return { capturedDate, captured, skipped };
}
