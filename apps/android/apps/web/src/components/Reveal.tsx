"use client";

import { useEffect, useRef, type CSSProperties, type ReactNode } from "react";

/** 玻璃岛错峰浮现（iOS 端 islandReveal 的 web 复刻），index 决定先后 */
export default function Reveal({
	children,
	index = 0,
	className = "",
}: {
	children: ReactNode;
	index?: number;
	className?: string;
}) {
	const ref = useRef<HTMLDivElement>(null);

	useEffect(() => {
		const el = ref.current;
		if (!el) return;
		const io = new IntersectionObserver(
			(entries) => {
				for (const entry of entries) {
					if (entry.isIntersecting) {
						el.classList.add("shown");
						io.disconnect();
					}
				}
			},
			{ threshold: 0.12, rootMargin: "0px 0px -32px 0px" },
		);
		io.observe(el);
		return () => io.disconnect();
	}, []);

	return (
		<div
			ref={ref}
			className={`reveal ${className}`}
			style={{ "--reveal-delay": `${index * 60}ms` } as CSSProperties}
		>
			{children}
		</div>
	);
}
