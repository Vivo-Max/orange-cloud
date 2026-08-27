"use client";

import { useEffect, useState } from "react";
import HorizonArc from "./HorizonArc";

export type PhoneStrings = {
	greetMorning: string;
	greetAfternoon: string;
	greetEvening: string;
	health: string;
	statZones: string;
	statZonesSub: string;
	statWorkers: string;
	statWorkersSub: string;
	statDNS: string;
	statDNSSub: string;
	statR2: string;
	statR2Sub: string;
	usage: string;
	tileWorkers: string;
	tileR2: string;
	zonesSection: string;
	all: string;
	zoneActive: string;
	tabs: string[];
};

type Phase = "dawn" | "day" | "dusk" | "ember" | "night";

function phaseFor(hour: number): Phase {
	if (hour >= 5 && hour < 9) return "dawn";
	if (hour >= 9 && hour < 16) return "day";
	if (hour >= 16 && hour < 18) return "dusk";
	if (hour >= 18 && hour < 23) return "ember";
	return "night";
}

function Ring({ ratio }: { ratio: number }) {
	const r = 9;
	const c = 2 * Math.PI * r;
	return (
		<svg width="26" height="26" viewBox="0 0 26 26" aria-hidden="true">
			<circle cx="13" cy="13" r={r} fill="none" stroke="var(--t-quaternary)" strokeWidth="3" />
			<circle
				cx="13"
				cy="13"
				r={r}
				fill="none"
				stroke="#F48120"
				strokeWidth="3"
				strokeLinecap="round"
				strokeDasharray={`${c * ratio} ${c}`}
				transform="rotate(-90 13 13)"
			/>
		</svg>
	);
}

function Spark({ points, w = 46 }: { points: string; w?: number }) {
	return (
		<svg width={w} height="14" viewBox={`0 0 ${w} 14`} aria-hidden="true">
			<polyline
				points={points}
				fill="none"
				stroke="#F48120"
				strokeWidth="1.4"
				strokeLinejoin="round"
				strokeLinecap="round"
			/>
		</svg>
	);
}

const TAB_ICONS = [
	// 概览（四宫格）
	<svg key="g" width="17" height="17" viewBox="0 0 17 17" fill="currentColor" aria-hidden="true">
		<rect x="1" y="1" width="6.4" height="6.4" rx="1.6" />
		<rect x="9.6" y="1" width="6.4" height="6.4" rx="1.6" />
		<rect x="1" y="9.6" width="6.4" height="6.4" rx="1.6" />
		<rect x="9.6" y="9.6" width="6.4" height="6.4" rx="1.6" />
	</svg>,
	// 域名（地球）
	<svg key="z" width="17" height="17" viewBox="0 0 17 17" fill="none" stroke="currentColor" strokeWidth="1.3" aria-hidden="true">
		<circle cx="8.5" cy="8.5" r="6.6" />
		<ellipse cx="8.5" cy="8.5" rx="3" ry="6.6" />
		<path d="M2.2 8.5h12.6" />
	</svg>,
	// Workers（闪电）
	<svg key="w" width="17" height="17" viewBox="0 0 17 17" fill="currentColor" aria-hidden="true">
		<path d="M9.8 1.2 3.4 9.6h3.9l-1 6.2 6.3-8.4H8.7l1.1-6.2Z" />
	</svg>,
	// 存储（圆柱）
	<svg key="s" width="17" height="17" viewBox="0 0 17 17" fill="none" stroke="currentColor" strokeWidth="1.3" aria-hidden="true">
		<ellipse cx="8.5" cy="3.6" rx="6" ry="2.3" />
		<path d="M2.5 3.6v9.8c0 1.3 2.7 2.3 6 2.3s6-1 6-2.3V3.6" />
		<path d="M2.5 8.5c0 1.3 2.7 2.3 6 2.3s6-1 6-2.3" />
	</svg>,
	// 设置（齿轮，简化为太阳形）
	<svg key="t" width="17" height="17" viewBox="0 0 17 17" fill="none" stroke="currentColor" strokeWidth="1.3" aria-hidden="true">
		<circle cx="8.5" cy="8.5" r="3.2" />
		<path d="M8.5 1.2v2.2M8.5 13.6v2.2M1.2 8.5h2.2M13.6 8.5h2.2M3.3 3.3l1.6 1.6M12.1 12.1l1.6 1.6M13.7 3.3l-1.6 1.6M4.9 12.1l-1.6 1.6" />
	</svg>,
];

