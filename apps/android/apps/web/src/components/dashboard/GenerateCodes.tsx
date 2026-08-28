"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

// 后台手动生成激活码（媒体 / 评测分发）。数量 + 备注（渠道）+ 选填邮箱（创建即绑定）。
export default function GenerateCodes() {
	const router = useRouter();
	const [count, setCount] = useState(1);
	const [note, setNote] = useState("");
	const [email, setEmail] = useState("");
	const [busy, setBusy] = useState(false);
	const [result, setResult] = useState<string[] | null>(null);
	const [err, setErr] = useState(false);

	async function generate() {
		setBusy(true);
		setErr(false);
		try {
			const r = await fetch("/api/admin/codes", {
				method: "POST",
				headers: { "content-type": "application/json" },
				body: JSON.stringify({ count, note, email }),
			});
			const j = (await r.json()) as { codes?: string[] };
			if (!r.ok || !j.codes) throw new Error();
			setResult(j.codes);
			router.refresh();
		} catch {
			setErr(true);
		} finally {
			setBusy(false);
		}
	}

	const inputCls = "rounded-md border border-border bg-surface px-2 py-1 text-sm";
	return (
		<div className="rounded-lg border border-border bg-surface-2 p-3">
			<div className="flex flex-wrap items-end gap-2">
				<label className="flex flex-col gap-1 text-[11px] text-muted">
					数量
					<input
						type="number"
						min={1}
						max={50}
						value={count}
						onChange={(e) => setCount(Number(e.target.value))}
						className={`${inputCls} w-16`}
					/>
				</label>
				<label className="flex flex-col gap-1 text-[11px] text-muted">
					备注（渠道）
					<input
						value={note}
						onChange={(e) => setNote(e.target.value)}
						placeholder="如 TechReview 评测"
						className={`${inputCls} w-40`}
					/>
				</label>
				<label className="flex flex-col gap-1 text-[11px] text-muted">
					邮箱（选填，便于自助重置）
					<input
						type="email"
						value={email}
						onChange={(e) => setEmail(e.target.value)}
						placeholder="reviewer@example.com"
						className={`${inputCls} w-52`}
					/>
				</label>
				<button
					type="button"
					onClick={generate}
					disabled={busy}
					className="rounded-md bg-accent px-3 py-1.5 text-sm font-medium text-white transition-opacity hover:opacity-90 disabled:opacity-50"
				>
					{busy ? "生成中…" : "生成激活码"}
				</button>
				{err ? <span className="text-xs text-negative">生成失败</span> : null}
			</div>
			{result && result.length > 0 ? (
				<div className="mt-3">
					<div className="mb-1 text-[11px] text-muted">已生成 {result.length} 枚（点击全选复制分发）：</div>
					<textarea
						readOnly
						value={result.join("\n")}
						rows={Math.min(result.length, 8)}
						onFocus={(e) => e.currentTarget.select()}
						className="w-full rounded-md border border-border bg-surface px-2 py-1.5 font-mono text-xs"
					/>
				</div>
			) : null}
		</div>
	);
}
