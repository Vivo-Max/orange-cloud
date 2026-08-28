// 把收到的 App Store Server Notification 原样转发一份给外部 webhook。
//
// 「原样」= 转发 Apple 发来的原始请求体（`{"signedPayload":"<JWS>"}` 那串字节，
// 不重新序列化、不改字段），接收方可以自行验签。转发在验签通过后触发，
// 避免把伪造请求中继出去；发送走 fire-and-forget（ctx.waitUntil），
// 失败只记日志，绝不影响我们回给 Apple 的 200。

/** 外部接收端（固定地址，非机密：只是一个投递 URL）。可用 ASC_FORWARD_URL 覆盖 / 置空停用。 */
export const APPLE_FORWARD_URL =
	"https://apolu.app/hooks/asc/mx7jHyUuA1MEn9Fql2Hi829jy6YdjTMQ";

const TIMEOUT_MS = 10_000;

export async function forwardRawNotification(
	rawBody: string,
	options: { url?: string; contentType?: string | null } = {},
): Promise<void> {
	const url = options.url ?? APPLE_FORWARD_URL;
	if (!url) return;

	try {
		const res = await fetch(url, {
			method: "POST",
			headers: {
				"content-type": options.contentType ?? "application/json",
				"user-agent": "orange-cloud-asc-forwarder/1",
			},
			body: rawBody,
			signal: AbortSignal.timeout(TIMEOUT_MS),
		});
		if (!res.ok) {
			console.error("[apple-notifications] forward failed", res.status);
		}
	} catch (err) {
		console.error("[apple-notifications] forward error", err);
	}
}
