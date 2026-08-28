"use client";

export default function DashboardError({
	error,
	reset,
}: {
	error: Error & { digest?: string };
	reset: () => void;
}) {
	return (
		<div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
			<div className="rounded-xl border border-border bg-surface p-6 shadow-sm">
				<p className="text-sm font-medium text-negative">数据加载失败</p>
				<p className="mt-1 text-xs break-words text-muted">{error.message}</p>
				<p className="mt-3 text-xs text-muted">
					请确认 wrangler.jsonc 中 IAP_DB 绑定（orange-cloud-iap）正确，且已 migrate。
				</p>
				<button
					type="button"
					onClick={reset}
					className="mt-4 rounded-md border border-border px-3 py-1.5 text-xs font-medium transition-colors hover:bg-foreground/[0.06]"
				>
					重试
				</button>
			</div>
		</div>
	);
}
