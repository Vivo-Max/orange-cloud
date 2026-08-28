"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

// 后台退款申请的操作按钮：通过（发起 Stripe 退款 + 撤销码）/ 拒绝。
export default function RefundActions({ code }: { code: string }) {
	const router = useRouter();
	const [busy, setBusy] = useState<"" | "approve" | "reject">("");
	const [err, setErr] = useState(false);

	async function act(action: "approve" | "reject") {
		if (action === "approve" && !confirm("确认通过退款？将向 Stripe 发起退款并撤销该激活码。")) return;
		setBusy(action);
		setErr(false);
		try {
			const r = await fetch("/api/admin/refund", {
				method: "POST",
				headers: { "content-type": "application/json" },
				body: JSON.stringify({ code, action }),
			});
			if (!r.ok) throw new Error();
			router.refresh();
		} catch {
			setErr(true);
			setBusy("");
		}
	}

	return (
		<div className="flex items-center gap-1.5 whitespace-nowrap">
			<button
				type="button"
				onClick={() => act("approve")}
				disabled={!!busy}
				className="rounded-md bg-accent/12 px-2 py-1 text-[11px] font-medium text-accent transition-opacity hover:opacity-80 disabled:opacity-50"
			>
				{busy === "approve" ? "…" : "通过退款"}
			</button>
			<button
				type="button"
				onClick={() => act("reject")}
				disabled={!!busy}
				className="rounded-md bg-foreground/[0.06] px-2 py-1 text-[11px] font-medium text-muted transition-opacity hover:opacity-80 disabled:opacity-50"
			>
				{busy === "reject" ? "…" : "拒绝"}
			</button>
			{err ? <span className="text-[11px] text-negative">失败</span> : null}
		</div>
	);
}
