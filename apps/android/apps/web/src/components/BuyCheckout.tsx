"use client";

import { useState } from "react";

// 购买按钮：POST /api/checkout（带当前 locale 用于回跳）→ 跳转 Stripe Checkout 托管页。
export default function BuyCheckout({
	locale,
	label,
	loadingText,
	errorText,
	payNote,
}: {
	locale: string;
	label: string;
	loadingText: string;
	errorText: string;
	payNote: string;
}) {
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState(false);

	async function go() {
		setLoading(true);
		setError(false);
		try {
			const r = await fetch("/api/checkout", {
				method: "POST",
				headers: { "content-type": "application/json" },
				body: JSON.stringify({ locale }),
			});
			const j = (await r.json()) as { url?: string };
			if (j.url) {
				window.location.href = j.url;
				return;
			}
			throw new Error("no url");
		} catch {
			setError(true);
			setLoading(false);
		}
	}

	return (
		<div>
			<button
				type="button"
				onClick={go}
				disabled={loading}
				className="cta-store"
				style={{
					width: "100%",
					justifyContent: "center",
					border: "none",
					cursor: loading ? "default" : "pointer",
					opacity: loading ? 0.7 : 1,
				}}
			>
				{loading ? loadingText : label}
			</button>
			<p className="mt-3 text-center text-[12.5px] t-tertiary">{payNote}</p>
			{error ? (
				<p className="mt-2 text-center text-[13px]" style={{ color: "var(--oc-orange-pressed)" }} role="alert">
					{errorText}
				</p>
			) : null}
		</div>
	);
}
