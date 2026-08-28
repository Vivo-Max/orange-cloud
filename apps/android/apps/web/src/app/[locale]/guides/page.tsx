import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { setRequestLocale } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import SiteHeader from "@/components/SiteHeader";
import SiteFooter from "@/components/SiteFooter";
import HorizonArc from "@/components/HorizonArc";
import {
	GUIDE_LOCALE,
	GUIDE_LOCALE_ZH,
	guidePath,
	guidesFor,
	guidesIndexFor,
	isGuideLocale,
} from "@/lib/guides/guides";

const SITE_URL = "https://90dd.adsl8.workers.dev";

/** 索引页两语并列，文章不互译，故只有这一页写 hreflang */
const INDEX_LANGUAGES = {
	en: "/guides",
	"zh-Hans": `/${GUIDE_LOCALE_ZH}/guides`,
	"x-default": "/guides",
};

export async function generateMetadata({
	params,
}: {
	params: Promise<{ locale: string }>;
}): Promise<Metadata> {
	const { locale } = await params;
	if (!isGuideLocale(locale)) return {};
	const index = guidesIndexFor(locale);
	const path = guidePath(locale, "/guides");
	const og = locale === GUIDE_LOCALE_ZH ? "/og/zh-Hans.jpg" : "/og/en.jpg";

	return {
		metadataBase: new URL(SITE_URL),
		title: index.title,
		description: index.description,
		alternates: { canonical: path, languages: INDEX_LANGUAGES },
		openGraph: {
			title: index.title,
			description: index.description,
			url: path,
			siteName: "Orange Cloud",
			type: "website",
			locale: locale === GUIDE_LOCALE_ZH ? "zh_CN" : "en_US",
			images: [{ url: og, width: 1280, height: 640, alt: "Orange Cloud" }],
		},
		twitter: {
			card: "summary_large_image",
			title: index.title,
			description: index.description,
			images: [og],
		},
	};
}

export default async function GuidesIndexPage({ params }: { params: Promise<{ locale: string }> }) {
	const { locale } = await params;
	// 指南只有英文与简体中文两套：其它语言前缀不提供内容，避免稀薄的机翻页与软 404
	if (!isGuideLocale(locale)) notFound();
	setRequestLocale(locale);

	const index = guidesIndexFor(locale);
	const guides = guidesFor(locale);

	const jsonLd = {
		"@context": "https://schema.org",
		"@type": "CollectionPage",
		name: index.h1,
		description: index.description,
		url: `${SITE_URL}${guidePath(locale, "/guides")}`,
		inLanguage: locale === GUIDE_LOCALE_ZH ? "zh-Hans" : "en",
		hasPart: guides.map((guide) => ({
			"@type": "TechArticle",
			headline: guide.h1,
			url: `${SITE_URL}${guidePath(locale, `/guides/${guide.slug}`)}`,
		})),
	};

	const otherLocale = locale === GUIDE_LOCALE ? GUIDE_LOCALE_ZH : GUIDE_LOCALE;
	const otherLabel = otherLocale === GUIDE_LOCALE_ZH ? "中文指南" : "Guides in English";

	return (
		<div className="guides theme-light sky-band band-dawn dawn-glow flex min-h-screen flex-col overflow-x-clip">
			<script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />
			<SiteHeader hideLocaleSwitcher />
			<main className="relative mx-auto w-full max-w-[760px] flex-1 px-6 pb-20 pt-32">
				<h1 className="f-display text-[34px] font-bold t-primary sm:text-[40px]">{index.h1}</h1>
				<p className="mt-4 max-w-[56ch] text-[17px] leading-relaxed t-secondary">{index.description}</p>
				<p className="mt-4 text-[14px] t-tertiary">
					<a href={guidePath(otherLocale, "/guides")} className="link-quiet underline underline-offset-2">
						{otherLabel} →
					</a>
				</p>
				<HorizonArc className="mt-9" />

				<div className="mt-10 grid gap-4">
					{guides.map((guide) => (
						<Link
							key={guide.slug}
							href={`/guides/${guide.slug}`}
							className="glass r-island block p-6 no-underline transition-transform duration-150 ease-out hover:scale-[1.008] active:scale-[0.995]"
						>
							<h2 className="f-display text-[21px] font-bold leading-snug t-primary">{guide.h1}</h2>
							<p className="mt-2.5 text-[15px] leading-relaxed t-secondary">{guide.blurb}</p>
							<p className="mt-4 text-[13px] t-tertiary">{guide.readingTime}</p>
						</Link>
					))}
				</div>
			</main>
			<SiteFooter />
		</div>
	);
}
