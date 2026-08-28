// parseRankPayload 纯函数单测：正常上榜 / 上架未上榜 / 不可用（空 data）/ 结构异常。

import { describe, expect, it } from "vitest";
import { parseRankPayload } from "./capture";

function payloadWithChart(position: number) {
	return {
		data: [
			{
				id: "6779323783",
				type: "apps",
				attributes: {
					chartPositions: {
						appStore: {
							chart: "top-free",
							chartLink: "https://apps.apple.com/us/charts/iphone/developer-tools-apps/6026",
							genre: 6026,
							genreName: "Developer Tools",
							genreShortName: "Dev Tools",
							position,
						},
					},
				},
			},
		],
	};
}

describe("parseRankPayload", () => {
	it("正常上榜：取出名次与类目", () => {
		const parsed = parseRankPayload(payloadWithChart(124));
		expect(parsed).toEqual({
			position: 124,
			chart: "top-free",
			genre: 6026,
			genreName: "Developer Tools",
		});
	});

	it("上架但未上榜（无 chartPositions）：position 为 null、类目为 null", () => {
		const json = { data: [{ id: "6779323783", attributes: { name: "Orange-Cloud" } }] };
		expect(parseRankPayload(json)).toEqual({
			position: null,
			chart: null,
			genre: null,
			genreName: null,
		});
	});

	it("不可用地区：空 data 数组 → null（跳过）", () => {
		expect(parseRankPayload({ data: [] })).toBeNull();
	});

	it("404 错误信封（无 data 字段）→ null", () => {
		const errBody = { errors: [{ title: "Resource Not Found", status: "404", code: "40400" }] };
		expect(parseRankPayload(errBody)).toBeNull();
	});

	it("结构异常输入一律安全返回 null", () => {
		expect(parseRankPayload(null)).toBeNull();
		expect(parseRankPayload(undefined)).toBeNull();
		expect(parseRankPayload("nope")).toBeNull();
		expect(parseRankPayload({})).toBeNull();
	});
});
