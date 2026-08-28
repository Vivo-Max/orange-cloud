// 用户自助「绑定邮箱」资格（纯函数）：仅「尚未绑定邮箱」的有效码可自助绑定；
// 已绑定的不允许自助改（防止知道码的人劫持找回邮箱），须走人工 / admin。

export type BindReason = "ok" | "invalid" | "already_bound" | "revoked";

export interface BindRow {
	status: string; // active | revoked
	buyer_email: string | null;
}

export function canSelfBindEmail(row: BindRow | null): { ok: boolean; reason: BindReason } {
	if (!row) return { ok: false, reason: "invalid" };
	if (row.status === "revoked") return { ok: false, reason: "revoked" };
	if (row.buyer_email != null && row.buyer_email.trim() !== "") {
		return { ok: false, reason: "already_bound" };
	}
	return { ok: true, reason: "ok" };
}
