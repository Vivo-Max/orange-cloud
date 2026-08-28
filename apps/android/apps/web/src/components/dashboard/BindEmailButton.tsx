"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

// 后台给某码绑定 / 修改邮箱（手动码无 buyer_email 时用）。prompt 取值，够用即可。
export default function BindEmailButton({ code, current }: { code: string; current: string | null }) {
	const router = useRouter();
	const [busy, setBusy] = useState(false);

	async function bind() {
		const input = window.prompt("绑定 / 修改邮箱（用于自助重置验证）", current ?? "");
		if (input == null) return;
		const email = input.trim();
		if (!email) return;
		setBusy(true);
		try {
			const r = await fetch("/api/admin/bind-email", {
				method: "POST",
				headers: { "content-type": "application/json" },
				body: JSON.stringify({ code, email }),
			});
			if (!r.ok) throw new Error();
			router.refresh();
		} catch {
			window.alert("绑定失败");
		} finally {
			setBusy(false);
		}
	}

	return (
		<button
			type="button"
			onClick={bind}
			disabled={busy}
			className="rounded-md bg-foreground/[0.06] px-1.5 py-0.5 text-[10px] font-medium text-muted transition-opacity hover:opacity-80 disabled:opacity-50"
		>
			{busy ? "…" : current ? "改" : "绑定"}
		</button>
	);
}
