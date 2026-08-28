// IndexNow：清单构造 / 变更判定 / 提交载荷 / 台账记账（台账部分跑真实 SQL，node:sqlite）。
import { readFileSync } from "node:fs";
import { DatabaseSync } from "node:sqlite";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { indexableUrls, siteUrls } from "../site/urls";
import { INDEXNOW_KEY, submitUrls } from "./client";
import { knownSignatures, recordSubmitted } from "./store";
import { pendingEntries, syncIndexNow } from "./sync";

// ---- 最小 D1 适配器（仅覆盖 store 用到的 prepare/bind/all/batch）----
class FakeStmt {
	private args: unknown[] = [];
	constructor(
		private readonly db: DatabaseSync,
		private readonly sql: string,
	) {}
	bind(...args: unknown[]): this {
		this.args = args;
		return this;
	}
	run() {
		this.db.prepare(this.sql).run(...(this.args as never[]));
		return { success: true };
	}
	async all<T>() {
		return { results: this.db.prepare(this.sql).all(...(this.args as never[])) as T[] };
	}
}
class FakeD1 {
	constructor(private readonly db: DatabaseSync) {}
	prepare(sql: string): FakeStmt {
		return new FakeStmt(this.db, sql);
	}
	async batch(statements: FakeStmt[]) {
		return statements.map((s) => s.run());
	}
}

let raw: DatabaseSync;
let db: FakeD1;
const schema = readFileSync(new URL("../../../migrations/0008_indexnow.sql", import.meta.url), "utf8");

beforeEach(() => {
	raw = new DatabaseSync(":memory:");
	raw.exec(schema);
	db = new FakeD1(raw);
});

afterEach(() => {
	vi.unstubAllGlobals();
});

/** 桩一个 fetch：按端点给状态码（默认全部 200），记下每次请求 */
function stubFetch(status: number | Record<string, number>) {
	const calls: { url: string; body: Record<string, unknown> }[] = [];
	vi.stubGlobal(
		"fetch",
		vi.fn(async (url: string, init: RequestInit) => {
			const href = String(url);
			calls.push({ url: href, body: JSON.parse(String(init.body)) });
			const code =
				typeof status === "number"
					? status
					: (Object.entries(status).find(([host]) => href.includes(host))?.[1] ?? 200);
			return new Response("", { status: code });
		}),
	);
	return calls;
}

describe("URL 清单", () => {
	it("全是 oss.omail.us.kg 的绝对 URL，且不重复", () => {
		const urls = indexableUrls().map((e) => e.url);
		expect(urls.length).toBeGreaterThan(0);
		for (const url of urls) expect(url.startsWith("https://oss.omail.us.kg")).toBe(true);
		expect(new Set(urls).size).toBe(urls.length);
	});

	it("静态页摊平成各语言版本，默认语言无前缀", () => {
		const urls = indexableUrls().map((e) => e.url);
		expect(urls).toContain("https://oss.omail.us.kg/privacy");
		expect(urls).toContain("https://oss.omail.us.kg/ja/privacy");
		expect(urls).not.toContain("https://oss.omail.us.kg/en/privacy");
	});

	it("指南两套语言各自成条，中文带 /zh-Hans 前缀", () => {
		const urls = indexableUrls().map((e) => e.url);
		expect(urls).toContain("https://oss.omail.us.kg/guides");
		expect(urls).toContain("https://oss.omail.us.kg/zh-Hans/guides");
		expect(urls.some((u) => u.startsWith("https://oss.omail.us.kg/guides/"))).toBe(true);
		expect(urls.some((u) => u.startsWith("https://oss.omail.us.kg/zh-Hans/guides/"))).toBe(true);
	});

	it("指南索引页的 lastmod 取最新一篇，不是清单里的第一篇", () => {
		const index = siteUrls().find((e) => e.url === "https://oss.omail.us.kg/guides")!;
		const articles = siteUrls()
			.filter((e) => e.url.startsWith("https://oss.omail.us.kg/guides/"))
			.map((e) => e.updated);
		expect(index.updated).toBe(articles.reduce((a, b) => (a > b ? a : b)));
	});
});

describe("变更判定", () => {
	const entries = [
		{ url: "https://oss.omail.us.kg/a", updated: "2026-08-01" },
		{ url: "https://oss.omail.us.kg/b", updated: "2026-08-02" },
	];

	it("台账里没有 → 需要推", () => {
		expect(pendingEntries(entries, new Map())).toHaveLength(2);
	});

	it("版本一致 → 跳过；版本变了 → 重推", () => {
		const known = new Map([
			["https://oss.omail.us.kg/a", "2026-08-01"],
			["https://oss.omail.us.kg/b", "2026-07-01"],
		]);
		expect(pendingEntries(entries, known).map((e) => e.url)).toEqual(["https://oss.omail.us.kg/b"]);
	});

	it("force 无视台账全量重推", () => {
		const known = new Map(entries.map((e) => [e.url, e.updated]));
		expect(pendingEntries(entries, known, true)).toHaveLength(2);
	});
});

