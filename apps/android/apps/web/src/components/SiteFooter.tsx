import { getLocale, getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";

/** 页脚：纯内容、不带天色——颜色全部取自所在 theme 上下文 */
export default async function SiteFooter() {
	const t = await getTranslations("footer");
	// 指南板块有英文与简体中文两套，只在这两种页脚露出入口（也是 /guides 的抓取入口）
	const locale = await getLocale();
	const guidesLabel = locale === "zh-Hans" ? "指南" : "Guides";

	return (
		<footer className="relative">
			<div
				className="mx-auto max-w-[1120px] px-6 py-10"
				style={{ borderTop: "0.5px solid var(--divider)" }}
			>
				<div className="flex flex-col items-start justify-between gap-4 sm:flex-row sm:items-center">
					<p className="text-[13px] t-secondary">{t("copyright")}</p>
					<nav className="flex items-center gap-6 text-[13px]">
						{(locale === "en" || locale === "zh-Hans") && (
							<Link href="/guides" className="link-quiet">
								{guidesLabel}
							</Link>
						)}
						<Link href="/privacy" className="link-quiet">
							{t("privacy")}
						</Link>
						<Link href="/terms" className="link-quiet">
							{t("terms")}
						</Link>
						<Link href="/contact" className="link-quiet">
							{t("contact")}
						</Link>
					</nav>
				</div>
				<p className="mt-5 max-w-[64ch] text-[12px] leading-relaxed t-tertiary">{t("disclaimer")}</p>
				<div className="mt-5 flex flex-wrap items-center gap-3">
					<a href="https://trendshift.io/repositories/53962?utm_source=trendshift-badge&utm_medium=badge&utm_campaign=badge-trendshift-53962" target="_blank" rel="noopener noreferrer"><img src="https://trendshift.io/api/badge/trendshift/repositories/53962/daily?language=Swift" alt="chen2he%2Forange-cloud | Trendshift" width="250" height="55" /></a>
				</div>
			</div>
		</footer>
	);
}
