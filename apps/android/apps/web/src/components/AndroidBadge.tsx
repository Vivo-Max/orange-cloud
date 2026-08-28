"use client";

import { useEffect, useState } from "react";
import type { DownloadStrings } from "@/lib/buy/content";
import GooglePlayBadge from "./GooglePlayBadge";
import DirectDownloadBadge from "./DirectDownloadBadge";

// Android 下载徽章，按访客地区：中国大陆 → 官网下载 APK + Google Play（部分用户偏好 Play）；
// 其余 → 仅 Google Play。首页 ISR 缓存页，故客户端挂载后读 /api/geo；默认先显示 Play。
export default function AndroidBadge({
	locale,
	strings,
	className = "",
}: {
	locale: string;
	strings: DownloadStrings;
	className?: string;
}) {
	const [isCn, setIsCn] = useState<boolean | null>(null);

	useEffect(() => {
		let alive = true;
		fetch("/api/geo", { headers: { accept: "application/json" } })
			.then((r) => (r.ok ? (r.json() as Promise<{ country?: string }>) : null))
			.then((j) => {
				if (alive) setIsCn(j?.country === "cn");
			})
			.catch(() => {
				if (alive) setIsCn(false);
			});
		return () => {
			alive = false;
		};
	}, []);

	const play = (
		<GooglePlayBadge
			locale={locale}
			alt={strings.playAlt}
			comingLabel={strings.playComing}
			className={className}
		/>
	);

	if (isCn) {
		// 大陆：官网下载（可直接装）优先 + Google Play（部分用户偏好）
		return (
			<>
				<DirectDownloadBadge
					topLabel={strings.directTop}
					mainLabel={strings.directMain}
					alt={strings.directAlt}
					className={className}
				/>
				{play}
			</>
		);
	}

	return play;
}
