// Play 入库层针对「真实 SQL 引擎」的集成测试（node:sqlite）：
// messageId 幂等、orderId upsert、eventTime 乱序保护、换 token 时旧行落幕。

import { readdirSync, readFileSync } from "node:fs";
import { DatabaseSync } from "node:sqlite";
import { beforeEach, describe, expect, it } from "vitest";
import { storeLedgerRows } from "./store";
import type { PlayLedgerRows } from "./logic";

// ---- 最小 D1 适配器（仅覆盖 store.ts 用到的接口）----
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
}
class FakeD1 {
	constructor(private readonly db: DatabaseSync) {}
	prepare(sql: string): FakeStmt {
		return new FakeStmt(this.db, sql);
	}
	async batch(stmts: FakeStmt[]) {
		this.db.exec("BEGIN");
		try {
			const results = stmts.map((s) => s.run());
			this.db.exec("COMMIT");
			return results;
		} catch (e) {
			this.db.exec("ROLLBACK");
			throw e;
		}
	}
}

const migDir = new URL("../../../migrations/", import.meta.url);
const migrations = readdirSync(migDir)
	.filter((f) => f.endsWith(".sql"))
	.sort()
	.map((f) => readFileSync(new URL(f, migDir), "utf8"));

let raw: DatabaseSync;
let db: FakeD1;

beforeEach(() => {
	raw = new DatabaseSync(":memory:");
	for (const m of migrations) raw.exec(m);
	db = new FakeD1(raw);
});

interface RowInput {
	messageId: string;
	type: string;
	eventTime: number;
	token?: string;
	orderId?: string | null;
	status?: string;
	priceMillis?: number | null;
	revocationDate?: number | null;
	linkedToken?: string | null;
	isLifetime?: boolean;
	expiresDate?: number | null;
}

function makeRows(i: RowInput): PlayLedgerRows {
	const token = i.token ?? "tok-A";
	const orderId = i.orderId === undefined ? "GPA.1" : i.orderId;
	return {
		notification: {
			notificationUuid: i.messageId,
			notificationType: i.type,
			subtype: null,
			purchaseToken: token,
			orderId,
			packageName: "jiamin.chen.orangecloud",
			environment: "Production",
			eventTime: i.eventTime,
			receivedAt: i.eventTime + 10,
			raw: "{}",
		},
		transaction: orderId
			? {
					orderId,
					purchaseToken: token,
					productId: "jiamin.chen.orange_cloud.pro.yearly",
					type: "Auto-Renewable Subscription",
					purchaseDate: i.eventTime,
					expiresDate: i.expiresDate ?? null,
					priceMillis: i.priceMillis === undefined ? 19_990 : i.priceMillis,
					devRevenueMillis: 16_990,
					currency: "USD",
					storefront: "US",
					offerIdentifier: null,
					revocationDate: i.revocationDate ?? null,
					environment: "Production",
				}
			: null,
		subscription: {
			purchaseToken: token,
			productId: "jiamin.chen.orange_cloud.pro.yearly",
			status: i.status ?? "active",
			autoRenewStatus: 1,
			linkedToken: i.linkedToken ?? null,
			environment: "Production",
			purchaseDate: i.eventTime,
			expiresDate: i.expiresDate ?? null,
			isLifetime: i.isLifetime ?? false,
			priceMillis: i.priceMillis === undefined ? 19_990 : i.priceMillis,
			currency: "USD",
		},
	};
}

function sub(token = "tok-A") {
	return raw.prepare("SELECT * FROM subscriptions WHERE original_transaction_id = ?").get(token) as
		| Record<string, unknown>
		| undefined;
}
function count(table: string): number {
	return Number((raw.prepare(`SELECT count(*) c FROM ${table}`).get() as { c: number }).c);
}

