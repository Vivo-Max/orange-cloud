import type { ReactNode } from "react";

/** Card surface used for every dashboard panel. */
export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
	return (
		<section className={`rounded-xl border border-border bg-surface shadow-sm ${className}`}>
			{children}
		</section>
	);
}

export function CardHead({
	title,
	hint,
	action,
}: {
	title: string;
	hint?: string;
	action?: ReactNode;
}) {
	return (
		<div className="flex items-start justify-between gap-3 px-5 pt-4">
			<div>
				<h2 className="text-sm font-semibold tracking-tight">{title}</h2>
				{hint ? <p className="mt-0.5 text-xs text-muted">{hint}</p> : null}
			</div>
			{action ? <div className="shrink-0">{action}</div> : null}
		</div>
	);
}

type Tone = "muted" | "accent" | "positive" | "negative" | "info";

const TONE_CLASS: Record<Tone, string> = {
	muted: "bg-foreground/[0.06] text-muted",
	accent: "bg-accent/12 text-accent",
	positive: "bg-positive/12 text-positive",
	negative: "bg-negative/12 text-negative",
	info: "bg-foreground/[0.06] text-foreground/80",
};

export function Badge({
	children,
	tone = "muted",
	className = "",
}: {
	children: ReactNode;
	tone?: Tone;
	className?: string;
}) {
	return (
		<span
			className={`inline-flex items-center gap-1 rounded-md px-1.5 py-0.5 text-[11px] font-medium whitespace-nowrap ${TONE_CLASS[tone]} ${className}`}
		>
			{children}
		</span>
	);
}

/** Environment pill with a consistent tone across the dashboard. */
export function EnvBadge({ env }: { env: string | null }) {
	if (!env) return <Badge tone="muted">—</Badge>;
	return <Badge tone={env === "Production" ? "accent" : "muted"}>{env}</Badge>;
}

/** 平台徽章（App Store / Google Play）。历史行无 platform，按 Apple 计。 */
export function PlatformBadge({ platform }: { platform: string | null }) {
	const isPlay = platform === "play";
	return <Badge tone={isPlay ? "info" : "muted"}>{isPlay ? "Google Play" : "App Store"}</Badge>;
}

export function Legend({ items }: { items: { label: string; color: string }[] }) {
	return (
		<ul className="flex flex-wrap items-center gap-x-4 gap-y-1.5">
			{items.map((it) => (
				<li key={it.label} className="flex items-center gap-1.5 text-xs text-muted">
					<span
						className="inline-block h-2.5 w-2.5 rounded-[3px]"
						style={{ background: it.color }}
					/>
					{it.label}
				</li>
			))}
		</ul>
	);
}

export function EmptyState({ label = "暂无数据" }: { label?: string }) {
	return (
		<div className="flex h-full min-h-32 items-center justify-center p-6 text-sm text-muted">
			{label}
		</div>
	);
}
