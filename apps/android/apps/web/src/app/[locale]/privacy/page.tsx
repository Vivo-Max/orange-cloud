import type { Metadata } from "next";
import { getTranslations, setRequestLocale } from "next-intl/server";
import SiteHeader from "@/components/SiteHeader";
import SiteFooter from "@/components/SiteFooter";

export async function generateMetadata({
	params,
}: {
	params: Promise<{ locale: string }>;
}): Promise<Metadata> {
	const { locale } = await params;
	const t = await getTranslations({ locale, namespace: "privacy" });
	return { title: `${t("title")} — Orange Cloud` };
}

export default async function PrivacyPage({ params }: { params: Promise<{ locale: string }> }) {
	const { locale } = await params;
	setRequestLocale(locale);
	const t = await getTranslations("privacy");
	const sections = t.raw("sections") as Array<{ h: string; p: string }>;

	return (
		<div className="theme-light sky-band band-dawn dawn-glow flex min-h-screen flex-col overflow-x-clip">
			<SiteHeader />
			<main className="relative mx-auto w-full max-w-[760px] flex-1 px-6 pb-20 pt-32">
				<h1 className="f-display text-[34px] font-bold t-primary">{t("title")}</h1>
				<div className="glass r-island mt-8 px-7 sm:px-9">
					{sections.map((section, i) => (
						<section
							key={section.h}
							className="py-7"
							style={i > 0 ? { borderTop: "0.5px solid var(--divider)" } : undefined}
						>
							<h2 className="text-[18px] font-semibold t-primary">{section.h}</h2>
							<p className="mt-2.5 text-[15px] leading-relaxed t-secondary">{section.p}</p>
						</section>
					))}
				</div>
				<p className="mt-6 text-[13px] t-tertiary">{t("updated")}</p>
			</main>
			<SiteFooter />
		</div>
	);
}
