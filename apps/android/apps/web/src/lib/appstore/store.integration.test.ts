// store 层针对「真实 SQL 引擎」的集成测试：用 node:sqlite 跑实际的
// INSERT OR IGNORE 幂等、ON CONFLICT upsert、以及 last_signed_date 乱序保护。
// 用一个最小 D1 适配器把 store.ts 期望的 prepare/bind/batch 映射到 node:sqlite。

import { readdirSync, readFileSync } from "node:fs";
import { DatabaseSync } from "node:sqlite";
import { beforeEach, describe, expect, it } from "vitest";
import { processNotification } from "./store";
import type { DecodedNotification } from "./types";

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

let raw: DatabaseSync;
let db: FakeD1;

// 应用全部 migration（含 0003 给 transactions 加的 offer_identifier / storefront），
// 让集成测试的表结构与生产一致。
const migDir = new URL("../../../migrations/", import.meta.url);
const migrations = readdirSync(migDir)
	.filter((f) => f.endsWith(".sql"))
	.sort()
	.map((f) => readFileSync(new URL(f, migDir), "utf8"));

beforeEach(() => {
	raw = new DatabaseSync(":memory:");
	for (const m of migrations) raw.exec(m);
	db = new FakeD1(raw);
});

interface NotifInput {
	type: string;
	subtype?: string;
	uuid: string;
	signedDate: number;
	otid?: string;
	txId?: string;
	productId?: string;
	txType?: string;
	expiresDate?: number;
	autoRenewStatus?: number;
	revocationDate?: number;
}

function makeDecoded(i: NotifInput): DecodedNotification {
	const otid = i.otid ?? "A";
	return {
		payload: {
			notificationType: i.type,
			subtype: i.subtype,
			notificationUUID: i.uuid,
			signedDate: i.signedDate,
			data: { bundleId: "jiamin.chen.orange-cloud", environment: "Sandbox" },
		},
		transaction: {
			transactionId: i.txId ?? otid,
			originalTransactionId: otid,
			productId: i.productId ?? "jiamin.chen.orange_cloud.pro.yearly",
			type: i.txType ?? "Auto-Renewable Subscription",
			purchaseDate: i.signedDate,
			expiresDate: i.expiresDate,
			price: 19_990,
			currency: "USD",
			revocationDate: i.revocationDate,
			signedDate: i.signedDate,
			environment: "Sandbox",
		},
		renewal: {
			originalTransactionId: otid,
			autoRenewStatus: i.autoRenewStatus ?? 1,
			autoRenewProductId: "jiamin.chen.orange_cloud.pro.yearly",
			signedDate: i.signedDate,
		},
	};
}

function sub(otid = "A") {
	return raw.prepare("SELECT * FROM subscriptions WHERE original_transaction_id = ?").get(otid) as
		| Record<string, unknown>
		| undefined;
}
function count(table: string): number {
	return Number((raw.prepare(`SELECT count(*) c FROM ${table}`).get() as { c: number }).c);
}

describe("processNotification（真实 SQL）", () => {
	it("首购 + 续订：状态有效、到期推进、流水累加", async () => {
		await processNotification(db as never, makeDecoded({ type: "SUBSCRIBED", subtype: "INITIAL_BUY", uuid: "u1", signedDate: 1000, txId: "t1", expiresDate: 5000 }));
		expect(sub()?.status).toBe("active");
		expect(sub()?.expires_date).toBe(5000);

		await processNotification(db as never, makeDecoded({ type: "DID_RENEW", uuid: "u2", signedDate: 2000, txId: "t2", expiresDate: 9000 }));
		expect(sub()?.status).toBe("active");
		expect(sub()?.expires_date).toBe(9000); // 到期推进
		expect(count("transactions")).toBe(2); // t1 + t2
		expect(count("notifications")).toBe(2);
	});

	it("重复 notificationUUID 幂等：标记 duplicate 且不重复入审计", async () => {
		const n = makeDecoded({ type: "SUBSCRIBED", uuid: "dup", signedDate: 1000, txId: "t1" });
		const first = await processNotification(db as never, n);
		const second = await processNotification(db as never, n);
		expect(first.duplicate).toBe(false);
		expect(second.duplicate).toBe(true);
		expect(count("notifications")).toBe(1);
	});

	it("乱序保护：迟到的更旧 EXPIRED 不把状态写回过期", async () => {
		await processNotification(db as never, makeDecoded({ type: "SUBSCRIBED", uuid: "u1", signedDate: 1000, txId: "t1", expiresDate: 5000 }));
		await processNotification(db as never, makeDecoded({ type: "DID_RENEW", uuid: "u2", signedDate: 2000, txId: "t2", expiresDate: 9000 }));
		// 一条 signedDate=1500（旧于当前 2000）的 EXPIRED 迟到
		await processNotification(db as never, makeDecoded({ type: "EXPIRED", subtype: "VOLUNTARY", uuid: "u3", signedDate: 1500, txId: "t2" }));
		expect(sub()?.status).toBe("active"); // 未回退
		expect(sub()?.last_signed_date).toBe(2000);
		expect(count("notifications")).toBe(3); // 审计仍记录

		// 一条 signedDate=3000（新）的 EXPIRED 才生效
		await processNotification(db as never, makeDecoded({ type: "EXPIRED", subtype: "VOLUNTARY", uuid: "u4", signedDate: 3000, txId: "t2" }));
		expect(sub()?.status).toBe("expired");
	});

	it("退款：状态 refunded、流水回写撤销时间", async () => {
		await processNotification(db as never, makeDecoded({ type: "SUBSCRIBED", uuid: "u1", signedDate: 1000, txId: "t1", expiresDate: 5000 }));
		await processNotification(db as never, makeDecoded({ type: "REFUND", uuid: "u2", signedDate: 4000, txId: "t1", revocationDate: 4000 }));
		expect(sub()?.status).toBe("refunded");
		const tx = raw.prepare("SELECT * FROM transactions WHERE transaction_id = ?").get("t1") as Record<string, unknown>;
		expect(tx.revocation_date).toBe(4000);
	});

	it("买断 ONE_TIME_CHARGE：is_lifetime=1、无到期", async () => {
		await processNotification(db as never, makeDecoded({ type: "ONE_TIME_CHARGE", uuid: "u1", signedDate: 1000, otid: "L", txId: "L", productId: "jiamin.chen.orange_cloud.pro.lifetime", txType: "Non-Consumable" }));
		expect(sub("L")?.status).toBe("active");
		expect(sub("L")?.is_lifetime).toBe(1);
		expect(sub("L")?.expires_date).toBeNull();
	});
});
