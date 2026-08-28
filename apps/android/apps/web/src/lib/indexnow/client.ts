// IndexNow 客户端：把变更的 URL 推给搜索引擎（Bing / Yandex / Seznam / Naver / Yep 共用同一协议）。
//
// 鉴权就是一个公开的密钥文件：https://90dd.adsl8.workers.dev/<key>.txt 里放着同一串 key，
// 搜索引擎回抓这个文件确认「提交者确实控制这个域名」。文件在 public/，别删。

import { SITE_HOST, SITE_URL } from "../site/urls";

export const INDEXNOW_KEY = "ae4368227a78d73327c42c34949e9075";
export const INDEXNOW_KEY_LOCATION = `${SITE_URL}/${INDEXNOW_KEY}.txt`;

/**
 * 端点按「从 Workers 打得通」排序，逐个试到第一个收下为止。
 * 协议规定提交给任一参与方的 URL 会自动共享给其余所有参与的搜索引擎，
 * 所以只要有一个收下就够了，不必逐个端点重复提交。
 *
 * 为什么中立端点 api.indexnow.org 反而排第二 —— 2026-08-21 实测：
 * 从 Cloudflare Workers 的出口 IP 打它一律 429（单条 URL 也 429、且不带 Retry-After），
 * 同一份请求体从本机出口打是 202。即 Workers 的共享出口 IP 被限流，
 * 与本站配额、批量大小无关；同一趟从 Workers 打 bing 返回 203 OK、打 yandex 返回 202。
 * 留着它做备选：换出口（本机 / CI）跑时它能覆盖最多的搜索引擎。
 */
const ENDPOINTS = [
	"https://www.bing.com/indexnow",
	"https://api.indexnow.org/indexnow",
	"https://yandex.com/indexnow",
] as const;

/** 协议上限 10000 条／次，本站远远用不到，仍按批切分兜底。 */
const BATCH_SIZE = 10_000;

export type Attempt = {
	endpoint: string;
	/** HTTP 状态码；请求本身就没打出去（超时 / DNS 等）时为 null */
	status: number | null;
	note: string;
};

export type SubmitResult = {
	ok: boolean;
	urls: string[];
	/** 每次尝试的结果，按顺序；后台接口直接回显它，出问题时一眼看出卡在哪个端点 */
	attempts: Attempt[];
	message?: string;
};

/** 状态码语义（照 IndexNow 规范；2xx 一律视为收下——bing 实际返回的是 203） */
function describe(status: number): string {
	if (status === 200) return "OK";
	if (status === 202) return "Accepted：已收下，密钥文件待校验";
	if (status >= 200 && status < 300) return `HTTP ${status}：已收下`;
	switch (status) {
		case 400:
			return "Bad request：请求体格式不对";
		case 403:
			return "Forbidden：密钥校验失败（检查 /<key>.txt 是否可访问且内容一致）";
		case 422:
			return "Unprocessable：URL 不属于本站，或与密钥不匹配";
		case 429:
			return "Too many requests：该出口 IP 被限流";
		default:
			return `HTTP ${status}`;
	}
}

const accepted = (status: number) => status >= 200 && status < 300;

/** 我们自己的错（密钥 / 格式 / URL 归属）——换个端点也是一样的结果，不必再试。 */
const ourFault = (status: number) => status >= 400 && status < 500 && status !== 429;

async function postBatch(endpoint: string, batch: string[]): Promise<Attempt> {
	try {
		const res = await fetch(endpoint, {
			method: "POST",
			headers: { "content-type": "application/json; charset=utf-8" },
			body: JSON.stringify({
				host: SITE_HOST,
				key: INDEXNOW_KEY,
				keyLocation: INDEXNOW_KEY_LOCATION,
				urlList: batch,
			}),
			signal: AbortSignal.timeout(15_000),
		});
		return { endpoint, status: res.status, note: describe(res.status) };
	} catch (err) {
		return { endpoint, status: null, note: `请求失败：${err instanceof Error ? err.message : String(err)}` };
	}
}

/**
 * 推一批 URL：端点逐个降级，任一端点 2xx 即算成功。
 * 429 / 5xx / 网络错误换下一个端点；403、422 这类自己的错直接失败，别拿同样的错去烦别家。
 */
export async function submitUrls(urls: string[]): Promise<SubmitResult> {
	if (urls.length === 0) return { ok: true, urls: [], attempts: [] };

	const attempts: Attempt[] = [];

	for (let i = 0; i < urls.length; i += BATCH_SIZE) {
		const batch = urls.slice(i, i + BATCH_SIZE);
		let done = false;

		for (const endpoint of ENDPOINTS) {
			const attempt = await postBatch(endpoint, batch);
			attempts.push(attempt);
			if (attempt.status !== null && accepted(attempt.status)) {
				done = true;
				break;
			}
			if (attempt.status !== null && ourFault(attempt.status)) {
				return { ok: false, urls, attempts, message: `${endpoint} → ${attempt.note}` };
			}
		}

		if (!done) {
			const last = attempts[attempts.length - 1];
			return { ok: false, urls, attempts, message: `所有端点都没收下，最后一次：${last.endpoint} → ${last.note}` };
		}
	}

	const win = attempts[attempts.length - 1];
	return { ok: true, urls, attempts, message: `${win.endpoint} → ${win.note}` };
}
