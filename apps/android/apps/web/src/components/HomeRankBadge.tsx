"use client";

import { useEffect, useState } from "react";

// 首页按访客 IP 所在地区展示当前 App Store 排名的小徽章。
// 首页是 ISR 缓存页，无法内联按 IP 个性化的内容，故挂载后请求 /api/ranks/me（边缘读 CF-IPCountry）。
// 未上榜 / 非追踪地区返回 ranked:false → 本组件渲染 null（不占位）；命中则淡入玻璃徽章。
// 文案走「服务端 t.raw 取模板 + 客户端代入 {rank}」（与本工程既有 props 模式一致，
// 不依赖 useTranslations 在客户端的消息注入）；地区名走 Intl.DisplayNames 按页面语言本地化。

interface RankResponse {
	ranked: boolean;
	country?: string;
	position?: number;
	genreName?: string | null;
}

/** 两位国家码 → 旗帜 emoji（regional indicator）。 */
function regionFlag(code: string): string {
	const cc = code.toUpperCase();
	if (!/^[A-Z]{2}$/.test(cc)) return "";
	return String.fromCodePoint(...[...cc].map((c) => 0x1f1e6 + c.charCodeAt(0) - 65));
}

export default function HomeRankBadge({
	locale,
	badgeTemplate,
	ariaLabel,
	className = "",
}: {
	locale: string;
	/** 含 {rank} 占位的本地化模板，如 "#{rank} in Developer Tools"。 */
	badgeTemplate: string;
	ariaLabel: string;
	className?: string;
}) {
	const [data, setData] = useState<RankResponse | null>(null);
	const [shown, setShown] = useState(false);

	useEffect(() => {
		let alive = true;
		fetch("/api/ranks/me", { headers: { accept: "application/json" } })
			.then((r) => (r.ok ? (r.json() as Promise<RankResponse>) : null))
			.then((j) => {
				if (!alive || !j?.ranked) return;
				setData(j);
				requestAnimationFrame(() => alive && setShown(true));
			})
			.catch(() => {});
		return () => {
			alive = false;
		};
	}, []);

	if (!data?.ranked || data.position == null || !data.country) return null;

	let region = data.country.toUpperCase();
	try {
		region = new Intl.DisplayNames([locale], { type: "region" }).of(region) ?? region;
	} catch {
		// 个别运行时缺 Intl.DisplayNames 区域数据：回退国家码。
	}
	const flag = regionFlag(data.country);
	const text = badgeTemplate.replace("{rank}", String(data.position));

	return (
		<div
			className={`glass r-chip inline-flex items-center gap-2 rounded-full px-4 py-1.5 text-[13px] font-medium t-secondary ${className}`}
			style={{ opacity: shown ? 1 : 0, transition: "opacity 0.5s ease" }}
			role="status"
			aria-label={ariaLabel}
		>
			{flag ? (
				<span aria-hidden="true" className="text-[15px] leading-none">
					{flag}
				</span>
			) : null}
			<span>
				{region} · {text}
			</span>
		</div>
	);
}
