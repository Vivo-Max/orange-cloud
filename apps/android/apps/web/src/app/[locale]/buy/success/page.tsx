import type { Metadata } from "next";
import { setRequestLocale } from "next-intl/server";
import SiteHeader from "@/components/SiteHeader";
import SiteFooter from "@/components/SiteFooter";
import BuyCodeResult from "@/components/BuyCodeResult";
import { getBuyContent } from "@/lib/buy/content";

export const metadata: Metadata = { robots: { index: false, follow: false } };

export default async function BuySuccessPage({
	params,
	searchParams,
}: {
	params: Promise<{ locale: string }>;
	searchParams: Promise<{ session_id?: string }>;
}) {
	const { locale } = await params;
	const { session_id } = await searchParams;
	setRequestLocale(locale);
	const c = getBuyContent(locale);

	return (
		<div className="theme-light">
			<section className="sky-band band-dawn dawn-glow min-h-screen overflow-hidden">
				<SiteHeader />
				<div className="relative mx-auto max-w-[560px] px-6 pb-24 pt-32 lg:pt-40">
					<BuyCodeResult sessionId={session_id ?? null} scheme="orangecloud://redeem" strings={c.success} />
				</div>
				<SiteFooter />
			</section>
		</div>
	);
}
