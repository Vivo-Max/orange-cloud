import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { APOLU_URL, apoluUrl } from "./apolu";
import { routing } from "../../i18n/routing";

const SIBLING_KEYS = ["eyebrow", "name", "tagline", "body", "cta"] as const;

function messages(locale: string): Record<string, Record<string, string>> {
	return JSON.parse(readFileSync(new URL(`../../messages/${locale}.json`, import.meta.url), "utf-8"));
}

describe("apolu 外链映射", () => {
	it("13 个语种都有显式落点，无遗漏", () => {
		expect(Object.keys(APOLU_URL).sort()).toEqual([...routing.locales].sort());
	});

	it("apolu 没有的四个语种走兜底，不拼出 404", () => {
		// 实测：/zh-HK、/es-MX、/pt-PT、/ar 在 apolu.app 上均为 404
		expect(APOLU_URL["zh-HK"]).toBe("https://apolu.app/zh-Hant");
		expect(APOLU_URL["es-MX"]).toBe("https://apolu.app/es");
		expect(APOLU_URL["pt-PT"]).toBe("https://apolu.app/pt-BR");
		expect(APOLU_URL.ar).toBe("https://apolu.app/");
		for (const url of Object.values(APOLU_URL)) {
			expect(url).not.toMatch(/\/(zh-HK|es-MX|pt-PT|ar)$/);
		}
	});

	it("未知 locale 回落到默认语种", () => {
		expect(apoluUrl("xx")).toBe(APOLU_URL[routing.defaultLocale]);
	});
});

describe("sibling 文案覆盖", () => {
	const en = messages("en").sibling;

	for (const locale of routing.locales) {
		it(`${locale} 补齐了 sibling.*`, () => {
			const m = messages(locale).sibling;
			expect(m, `${locale} 缺 sibling 块`).toBeDefined();
			for (const key of SIBLING_KEYS) {
				expect(m[key]?.trim(), `${locale}.sibling.${key} 缺失或为空`).toBeTruthy();
			}
			// 产品名各语种一致，其余字段不得留英文占位
			expect(m.name).toBe("Apolu");
			if (locale !== "en") {
				for (const key of ["eyebrow", "tagline", "body", "cta"] as const) {
					expect(m[key], `${locale}.sibling.${key} 仍是英文原文`).not.toBe(en[key]);
				}
			}
		});
	}
});
