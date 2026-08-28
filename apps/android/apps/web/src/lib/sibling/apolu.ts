import { routing, type AppLocale } from "../../i18n/routing";

/**
 * 首页「同一个开发者」卡片指向 apolu.app 的落点。
 *
 * 两站语种列表不重合，**必须显式列表映射、不能拼接 locale**：
 * apolu.app 没有 zh-HK / es-MX / pt-PT / ar，直接拼会让这四个语种的访客与爬虫撞 404。
 * 兜底规则：zh-HK → 繁体、es-MX → 通用西语、pt-PT → 巴西葡语、ar → 英文（apolu 无阿语）。
 */
export const APOLU_URL: Record<AppLocale, string> = {
	en: "https://apolu.app/",
	"zh-Hans": "https://apolu.app/zh-Hans",
	"zh-Hant": "https://apolu.app/zh-Hant",
	"zh-HK": "https://apolu.app/zh-Hant",
	ja: "https://apolu.app/ja",
	"es-MX": "https://apolu.app/es",
	ko: "https://apolu.app/ko",
	"pt-BR": "https://apolu.app/pt-BR",
	"pt-PT": "https://apolu.app/pt-BR",
	de: "https://apolu.app/de",
	fr: "https://apolu.app/fr",
	ar: "https://apolu.app/",
	tr: "https://apolu.app/tr",
};

export function apoluUrl(locale: string): string {
	return APOLU_URL[locale as AppLocale] ?? APOLU_URL[routing.defaultLocale];
}
