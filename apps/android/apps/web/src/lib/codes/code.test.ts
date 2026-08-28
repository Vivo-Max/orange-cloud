import { describe, it, expect } from "vitest";
import { generateCode, formatCode, normalizeCode } from "./code";

const ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

describe("generateCode", () => {
	it("产出 10 位且全在 Crockford 字母表内", () => {
		for (let i = 0; i < 200; i++) {
			const c = generateCode();
			expect(c).toHaveLength(10);
			for (const ch of c) expect(ALPHABET).toContain(ch);
		}
	});

	it("不含易混字符 I/L/O/U", () => {
		const joined = Array.from({ length: 200 }, () => generateCode()).join("");
		expect(joined).not.toMatch(/[ILOU]/);
	});

	it("基本不重复（200 枚去重后仍 200）", () => {
		const set = new Set(Array.from({ length: 200 }, () => generateCode()));
		expect(set.size).toBe(200);
	});
});

describe("formatCode / normalizeCode 往返", () => {
	it("normalize(format(core)) === core", () => {
		for (let i = 0; i < 50; i++) {
			const core = generateCode();
			expect(normalizeCode(formatCode(core))).toBe(core);
		}
	});

	it("format 形如 OC-XXXXX-XXXXX", () => {
		expect(formatCode("0123456789")).toBe("OC-01234-56789");
	});
});

describe("normalizeCode 容错", () => {
	const core = "0123456789"; // 合法核心串

	it("接受小写 + 空格 + 缺前缀", () => {
		expect(normalizeCode("01234 56789")).toBe(core);
		expect(normalizeCode("oc-01234-56789")).toBe(core);
	});

	it("Crockford 把 I/L->1、O->0 当作笔误纠正", () => {
		// 期望核心串 1100（再补满 10 位）：用户把 1 打成 I/L、0 打成 O
		expect(normalizeCode("OC-ILO00-00000")).toBe("1100000000");
	});

	it("长度不对或越界字符返回 null", () => {
		expect(normalizeCode("")).toBeNull();
		expect(normalizeCode("OC-123-456")).toBeNull(); // 太短
		expect(normalizeCode("OC-01234-5678U")).toBeNull(); // U 不在字母表（不被映射）
	});
});
