// 官网下载（中国大陆 Android direct APK）。无官方徽章，故自绘黑色商店风徽章，与 App Store / Play 配对。
// APK 文件放 apps/web/public/orange-cloud.apk；发布新版替换该文件即可（URL 不变）。
export const DIRECT_APK_URL = "/orange-cloud.apk";

export default function DirectDownloadBadge({
	topLabel,
	mainLabel,
	alt,
	className = "",
}: {
	topLabel: string;
	mainLabel: string;
	alt: string;
	className?: string;
}) {
	return (
		<a
			href={DIRECT_APK_URL}
			download
			aria-label={alt}
			className={`inline-flex h-[44px] items-center gap-2.5 rounded-[8px] bg-black px-3.5 no-underline transition-transform duration-150 ease-out hover:scale-[1.03] active:scale-[0.97] ${className}`}
		>
			<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
				<path d="M12 3v12" />
				<path d="m7 11 5 5 5-5" />
				<path d="M5 21h14" />
			</svg>
			<span className="flex flex-col leading-none">
				<span className="text-[9px] font-medium tracking-wide text-white/85">{topLabel}</span>
				<span className="mt-1 text-[16px] font-semibold leading-none text-white">{mainLabel}</span>
			</span>
		</a>
	);
}
