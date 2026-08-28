// 针对真实 SQL 引擎（node:sqlite）的 release_state 读写测试。
import { readFileSync } from "node:fs";
import { DatabaseSync } from "node:sqlite";
import { beforeEach, describe, expect, it } from "vitest";
import { getReleaseState, putTrackState } from "./store";

// ---- 最小 D1 适配器（仅覆盖 store 用到的 prepare/bind/run/all）----
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
		const info = this.db.prepare(this.sql).run(...(this.args as never[]));
		return { success: true, meta: { changes: Number(info.changes) } };
	}
	async all<T>() {
		const rows = this.db.prepare(this.sql).all(...(this.args as never[]));
		return { results: rows as T[] };
	}
}
class FakeD1 {
	constructor(private readonly db: DatabaseSync) {}
	prepare(sql: string): FakeStmt {
		return new FakeStmt(this.db, sql);
	}
}

let raw: DatabaseSync;
let db: FakeD1;
const schema = readFileSync(new URL("../../../migrations/0002_live_versions.sql", import.meta.url), "utf8");

beforeEach(() => {
	raw = new DatabaseSync(":memory:");
	raw.exec(schema);
	db = new FakeD1(raw);
});

describe("release_state store（真实 SQL）", () => {
	it("初始为空", async () => {
		expect(await getReleaseState(db as never)).toEqual({});
	});

	it("写入并读回 live + pending（双轨）", async () => {
		await putTrackState(db as never, "ios", { liveVersion: "1.3.0", pendingVersion: "1.4.0", pendingState: "in_review" });
		await putTrackState(db as never, "android", { liveVersion: "1.1" });
		expect(await getReleaseState(db as never)).toEqual({
			ios: { liveVersion: "1.3.0", pendingVersion: "1.4.0", pendingState: "in_review" },
			android: { liveVersion: "1.1" },
		});
	});

	it("整行覆写：上架后清空 pending", async () => {
		await putTrackState(db as never, "ios", { liveVersion: "1.3.0", pendingVersion: "1.4.0", pendingState: "in_review" });
		await putTrackState(db as never, "ios", { liveVersion: "1.4.0" });
		expect((await getReleaseState(db as never)).ios).toEqual({ liveVersion: "1.4.0" });
	});
});
