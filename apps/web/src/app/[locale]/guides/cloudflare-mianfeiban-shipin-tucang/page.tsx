import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { setRequestLocale } from "next-intl/server";
import GuideShell, { type RelatedLink } from "@/components/guides/GuideShell";
import LargeFileRoutes from "@/components/guides/LargeFileRoutes";
import { guideBySlug, guidePath, GUIDE_LOCALE_ZH } from "@/lib/guides/guides";

const SITE_URL = "https://90dd.adsl8.workers.dev";
const guide = guideBySlug("cloudflare-mianfeiban-shipin-tucang", GUIDE_LOCALE_ZH);
const PATH = guidePath(GUIDE_LOCALE_ZH, `/guides/${guide.slug}`);

const TERMS_CDN =
	"https://www.cloudflare.com/service-specific-terms-application-services/#content-delivery-network-free-pro-or-business";
const DOCS_CACHE_DEFAULT = "https://developers.cloudflare.com/cache/concepts/default-cache-behavior/";
const DOCS_CACHE_EXT =
	"https://developers.cloudflare.com/cache/concepts/default-cache-behavior/#default-cached-file-extensions";
const DOCS_CACHE_LIMITS =
	"https://developers.cloudflare.com/cache/concepts/default-cache-behavior/#cacheable-size-limits";
const DOCS_413 =
	"https://developers.cloudflare.com/support/troubleshooting/http-status-codes/4xx-client-error/error-413/";
const DOCS_R2_PRICING = "https://developers.cloudflare.com/r2/pricing/";
const DOCS_R2_PUBLIC = "https://developers.cloudflare.com/r2/buckets/public-buckets/";
const DOCS_STREAM_PRICING = "https://developers.cloudflare.com/stream/pricing/";
const DOCS_IMAGES_PRICING = "https://developers.cloudflare.com/images/pricing/";
const DOCS_CACHE_RULES = "https://developers.cloudflare.com/cache/how-to/cache-rules/";
const DOCS_CHINA = "https://developers.cloudflare.com/china-network/";

/** FAQ 一处定义：可见文本与 FAQPage JSON-LD 同源，保证逐字一致 */
const FAQ: Array<{ q: string; a: string }> = [
	{
		q: "Cloudflare 免费版有流量限制吗？",
		a: "免费方案没有公布的月流量额度，请求数和带宽本身不计费，所以「用超了要补钱」这件事不存在。真正的边界有两类：一是服务条款里那条内容限制——不使用指定付费服务就拿 CDN 分发视频、或分发比例失衡的大文件，Cloudflare 有权停用或限制你的 CDN；二是硬性技术上限——单次请求体 100 MB，可缓存文件 512 MB。",
	},
	{
		q: "用 Cloudflare 免费版做图床会被封吗？",
		a: "个人博客配图、站点自己的静态资源这种量级，正是 CDN 设计用来承载的东西，不用担心。会出问题的是另一种用法：把域名当成公开的图片外链空间，图片流量占了这个域名的绝大部分，条款里说的「比例失衡的图片」就是这种情况。真要做规模化图床，官方路径是 Images，或者 R2 加自定义域，后者出网流量不计费。",
	},
	{
		q: "Cloudflare 免费版能放视频吗？",
		a: "不能。条款把视频单独拎出来写死了：非企业客户要通过 CDN 分发视频，必须使用 Cloudflare 指定的付费服务，Stream 就是为此而设的。技术上它当然跑得起来，MP4 本来就在默认缓存的扩展名表里，但跑得起来不等于被允许，Cloudflare 保留停用或限制 CDN 的权利。",
	},
	{
		q: "上传文件超过 100 MB 报 413 怎么办？",
		a: "413 是 Cloudflare 代理层的请求体上限，免费和 Pro 方案是 100 MB，Business 是 200 MB，企业版默认 500 MB 起。这个值只能在域名的 Network 页往下调，不能往上调。可行的办法有三个：把上传改成分片；把上传用的那个子域改成灰云直连以绕开代理，代价是源站真实 IP 会暴露给上传方；或者升级套餐。",
	},
	{
		q: "R2 的出网流量真的完全免费吗？",
		a: "是的，R2 没有出网带宽这一项计费，这也是它和多数对象存储最不一样的地方。要花钱的是存储和操作次数：每月 10 GB-月存储、100 万次 A 类操作、1,000 万次 B 类操作在免费额度内，超出后标准存储按 0.015 美元每 GB-月计。要注意免费额度只适用于标准存储类，不适用于低频访问存储类。",
	},
];

