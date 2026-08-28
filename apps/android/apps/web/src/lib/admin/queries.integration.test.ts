// loadAdminStats 的真实 SQL 集成测试（node:sqlite）：混入 Production / Sandbox 行，
// 断言所有聚合都把 Sandbox 排除，并验证跨币种 USD 归一。

import { readdirSync, readFileSync } from "node:fs";
import { DatabaseSync } from "node:sqlite";
import { beforeAll, describe, expect, it } from "vitest";
import { loadAdminStats } from "./queries";

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

// 全量 migration（含 0007 的 platform 列），保持与生产表结构一致。
const migDir = new URL("../../../migrations/", import.meta.url);
const schema = readdirSync(migDir)
	.filter((f) => f.endsWith(".sql"))
	.sort()
	.map((f) => readFileSync(new URL(f, migDir), "utf8"))
	.join("\n");
let db: FakeD1;
const PD = Date.now() - 2 * 86_400_000; // 两天前，落在近 30 天 / 本月窗口内

beforeAll(() => {
	const raw = new DatabaseSync(":memory:");
	raw.exec(schema);
	raw.exec(`
		INSERT INTO subscriptions (original_transaction_id, status, environment, is_lifetime, price_millis, currency, last_signed_date, updated_at, purchase_date) VALUES
		 ('P1','active','Production',0,19990,'USD',1,${PD},${PD}),
		 ('P2','active','Production',0,2990,'CNY',1,${PD},${PD}),
		 ('P3','active','Production',1,49990,'USD',1,${PD},${PD}),
		 ('S1','active','Sandbox',0,19990,'USD',1,${PD},${PD});
		INSERT INTO transactions (transaction_id, original_transaction_id, type, environment, price_millis, currency, purchase_date, created_at, updated_at) VALUES
		 ('t1','P1','Auto-Renewable Subscription','Production',19990,'USD',${PD},${PD},${PD}),
		 ('t2','P2','Auto-Renewable Subscription','Production',2990,'USD',${PD},${PD},${PD}),
		 ('t3','P3','Non-Consumable','Production',49990,'USD',${PD},${PD},${PD}),
		 ('tS','S1','Auto-Renewable Subscription','Sandbox',19990,'USD',${PD},${PD},${PD});
		INSERT INTO notifications (notification_uuid, notification_type, environment, received_at, raw_payload) VALUES
		 ('n1','SUBSCRIBED','Production',${PD},'{}'),
		 ('n2','DID_RENEW','Production',${PD},'{}'),
		 ('n3','SUBSCRIBED','Sandbox',${PD},'{}');
	`);
	db = new FakeD1(raw);
});

describe("loadAdminStats 排除 Sandbox", () => {
	it("KPI 计数只算 Production", async () => {
		const s = await loadAdminStats(db as never);
		expect(s.kpis.totalSubs).toBe(3); // S1 不计
		expect(s.kpis.totalNotifications).toBe(2); // n3 不计
		expect(s.kpis.activeSubs).toBe(3);
		expect(s.hasData).toBe(true);
	});

	it("累计净收入按 USD 归一、排除 Sandbox", async () => {
		const s = await loadAdminStats(db as never);
		// t1+t2+t3 = (19990+2990+49990)/1000 = 72.97 USD；tS(Sandbox) 不计
		expect(s.kpis.cumulativeNetUsd).toBeCloseTo(72.97, 2);
	});

	it("财务流水 / 状态分布里没有 Sandbox", async () => {
		const s = await loadAdminStats(db as never);
		expect(s.transactions).toHaveLength(3);
		expect(s.transactions.map((t) => t.transaction_id)).not.toContain("tS");
		const active = s.statusBreakdown.find((x) => x.key === "active");
		expect(active?.value).toBe(3);
	});
});
