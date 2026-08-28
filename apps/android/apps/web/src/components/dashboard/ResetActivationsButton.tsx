"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

// 后台「解绑」：解除某码的全部设备绑定（用户需在设备上重新激活）。
export default function ResetActivationsButton({ code }: { code: string }) {
	const router = useRouter();
	const [busy, setBusy] = useState(false);
	const [err, setErr] = useState(false);

	async function reset() {
		if (!confirm("解除该激活码的全部设备绑定？用户需在设备上重新激活。")) return;
		setBusy(true);
		setErr(false);
		try {
			const r = await fetch("/api/admin/reset-activations", {
				method: "POST",
				headers: { "content-type": "application/json" },
				body: JSON.stringify({ code }),
			});
			if (!r.ok) throw new Error();
			router.refresh();
		} catch {
			setErr(true);
			setBusy(false);
		}
	}

	return (
		<button
			type="button"
			onClick={reset}
			disabled={busy}
			className="rounded-md bg-foreground/[0.06] px-1.5 py-0.5 text-[10px] font-medium text-muted transition-opacity hover:opacity-80 disabled:opacity-50"
		>
			{busy ? "…" : err ? "失败" : "解绑"}
		</button>
	);
}
