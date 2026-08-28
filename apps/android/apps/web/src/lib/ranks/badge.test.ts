import { describe, expect, it } from "vitest";
import { BLANK_SVG, renderRankBadge } from "./badge";

describe("renderRankBadge", () => {
	it("渲染左标签 + 右值，含品牌橙与尺寸", () => {
		const svg = renderRankBadge({ label: "App Store", value: "US #117" });
		expect(svg.startsWith("<svg")).toBe(true);
		expect(svg).toContain("App Store");
		expect(svg).toContain("US #117");
		expect(svg).toContain("#F48120");
		expect(svg).toContain('height="20"');
	});

	it("自定义颜色生效", () => {
		const svg = renderRankBadge({ label: "x", value: "y", color: "#123456" });
		expect(svg).toContain("#123456");
	});

	it("XML 转义防注入", () => {
		const svg = renderRankBadge({ label: "a&b", value: '<script>"</script>' });
		expect(svg).toContain("a&amp;b");
		expect(svg).toContain("&lt;script&gt;");
		expect(svg).not.toContain("<script>");
	});

	it("value 为空 → 1×1 透明空白图", () => {
		expect(renderRankBadge({ label: "App Store", value: null })).toBe(BLANK_SVG);
		expect(BLANK_SVG).toContain('width="1"');
	});
});
