import type { Metadata } from "next";
import { setRequestLocale } from "next-intl/server";
import SiteHeader from "@/components/SiteHeader";
import SiteFooter from "@/components/SiteFooter";
import Reveal from "@/components/Reveal";
import BindEmailForm from "@/components/BindEmailForm";
import { getBuyContent } from "@/lib/buy/content";

export const metadata: Metadata = { robots: { index: false, follow: false } };

export default async function BindEmailPage({ params, searchParams }: { params: Promise<{ locale: string }>, searchParams: Promise<{ code?: string }> }) {
	const { locale } = await params;
	const code = (await searchParams).code;

	setRequestLocale(locale);
	const c = getBuyContent(locale);

	return (
		<div className="theme-light">
			<section className="sky-band band-dawn dawn-glow min-h-screen overflow-hidden">
				<SiteHeader />
				<div className="relative mx-auto max-w-[560px] px-6 pb-24 pt-28 lg:pt-36">
					<Reveal index={0}>
						<h1 className="f-display text-[32px] font-extrabold leading-[1.1] t-primary sm:text-[38px]">
							{c.bind.title}
						</h1>
						<p className="mt-4 text-[15px] leading-relaxed t-secondary">{c.bind.sub}</p>
					</Reveal>
					<Reveal index={1}>
						<div className="mt-8">
							<BindEmailForm s={c.bind} pre_code={code} />
						</div>
					</Reveal>
				</div>
				<SiteFooter />
			</section>
		</div>
	);
}
