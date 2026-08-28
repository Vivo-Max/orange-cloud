"use client";

import { useEffect, useState } from "react";
import type { BuySuccessStrings } from "@/lib/buy/content";

// 成功页：轮询 /api/checkout/result?session_id 拿激活码。
// 微信/支付宝异步支付时码可能未就绪 -> 轮询约 30s，超时提示查邮箱。
type Phase = "confirming" | "ready" | "timeout";

export default function BuyCodeResult({
	sessionId,
	scheme,
	strings: s,
}: {
	sessionId: string | null;
	scheme: string; // orangecloud://redeem
	strings: BuySuccessStrings;
}) {
	const [phase, setPhase] = useState<Phase>("confirming");
	const [code, setCode] = useState("");
	const [revoked, setRevoked] = useState(false);
	const [copied, setCopied] = useState(false);

	useEffect(() => {
		if (!sessionId) {
			setPhase("timeout");
			return;
		}
		let alive = true;
		let tries = 0;
		const poll = async () => {
			tries++;
			try {
				const r = await fetch(`/api/checkout/result?session_id=${encodeURIComponent(sessionId)}`);
				const j = (await r.json()) as { ready: boolean; code?: string; revoked?: boolean };
				if (!alive) return;
				if (j.ready && j.code) {
					setCode(j.code);
					setRevoked(Boolean(j.revoked));
					setPhase("ready");
					return;
				}
			} catch {
				// 忽略单次失败，继续轮询
			}
			if (!alive) return;
			if (tries >= 15) {
				setPhase("timeout");
				return;
			}
			setTimeout(poll, 2000);
		};
		poll();
		return () => {
			alive = false;
		};
	}, [sessionId]);

	async function copy() {
		try {
			await navigator.clipboard.writeText(code);
			setCopied(true);
			setTimeout(() => setCopied(false), 1800);
		} catch {
			// 剪贴板不可用：用户可手动选中复制
		}
	}

	if (phase === "confirming") {
		return (
			<div className="glass r-island p-8 text-center">
				<div className="mx-auto mb-5 h-8 w-8 animate-spin rounded-full" style={{ border: "3px solid var(--t-quaternary)", borderTopColor: "var(--oc-orange)" }} aria-hidden="true" />
				<h1 className="f-display text-[24px] font-bold t-primary">{s.confirmingTitle}</h1>
				<p className="mx-auto mt-3 max-w-[42ch] text-[14px] leading-relaxed t-secondary">{s.confirmingSub}</p>
			</div>
		);
	}

	if (phase === "timeout") {
		return (
			<div className="glass r-island p-8 text-center">
				<h1 className="f-display text-[24px] font-bold t-primary">{s.timeoutTitle}</h1>
				<p className="mx-auto mt-3 max-w-[42ch] text-[14px] leading-relaxed t-secondary">{s.timeoutSub}</p>
			</div>
		);
	}

	return (
		<div className="glass r-island p-8 text-center">
			<h1 className="f-display text-[26px] font-bold t-primary">{s.readyTitle}</h1>
			<p className="mt-3 text-[14px] t-secondary">{s.readySub}</p>

			<div className="mt-5">
				<div className="text-[11px] font-medium uppercase tracking-wider t-tertiary">{s.codeLabel}</div>
				<div
					className="f-mono mt-2 rounded-2xl px-4 py-4 text-[24px] font-bold tracking-[2px]"
					style={{ color: "var(--oc-orange)", background: "rgba(244,129,32,0.08)", border: "1px solid rgba(244,129,32,0.28)" }}
				>
					{code}
				</div>
				<button type="button" onClick={copy} className="link-quiet mt-2 text-[13px]" style={{ cursor: "pointer", background: "none", border: "none" }}>
					{copied ? s.copied : s.copy}
				</button>
			</div>

			{revoked ? (
				<p className="mx-auto mt-4 max-w-[40ch] text-[13px]" style={{ color: "var(--oc-orange-pressed)" }}>
					{s.revokedNote}
				</p>
			) : (
				<>
					<a href={`${scheme}?code=${encodeURIComponent(code)}`} className="cta-store mt-6" style={{ width: "100%", justifyContent: "center" }}>
						{s.openApp}
					</a>
					<p className="mx-auto mt-4 max-w-[42ch] text-[13px] leading-relaxed t-tertiary">{s.activateNote}</p>
					<p className="mx-auto mt-2 max-w-[42ch] text-[13px] leading-relaxed t-tertiary">{s.emailedNote}</p>
				</>
			)}
		</div>
	);
}
