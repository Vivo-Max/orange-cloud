"use client";

import { useState, type FormEvent } from "react";
import { Link } from "@/i18n/navigation";
import type { BindStrings } from "@/lib/buy/content";

const fieldStyle: React.CSSProperties = {
	width: "100%",
	borderRadius: "12px",
	border: "1px solid var(--divider)",
	background: "rgba(255,255,255,0.5)",
	padding: "11px 13px",
	fontSize: "15px",
	color: "var(--t-primary)",
};

export default function BindEmailForm({ s, pre_code }: { s: BindStrings, pre_code?: string }) {
	const [code, setCode] = useState(pre_code || "");
	const [email, setEmail] = useState("");
	const [state, setState] = useState<"idle" | "submitting" | "ok">("idle");
	const [err, setErr] = useState("");

	async function submit(e: FormEvent) {
		e.preventDefault();
		if (!code.trim() || !email.trim() || state === "submitting") return;
		setState("submitting");
		setErr("");
		try {
			const r = await fetch("/api/bind-email", {
				method: "POST",
				headers: { "content-type": "application/json" },
				body: JSON.stringify({ code, email }),
			});
			const j = (await r.json()) as { ok: boolean; reason?: string };
			if (j.ok) {
				setState("ok");
				return;
			}
			setState("idle");
			setErr(
				j.reason === "already_bound" ? s.errAlready : j.reason === "revoked" ? s.errRevoked : s.errInvalid,
			);
		} catch {
			setState("idle");
			setErr(s.errGeneric);
		}
	}

	if (state === "ok") {
		return (
			<div className="glass r-island p-8 text-center">
				<h2 className="f-display text-[22px] font-bold t-primary">{s.okTitle}</h2>
				<p className="mx-auto mt-3 max-w-[42ch] text-[14px] leading-relaxed t-secondary">{s.okSub}</p>
				<Link href="/buy" className="link-quiet mt-6 inline-block text-[14px]">
					{s.back}
				</Link>
			</div>
		);
	}

	return (
		<form onSubmit={submit} className="glass r-island p-7 sm:p-8">
			<label className="block text-[13px] font-medium t-primary" htmlFor="be-code">
				{s.codeLabel}
			</label>
			<input id="be-code" value={code} onChange={(e) => setCode(e.target.value)} placeholder={s.codePlaceholder} autoComplete="off" className="f-mono mt-2" style={fieldStyle} />

			<label className="mt-5 block text-[13px] font-medium t-primary" htmlFor="be-email">
				{s.emailLabel}
			</label>
			<input id="be-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder={s.emailPlaceholder} autoComplete="email" className="mt-2" style={fieldStyle} />

			{err ? (
				<p className="mt-4 text-[13px]" style={{ color: "var(--oc-orange-pressed)" }} role="alert">
					{err}
				</p>
			) : null}

			<button type="submit" disabled={state === "submitting"} className="cta-store mt-6" style={{ width: "100%", justifyContent: "center", border: "none", cursor: "pointer", opacity: state === "submitting" ? 0.7 : 1 }}>
				{state === "submitting" ? s.submitting : s.submit}
			</button>
			<p className="mt-3 text-[12.5px] leading-relaxed t-tertiary">{s.note}</p>
		</form>
	);
}
