// 站点可索引 URL 的唯一清单：sitemap.xml 与 IndexNow 推送都读这里，避免两处各写一份而漂移。
//
// 每个条目带 `updated`（内容版本，YYYY-MM-DD）：
//   - sitemap 用它做 <lastmod>；
//   - IndexNow 用它判断「这个 URL 自上次推送后有没有变」（见 src/lib/indexnow/）。
// 所以改了页面内容就要把日期往前推，否则搜索引擎不会被通知。

import { routing } from "../../i18n/routing";
import { GUIDE_LOCALES, guidePath, guidesFor } from "../guides/guides";

export const SITE_URL = "https://oss.omail.us.kg";
export const SITE_HOST = "oss.omail.us.kg";

export type SiteUrlEntry = {
	url: string;
	/** 内容版本 'YYYY-MM-DD'：sitemap 的 lastmod + IndexNow 的变更判定 */
	updated: string;
	/** 同一内容的各语言 URL（hreflang），键为 locale；无多语言版本时省略 */
	languages?: Record<string, string>;
};

/**
 * 落地页 / 法务页——**改动这些页面（含 src/messages 的文案）后，手动把日期改成当天**。
 * 以前 sitemap 这几条的 lastmod 写的是 `new Date()`（每次抓取都是"刚刚更新"），
 * 既不真实，也没法用来判断该不该推送 IndexNow。
 */
const STATIC_PAGES: { path: string; updated: string }[] = [
	{ path: "", updated: "2026-08-08" },
	{ path: "/privacy", updated: "2026-06-12" },
	{ path: "/terms", updated: "2026-06-12" },
	{ path: "/contact", updated: "2026-06-12" },
];

/** 站内绝对 URL：默认语言无前缀（与 next-intl 的 as-needed 一致） */
export function localeUrl(locale: string, path: string): string {
	const prefix = locale === routing.defaultLocale ? "" : `/${locale}`;
	return `${SITE_URL}${prefix}${path}`;
}

function newest(dates: string[]): string {
	return dates.reduce((a, b) => (a > b ? a : b));
}

/** 全站可索引条目（静态页 + 指南索引 + 指南文章），带 hreflang 分组。 */
export function siteUrls(): SiteUrlEntry[] {
	const statics: SiteUrlEntry[] = STATIC_PAGES.map(({ path, updated }) => ({
		url: localeUrl(routing.defaultLocale, path),
		updated,
		languages: Object.fromEntries(routing.locales.map((locale) => [locale, localeUrl(locale, path)])),
	}));

	// 指南板块只有英文与简体中文两套，且文章各写各的：
	// 只有索引页互为 alternates，文章不列，避免指向不存在的语言版本
	const guides: SiteUrlEntry[] = GUIDE_LOCALES.flatMap((locale) => {
		const list = guidesFor(locale);
		return [
			{
				url: `${SITE_URL}${guidePath(locale, "/guides")}`,
				// 索引页跟着「最新的一篇」走（原来取 list[0]，而清单是按时间正序排的，取到的是最老的一篇）
				updated: newest(list.map((guide) => guide.updated)),
				languages: Object.fromEntries(
					GUIDE_LOCALES.map((l) => [l, `${SITE_URL}${guidePath(l, "/guides")}`]),
				),
			},
			...list.map((guide) => ({
				url: `${SITE_URL}${guidePath(locale, `/guides/${guide.slug}`)}`,
				updated: guide.updated,
			})),
		];
	});

	return [...statics, ...guides];
}

/**
 * 摊平成「一个 URL 一行」：IndexNow 推的是真实页面地址，
 * 所以静态页的 13 个语言版本各算一条（sitemap 里它们是 alternates，只出现一次）。
 */
export function indexableUrls(): { url: string; updated: string }[] {
	const seen = new Map<string, string>();
	for (const entry of siteUrls()) {
		seen.set(entry.url, entry.updated);
		for (const url of Object.values(entry.languages ?? {})) seen.set(url, entry.updated);
	}
	return [...seen].map(([url, updated]) => ({ url, updated }));
}
