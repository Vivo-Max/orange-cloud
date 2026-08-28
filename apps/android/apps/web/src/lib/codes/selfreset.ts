// 用户自助「重置设备绑定」的资格判定（纯函数）。
// 校验：激活码存在 + 邮箱匹配购买邮箱 + 未撤销 + 距上次自助重置 ≥ 30 天（防止靠反复重置绕过设备上限）。
// 反枚举：码不存在与邮箱不匹配都归为 "invalid"（不泄露某码是否存在）。

export type SelfResetReason = "ok" | "invalid" | "no_email" | "revoked" | "rate_limited";

export interface SelfResetRow {
	status: string; // active | revoked
	buyer_email: string | null;
	last_self_reset_at: number | null; // ms epoch
}

export const SELF_RESET_WINDOW_DAYS = 30;
const WINDOW_MS = SELF_RESET_WINDOW_DAYS * 86_400_000;

export interface SelfResetDecision {
	ok: boolean;
	reason: SelfResetReason;
	/** rate_limited 时：下次可重置的时刻（ms epoch）。 */
	nextAt?: number;
}

export function canSelfReset(
	row: SelfResetRow | null,
	email: string,
	now: number = Date.now(),
	windowMs: number = WINDOW_MS,
): SelfResetDecision {
	if (!row) return { ok: false, reason: "invalid" }; // 不存在 —— 与邮箱不符同口径
	if (!row.buyer_email) return { ok: false, reason: "no_email" }; // 无登记邮箱（手动/赠码）→ 走人工
	if (row.buyer_email.trim().toLowerCase() !== email.trim().toLowerCase()) {
		return { ok: false, reason: "invalid" }; // 邮箱不符 —— 不区分于「不存在」，防枚举
	}
	if (row.status === "revoked") return { ok: false, reason: "revoked" };
	if (row.last_self_reset_at != null && now - row.last_self_reset_at < windowMs) {
		return { ok: false, reason: "rate_limited", nextAt: row.last_self_reset_at + windowMs };
	}
	return { ok: true, reason: "ok" };
}
