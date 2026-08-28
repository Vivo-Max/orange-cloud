import Image from "next/image";
import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import LocaleSwitcher from "./LocaleSwitcher";
import { APP_STORE_URL, APP_STORE_COMING } from "./AppStoreBadge";

export const GITHUB_URL = "https://github.com/chen2he/orange-cloud";
export const KOFI_URL = "https://ko-fi.com/chen2he";

/** 顶部导航：绝对定位浮在天色上，不做 sticky。
 *  hideLocaleSwitcher：指南文章只有英文版，切语言会落到 404，故在这些页面隐藏切换器。 */
export default async function SiteHeader({
	hideLocaleSwitcher = false,
}: {
	hideLocaleSwitcher?: boolean;
} = {}) {
	const t = await getTranslations("header");

	return (
		<header className="absolute inset-x-0 top-0 z-20">
			<div className="mx-auto flex max-w-[1120px] items-center justify-between px-6 py-5">
				<Link href="/" className="flex items-center gap-2.5 no-underline">
					<Image
						src="/icons/icon-64.png"
						alt=""
						width={30}
						height={30}
						className="rounded-[8px]"
						priority
					/>
					<span className="f-display text-[17px] font-bold t-primary">Orange Cloud</span>
				</Link>
				<div className="flex items-center gap-3">
					{!hideLocaleSwitcher && <LocaleSwitcher label={t("language")} />}
					<a
						href={GITHUB_URL}
						aria-label="GitHub"
						className="glass t-primary flex h-[31px] w-[31px] items-center justify-center rounded-full no-underline"
					>
						<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
							<path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.42 7.42 0 0 1 2-.27c.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z" />
						</svg>
					</a>
					<a
						href={KOFI_URL}
						target="_blank"
						rel="noopener noreferrer"
						aria-label={t("support")}
						title={t("support")}
						className="glass t-primary flex h-[31px] w-[31px] items-center justify-center rounded-full no-underline"
					>
						<svg width="17" height="17" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
							<path d="M18.5 3H6c-1.1 0-2 .9-2 2v5.71c0 3.83 2.95 7.18 6.78 7.29 3.96.12 7.22-3.06 7.22-7v-1h.5c1.93 0 3.5-1.57 3.5-3.5S20.43 3 18.5 3zM16 5v3H6V5h10zm2.5 3H18V5h.5c.83 0 1.5.67 1.5 1.5S19.33 8 18.5 8zM4 19h16v2H4z" />
						</svg>
					</a>
					{APP_STORE_COMING ? (
						<span
							className="hidden cursor-not-allowed select-none rounded-full px-4 py-[7px] text-[13px] font-semibold text-white opacity-50 sm:block"
							style={{ background: "var(--oc-orange)" }}
						>
							{t("downloadComing")}
						</span>
					) : (
						<a
							href={APP_STORE_URL}
							className="hidden rounded-full px-4 py-[7px] text-[13px] font-semibold text-white no-underline sm:block"
							style={{ background: "var(--oc-orange)" }}
						>
							{t("download")}
						</a>
					)}
				</div>
			</div>
		</header>
	);
}