const RELATED: RelatedLink[] = [
	{
		href: "/guides/cloudflare-tunnel-neiwang-chuantou",
		label: "没有公网 IP，怎么用 Cloudflare Tunnel 做内网穿透？",
		note: "把 NAS 挂出去会撞上同一条条款，外加隧道自己的那几条限制。",
	},
	{
		href: "/guides/cloudflare-xiaohuangyun",
		label: "Cloudflare 的小黄云到底是什么？什么时候该关掉？",
		note: "上传大文件时把子域改成灰云直连是常见解法——先看清关掉代理会连带失去什么。",
	},
	{
		href: "/guides",
		label: "全部中文指南",
		note: "Cloudflare 的其它设置项：代理状态、回源、缓存与证书。",
	},
	{
		href: TERMS_CDN,
		label: "Cloudflare 应用服务专项条款：CDN 一节",
		note: "本文关于「允许与不允许」的全部依据，原文只有三句话。",
		external: true,
	},
	{
		href: DOCS_CACHE_DEFAULT,
		label: "Cloudflare 官方文档：默认缓存行为",
		note: "默认缓存的扩展名表、上传上限与可缓存文件上限都在这一页。",
		external: true,
	},
	{
		href: "/contact",
		label: "这篇有错？告诉我们",
		note: "勘误和提问都欢迎，每一封都会看。",
	},
];

export const metadata: Metadata = {
	metadataBase: new URL(SITE_URL),
	title: guide.title,
	description: guide.description,
	alternates: {
		canonical: PATH,
		languages: { "zh-Hans": PATH },
	},
	openGraph: {
		title: guide.title,
		description: guide.description,
		url: PATH,
		siteName: "Orange Cloud",
		type: "article",
		locale: "zh_CN",
		images: [{ url: "/og/zh-Hans.jpg", width: 1280, height: 640, alt: guide.h1 }],
	},
	twitter: {
		card: "summary_large_image",
		title: guide.title,
		description: guide.description,
		images: ["/og/zh-Hans.jpg"],
	},
};