/** Hero 的手机演示：DOM 复刻 App 概览页，天色与问候随访客本地时间走 */
export default function PhoneDemo({ locale, s }: { locale: string; s: PhoneStrings }) {
	// SSR 固定为白昼 9:41，挂载后切到本地时间
	const [now, setNow] = useState<Date | null>(null);

	useEffect(() => {
		const update = () => setNow(new Date());
		update();
		const timer = setInterval(update, 30_000);
		return () => clearInterval(timer);
	}, []);

	const hour = now?.getHours() ?? 9;
	const phase = phaseFor(hour);
	const isDark = phase === "ember" || phase === "night";
	const greeting = hour < 12 ? s.greetMorning : hour < 18 ? s.greetAfternoon : s.greetEvening;
	const clock = now
		? new Intl.DateTimeFormat(locale, { hour: "numeric", minute: "2-digit" }).format(now)
		: "9:41";
	const dateLine = now
		? new Intl.DateTimeFormat(locale, { month: "long", day: "numeric", weekday: "long" }).format(now)
		: "";

	const stats = [
		{ label: s.statZones, value: "6", sub: s.statZonesSub },
		{ label: s.statWorkers, value: "12", sub: s.statWorkersSub },
		{ label: s.statDNS, value: "86", sub: s.statDNSSub },
		{ label: s.statR2, value: "4", sub: s.statR2Sub },
	];

	return (
		<div className="demo-device" aria-hidden="true">
			<div className={`screen ${isDark ? "theme-dark" : "theme-light"}`}>
				<div className={`demo-sky p-${phase}`} />
				<div className="dynamic-island" />

				{/* 状态栏 */}
				<div className="absolute left-0 right-0 top-0 z-50 flex items-center justify-between px-7 pt-3.5 t-primary">
					<span className="f-display tabular text-[12px] font-semibold">{clock}</span>
					<span className="flex items-center gap-1.5">
						<svg width="14" height="9" viewBox="0 0 14 9" fill="currentColor" aria-hidden="true">
							<rect x="0" y="5.5" width="2.4" height="3.5" rx="0.7" />
							<rect x="3.7" y="3.6" width="2.4" height="5.4" rx="0.7" />
							<rect x="7.4" y="1.8" width="2.4" height="7.2" rx="0.7" />
							<rect x="11.1" y="0" width="2.4" height="9" rx="0.7" opacity="0.35" />
						</svg>
						<svg width="18" height="9" viewBox="0 0 18 9" fill="none" aria-hidden="true">
							<rect x="0.5" y="0.5" width="14" height="8" rx="2.2" stroke="currentColor" opacity="0.5" />
							<rect x="2" y="2" width="9" height="5" rx="1.1" fill="currentColor" />
							<path d="M16.3 3v3a1.7 1.7 0 0 0 0-3Z" fill="currentColor" opacity="0.5" />
						</svg>
					</span>
				</div>

				{/* 页面内容 */}
				<div className="absolute inset-x-0 bottom-0 top-[42px] overflow-hidden px-3">
					<p className="text-[10px] t-secondary">{dateLine || " "}</p>
					<h3 className="f-display mt-0.5 whitespace-nowrap text-[23px] font-extrabold t-primary">
						{greeting}
					</h3>
					<p className="mt-0.5 text-[11px] t-secondary">{s.health}</p>

					<HorizonArc className="mt-1 -mx-1 scale-y-[0.8]" />

					{/* 指标 2×2 */}
					<div className="mt-1 grid grid-cols-2 gap-2">
						{stats.map((stat) => (
							<div key={stat.label} className="glass r-chip px-2.5 py-2">
								<p className="text-[9px] t-secondary">{stat.label}</p>
								<p className="f-display tabular text-[19px] font-bold leading-tight t-primary">{stat.value}</p>
								<p className="text-[8px] t-tertiary">{stat.sub}</p>
							</div>
						))}
					</div>

					{/* 用量 */}
					<div className="mt-2.5 flex items-center justify-between px-0.5">
						<p className="text-[13px] font-bold t-primary">{s.usage}</p>
					</div>
					<div className="mt-1.5 grid grid-cols-2 gap-2">
						<div className="glass r-chip px-2.5 py-2">
							<p className="whitespace-nowrap text-[8.5px] t-secondary">{s.tileWorkers}</p>
							<div className="mt-1 flex items-center gap-1.5">
								<Ring ratio={0.24} />
								<div>
									<p className="f-display tabular text-[13px] font-bold leading-none t-primary">2.4M</p>
									<p className="mt-0.5 text-[8px] t-secondary">/ 10M</p>
								</div>
							</div>
						</div>
						<div className="glass r-chip px-2.5 py-2">
							<p className="whitespace-nowrap text-[8.5px] t-secondary">{s.tileR2}</p>
							<div className="mt-1 flex items-center gap-1.5">
								<Ring ratio={0.18} />
								<div>
									<p className="f-display tabular text-[13px] font-bold leading-none t-primary">1.8M</p>
									<p className="mt-0.5 text-[8px] t-secondary">/ 10M</p>
								</div>
							</div>
						</div>
					</div>

					{/* 域名 */}
					<div className="mt-2.5 flex items-center justify-between px-0.5">
						<p className="text-[13px] font-bold t-primary">{s.zonesSection}</p>
						<p className="text-[11px]" style={{ color: "#F48120" }}>
							{s.all}
						</p>
					</div>
					<div className="glass r-chip mt-1.5 overflow-hidden">
						{[
							{ initial: "O", color: "#3D86E0", name: "90dd.adsl8.workers.dev", plan: "Pro", total: "412K", spark: "0,11 8,9.5 16,10 24,7.5 32,6.5 40,4 46,2.5" },
							{ initial: "M", color: "#E8743B", name: "mooncake.dev", plan: "Free", total: "86K", spark: "0,8 8,10.5 16,6 24,9 32,5.5 40,8.5 46,6" },
						].map((zone, i) => (
							<div
								key={zone.name}
								className="flex items-center gap-2 px-2.5 py-2"
								style={i > 0 ? { borderTop: "0.5px solid var(--divider)" } : undefined}
							>
								<span
									className="f-display flex h-6 w-6 flex-none items-center justify-center rounded-full text-[10px] font-bold text-white"
									style={{ background: zone.color }}
								>
									{zone.initial}
								</span>
								<span className="min-w-0 flex-1">
									<span className="block text-[11px] font-semibold leading-tight t-primary">{zone.name}</span>
									<span className="block text-[8.5px] t-secondary">
										{zone.plan} · {s.zoneActive}
									</span>
								</span>
								<span className="text-right">
									<Spark points={zone.spark} />
									<span className="tabular block text-[8px] t-secondary">{zone.total}</span>
								</span>
								<span className="h-[5px] w-[5px] flex-none rounded-full bg-[#30C758]" />
							</div>
						))}
					</div>
				</div>

				{/* 浮动玻璃 Tab 栏 */}
				<div className="glass absolute bottom-3 left-1/2 z-[55] flex h-[46px] w-[262px] -translate-x-1/2 items-center justify-around rounded-full px-1.5">
					{s.tabs.map((tab, i) => (
						<span
							key={tab}
							className="flex w-[46px] flex-col items-center gap-0.5"
							style={{ color: i === 0 ? "#F48120" : "var(--t-secondary)" }}
						>
							{TAB_ICONS[i]}
							<span className="text-[7px] font-medium">{tab}</span>
						</span>
					))}
				</div>
			</div>
		</div>
	);
}
