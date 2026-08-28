import type { Metadata } from "next";
import { setRequestLocale } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import SiteHeader, { GITHUB_URL } from "@/components/SiteHeader";
import SiteFooter from "@/components/SiteFooter";
import Reveal from "@/components/Reveal";
import BuyCheckout from "@/components/BuyCheckout";
import { getBuyContent } from "@/lib/buy/content";

// 单一市场渠道页，不进搜索索引。
export const metadata: Metadata = { robots: { index: false, follow: false } };

export default async function BuyPage({ params }: { params: Promise<{ locale: string }> }) {
	const { locale } = await params;
	setRequestLocale(locale);
	const c = getBuyContent(locale);

	return (
		<div className="theme-light">
			<section className="sky-band band-dawn dawn-glow min-h-screen overflow-hidden">
				<SiteHeader />
				<div className="relative mx-auto max-w-[760px] px-6 pb-20 pt-28 lg:pt-36">
					<Reveal index={0}>
						<span className="glass r-chip inline-flex items-center gap-2 rounded-full px-4 py-1.5 text-[13px] font-medium t-secondary">
							<span
								className="h-[6px] w-[6px] rounded-full"
								style={{ background: "var(--oc-orange)", boxShadow: "0 0 6px rgba(244,129,32,0.7)" }}
							/>
							{c.kicker}
						</span>
					</Reveal>
					<Reveal index={1}>
						<h1 className="f-display mt-5 text-[36px] font-extrabold leading-[1.1] t-primary sm:text-[44px]">
							{c.title}
						</h1>
					</Reveal>
					<Reveal index={2}>
						<p className="mt-4 max-w-[46ch] text-[16px] leading-relaxed t-secondary">{c.sub}</p>
					</Reveal>

					{/* 定价玻璃岛 —— 页面的单一视觉锚点 */}
					<Reveal index={3}>
						<div className="glass r-island mt-10 p-7 sm:p-9">
							<div className="flex flex-col items-baseline gap-3">
								<span className="f-display tabular text-[52px] font-extrabold leading-none t-primary">{c.price}</span>
								<span className="text-[13.5px] t-tertiary">{c.priceCaption}</span>
							</div>

							<h2 className="mt-7 text-[14px] font-semibold t-primary">{c.includesTitle}</h2>
							<ul className="mt-4 grid gap-3 sm:grid-cols-2">
								{c.includes.map((it) => (
									<li key={it} className="flex items-start gap-2.5 text-[14.5px] t-secondary">
										<svg className="mt-1 flex-none" width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="#F48120" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
											<path d="M2.5 7.5 5.5 10.5 11.5 3.5" />
										</svg>
										{it}
									</li>
								))}
							</ul>
							<p className="mt-5 text-[13px] t-tertiary">{c.deviceNote}</p>

							<div className="mt-7">
								<BuyCheckout
									locale={locale}
									label={c.buyButton}
									loadingText={c.buyLoading}
									errorText={c.buyError}
									payNote={c.payNote}
								/>
							</div>
						</div>
					</Reveal>

					{/* 怎么用 */}
					<Reveal index={4}>
						<div className="mt-12">
							<h2 className="f-display text-[22px] font-bold t-primary">{c.howTitle}</h2>
							<ol className="mt-5 grid gap-4 sm:grid-cols-3">
								{c.howSteps.map((step, i) => (
									<li key={i} className="glass r-island p-5">
										<span className="f-display text-[20px] font-bold" style={{ color: "var(--oc-orange)" }}>
											{i + 1}
										</span>
										<p className="mt-2 text-[14px] leading-relaxed t-secondary">{step}</p>
									</li>
								))}
							</ol>
							<p className="mt-6 text-[13px] t-tertiary">{c.recoverNote}</p>
							<p className="mt-2 text-[13px]">
								<a
									href={GITHUB_URL}
									target="_blank"
									rel="noopener noreferrer"
									className="link-quiet"
									style={{ textDecoration: "underline" }}
								>
									{c.ossText}
								</a>
							</p>
							<p className="mt-2 text-[13px]">
								<Link href="/buy/refund" className="link-quiet" style={{ textDecoration: "underline" }}>
									{c.refundEntry}
								</Link>
							</p>
							<p className="mt-2 text-[13px]">
								<Link href="/buy/reset" className="link-quiet" style={{ textDecoration: "underline" }}>
									{c.resetEntry}
								</Link>
							</p>
							<p className="mt-2 text-[13px]">
								<Link href="/buy/recover" className="link-quiet" style={{ textDecoration: "underline" }}>
									{c.recoverEntry}
								</Link>
							</p>
							<p className="mt-2 text-[13px]">
								<Link href="/buy/bind" className="link-quiet" style={{ textDecoration: "underline" }}>
									{c.bindEntry}
								</Link>
							</p>
						</div>
					</Reveal>
				</div>
				<SiteFooter />
			</section>
		</div>
	);
}