export default async function FreePlanMediaGuideZh({ params }: { params: Promise<{ locale: string }> }) {
	const { locale } = await params;
	if (locale !== GUIDE_LOCALE_ZH) notFound();
	setRequestLocale(locale);

	const jsonLd = [
		{
			"@context": "https://schema.org",
			"@type": "TechArticle",
			headline: guide.h1,
			description: guide.description,
			url: `${SITE_URL}${PATH}`,
			inLanguage: "zh-Hans",
			datePublished: guide.updated,
			dateModified: guide.updated,
			image: `${SITE_URL}/og/zh-Hans.jpg`,
			author: { "@type": "Organization", name: "Orange Cloud", url: SITE_URL },
			publisher: { "@type": "Organization", name: "Orange Cloud", url: SITE_URL },
			mainEntityOfPage: { "@type": "WebPage", "@id": `${SITE_URL}${PATH}` },
		},
		{
			"@context": "https://schema.org",
			"@type": "FAQPage",
			mainEntity: FAQ.map((item) => ({
				"@type": "Question",
				name: item.q,
				acceptedAnswer: { "@type": "Answer", text: item.a },
			})),
		},
		{
			"@context": "https://schema.org",
			"@type": "BreadcrumbList",
			itemListElement: [
				{ "@type": "ListItem", position: 1, name: "指南", item: `${SITE_URL}${guidePath(GUIDE_LOCALE_ZH, "/guides")}` },
				{ "@type": "ListItem", position: 2, name: guide.h1, item: `${SITE_URL}${PATH}` },
			],
		},
	];

	return (
		<>
			<script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />
			<GuideShell
				locale={GUIDE_LOCALE_ZH}
				title={guide.h1}
				lede="跑得起来和被允许是两件事。默认缓存的扩展名表里就有 MP4，所以把电影丢上去当然能播——但条款那一页写着，不用指定的付费服务就拿免费版 CDN 分发视频，Cloudflare 有权把你的 CDN 关掉。"
				updated={guide.updated}
				readingTime={guide.readingTime}
				related={RELATED}
			>
				<div className="glass r-island note p-6 sm:p-7">
					<p>
						图片一般没问题，视频不行。免费、Pro、Business 的 CDN 不得用来分发视频，或让大文件占到失衡的比例，否则
						Cloudflare 有权停用它。
					</p>
				</div>

				<p>
					这个问题在中文社区被反复问，答案却总是各说各话——有人说用了三年没事，有人说一封邮件就被停了。两边说的都是真的，因为它们回答的其实是两个不同的问题：一个是「技术上能不能跑」，一个是「条款上允不允许」。把这两件事分开，剩下的就都清楚了。
				</p>

				<h2 id="why-it-runs">先说为什么它确实跑得起来</h2>
				<p>
					Cloudflare 的 CDN 默认按文件扩展名决定缓不缓存，而
					<a href={DOCS_CACHE_EXT} target="_blank" rel="noopener noreferrer">
						那张默认扩展名表
					</a>
					里明明白白列着 <code>MP4</code>、<code>MKV</code>、<code>WEBM</code>、<code>MP3</code>、<code>FLAC</code>，连{" "}
					<code>ISO</code>、<code>DMG</code>、<code>APK</code>、<code>EXE</code> 都在。
				</p>
				<p>
					所以把一部电影丢进源站的静态目录、域名上的小黄云开着，第一个访客拉走时回一次源，之后就命中缓存了，
					<code>cf-cache-status</code> 会老老实实返回 <code>HIT</code>。播放流畅、带宽不算钱，看上去一切正常。
				</p>
				<p>
					但那张表回答的是「哪些扩展名默认进缓存」。它是一份缓存行为说明，不是一份许可清单。允不允许这么用，写在另一份文件里。
				</p>

				<h2 id="the-terms">条款那一节到底写了什么</h2>
				<p>
					在 Cloudflare 的
					<a href={TERMS_CDN} target="_blank" rel="noopener noreferrer">
						应用服务专项条款
					</a>
					里，有一节标题就叫 Content Delivery Network (Free, Pro, or Business)。三句话讲完三件事：
				</p>
				<ul>
					<li>CDN 这项服务的定位，是缓存和分发网页与网站；</li>
					<li>
						非企业客户要通过 CDN 分发视频和其它大文件，必须使用 Cloudflare 为此提供的付费服务，条款点名的是 Developer
						Platform、Images 和 Stream；
					</li>
					<li>
						如果 Cloudflare 认为你在没有这些付费服务的情况下用 CDN 分发视频，或分发的图片、音频及其它大文件占了失衡的比例，它有权停用或限制你对
						CDN 的使用，也可以限制访客对某些资源的访问；执行前会尽合理努力提前通知。
					</li>
				</ul>
				<p>有两个细节值得单独拎出来说。</p>
				<p>
					<strong>一个数字都没有。</strong>{" "}
					中文帖子里流传过各种版本的「超过多少 GB 会被封」「免费版每月上限多少流量」，条款里一个阈值也找不到，用的词是「失衡的比例」。这既意味着一个小博客偶尔挂几个安装包不会有人来管，也意味着你没办法靠算流量来给自己开一张安全证明。判断的主动权在对方手里。
				</p>
				<p>
					<strong>那个编号已经没了。</strong>{" "}
					很多中文资料引用的是「2.8 条款」，那是旧版条款文档里的编号。现行的应用服务专项条款按标题分节，不再用数字编号，找的时候直接搜 Content
					Delivery Network 这一节即可。
				</p>

				<h2 id="hard-limits">两道谁都躲不掉的硬上限</h2>
				<p>条款划的是政策边界，另外还有两个数字属于技术层面的硬上限，跟你守不守条款无关，撞上就是撞上。</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">限制</th>
								<th scope="col">Free</th>
								<th scope="col">Pro</th>
								<th scope="col">Business</th>
								<th scope="col">Enterprise</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">单次请求体上限</th>
								<td>100 MB</td>
								<td>100 MB</td>
								<td>200 MB</td>
								<td>500 MB 起</td>
							</tr>
							<tr>
								<th scope="row">可缓存文件上限</th>
								<td>512 MB</td>
								<td>512 MB</td>
								<td>512 MB</td>
								<td>5 GB（默认）</td>
							</tr>
						</tbody>
					</table>
				</div>
				<h3 id="limit-upload">上传方向：100 MB 之后是 413</h3>
				<p>
					请求体上限作用在整个代理层上。任何一个请求的正文超过它，Cloudflare 直接回{" "}
					<a href={DOCS_413} target="_blank" rel="noopener noreferrer">
						<code>413</code>
					</a>
					，请求根本到不了源站。自建网盘往上传大文件、Git 推一个大提交、备份脚本上传归档，都会在这里断掉。
				</p>
				<p>
					这个值可以在域名的 Network 页往下调，但不能往上调。要更大只有三条路：把上传拆成分片；把上传用的那个子域改成灰云直连，绕开代理；或者升级套餐。第二条要想清楚代价——灰云意味着这个子域的解析结果就是源站真实
					IP，等于把地址交给了每一个上传方。
				</p>
				<h3 id="limit-cache">分发方向：512 MB 之后不进缓存</h3>
				<p>
					<a href={DOCS_CACHE_LIMITS} target="_blank" rel="noopener noreferrer">
						可缓存文件上限
					</a>
					是另一回事：超过 512 MB 的文件不是传不了，是不进缓存。每一次请求都要完整回源，CDN 在这条路径上除了转发什么也没做。
				</p>
				<p>
					后果是省下来的带宽等于零，账单落在你源站的出网流量上。如果源站是国内的云主机、带宽按 Mbps
					计费，这一条通常比条款风险更早让人肉疼——一个几百 MB 的安装包被人发到群里，一晚上就能把月度带宽预算烧穿。
				</p>

				<h2 id="routes">那么一份文件该走哪条路</h2>
				<LargeFileRoutes />

				<h2 id="paid-paths">官方给的三条正路，各自的免费额度</h2>
				<p>条款点名的三个服务不是同一类东西，用途分得很开，别混着看。</p>
				<h3 id="images">Images：图片规模化</h3>
				<p>
					按
					<a href={DOCS_IMAGES_PRICING} target="_blank" rel="noopener noreferrer">
						官方定价
					</a>
					，免费方案每月有 5,000 次唯一转换的额度，同一个自然月里重复请求同一种变换只计一次；超出部分每千次 0.50
					美元。想把图片本身也存在 Cloudflare 则要 Images 付费方案，存储 5 美元每 10 万张每月，分发 1 美元每 10 万张每月。
				</p>
				<h3 id="stream">Stream：视频</h3>
				<p>
					<a href={DOCS_STREAM_PRICING} target="_blank" rel="noopener noreferrer">
						计费只看时长，不看文件大小
					</a>
					：存储按 5 美元每 1,000 分钟预付，分发按 1 美元每 1,000
					分钟后付，转码、自适应码率和播放器都包在里面。它是条款唯一认可的视频路径。要注意分发是按实际下发的片段计的，播放器的预加载和缓冲也算在里面。
				</p>
				<h3 id="r2">R2：安装包、备份、网盘、图床</h3>
				<p>
					R2 是三条里最被低估的一条，也是自建图床和文件分发最该走的一条。
					<a href={DOCS_R2_PRICING} target="_blank" rel="noopener noreferrer">
						免费额度
					</a>
					是每月 10 GB-月存储、100 万次 A 类操作、1,000 万次 B
					类操作，而最关键的一点是出网流量不收费——不是「给一份额度」，是根本没有这一项计费。
				</p>
				<p>
					但它有个必须讲清楚的坑。桶开启公开访问时给的那个 <code>r2.dev</code> 域名是开发用的：
					<a href={DOCS_R2_PUBLIC} target="_blank" rel="noopener noreferrer">
						官方文档
					</a>
					明说它有速率限制、只面向非生产流量，而且不走 Cloudflare 缓存，WAF、Access、Bot
					管理这些也一概用不上。要认真用，必须给桶绑一个自定义域。
				</p>
				<p>
					绑上自定义域之后缓存才生效——而且默认仍然只缓存那张扩展名表里的类型，想全部缓存要自己加一条
					<a href={DOCS_CACHE_RULES} target="_blank" rel="noopener noreferrer">
						缓存规则
					</a>
					。还有一点：别为图省事拿一条 CNAME 指向 <code>r2.dev</code>，官方把这条路径明确列为不支持，可靠性和性能都不做保证。
				</p>

				<h2 id="china">国内这边的几个额外变量</h2>
				<p>
					在国内，拿 Cloudflare 免费版托管图片和文件之所以流行，很大程度上不是因为便宜——同价位的对象存储并不少——而是因为它绕开了境内主机那一整套接入门槛。这个动机很实在，但随之而来的几个后果值得先摆在桌面上。
				</p>
				<p>
					<strong>免费方案不落地大陆节点。</strong> 大陆访客的请求会被路由到香港、日本、新加坡这些地方，跨境链路本来就抖，再压上几百
					MB 的文件，体验会比想象中差不少。要用境内节点得上{" "}
					<a href={DOCS_CHINA} target="_blank" rel="noopener noreferrer">
						China Network
					</a>
					，那是企业级方案，并且要求域名已完成 ICP 备案。换句话说，「不备案 + 大文件 +
					大陆访客」这三个条件同时成立时，你拿到的恰好是这套体系里最差的那条链路。
				</p>
				<p>
					<strong>R2 出网免费这一条，对国内自建的人价值最大。</strong> 源站放在境内小机器上时，出网带宽通常是整台机器里最贵的一项。把静态大文件挪进
					R2、绑好自定义域之后，这部分带宽账单直接归零，访客也不再打到源站——顺带还把源站真实 IP 的暴露面收窄了一圈。
				</p>
				<p>
					<strong>合规看的是你的业务，不是流量路径。</strong>{" "}
					文件绕经境外节点不等于业务出境。服务面向公众提供、内容责任在你，这些不会因为中间多了一层 CDN
					而改变。这和隧道那篇里讲的是同一件事。
				</p>

				<h2 id="self-check">怎么自查，真被限制时会看到什么</h2>
				<p>
					条款写的是「尽合理努力提前通知」，实务上通常是一封邮件，随后 CDN
					被停用或被限制——表现出来就是访客突然开始直连源站，或者某些资源直接打不开。想提前知道自己站在哪一边，两个动作就够：
				</p>
				<ol>
					<li>
						在 Cloudflare 的分析页看带宽的构成。如果视频和压缩包吃掉了大部分带宽，而你这个站点本身是博客或工具站，那就是「失衡的比例」的字面意思。
					</li>
					<li>
						对着可疑的 URL 跑一条 <code>curl -I</code>，看 <code>content-length</code> 和 <code>cf-cache-status</code>
						。体积超过 512 MB、又长期停在 <code>MISS</code> 的，说明它每次都在回源，这类文件优先挪走。
					</li>
				</ol>
				<p>
					挪的方向很明确：图片进 Images 或 R2，视频进 Stream，安装包和备份进 R2 加自定义域。对多数站点来说，改动量不过是换一批
					URL。
				</p>

				<h2 id="faq">常见问题</h2>
				{FAQ.map((item, i) => (
					<div key={item.q}>
						<h3 id={`faq-${i + 1}`}>{item.q}</h3>
						<p>{item.a}</p>
					</div>
				))}
			</GuideShell>
		</>
	);
}