describe("提交载荷", () => {
	it("默认打 bing 端点，带 host/key/keyLocation/urlList", async () => {
		const calls = stubFetch(200);
		const result = await submitUrls(["https://oss.omail.us.kg/guides"]);
		expect(result.ok).toBe(true);
		expect(calls).toHaveLength(1);
		expect(calls[0].url).toBe("https://www.bing.com/indexnow");
		expect(calls[0].body).toEqual({
			host: "oss.omail.us.kg",
			key: INDEXNOW_KEY,
			keyLocation: `https://oss.omail.us.kg/${INDEXNOW_KEY}.txt`,
			urlList: ["https://oss.omail.us.kg/guides"],
		});
	});

	it("空清单不发请求", async () => {
		const calls = stubFetch(200);
		expect((await submitUrls([])).ok).toBe(true);
		expect(calls).toHaveLength(0);
	});

	it("2xx 都算收下：202（密钥待校验）与 bing 的 203", async () => {
		stubFetch(202);
		expect((await submitUrls(["https://oss.omail.us.kg/"])).ok).toBe(true);
		stubFetch(203);
		expect((await submitUrls(["https://oss.omail.us.kg/"])).ok).toBe(true);
	});

	it("429 换下一个端点，直到有人收下", async () => {
		const calls = stubFetch({ "bing.com": 429, "api.indexnow.org": 429, "yandex.com": 202 });
		const result = await submitUrls(["https://oss.omail.us.kg/"]);
		expect(result.ok).toBe(true);
		expect(calls.map((c) => new URL(c.url).host)).toEqual([
			"www.bing.com",
			"api.indexnow.org",
			"yandex.com",
		]);
	});

	it("全部端点 429 才算失败", async () => {
		stubFetch(429);
		const result = await submitUrls(["https://oss.omail.us.kg/"]);
		expect(result.ok).toBe(false);
		expect(result.attempts).toHaveLength(3);
		expect(result.message).toContain("限流");
	});

	it("403（密钥不对）立刻失败，不拿同样的错去烦别的端点", async () => {
		const calls = stubFetch(403);
		const result = await submitUrls(["https://oss.omail.us.kg/"]);
		expect(result.ok).toBe(false);
		expect(calls).toHaveLength(1);
		expect(result.message).toContain("密钥");
	});

	it("网络异常也换下一个端点", async () => {
		let first = true;
		vi.stubGlobal(
			"fetch",
			vi.fn(async () => {
				if (first) {
					first = false;
					throw new Error("timeout");
				}
				return new Response("", { status: 200 });
			}),
		);
		const result = await submitUrls(["https://oss.omail.us.kg/"]);
		expect(result.ok).toBe(true);
		expect(result.attempts[0].status).toBe(null);
	});
});

describe("同步与台账（真实 SQL）", () => {
	it("首轮全推并记账，第二轮无变更不再发请求", async () => {
		const first = stubFetch(200);
		const total = indexableUrls().length;
		const run1 = await syncIndexNow(db as never, { now: 1 });
		expect(run1.ok).toBe(true);
		expect(run1.submitted).toHaveLength(total);
		expect(first).toHaveLength(1);
		expect((await knownSignatures(db as never)).size).toBe(total);

		const second = stubFetch(200);
		const run2 = await syncIndexNow(db as never, { now: 2 });
		expect(run2.submitted).toHaveLength(0);
		expect(run2.skipped).toBe(total);
		expect(second).toHaveLength(0);
	});

	it("全端点失败不记账，下一轮重试", async () => {
		stubFetch(429);
		const failed = await syncIndexNow(db as never, { now: 1 });
		expect(failed.ok).toBe(false);
		expect((await knownSignatures(db as never)).size).toBe(0);

		const retry = stubFetch(200);
		expect((await syncIndexNow(db as never, { now: 2 })).ok).toBe(true);
		expect(retry).toHaveLength(1);
	});

	it("同一 URL 版本变化后覆盖记账", async () => {
		await recordSubmitted(db as never, [{ url: "https://oss.omail.us.kg/x", updated: "2026-08-01" }], 1);
		await recordSubmitted(db as never, [{ url: "https://oss.omail.us.kg/x", updated: "2026-08-09" }], 2);
		const rows = raw.prepare("SELECT url, signature, submitted_at FROM indexnow_urls").all();
		expect(rows).toEqual([{ url: "https://oss.omail.us.kg/x", signature: "2026-08-09", submitted_at: 2 }]);
	});
});
