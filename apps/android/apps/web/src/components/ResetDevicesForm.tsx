"use client";

import { useState, type FormEvent } from "react";
import { Link } from "@/i18n/navigation";
import type { ResetStrings } from "@/lib/buy/content";

const fieldStyle: React.CSSProperties = {
	width: "100%",
	borderRadius: "12px",
	border: "1px solid var(--divider)",
	background: "rgba(255,255,255,0.5)",
	padding: "11px 13px",
	fontSize: "15px",
	color: "var(--t-primary)",
};

interface ResetResp {
	ok: boolean;
	reason?: string;
	nextAt?: number | null;
}

export default function ResetDevicesForm({ s, locale }: { s: ResetStrings; locale: string }) {
	const [code, setCode] = useState("");
	const [email, setEmail] = useState("");
	const [state, setState] = useState<"idle" | "submitting" | "ok">("idle");
	const [err, setErr] = useState("");

	function messageFor(j: ResetResp): string {
		switch (j.reason) {
			case "no_email":
				return s.errNoEmail;
			case "revoked":
				return s.errRevoked;
			case "rate_limited": {
				const date = j.nextAt ? new Date(j.nextAt).toLocaleDateString(locale) : "";
				return s.errRateLimited.replace("{date}", date);
			}
			default:
				return s.errInvalid; // invalid / bad_request
		}
	}

	async function submit(e: FormEvent) {
		e.preventDefault();
		if (!code.trim() || !email.trim() || state === "submitting") return;
		setState("submitting");
		setErr("");
		try {
			const r = await fetch("/api/reset-devices", {
				method: "POST",
				headers: { "content-type": "application/json" },
				body: JSON.stringify({ code, email }),
			});
			const j = (await r.json()) as ResetResp;
			if (j.ok) {
				setState("ok");
				return;
			}
			setState("idle");
			setErr(messageFor(j));
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
			<label className="block text-[13px] font-medium t-primary" htmlFor="rd-code">
				{s.codeLabel}
			</label>
			<input
				id="rd-code"
				value={code}
				onChange={(e) => setCode(e.target.value)}
				placeholder={s.codePlaceholder}
				autoComplete="off"
				className="f-mono mt-2"
				style={fieldStyle}
			/>

			<label className="mt-5 block text-[13px] font-medium t-primary" htmlFor="rd-email">
				{s.emailLabel}
			</label>
			<input
				id="rd-email"
				type="email"
				value={email}
				onChange={(e) => setEmail(e.target.value)}
				placeholder={s.emailPlaceholder}
				autoComplete="email"
				className="mt-2"
				style={fieldStyle}
			/>

			{err ? (
				<p className="mt-4 text-[13px]" style={{ color: "var(--oc-orange-pressed)" }} role="alert">
					{err}
				</p>
			) : null}

			<button
				type="submit"
				disabled={state === "submitting"}
				className="cta-store mt-6"
				style={{ width: "100%", justifyContent: "center", border: "none", cursor: "pointer", opacity: state === "submitting" ? 0.7 : 1 }}
			>
				{state === "submitting" ? s.submitting : s.submit}
			</button>
			<p className="mt-3 text-[12.5px] leading-relaxed t-tertiary">{s.policyNote}</p>
		</form>
	);
}
