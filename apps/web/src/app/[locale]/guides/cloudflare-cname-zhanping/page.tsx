import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Link } from "@/i18n/navigation";
import { setRequestLocale } from "next-intl/server";
import GuideShell, { type RelatedLink } from "@/components/guides/GuideShell";
import FlattenAnswer from "@/components/guides/FlattenAnswer";
import { guideBySlug, guidePath, GUIDE_LOCALE_ZH } from "@/lib/guides/guides";

const SITE_URL = "https://oss.omail.us.kg";
const guide = guideBySlug("cloudflare-cname-zhanping", GUIDE_LOCALE_ZH);
const PATH = guidePath(GUIDE_LOCALE_ZH, `/guides/${guide.slug}`);

const DOCS_FLATTENING = "https://developers.cloudflare.com/dns/cname-flattening/";
const DOCS_SETUP = "https://developers.cloudflare.com/dns/cname-flattening/set-up-cname-flattening/";
const DOCS_DIAGRAM = "https://developers.cloudflare.com/dns/cname-flattening/cname-flattening-diagram/";
const DOCS_APEX = "https://developers.cloudflare.com/dns/concepts/#zone-apex";
const DOCS_CREATE_APEX = "https://developers.cloudflare.com/dns/manage-dns-records/how-to/create-zone-apex/";
const DOCS_RECORD_TYPES = "https://developers.cloudflare.com/dns/manage-dns-records/reference/dns-record-types/#cname";
const DOCS_ATTRIBUTES = "https://developers.cloudflare.com/dns/manage-dns-records/reference/record-attributes/";
const DOCS_PROXY = "https://developers.cloudflare.com/dns/proxy-status/";
const DOCS_1014 =
	"https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-1xxx-errors/error-1014/";
const DOCS_PAGES_DOMAIN = "https://developers.cloudflare.com/pages/configuration/custom-domains/";
const DOCS_SETTINGS_API =
	"https://developers.cloudflare.com/api/resources/dns/subresources/settings/subresources/zone/methods/edit/";

/** FAQ 一处定义：可见文本与 FAQPage JSON-LD 同源，保证逐字一致 */
const FAQ: Array<{ q: string; a: string }> = [
	{
		q: "Cloudflare 的 CNAME 展平能关掉吗？",
		a: "根域名上的那条关不掉。只要记录名是 @，也就是落在域名的顶点上，Cloudflare 在所有方案下都会强制展平，面板里连开关都不会出现。能关的只有另外两件事：付费方案上的「对所有 CNAME 记录展平」这个全局开关，以及单条记录上的 Flatten 选项——它们管的是子域名上那些 CNAME 要不要一并展平。想让根域名真正返回一条 CNAME，只能把域名的 DNS 托管换到别家。",
	},
	{
		q: "CNAME 展平和 CNAME 拉平是一回事吗？",
		a: "是同一件事。CNAME flattening 在中文里有人译成展平，有人译成拉平，也有人直接叫「根域名 CNAME」，指的都是 Cloudflare 替你把 CNAME 一路解析到最终的 IP、然后返回 A 或 AAAA 记录而不是 CNAME 记录这个行为。Cloudflare 面板里的设置项叫 CNAME Flattening，API 字段是 flatten_all_cnames 和 flatten_cname。",
	},
	{
		q: "根域名 CNAME 到国内 CDN，为什么国内访问反而变慢了？",
		a: "因为那次解析不是访客做的，是 Cloudflare 做的。国内 CDN 的调度靠的是「谁在查」——按解析请求的来源判断访客大概在哪，再返回就近节点的 IP。展平把这一步搬到了 Cloudflare 的权威侧，CDN 看到的提问者变成了 Cloudflare，返回的节点自然也不再是照着你的访客选的。更要命的是这个结果会被当成一条普通 A 记录发给全世界，所有访客共用同一个 IP，调度整个失效。",
	},
	{
		q: "展平之后 TTL 是多少？",
		a: "分两种。这条记录开着代理时，返回的是多条 Cloudflare 任播 IP，TTL 固定为 300 秒。关着代理时，返回的是展平出来的源站 IP，TTL 取 Cloudflare 这条 CNAME 记录和目标那条 A 记录里较小的那个值。所以把 CNAME 的 TTL 设小并不能让整体变快，设大也未必生效——真正说了算的是两者中更保守的一方。",
	},
	{
		q: "第三方服务让我加一条 CNAME 做域名验证，加完一直验证不过怎么办？",
		a: "先看这条记录是不是被展平了。验证方通常要查到 CNAME 记录本身，而展平之后返回的是 IP，CNAME 记录不再直接出现在应答里，验证就会失败。对应的处理是：关掉「对所有 CNAME 记录展平」这个全局开关，或者取消这条记录上的 Flatten，同时确认它是灰云状态。另一种情形是目标主机名底下压根没有 A 或 AAAA 记录，展平会返回一个空应答，看起来就像解析一直没生效。",
	},
];

