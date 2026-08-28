import Image from "next/image";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { shotLocale } from "@/i18n/routing";
import SiteHeader from "@/components/SiteHeader";
import SiteFooter from "@/components/SiteFooter";
import AppStoreBadge, { TESTFLIGHT_URL, APP_STORE_COMING } from "@/components/AppStoreBadge";
import HomeRankBadge from "@/components/HomeRankBadge";
import AndroidBadge from "@/components/AndroidBadge";
import { getBuyContent } from "@/lib/buy/content";
import PhoneDemo, { type PhoneStrings } from "@/components/PhoneDemo";
import HorizonArc from "@/components/HorizonArc";
import Reveal from "@/components/Reveal";
import Stars from "@/components/Stars";
import FeatureIcon from "@/components/FeatureIcon";
import ChangelogTabs from "@/components/ChangelogTabs";
import { decoratedReleases, localize } from "@orange-cloud/changelog";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { getReleaseState, type ReleaseState } from "@/lib/livestate/store";
import { apoluUrl } from "@/lib/sibling/apolu";

const SHOT_FILES = [
	"01_dashboard",
	"02_analytics",
	"03_dns",
	"04_storage",
	"05_daynight",
	"06_workers_tail",
	"07_widgets",
	"08_login",
];

const FEATURE_ICONS = ["dns", "analytics", "tail", "storage", "waf", "tunnel", "widget", "accounts"];

// 更新历史按 D1 release_state 门控（审核中 / 已上架）；ISR 每 60s 重新生成以反映 ASC/Play 信号（约 1 分钟内翻牌）。
export const revalidate = 60;

