// getCodesStats 的真实 SQL 集成测试（node:sqlite）：用迁移 0005/0006 的真实建表，
// 断言 sold/active/revoked/pendingRefunds 计数与「仅 active、按币种」的收入聚合口径。

import { readFileSync } from "node:fs";
import { DatabaseSync } from "node:sqlite";
import { beforeAll, describe, expect, it } from "vitest";
import { getCodesStats } from "./admin";

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
	async all() {
		return { results: this.db.prepare(this.sql).all(...(this.args as never[])) };
	}
	async first() {
		return this.db.prepare(this.sql).get(...(this.args as never[])) ?? null;
	}
}
class FakeD1 {
	constructor(private readonly db: DatabaseSync) {}
	prepare(sql: string): FakeStmt {
		return new FakeStmt(this.db, sql);
	}
}

const s0005 = readFileSync(new URL("../../../migrations/0005_codes.sql", import.meta.url), "utf8");
const s0006 = readFileSync(new URL("../../../migrations/0006_codes_self_reset.sql", import.meta.url), "utf8");
let db: FakeD1;
const T = Date.now() - 86_400_000;

beforeAll(() => {
	const raw = new DatabaseSync(":memory:");
	raw.exec(s0005);
	raw.exec(s0006);
	raw.exec(`
		INSERT INTO codes (code, status, source, amount_total, currency, refund_status, created_at) VALUES
		 ('A1','active','stripe',3499,'HKD','none',${T}),
		 ('A2','active','stripe',3499,'HKD','none',${T}),
		 ('A3','active','stripe',2990,'CNY','none',${T}),
		 ('R1','revoked','stripe',3499,'HKD','approved',${T}),
		 ('P1','active','stripe',3499,'HKD','requested',${T}),
		 ('M1','active','manual',NULL,NULL,'none',${T});
		INSERT INTO code_activations (code, install_id, activated_at, last_seen_at) VALUES
		 ('A1','i1',${T},${T}),
		 ('A1','i2',${T},${T});
	`);
	db = new FakeD1(raw);
});

describe("getCodesStats", () => {
	it("计数只算 stripe 渠道，manual 不计入 sold", async () => {
		const s = await getCodesStats(db as never);
		expect(s.sold).toBe(5); // A1 A2 A3 R1 P1（M1=manual 不算）
		expect(s.active).toBe(4); // A1 A2 A3 P1
		expect(s.revoked).toBe(1); // R1
		expect(s.pendingRefunds).toBe(1); // P1
	});

	it("收入按币种分组，仅 active、有金额与币种者计入", async () => {
		const s = await getCodesStats(db as never);
		const hkd = s.revenue.find((r) => r.currency === "HKD");
		const cny = s.revenue.find((r) => r.currency === "CNY");
		// R1 已撤销不计；M1 无金额不计；P1(requested 但仍 active) 计入
		expect(hkd).toEqual({ currency: "HKD", minor: 3499 * 3, count: 3 }); // A1 A2 P1
		expect(cny).toEqual({ currency: "CNY", minor: 2990, count: 1 }); // A3
		// 按 minor 降序
		expect(s.revenue[0].currency).toBe("HKD");
	});

	it("空库返回全 0（迁移未应用时的容错同形状）", async () => {
		const empty = new DatabaseSync(":memory:");
		empty.exec(s0005);
		empty.exec(s0006);
		const s = await getCodesStats(new FakeD1(empty) as never);
		expect(s).toEqual({ sold: 0, active: 0, revoked: 0, pendingRefunds: 0, revenue: [] });
	});
});
