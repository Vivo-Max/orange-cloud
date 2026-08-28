"use client";

import { useState } from "react";

type ReleaseStatus = "live" | "in_review" | "pending_release";
type RenderedItem = { title: string; detail?: string };
type RenderedRelease = { version: string; date: string; channel: string; status: ReleaseStatus; items: RenderedItem[] };

/**
 * 更新历史的 iOS / Android 双轨切换。数据由服务端按 locale 预本地化后传入；
 * status==="live" 显示渠道徽章，否则显示「审核中」徽章（节点也变空心）。
 * 某轨为空（如 Android 尚未公开上架）时展示「即将上线」占位。
 */
export default function ChangelogTabs({
	ios,
	android,
	iosNote,
	androidSoon,
	statusInReview,
	labelIOS,
	labelAndroid,
}: {
	ios: RenderedRelease[];
	android: RenderedRelease[];
	iosNote: string;
	androidSoon: string;
	statusInReview: string;
	labelIOS: string;
	labelAndroid: string;
}) {
	const [tab, setTab] = useState<"ios" | "android">("ios");
	const active = tab === "ios" ? ios : android;

	return (
		<div className="mt-10">
			<div className="inline-flex rounded-full border border-white/10 bg-white/5 p-1" role="tablist" aria-label="平台">
				{([
					["ios", labelIOS],
					["android", labelAndroid],
				] as const).map(([key, label]) => (
					<button
						key={key}
						type="button"
						role="tab"
						aria-selected={tab === key}
						onClick={() => setTab(key)}
						className="rounded-full px-5 py-1.5 text-[14px] font-semibold transition-colors duration-150"
						style={tab === key ? { background: "var(--oc-orange)", color: "#fff" } : { color: "var(--t-secondary)" }}
					>
						{label}
					</button>
				))}
			</div>

			{active.length === 0 ? (
				<div className="glass r-island mt-8 p-8 text-center">
					<p className="text-[15px] t-secondary">{androidSoon}</p>
				</div>
			) : (
				<>
					<div className="relative mt-8">
						<div className="absolute bottom-0 top-3 w-px bg-white/10" style={{ left: "0.5rem" }} />
						<div className="space-y-8">
							{active.map((r) => (
								<div key={`${r.version}-${r.date}`} className="relative pl-10">
									<div
										className="absolute top-2 h-3 w-3 -translate-x-1/2 rounded-full ring-2 ring-orange-400/60"
										style={{ left: "0.5rem", background: r.status === "live" ? "var(--oc-orange)" : "transparent" }}
									/>
									<span className="text-[12px] font-medium tracking-wide t-tertiary">{r.date}</span>
									<div className="glass r-island mt-2 p-5 sm:p-6">
										<div className="flex flex-wrap items-center gap-2">
											<h3 className="f-display text-[17px] font-bold t-primary">v{r.version}</h3>
											{r.status === "live" ? (
												<span
													className="rounded-full px-2 py-0.5 text-[11px] font-medium"
													style={{ background: "rgba(244,129,32,0.15)", color: "var(--oc-orange)" }}
												>
													{r.channel}
												</span>
											) : (
												<span
													className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-medium"
													style={{ background: "rgba(245,158,11,0.16)", color: "#f59e0b" }}
												>
													<span className="h-[5px] w-[5px] rounded-full" style={{ background: "#f59e0b" }} />
													{statusInReview}
												</span>
											)}
										</div>
										<ul className="mt-3 space-y-1.5">
											{r.items.map((it, i) => (
												<li key={i} className="flex items-start gap-2 text-[13.5px] leading-relaxed t-secondary">
													<span
														className="mt-[7px] h-[5px] w-[5px] flex-none rounded-full"
														style={{ background: "var(--t-tertiary)" }}
													/>
													<span>
														<span className="font-medium t-primary">{it.title}</span>
														{it.detail ? <span> — {it.detail}</span> : null}
													</span>
												</li>
											))}
										</ul>
									</div>
								</div>
							))}
						</div>
					</div>
					{tab === "ios" && (
						<p className="mt-6 text-[12px] t-tertiary" style={{ marginLeft: "2.5rem" }}>
							{iosNote}
						</p>
					)}
				</>
			)}
		</div>
	);
}
