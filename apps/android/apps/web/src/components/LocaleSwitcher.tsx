"use client";

import { useLocale } from "next-intl";
import { usePathname, useRouter } from "@/i18n/navigation";
import { routing, type AppLocale } from "@/i18n/routing";

const LOCALE_NAMES: Record<AppLocale, string> = {
	en: "English",
	"zh-Hans": "简体中文",
	"zh-Hant": "繁體中文（台灣）",
	"zh-HK": "繁體中文（香港）",
	ja: "日本語",
	"es-MX": "Español (México)",
	ko: "한국어",
	"pt-BR": "Português (Brasil)",
	"pt-PT": "Português (Portugal)",
	de: "Deutsch",
	fr: "Français",
	ar: "العربية",
	tr: "Türkçe",
};

export default function LocaleSwitcher({ label }: { label: string }) {
	const locale = useLocale();
	const router = useRouter();
	const pathname = usePathname();

	return (
		<select
			className="locale-select"
			value={locale}
			aria-label={label}
			onChange={(event) => {
				router.replace(pathname, { locale: event.target.value as AppLocale });
			}}
		>
			{routing.locales.map((l) => (
				<option key={l} value={l}>
					{LOCALE_NAMES[l]}
				</option>
			))}
		</select>
	);
}
