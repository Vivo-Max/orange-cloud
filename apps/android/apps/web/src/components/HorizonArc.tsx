"use client";

import { useEffect, useState } from "react";

/**
 * 地平线弧（iOS 端 HorizonArc 的 web 复刻）：
 * 虚线弧上的天体按访客本地时间走位——昼（6–18 时）画太阳，夜画月亮，
 * 进度 = 当前时刻在所属半日里的进度，与 App 同一套口径。
 */
function progressAndPhase(date: Date): { t: number; isDay: boolean } {
	const minutes = date.getHours() * 60 + date.getMinutes();
	const day = (minutes - 6 * 60) / (12 * 60);
	if (day >= 0 && day < 1) {
		return { t: Math.min(Math.max(day, 0.02), 0.98), isDay: true };
	}
	const night = minutes >= 18 * 60 ? minutes - 18 * 60 : minutes + 6 * 60;
	return { t: Math.min(Math.max(night / (12 * 60), 0.02), 0.98), isDay: false };
}

export default function HorizonArc({ className = "" }: { className?: string }) {
	// SSR 先画正午太阳，挂载后切到访客本地时间，每分钟自走
	const [state, setState] = useState({ t: 0.5, isDay: true });

	useEffect(() => {
		const update = () => setState(progressAndPhase(new Date()));
		update();
		const timer = setInterval(update, 60_000);
		return () => clearInterval(timer);
	}, []);

	// 二次贝塞尔：start(0, H-2) → control(50%, -0.55H) → end(100%, H-2)，与 App 同形
	const H = 44;
	const t = state.t;
	const mt = 1 - t;
	const xPct = (2 * mt * t * 0.5 + t * t) * 100;
	const yPx = mt * mt * (H - 2) + 2 * mt * t * (-H * 0.55) + t * t * (H - 2);

	return (
		<div className={`relative ${className}`} style={{ height: H }} aria-hidden="true">
			<svg
				viewBox={`0 0 100 ${H}`}
				preserveAspectRatio="none"
				className="absolute inset-0 h-full w-full"
			>
				<path
					d={`M 0 ${H - 2} Q 50 ${-H * 0.55} 100 ${H - 2}`}
					className="arc-dash"
					vectorEffect="non-scaling-stroke"
				/>
			</svg>
			<span
				className="absolute h-[7px] w-[7px] rounded-full"
				style={{
					left: `${xPct}%`,
					top: yPx,
					transform: "translate(-50%, -50%)",
					background: state.isDay ? "#F48120" : "#EDEDFA",
					boxShadow: state.isDay
						? "0 0 10px 2px rgba(244,129,32,0.6)"
						: "0 0 10px 2px rgba(255,255,255,0.55)",
				}}
			/>
		</div>
	);
}
