// Bark 推送（https://bark.day.app）。V2 接口：POST {server}/push，JSON 带 device_key。
// 自托管可改 server；官方默认 https://api.day.app。

export interface BarkPush {
	title?: string;
	subtitle?: string;
	body: string;
	group?: string;
	sound?: string;
	level?: "critical" | "active" | "timeSensitive" | "passive";
	icon?: string;
	url?: string;
}

const DEFAULT_SERVER = "https://api.day.app";

/**
 * 发一条 Bark 推送。非 2xx 抛错；调用方（fire-and-forget）自行 try/catch 吞掉，
 * 推送失败不影响主流程。8s 超时防止挂住。
 */
export async function sendBark(
	deviceKey: string,
	push: BarkPush,
	server: string = DEFAULT_SERVER,
): Promise<void> {
	const res = await fetch(`${server.replace(/\/+$/, "")}/push`, {
		method: "POST",
		headers: { "content-type": "application/json" },
		body: JSON.stringify({ device_key: deviceKey, ...push }),
		signal: AbortSignal.timeout(8000),
	});
	if (!res.ok) {
		throw new Error(`bark push failed: HTTP ${res.status}`);
	}
}