const RELATED: RelatedLink[] = [
	{
		href: "/guides/cloudflare-dns-jiexi-bu-shengxiao",
		label: "改了 DNS 解析，为什么一直不生效？",
		note: "展平后的上游一旦超时或 SERVFAIL，Cloudflare 会拿过期应答顶上，看起来就像改了不生效。",
	},
	{
		href: "/guides/cloudflare-xiaohuangyun",
		label: "Cloudflare 的小黄云到底是什么？什么时候该关掉？",
		note: "展平后返回的是任播 IP 还是源站 IP，全看这条记录的代理开关拨在哪一边。",
	},
	{
		href: "/guides/cloudflare-yuanzhan-ip-xielou",
		label: "套了 Cloudflare，源站真实 IP 还会泄露吗？",
		note: "灰云记录被展平后，源站 IP 会以一条普通 A 记录的形式挂在域名顶点上，谁都能查。",
	},
	{
		href: "/guides/cloudflare-522-error",
		label: "Cloudflare 为什么会报 522 错误？",
		note: "展平指错了 IP，回源就会卡在建连这一步——两件事常常一起出现。",
	},
	{
		href: DOCS_FLATTENING,
		label: "Cloudflare 官方文档：CNAME flattening",
		note: "本文所有行为描述的原始出处，含悬空 CNAME 与跨账号 CNAME 的处理方式。",
		external: true,
	},
	{
		href: DOCS_DIAGRAM,
		label: "Cloudflare 官方文档：展平流程示意",
		note: "一次查询在 Cloudflare 内部走过的每一步，以及两种代理状态下 TTL 的取值规则。",
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

export default async function CnameFlatteningGuideZh({ params }: { params: Promise<{ locale: string }> }) {
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
				lede="在别家 DNS 上，根域名填 CNAME 会被直接拒掉；在 Cloudflare 上它不但能填，还会静悄悄变成另一种东西。这中间被替换掉的那一步，正是后面所有奇怪现象的来源。"
				updated={guide.updated}
				readingTime={guide.readingTime}
				related={RELATED}
			>
				<div className="glass r-island note p-6 sm:p-7">
					<p>
						展平就是 Cloudflare 替你把 CNAME 解析成 IP，返回一条 A 记录而不是 CNAME。根域名上的 CNAME
						在所有方案下都强制展平，关不掉。
					</p>
				</div>

				<p>
					把域名接进 Cloudflare 之后，很多人第一次发现「原来根域名也能填 CNAME」。别的 DNS
					服务商在这里会直接报错，Cloudflare 却连眉头都不皱，顶多在保存时提示一句「我们用 CNAME
					展平来实现这一点」。于是这条记录就这么留下了，一切看着都正常。
				</p>
				<p>
					问题往往几个月后才浮上来：第三方域名验证怎么都过不去；国内 CDN 买了却没提速；<code>dig</code>{" "}
					查自己的域名，回来的居然是一条 A 记录。这些症状看着不相干，根却在同一处——展平不是把你的 CNAME 原样搬到线上，而是用另一样东西替换了它。
				</p>

				<h2 id="why-apex">根域名上，本来就不该有 CNAME</h2>
				<p>
					DNS 规范里有一条硬约束：一个名字上如果有 CNAME，就不能再有同名的其它记录。CNAME
					的语义是「这个名字整体是另一个名字的别名」，别名之下再挂别的东西，语义上说不通。
				</p>
				<p>
					而域名的顶点——也就是 <code>example.com</code> 本身，Cloudflare 文档里叫
					<a href={DOCS_APEX} target="_blank" rel="noopener noreferrer">
						zone apex
					</a>
					，面板里填 <code>@</code> 的那一格——身上必然带着 SOA 和 NS 记录，它们是这个域名存在的凭据，删不得。两条规矩撞在一起，结论就是根域名上放不了
					CNAME。想让 <code>example.com</code>（而不是 <code>www.example.com</code>）指向某个 SaaS 或 CDN
					给的主机名，标准 DNS 里没有正路。
				</p>
				<p>
					Cloudflare 给出的解法是绕过去而不是打破它：面板里允许你填 CNAME，但对外<strong>永远不把它当 CNAME 返回</strong>
					。官方对这套机制的定位写得很直白——让根域名上的 CNAME
					「符合 DNS 标准地」被解析掉。换句话说，你填的那条记录是一份配置意图，不是最终会被广播出去的答案。
				</p>

				<h2 id="what-happens">展平那一刻，Cloudflare 做了什么</h2>
				<p>
					当解析器来问 <code>example.com</code> 的 A 记录，Cloudflare 查到顶点上是一条 CNAME，不会把这条 CNAME
					递回去，而是就地把它当成一次待办：去解析目标主机名，拿到 IP；如果目标底下还是 CNAME，就继续追，直到落在 A 或 AAAA
					上。拿到最终 IP 之后，把答案里的记录名改写成 <code>example.com</code>，再返回。
				</p>
				<FlattenAnswer />
				<p>
					这一步最容易被忽略的性质是：<strong>那次对目标主机名的解析，发生在 Cloudflare 一侧，不在访客一侧。</strong>
					访客的解析器只发出了一个问题、收到了一条 A 记录，全程不知道中间还隔着一个主机名。后文那些看着不相干的坑，几乎都是这句话的推论。
				</p>
				<p>返回的答案长什么样，取决于这条记录的代理状态，两种情况的 TTL 规则也不一样：</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">代理状态</th>
								<th scope="col">返回的内容</th>
								<th scope="col">TTL</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">开着代理（小黄云）</th>
								<td>多条 Cloudflare 任播 IP，跟你填的目标无关</td>
								<td>固定 300 秒</td>
							</tr>
							<tr>
								<th scope="row">关着代理（灰云）</th>
								<td>展平出来的那个真实 IP</td>
								<td>Cloudflare 这条 CNAME 与目标那条 A 记录中较小的一个</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					第二行值得多看一眼。把 CNAME 的 TTL 调到 60 秒，并不意味着改动一分钟内就能扩散开；调到 86400
					秒也未必留得住——对方那条记录一小时就到期的话，整条答案跟着一小时过期。TTL 的话语权在更保守的那一方手上。
				</p>

				<h2 id="when">什么时候会展平，什么时候不会</h2>
				<p>
					展平不是一个非开即关的全局设置，而是几条触发条件叠在一起的结果。<a href={DOCS_SETUP} target="_blank" rel="noopener noreferrer">
						官方的设置文档
					</a>
					把它们分得很清楚：
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">情形</th>
								<th scope="col">会不会展平</th>
								<th scope="col">方案要求</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">
									记录在域名顶点（<code>@</code>）
								</th>
								<td>强制展平，没有开关</td>
								<td>所有方案，含免费</td>
							</tr>
							<tr>
								<th scope="row">
									记录
									<a href={DOCS_PROXY} target="_blank" rel="noopener noreferrer">
										开着代理
									</a>
								</th>
								<td>默认展平，因为返回的本来就是任播 IP</td>
								<td>所有方案</td>
							</tr>
							<tr>
								<th scope="row">目标主机名在同一个域名内</th>
								<td>直接展平，无视下面两个开关</td>
								<td>所有方案</td>
							</tr>
							<tr>
								<th scope="row">「对所有 CNAME 记录展平」全局开关</th>
								<td>打开后所有 CNAME 一并展平</td>
								<td>仅付费方案</td>
							</tr>
							<tr>
								<th scope="row">单条记录上的 Flatten 选项</th>
								<td>只展平勾中的那条</td>
								<td>仅付费方案</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					所以免费方案能碰到的展平只有前三种，而且都不可关闭。付费方案多出来的两个开关，作用范围也只在子域名的灰云 CNAME
					上——真正让人栽跟头的顶点那条，任何方案都动不了。
				</p>
				<p>
					单条勾选的 Flatten 会在区域文件里留下 <code>cf-flatten-cname</code>{" "}
					<a href={DOCS_ATTRIBUTES} target="_blank" rel="noopener noreferrer">
						标记
					</a>
					，导入导出不丢配置；走 API 的话，全局开关是 DNS 设置里的{" "}
					<a href={DOCS_SETTINGS_API} target="_blank" rel="noopener noreferrer">
						<code>flatten_all_cnames</code>
					</a>
					，单条的是记录 <code>settings</code> 里的 <code>flatten_cname</code>。面板上有三种情况看不到 Flatten
					这个勾选框：记录在顶点上、记录已经开着代理、全局开关已经打开——这三种都意味着「已经在展平了，不用你选」。
				</p>

				<h2 id="gotchas">展平会咬到你的四种情况</h2>
				<p>
					<strong>第三方域名验证过不去。</strong>不少服务要求你加一条 CNAME 指向它给的主机名，然后它去查这条 CNAME
					是否存在。展平之后应答里只有 IP，CNAME 记录本身不再直接返回，验证方查不到自己要的东西，就会一直卡在「等待验证」。官方文档专门为这种场景留了警告。处理方式是把这条记录留成灰云，并确认全局展平没打开。
				</p>
				<p>
					<strong>目标是悬空 CNAME，应答直接变空。</strong>如果一路追下去，最终那个主机名底下并没有 A 或 AAAA
					记录，展平就没有 IP 可返回，Cloudflare 会给出一个空应答（NODATA）。表现出来很像「解析怎么还没生效」，其实是解析已经完成、答案里什么都没有。查一下目标主机名本身能不能解析出
					IP，比反复刷新解析工具管用。
				</p>
				<p>
					<strong>指向别人 Cloudflare 账号下的主机名会被封。</strong>CNAME 到另一个 Cloudflare
					账号里的域名是被明确禁止的，触发的是{" "}
					<a href={DOCS_1014} target="_blank" rel="noopener noreferrer">
						Error 1014：CNAME Cross-User Banned
					</a>
					。这条在「我用 CNAME 接一个朋友的服务」这类场景里踩得最多。
				</p>
				<p>
					<strong>灰云顶点会把源站 IP 摆到台面上。</strong>顶点上的 CNAME 只要是灰云，展平的结果就是把目标的真实 IP
					当成一条普通 A 记录返回。任何人 <code>dig example.com</code> 都能直接拿到它——本来隔着一个主机名的间接性，被展平这一步抹平了。这一点值得和
					<Link href="/guides/cloudflare-yuanzhan-ip-xielou">源站 IP 泄露的其它路径</Link>放在一起看。
				</p>
				<p>
					反过来说，展平也不全是麻烦。Cloudflare Pages 之所以能把
					<a href={DOCS_PAGES_DOMAIN} target="_blank" rel="noopener noreferrer">
						根域名直接绑成自定义域
					</a>
					，靠的正是这套机制；解析路径少一跳，查询也确实更快。它只是有代价，而代价通常不写在你保存记录的那个弹窗里。
				</p>

				<h2 id="china">在国内用，多出来的那一层麻烦</h2>
				<p>
					把「那次解析发生在 Cloudflare 一侧」这句话放到国内的网络环境里，麻烦会明显放大，因为国内这套体系高度依赖「按解析来源做调度」。
				</p>
				<p>
					<strong>最常见的一种是把根域名 CNAME 到国内 CDN。</strong>国内 CDN
					给你的是一个主机名，它的智能调度全靠这个主机名被查询时的来源判断访客大致位置，再返回就近节点。展平把发问的人从你的访客换成了
					Cloudflare，调度依据当场消失；更麻烦的是，展平出来的结果会作为一条普通 A
					记录发给所有人，等于把一次本该千人千面的调度，冻结成了一个全球统一的 IP。买了 CDN 却没有提速，多半就卡在这里。
				</p>
				<p>
					这件事没有「在 Cloudflare 里关掉展平」这个解，顶点那条关不掉。可行的路只有三条：把 DNS 托管交回给 CDN
					厂商或别的支持根域名 CNAME 的服务商；退回到 A 记录直接写 IP，接受失去调度、对方换 IP 时手动跟；或者承认这个域名的主力访客不在国内，让它走
					Cloudflare。三条各有代价，但都好过一直以为「配好了只是还没生效」。
				</p>
				<p>
					<strong>另一种是备案与解析分家的场景。</strong>国内业务常常要求域名解析到已备案的境内 IP，而顶点上一旦挂了 CNAME
					并被展平，最终返回什么 IP 就取决于展平那一刻解析到的结果——你在面板里看到的是主机名，实际对外广播的是 IP，两者不是一回事。做备案核验或者排查解析问题时，一律以{" "}
					<code>dig example.com</code> 实际收到的应答为准，别拿面板截图当证据。
				</p>
				<p>
					<strong>还有一种是跨境链路带来的观感差异。</strong>顶点记录一旦开着代理，展平返回的就是 Cloudflare
					的任播 IP，TTL 固定 300 秒。国内访客大概率被引到境外节点，链路本身的抖动会盖过 DNS 层面的任何优化——这时候纠结 TTL
					设多少是没有意义的，瓶颈不在这里。把顶点改成灰云也只是换了个方向：解析快了，但代理带来的缓存、WAF 和 DDoS
					防护同时也就没了，这笔账要一起算。
				</p>

				<h2 id="check">怎么确认自己这条记录到底是什么状态</h2>
				<p>
					面板不会告诉你对外返回的是什么，命令行会。查一次顶点，看回来的记录类型是 CNAME 还是 A，基本就有答案了：
				</p>
				<pre>
					<code>{`dig +short example.com A
dig +noall +answer example.com`}</code>
				</pre>
				<p>
					第二条会连 TTL 一起显示。看到的是 A 记录且 TTL 是 300，说明顶点开着代理；是 A 记录但 TTL
					是个别的数字，说明灰云展平生效，那个 IP 就是你的源站或 CDN 节点；如果什么都没返回，多半撞上了前面说的悬空 CNAME。想确认展平之前那一层长什么样，直接去查目标主机名本身，把两边的结果对着看。
				</p>
				<p>
					官方的{" "}
					<a href={DOCS_CREATE_APEX} target="_blank" rel="noopener noreferrer">
						顶点记录
					</a>
					与{" "}
					<a href={DOCS_RECORD_TYPES} target="_blank" rel="noopener noreferrer">
						CNAME 字段说明
					</a>
					可以对照着看：面板里那个 TTL「自动」，在代理开着时就是 300。
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
