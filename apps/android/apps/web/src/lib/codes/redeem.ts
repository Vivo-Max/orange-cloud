// 兑换决策（纯函数，便于单测）：给定码记录 + 已绑定设备 + 申请设备，判定是否放行。
// 在线兑换 / 周期复核共用此入口：已绑定的设备复核即幂等放行（用于尊重退款撤销）。

export type RedeemReason = "ok" | "not_found" | "revoked" | "device_limit";

export interface CodeRow {
	code: string;
	product: string;
	status: string; // active | revoked
}

export interface RedeemDecision {
	ok: boolean;
	reason: RedeemReason;
	product?: string;
	/** 需要新增一条设备绑定（首次在此设备激活）。 */
	bind: boolean;
}

/** 一码最多激活台数（主力机 + 备用机够用）。客户端用 ANDROID_ID 绑「设备」，重装不占新名额。 */
export const MAX_ACTIVATIONS = 2;

export function decideRedeem(
	row: CodeRow | null,
	boundInstallIds: string[],
	installId: string,
	max: number = MAX_ACTIVATIONS,
): RedeemDecision {
	if (!row) return { ok: false, reason: "not_found", bind: false };
	if (row.status === "revoked")
		return { ok: false, reason: "revoked", product: row.product, bind: false };
	// 已绑定 -> 幂等放行（周期复核走这里）
	if (boundInstallIds.includes(installId))
		return { ok: true, reason: "ok", product: row.product, bind: false };
	// 新设备且已达上限 -> 拒绝
	if (boundInstallIds.length >= max)
		return { ok: false, reason: "device_limit", product: row.product, bind: false };
	// 新设备且名额未满 -> 放行并绑定
	return { ok: true, reason: "ok", product: row.product, bind: true };
}
