import { afterEach, describe, expect, it, vi } from "vitest";
import { APPLE_FORWARD_URL, forwardRawNotification } from "./forward";

afterEach(() => {
	vi.unstubAllGlobals();
});

describe("forwardRawNotification", () => {
	it("原封不动地 POST 原始请求体到默认地址", async () => {
		const calls: Array<{ url: string; init: RequestInit }> = [];
		vi.stubGlobal(
			"fetch",
			vi.fn(async (url: string, init: RequestInit) => {
				calls.push({ url, init });
				return new Response("ok", { status: 200 });
			}),
		);

		const raw = '{"signedPayload":"aaa.bbb.ccc"}';
		await forwardRawNotification(raw);

		expect(calls).toHaveLength(1);
		expect(calls[0].url).toBe(APPLE_FORWARD_URL);
		expect(calls[0].init.method).toBe("POST");
		expect(calls[0].init.body).toBe(raw);
	});

	it("沿用来源的 content-type，可覆盖目标地址", async () => {
		const fetchMock = vi.fn(async () => new Response("", { status: 202 }));
		vi.stubGlobal("fetch", fetchMock);

		await forwardRawNotification("{}", {
			url: "https://example.test/hook",
			contentType: "application/json; charset=utf-8",
		});

		const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
		expect(url).toBe("https://example.test/hook");
		expect((init.headers as Record<string, string>)["content-type"]).toBe(
			"application/json; charset=utf-8",
		);
	});

	it("接收端报错 / 网络失败都不抛出（不影响给 Apple 的 200）", async () => {
		vi.stubGlobal(
			"fetch",
			vi.fn(async () => new Response("boom", { status: 500 })),
		);
		await expect(forwardRawNotification("{}")).resolves.toBeUndefined();

		vi.stubGlobal(
			"fetch",
			vi.fn(async () => {
				throw new Error("network down");
			}),
		);
		await expect(forwardRawNotification("{}")).resolves.toBeUndefined();
	});

	it("地址置空则不发请求", async () => {
		const fetchMock = vi.fn();
		vi.stubGlobal("fetch", fetchMock);
		await forwardRawNotification("{}", { url: "" });
		expect(fetchMock).not.toHaveBeenCalled();
	});
});
