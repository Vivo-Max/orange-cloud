import { getCloudflareContext } from "@opennextjs/cloudflare";

/**
 * Resolve the bound D1 database (`IAP_DB` binding -> orange-cloud-iap).
 * Uses the async form of getCloudflareContext so it also works outside of a
 * strict request scope; in dev it reaches the remote D1 via `remote: true`.
 */
export async function getDb(): Promise<D1Database> {
	const { env } = await getCloudflareContext({ async: true });
	return env.IAP_DB;
}

/** Run a query and return its rows (empty array if none). */
export async function queryAll<T = Record<string, unknown>>(
	db: D1Database,
	sql: string,
	params: unknown[] = [],
): Promise<T[]> {
	const stmt = params.length ? db.prepare(sql).bind(...params) : db.prepare(sql);
	const { results } = await stmt.all<T>();
	return results ?? [];
}

/** Run a query expected to return a single row (or null). */
export async function queryFirst<T = Record<string, unknown>>(
	db: D1Database,
	sql: string,
	params: unknown[] = [],
): Promise<T | null> {
	const stmt = params.length ? db.prepare(sql).bind(...params) : db.prepare(sql);
	return (await stmt.first<T>()) ?? null;
}