describe("storeLedgerRows（真实 SQL）", () => {
	it("首购 + 续订：状态有效、到期推进、流水累加，platform 标记为 play", async () => {
		await storeLedgerRows(
			db as never,
			makeRows({ messageId: "m1", type: "SUBSCRIPTION_PURCHASED", eventTime: 1000, orderId: "GPA.1", expiresDate: 5000 }),
		);
		await storeLedgerRows(
			db as never,
			makeRows({ messageId: "m2", type: "SUBSCRIPTION_RENEWED", eventTime: 2000, orderId: "GPA.2", expiresDate: 9000 }),
		);
		expect(sub()?.status).toBe("active");
		expect(sub()?.expires_date).toBe(9000);
		expect(sub()?.platform).toBe("play");
		expect(count("transactions")).toBe(2);
		const tx = raw.prepare("SELECT * FROM transactions WHERE transaction_id = 'GPA.2'").get() as Record<string, unknown>;
		expect(tx.platform).toBe("play");
		expect(tx.dev_revenue_millis).toBe(16_990);
	});

	it("重复 messageId 幂等：标记 duplicate 且不重复入审计", async () => {
		const rows = makeRows({ messageId: "dup", type: "SUBSCRIPTION_PURCHASED", eventTime: 1000 });
		const first = await storeLedgerRows(db as never, rows);
		const second = await storeLedgerRows(db as never, rows);
		expect(first.duplicate).toBe(false);
		expect(second.duplicate).toBe(true);
		expect(count("notifications")).toBe(1);
		expect(count("transactions")).toBe(1);
	});

	it("重投递能补齐金额：先无价入库，重放带价时回填", async () => {
		await storeLedgerRows(
			db as never,
			makeRows({ messageId: "m1", type: "SUBSCRIPTION_PURCHASED", eventTime: 1000, priceMillis: null }),
		);
		expect(
			(raw.prepare("SELECT price_millis p FROM transactions WHERE transaction_id='GPA.1'").get() as { p: number | null }).p,
		).toBeNull();

		await storeLedgerRows(
			db as never,
			makeRows({ messageId: "m1", type: "SUBSCRIPTION_PURCHASED", eventTime: 1000, priceMillis: 19_990 }),
		);
		expect(
			(raw.prepare("SELECT price_millis p FROM transactions WHERE transaction_id='GPA.1'").get() as { p: number }).p,
		).toBe(19_990);
	});

	it("乱序保护：迟到的更旧 EXPIRED 不把状态写回过期", async () => {
		await storeLedgerRows(db as never, makeRows({ messageId: "m1", type: "SUBSCRIPTION_PURCHASED", eventTime: 1000, expiresDate: 5000 }));
		await storeLedgerRows(db as never, makeRows({ messageId: "m2", type: "SUBSCRIPTION_RENEWED", eventTime: 2000, orderId: "GPA.2", expiresDate: 9000 }));
		await storeLedgerRows(db as never, makeRows({ messageId: "m3", type: "SUBSCRIPTION_EXPIRED", eventTime: 1500, orderId: null, status: "expired" }));
		expect(sub()?.status).toBe("active");
		expect(sub()?.last_signed_date).toBe(2000);
		expect(count("notifications")).toBe(3); // 审计仍记录

		await storeLedgerRows(db as never, makeRows({ messageId: "m4", type: "SUBSCRIPTION_EXPIRED", eventTime: 3000, orderId: null, status: "expired" }));
		expect(sub()?.status).toBe("expired");
	});

	it("退款：状态 refunded、流水回写撤销时间", async () => {
		await storeLedgerRows(db as never, makeRows({ messageId: "m1", type: "SUBSCRIPTION_PURCHASED", eventTime: 1000 }));
		await storeLedgerRows(
			db as never,
			makeRows({ messageId: "m2", type: "VOIDED_PURCHASE", eventTime: 4000, status: "refunded", revocationDate: 4000 }),
		);
		expect(sub()?.status).toBe("refunded");
		const tx = raw.prepare("SELECT * FROM transactions WHERE transaction_id = 'GPA.1'").get() as Record<string, unknown>;
		expect(tx.revocation_date).toBe(4000);
	});

	it("升降级换发 token：旧行落幕，活跃权益不重复计数", async () => {
		await storeLedgerRows(db as never, makeRows({ messageId: "m1", type: "SUBSCRIPTION_PURCHASED", eventTime: 1000, token: "tok-old" }));
		await storeLedgerRows(
			db as never,
			makeRows({ messageId: "m2", type: "SUBSCRIPTION_PURCHASED", eventTime: 2000, token: "tok-new", orderId: "GPA.2", linkedToken: "tok-old" }),
		);
		expect(sub("tok-old")?.status).toBe("expired");
		expect(sub("tok-new")?.status).toBe("active");
		const active = raw.prepare("SELECT count(*) c FROM subscriptions WHERE status='active'").get() as { c: number };
		expect(Number(active.c)).toBe(1);
	});

	it("与 Apple 行共存：platform 列把两边分得开", async () => {
		raw.exec(
			`INSERT INTO subscriptions (original_transaction_id, status, environment, is_lifetime, last_signed_date, updated_at)
			 VALUES ('apple-1', 'active', 'Production', 0, 1, 1)`,
		);
		await storeLedgerRows(db as never, makeRows({ messageId: "m1", type: "SUBSCRIPTION_PURCHASED", eventTime: 1000 }));
		const byPlatform = raw
			.prepare("SELECT platform, count(*) c FROM subscriptions GROUP BY platform ORDER BY platform")
			.all() as { platform: string; c: number }[];
		expect(byPlatform.map((r) => [r.platform, Number(r.c)])).toEqual([
			["apple", 1],
			["play", 1],
		]);
	});
});