export default async function HomePage({ params }: { params: Promise<{ locale: string }> }) {
	const { locale } = await params;
	setRequestLocale(locale);
	const t = await getTranslations();
	const shots = shotLocale(locale);
	const buy = getBuyContent(locale);

	const phoneStrings = t.raw("phone") as PhoneStrings;
	const trustCards = t.raw("trust.cards") as Array<{ t: string; b: string }>;
	const galleryAlts = t.raw("gallery.alts") as string[];
	const featureItems = t.raw("features.items") as Array<{ t: string; b: string }>;
	const freeItems = t.raw("pro.freeItems") as string[];
	const proItems = t.raw("pro.proItems") as string[];
	let releaseState: ReleaseState = {};
	try {
		const { env } = getCloudflareContext();
		if (env.IAP_DB) releaseState = await getReleaseState(env.IAP_DB);
	} catch {
		// 构建期 / 无 D1 绑定：回退到 live 标志门控（无 pending，仅展示 live:true 条目）
	}
	const renderTrack = (track: "ios" | "android") =>
		decoratedReleases(track, releaseState[track]).map(({ release: r, status }) => ({
			version: r.version,
			date: r.date,
			channel: r.channel,
			status,
			items: r.items.map((it) => ({
				title: localize(it.title, locale) ?? "",
				detail: it.detail ? localize(it.detail, locale) : undefined,
			})),
		}));
	const iosReleases = renderTrack("ios");
	const androidReleases = renderTrack("android");

	return (
		<>
			{/* ============ 昼（亮主题）：晨 → 昼 → 黄昏 ============ */}
			<div className="theme-light">
				{/* —— Hero · 清晨 —— */}
				<section className="sky-band band-dawn dawn-glow overflow-hidden">
					<SiteHeader />
					<div className="relative mx-auto grid max-w-[1120px] items-center gap-14 px-6 pb-20 pt-28 lg:grid-cols-[1fr_auto] lg:pb-24 lg:pt-36">
						<div>
							<Reveal index={0}>
								<span className="glass r-chip inline-flex items-center gap-2 rounded-full px-4 py-1.5 text-[13px] font-medium t-secondary">
									<span
										className="h-[6px] w-[6px] rounded-full"
										style={{ background: "var(--oc-orange)", boxShadow: "0 0 6px rgba(244,129,32,0.7)" }}
									/>
									{t("hero.kicker")}
								</span>
							</Reveal>
							<Reveal index={1}>
								<h1 className="f-display mt-5 text-[42px] font-extrabold leading-[1.1] t-primary sm:text-[56px]">
									{t("hero.title1")}
									<br />
									{t("hero.title2")}
								</h1>
							</Reveal>
							<Reveal index={2}>
								<p className="mt-5 max-w-[46ch] text-[17px] leading-relaxed t-secondary">{t("hero.sub")}</p>
							</Reveal>
							<Reveal index={3}>
								<div className="mt-8 flex flex-wrap items-start gap-4">
									<AppStoreBadge
										locale={locale}
										alt={t("badge.alt")}
										comingLabel={t("badge.comingLabel")}
										coming={APP_STORE_COMING}
									/>
									{/* Android：大陆 → 官网下载 APK + Google Play；其余 → 仅 Google Play。客户端按 /api/geo 判定。 */}
									<AndroidBadge locale={locale} strings={buy.download} />
								</div>
								<p className="mt-3 text-[13px] t-tertiary">{t("hero.note")}</p>
							</Reveal>
							{/* 按访客 IP 所在地区展示当前 App Store 排名（未上榜/非追踪地区不渲染）。 */}
							<HomeRankBadge
								className="mt-5"
								locale={locale}
								badgeTemplate={t.raw("rank.badge")}
								ariaLabel={t("rank.ariaLabel")}
							/>
							<Reveal index={4}>
								<HorizonArc className="mt-10 max-w-[520px]" />
							</Reveal>
						</div>
						<Reveal index={2} className="justify-self-center">
							<PhoneDemo locale={locale} s={phoneStrings} />
						</Reveal>
					</div>
				</section>

				{/* —— 安全 · 上午 —— */}
				<section className="sky-band band-morning">
					<div className="mx-auto max-w-[1120px] px-6 py-20">
						<Reveal index={0}>
							<h2 className="f-display text-[32px] font-bold t-primary sm:text-[38px]">{t("trust.title")}</h2>
							<p className="mt-3 max-w-[56ch] text-[16px] t-secondary">{t("trust.sub")}</p>
						</Reveal>
						<div className="mt-10 grid gap-4 md:grid-cols-3">
							{trustCards.map((card, i) => (
								<Reveal key={card.t} index={i + 1}>
									<div className="glass r-island h-full p-6">
										<div
											className="flex h-10 w-10 items-center justify-center rounded-full t-secondary"
											style={{ background: "var(--t-quaternary)" }}
										>
											{i === 0 ? (
												<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
													<rect x="4" y="10.5" width="16" height="10" rx="2.5" />
													<path d="M8 10.5V7.5a4 4 0 0 1 8 0v3" />
													<circle cx="12" cy="15.5" r="1.6" fill="currentColor" stroke="none" />
												</svg>
											) : i === 1 ? (
												<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
													<circle cx="9" cy="9" r="5.5" />
													<path d="M13 13l7 7M17.5 17.5l2-2M15 15l2-2" />
												</svg>
											) : (
												<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
													<path d="M5 12h14M5 12a7 7 0 0 1 14 0M5 12a7 7 0 0 0 14 0" />
													<circle cx="5" cy="12" r="1.4" fill="currentColor" stroke="none" />
													<circle cx="19" cy="12" r="1.4" fill="currentColor" stroke="none" />
												</svg>
											)}
										</div>
										<h3 className="mt-4 text-[17px] font-semibold t-primary">{card.t}</h3>
										<p className="mt-2 text-[14px] leading-relaxed t-secondary">{card.b}</p>
									</div>
								</Reveal>
							))}
						</div>
					</div>
				</section>

				{/* —— 日轨画廊 · 白昼 —— */}
				<section className="sky-band band-day">
					<div className="mx-auto max-w-[1120px] px-6 pt-20">
						<Reveal index={0}>
							<h2 className="f-display text-[32px] font-bold t-primary sm:text-[38px]">{t("gallery.title")}</h2>
							<p className="mt-3 max-w-[56ch] text-[16px] t-secondary">{t("gallery.sub")}</p>
						</Reveal>
					</div>
					<Reveal index={1}>
						<div className="gallery mt-10">
							{SHOT_FILES.map((file, i) => (
								<div key={file} className="shot">
									<Image
										src={`/shots/${shots}/${file}.jpg`}
										alt={galleryAlts[i]}
										width={630}
										height={1368}
										loading="lazy"
										unoptimized
									/>
								</div>
							))}
						</div>
					</Reveal>
				</section>

				{/* —— 功能宫格 + iPad · 黄昏 —— */}
				<section className="sky-band band-dusk">
					<div className="mx-auto max-w-[1120px] px-6 py-20">
						<Reveal index={0}>
							<h2 className="f-display text-[32px] font-bold t-primary sm:text-[38px]">{t("features.title")}</h2>
							<p className="mt-3 max-w-[56ch] text-[16px] t-secondary">{t("features.sub")}</p>
						</Reveal>
						<div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
							{featureItems.map((item, i) => (
								<Reveal key={item.t} index={(i % 4) + 1}>
									<div className="glass r-island h-full p-5">
										<span className="t-secondary">
											<FeatureIcon name={FEATURE_ICONS[i]} />
										</span>
										<h3 className="mt-3 text-[16px] font-semibold t-primary">{item.t}</h3>
										<p className="mt-1.5 text-[13.5px] leading-relaxed t-secondary">{item.b}</p>
									</div>
								</Reveal>
							))}
						</div>

						<Reveal index={1}>
							<div className="glass r-island mt-12 grid items-center gap-8 overflow-hidden p-7 lg:grid-cols-[1fr_1.15fr] lg:p-10">
								<div>
									<h2 className="f-display text-[28px] font-bold t-primary sm:text-[32px]">{t("ipad.title")}</h2>
									<p className="mt-3 max-w-[44ch] text-[15px] leading-relaxed t-secondary">{t("ipad.body")}</p>
								</div>
								{/* 竖版成品图裁掉自带文案区，只露 iPad 设备部分 */}
								<div className="h-[300px] overflow-hidden rounded-2xl shadow-[0_18px_44px_rgba(31,18,8,0.22)] sm:h-[420px]">
									<Image
										src={`/shots/${shots}/ipad_split.jpg`}
										alt={t("ipad.title")}
										width={1032}
										height={1376}
										loading="lazy"
										unoptimized
										className="h-full w-full object-cover object-[50%_72%]"
									/>
								</div>
							</div>
						</Reveal>
					</div>
				</section>
			</div>

			{/* ============ 日落缝：落日把页面从昼带入夜 ============ */}
			<div className="band-sunset relative h-[220px] overflow-hidden" aria-hidden="true">
				{/* 沿地平线铺开的余晖（横向椭圆，而非圆球） */}
				<div
					className="absolute left-1/2 top-[46%] h-[150px] w-[760px] max-w-[160vw] -translate-x-1/2 -translate-y-1/2"
					style={{
						borderRadius: "50%",
						background: "radial-gradient(50% 50% at 50% 50%, rgba(255,200,130,0.5) 0%, rgba(255,170,90,0) 70%)",
					}}
				/>
				<div
					className="absolute left-1/2 top-[46%] h-8 w-8 -translate-x-1/2 -translate-y-1/2 rounded-full"
					style={{ background: "#FFDCAC", boxShadow: "0 0 44px 14px rgba(255,180,100,0.65)" }}
				/>
			</div>

			{/* ============ 夜（暗主题）：入夜 → 深夜 ============ */}
			<div className="theme-dark">
				{/* —— 免费 / Pro · 入夜 —— */}
				<section className="sky-band band-ember">
					<div className="mx-auto max-w-[1120px] px-6 py-20">
						<Reveal index={0}>
							<h2 className="f-display text-[32px] font-bold t-primary sm:text-[38px]">{t("pro.title")}</h2>
							<p className="mt-3 max-w-[56ch] text-[16px] t-secondary">{t("pro.sub")}</p>
						</Reveal>
						<div className="mt-10 grid gap-4 md:grid-cols-2">
							<Reveal index={1}>
								<div className="glass r-island h-full p-7">
									<h3 className="f-display text-[22px] font-bold t-primary">{t("pro.freeName")}</h3>
									<ul className="mt-5 space-y-3">
										{freeItems.map((item) => (
											<li key={item} className="flex items-start gap-2.5 text-[15px] t-secondary">
												<svg className="mt-1 flex-none" width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
													<path d="M2.5 7.5 5.5 10.5 11.5 3.5" />
												</svg>
												{item}
											</li>
										))}
									</ul>
								</div>
							</Reveal>
							<Reveal index={2}>
								<div
									className="glass r-island h-full p-7"
									style={{ borderColor: "rgba(244,129,32,0.35)" }}
								>
									<h3 className="f-display flex items-center gap-2.5 text-[22px] font-bold t-primary">
										Orange Cloud
										<span
											className="rounded-full px-2.5 py-0.5 text-[12px] font-bold text-white"
											style={{ background: "var(--oc-orange)" }}
										>
											{t("pro.proName")}
										</span>
									</h3>
									<ul className="mt-5 space-y-3">
										{proItems.map((item) => (
											<li key={item} className="flex items-start gap-2.5 text-[15px] t-secondary">
												<svg className="mt-1 flex-none" width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="#F48120" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
													<path d="M2.5 7.5 5.5 10.5 11.5 3.5" />
												</svg>
												{item}
											</li>
										))}
									</ul>
								</div>
							</Reveal>
						</div>
						<Reveal index={3}>
							<p className="mt-6 text-[13px] t-tertiary">{t("pro.note")}</p>
						</Reveal>
					</div>
				</section>

				{/* —— 版本历史 · 深夜 —— */}
				<section className="sky-band band-night">
					<div className="mx-auto max-w-[1120px] px-6 py-20">
						<Reveal index={0}>
							<div className="flex flex-wrap items-end justify-between gap-6">
								<div>
									<h2 className="f-display text-[32px] font-bold t-primary sm:text-[38px]">{t("changelog.title")}</h2>
									<p className="mt-3 max-w-[52ch] text-[16px] t-secondary">{t("changelog.sub")}</p>
								</div>
								<a
									href={TESTFLIGHT_URL}
									target="_blank"
									rel="noopener noreferrer"
									className="inline-flex shrink-0 items-center gap-2 rounded-[12px] px-5 py-2.5 text-[15px] font-semibold text-white no-underline transition-transform duration-150 ease-out hover:scale-[1.03] active:scale-[0.97]"
									style={{ background: "var(--oc-orange)" }}
								>
									<svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
										<path d="M21.7 2.3a1 1 0 0 0-1.1-.2L3.1 9.1a1 1 0 0 0 .2 1.9l7.7 1.5 1.5 7.7a1 1 0 0 0 1.9.2L21.9 3.4a1 1 0 0 0-.2-1.1z" />
									</svg>
									{t("changelog.tfButton")}
								</a>
							</div>
						</Reveal>

						<ChangelogTabs
							ios={iosReleases}
							android={androidReleases}
							iosNote={t("changelog.tfNote")}
							androidSoon={t("changelog.androidSoon")}
							statusInReview={t("changelog.statusInReview")}
							labelIOS="iOS"
							labelAndroid="Android"
						/>
					</div>
				</section>

				{/* —— 同一个开发者：Apolu · 深夜 —— */}
				<section className="sky-band band-night" aria-labelledby="sibling-heading">
					<div className="mx-auto max-w-[820px] px-6 pb-4 pt-16">
						<Reveal index={0}>
							<a
								href={apoluUrl(locale)}
								target="_blank"
								rel="noopener"
								className="glass r-island sibling-card group flex flex-col items-center gap-5 p-7 text-center no-underline transition-colors duration-200 sm:flex-row sm:items-center sm:text-start"
							>
								<Image
									src="/icons/apolu-180.png"
									alt=""
									width={180}
									height={180}
									unoptimized
									className="h-16 w-16 shrink-0 rounded-[14px] shadow-[0_8px_22px_rgba(0,0,0,0.28)]"
								/>
								<div className="min-w-0">
									<p className="text-[11px] font-semibold uppercase tracking-[0.18em] t-secondary">{t("sibling.eyebrow")}</p>
									<h2 id="sibling-heading" className="f-display mt-1.5 text-[20px] font-semibold t-primary">
										{t("sibling.name")}
										<span className="t-secondary"> · </span>
										<span className="font-normal t-secondary">{t("sibling.tagline")}</span>
									</h2>
									<p className="mt-2 text-[15px] leading-relaxed t-secondary">{t("sibling.body")}</p>
									<p className="mt-3 text-[14px] font-semibold" style={{ color: "var(--oc-orange)" }}>
										{t("sibling.cta")}
										<span className="ms-1 inline-block transition-transform duration-200 rtl:rotate-180 group-hover:translate-x-1 rtl:group-hover:-translate-x-1" aria-hidden="true">
											&rarr;
										</span>
									</p>
								</div>
							</a>
						</Reveal>
					</div>
				</section>

				{/* —— 终幕 CTA + 页脚 · 深夜 —— */}
				<section className="sky-band band-night relative">
					<Stars />
					<div className="relative mx-auto max-w-[1120px] px-6 pb-10 pt-28 text-center">
						<Reveal index={0}>
							<Image
								src="/icons/icon-180.png"
								alt="Orange Cloud"
								width={96}
								height={96}
								unoptimized
								className="mx-auto rounded-[24px] shadow-[0_14px_38px_rgba(244,129,32,0.25)]"
							/>
						</Reveal>
						<Reveal index={1}>
							<h2 className="f-display mt-8 text-[34px] font-bold t-primary sm:text-[42px]">{t("cta.title")}</h2>
							<p className="mt-3 text-[17px] t-secondary">{t("cta.sub")}</p>
						</Reveal>
						<Reveal index={2}>
							<div className="mt-9 flex flex-col items-center gap-4">
								<AppStoreBadge
									locale={locale}
									alt={t("badge.alt")}
									comingLabel={t("badge.comingLabel")}
									coming={APP_STORE_COMING}
								/>
								<AndroidBadge locale={locale} strings={buy.download} />
							</div>
							<p className="mt-5 text-[13px] t-tertiary">{t("cta.requirement")}</p>
						</Reveal>
					</div>
					<div className="relative mt-14">
						<SiteFooter />
					</div>
				</section>
			</div>
		</>
	);
}
