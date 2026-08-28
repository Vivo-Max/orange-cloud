export const GOOGLE_PLAY_URL = "https://play.google.com/store/apps/details?id=jiamin.chen.orangecloud";
export const GOOGLE_PLAY_COMING = false;

const PLAY_LOCALES = ["en", "zh-Hans", "zh-Hant", "zh-HK", "ja", "es-MX", "ko", "pt-BR", "pt-PT", "de", "fr", "ar", "tr"];

function badgeSrc(locale: string): string {
	return `/play/${PLAY_LOCALES.includes(locale) ? locale : "en"}.svg`;
}

/**
 * Google Play 官方本地化徽章（SVG 存 public/play/，遵循 Play 品牌指南，不可改图）。
 * coming=true 时降透明度、不可点，下方显示「即将上线」状态标签（同 AppStoreBadge）。
 */
export default function GooglePlayBadge({
	locale = "en",
	alt,
	comingLabel,
	coming = GOOGLE_PLAY_COMING,
	className = "",
}: {
	locale?: string;
	alt: string;
	comingLabel?: string;
	coming?: boolean;
	className?: string;
}) {
	const img = (
		// eslint-disable-next-line @next/next/no-img-element
		<img src={badgeSrc(locale)} alt={alt} style={{ height: "44px", width: "auto", display: "block" }} />
	);

	if (coming) {
		return (
			<div className={`inline-block cursor-not-allowed select-none ${className}`}>
				<div className="opacity-50">{img}</div>
				{comingLabel && (
					<p className="mt-1.5 flex items-center gap-1.5 text-[11px] t-secondary">
						<span
							className="inline-block h-[5px] w-[5px] flex-none animate-pulse rounded-full"
							style={{ background: "#fbbf24" }}
						/>
						{comingLabel}
					</p>
				)}
			</div>
		);
	}

	return (
		<a
			href={GOOGLE_PLAY_URL}
			className={`inline-block no-underline transition-transform duration-150 ease-out hover:scale-[1.03] active:scale-[0.97] ${className}`}
		>
			{img}
		</a>
	);
}
