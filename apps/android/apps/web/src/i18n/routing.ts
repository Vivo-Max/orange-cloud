import { defineRouting } from "next-intl/routing";

export const routing = defineRouting({
	locales: ["en", "zh-Hans", "zh-Hant", "zh-HK", "ja", "es-MX", "ko", "pt-BR", "pt-PT", "de", "fr", "ar", "tr"],
	defaultLocale: "en",
	localePrefix: "as-needed",
});

export type AppLocale = (typeof routing.locales)[number];

/** 截图按四套资源存放，zh-HK 复用 zh-Hant（繁体截图） */
export function shotLocale(locale: string): "en" | "zh-Hans" | "zh-Hant" | "ja" {
	switch (locale) {
		case "zh-Hans":
			return "zh-Hans";
		case "zh-Hant":
		case "zh-HK":
			return "zh-Hant";
		case "ja":
			return "ja";
		default:
			return "en";
	}
}
