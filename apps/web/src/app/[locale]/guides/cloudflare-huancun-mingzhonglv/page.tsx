import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Link } from "@/i18n/navigation";
import { setRequestLocale } from "next-intl/server";
import GuideShell, { type RelatedLink } from "@/components/guides/GuideShell";
import CacheFanout from "@/components/guides/CacheFanout";
import { guideBySlug, guidePath, GUIDE_LOCALE_ZH } from "@/lib/guides/guides";

const SITE_URL = "https://oss.omail.us.kg";
const guide = guideBySlug("cloudflare-huancun-mingzhonglv", GUIDE_LOCALE_ZH);
const PATH = guidePath(GUIDE_LOCALE_ZH, `/guides/${guide.slug}`);

const DOCS_DEFAULT = "https://developers.cloudflare.com/cache/concepts/default-cache-behavior/";
const DOCS_EXTENSIONS =
	"https://developers.cloudflare.com/cache/concepts/default-cache-behavior/#default-cached-file-extensions";
const DOCS_STATUSES = "https://developers.cloudflare.com/cache/concepts/cache-responses/";
const DOCS_UNCACHED = "https://developers.cloudflare.com/cache/troubleshooting/investigating-uncached-responses/";
const DOCS_DEVMODE = "https://developers.cloudflare.com/cache/reference/development-mode/";
const DOCS_RULES = "https://developers.cloudflare.com/cache/how-to/cache-rules/";
const DOCS_KEYS = "https://developers.cloudflare.com/cache/how-to/cache-keys/";
const DOCS_TIERED = "https://developers.cloudflare.com/cache/how-to/tiered-cache/";
const DOCS_ANALYTICS = "https://developers.cloudflare.com/cache/performance-review/cache-analytics/";
const DOCS_TTL = "https://developers.cloudflare.com/cache/how-to/edge-browser-cache-ttl/";
const DOCS_PURGE = "https://developers.cloudflare.com/cache/how-to/purge-cache/";
const DOCS_CDNCC = "https://developers.cloudflare.com/cache/concepts/cdn-cache-control/";
const DOCS_CHINA = "https://developers.cloudflare.com/china-network/";
const DOCS_ICP = "https://developers.cloudflare.com/china-network/concepts/icp/";

/** FAQ 一处定义：可见文本与 FAQPage JSON-LD 同源，保证逐字一致 */
const FAQ: Array<{ q: string; a: string }> = [
	{
		q: "Cloudflare 免费版能缓存 HTML 吗？",
		a: "能，但要你自己去开。默认缓存只认扩展名白名单，HTML 不在里面，所以首页永远是 DYNAMIC。想让它进缓存，去缓存规则里加一条，把「符合缓存条件」设成是，再给一个边缘 TTL。免费方案有 10 条规则额度，够用。真正要当心的不是能不能开，而是开完之后带登录态的页面会不会被当成公共副本发给别人——先把带 Cookie、带 Authorization 的路径排除掉再动手。",
	},
	{
		q: "cf-cache-status 显示 DYNAMIC 是什么意思？",
		a: "意思是 Cloudflare 在请求刚进来时就判定这个资源没资格进缓存，压根没去缓存里找过。最常见的原因是扩展名不在默认表里，比如 HTML 和 JSON 接口；其次是有规则明确写了绕过缓存，或者开发模式还开着，或者请求方法不是 GET 和 HEAD。它和 BYPASS 的区别在于时机：DYNAMIC 是请求时判的，BYPASS 是响应回来之后才判的。",
	},
	{
		q: "为什么清了 Cloudflare 缓存，页面还是旧的？",
		a: "清缓存只清 Cloudflare 边缘那一份，动不了访客浏览器里已经存下的副本。浏览器缓存 TTL 默认是 4 小时，在这段时间里回头客根本不会来问你的服务器。换个无痕窗口或者带随机查询参数请求一次，就能分清到底是边缘没清干净还是浏览器在拿旧的。另外清完之后每个数据中心的第一次请求都会是未命中，需要重新回源填一遍，这段时间看到的也不是命中。",
	},
	{
		q: "命中率低是不是得升级到 Pro？",
		a: "先别急着掏钱。分层缓存在免费方案上就能开，对访客分散的站点通常是收益最大的一步；把 utm 之类每次都变的查询参数从缓存键里剔掉，效果也很直接。Pro 买到的主要是两样东西：能看见命中率的缓存分析面板，以及更低的最小边缘 TTL——免费方案的边缘 TTL 最低只能设到 2 小时，Pro 是 1 小时。如果你的问题是全站 DYNAMIC，升级方案一分钱都帮不上，那是规则没配。",
	},
	{
		q: "Cloudflare 在中国大陆有节点吗？",
		a: "有，但普通方案用不到。境内节点属于中国网络，由京东云运营，是企业方案之外单独买的订阅，而且每个根域名都要有有效的 ICP 备案或许可证。免费、Pro、Business 方案的大陆访客一律走境外节点，具体落到香港、东京还是更远的地方，由运营商的路由决定。",
	},
];

const RELATED: RelatedLink[] = [
	{
		href: "/guides/cloudflare-xiaohuangyun",
		label: "Cloudflare 的小黄云到底是什么？什么时候该关掉？",
		note: "缓存只对代理流量生效——记录是灰云，缓存规则一条都不会触发。",
	},
	{
		href: "/guides/cloudflare-mianfeiban-shipin-tucang",
		label: "Cloudflare 免费版能拿来放视频、当图床吗？",
		note: "512 MB 的可缓存体积上限从哪来，以及条款那一节对大文件是怎么写的。",
	},
	{
		href: "/guides/cloudflare-522-error",
		label: "Cloudflare 为什么会报 522 错误？",
		note: "命中率低意味着回源多，源站扛不住的时候先冒出来的往往是这个错误码。",
	},
	{
		href: DOCS_STATUSES,
		label: "Cloudflare 官方文档：cf-cache-status 全部取值",
		note: "本文那张状态表的原始出处，每个取值的判定时机都写在里面。",
		external: true,
	},
	{
		href: DOCS_UNCACHED,
		label: "Cloudflare 官方文档：排查没被缓存的响应",
		note: "按 DYNAMIC、BYPASS、反复未命中三条线走的官方排查步骤。",
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

export default async function CacheHitRateGuideZh({ params }: { params: Promise<{ locale: string }> }) {
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
				lede="把域名接进来、小黄云点亮，很多人默认这就等于全站加速了。但 Cloudflare 的缓存从来不是「默认全都存」，它有一份自己的白名单，而你的首页多半不在上面。"
				updated={guide.updated}
				readingTime={guide.readingTime}
				related={RELATED}
			>
				<div className="glass r-island note p-6 sm:p-7">
					<p>
						Cloudflare 默认只按文件扩展名缓存静态资源，HTML、JSON 一概不缓存。命中率上不去，通常不是缓存失效，而是请求根本没被当成可缓存的——先看 <code>cf-cache-status</code> 这个响应头。
					</p>
				</div>

				<p>
					用惯国内 CDN 的人接手 Cloudflare，最容易带进来的一个预设是「买了加速就是全站加速」。国内厂商的控制台上，命中率和回源率是首页最大的两个数字，默认策略也倾向于先存下来再让你排除。
				</p>
				<p>
					Cloudflare 是反过来的：它默认只认一份扩展名白名单，白名单之外一律直接回源，而且免费方案连命中率面板都没有。于是常见的场景就成了——网站确实变快了一点（那是就近接入和连接复用的功劳），但源站的负载几乎没降，一查才发现绝大多数请求压根没进过缓存。
				</p>

				<h2 id="header">先看响应头，别靠猜</h2>
				<p>
					所有关于缓存的争论，都可以被一个响应头终结。请求任意一个 URL，把这几行拉出来看：
				</p>
				<pre>
					<code>{`curl -sI https://example.com/ | grep -iE 'cf-cache-status|cf-ray|^age|cache-control'`}</code>
				</pre>
				<p>
					<code>cf-cache-status</code>{" "}
					给的是这一次请求的判定结果，官方把
					<a href={DOCS_STATUSES} target="_blank" rel="noopener noreferrer">
						每个取值
					</a>
					分得很细，实际排查时用得上的是这几个：
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">取值</th>
								<th scope="col">发生了什么</th>
								<th scope="col">该往哪查</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">HIT</th>
								<td>从边缘缓存里直接取出来的</td>
								<td>正常，看 <code>Age</code> 判断存了多久</td>
							</tr>
							<tr>
								<th scope="row">MISS</th>
								<td>够资格缓存，但这个节点上没有，去源站取了</td>
								<td>偶尔出现正常，反复出现要查缓存键</td>
							</tr>
							<tr>
								<th scope="row">DYNAMIC</th>
								<td>请求时就判定没资格，缓存看都没看</td>
								<td>扩展名、规则、开发模式</td>
							</tr>
							<tr>
								<th scope="row">BYPASS</th>
								<td>本来够格，但响应本身不让存</td>
								<td>源站的响应头</td>
							</tr>
							<tr>
								<th scope="row">EXPIRED / REVALIDATED</th>
								<td>缓存里有，但过期了，回源确认过</td>
								<td>边缘 TTL 设得太短</td>
							</tr>
							<tr>
								<th scope="row">UPDATING / STALE</th>
								<td>先把旧副本发出去，后台再更新</td>
								<td>正常，或源站联系不上</td>
							</tr>
							<tr>
								<th scope="row">NONE / UNKNOWN</th>
								<td>响应没走到缓存这一层</td>
								<td>Workers、跳转规则、被 WAF 拦下</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					这张表里最该记住的是 DYNAMIC 和 BYPASS 的分界：<strong>判定的时机不一样</strong>。DYNAMIC 是请求刚进门就被拦下，Cloudflare 连缓存都没去翻；BYPASS 是已经打算存了，结果源站发回来的东西不允许存。两者的排查方向也不同：一个查配置，一个查源站。
				</p>

				<h2 id="dynamic">DYNAMIC：请求在门口就被判了没资格</h2>
				<p>
					默认情况下 Cloudflare{" "}
					<a href={DOCS_DEFAULT} target="_blank" rel="noopener noreferrer">
						只按扩展名决定缓存
					</a>
					，不看 MIME 类型。
					<a href={DOCS_EXTENSIONS} target="_blank" rel="noopener noreferrer">
						那份白名单
					</a>
					里有 CSS、JS、各类图片、字体、PDF、ZIP，甚至 MP4 也在其中，另外 robots.txt 也默认缓存。<strong>HTML 和 JSON 不在里面</strong>——这意味着你的首页、文章页、接口响应，默认全都是 DYNAMIC。
				</p>
				<p>另外三种情况也会给出 DYNAMIC：</p>
				<ul>
					<li>有规则明确写了绕过缓存，包括缓存规则里的「绕过缓存」和老页面规则里的 Cache Level: Bypass；</li>
					<li>
						请求方法不是 <code>GET</code> 或 <code>HEAD</code>，Cloudflare 只缓存这两种；
					</li>
					<li>
						<a href={DOCS_DEVMODE} target="_blank" rel="noopener noreferrer">
							开发模式
						</a>
						开着。它会挂起边缘缓存三小时，期间全部返回 DYNAMIC，到点自动关。注意它只是旁路，不清缓存——调试完忘了关，是「上线一整天没缓存」的经典原因。
					</li>
				</ul>
				<p>
					想让 HTML 进缓存，得自己去{" "}
					<a href={DOCS_RULES} target="_blank" rel="noopener noreferrer">
						缓存规则
					</a>
					里加一条，把「符合缓存条件」设成是，再配一个边缘 TTL。免费方案有 10 条规则的额度。有个前提容易被忽略：缓存规则要求对应的 DNS 记录是
					<Link href="/guides/cloudflare-xiaohuangyun">代理状态</Link>
					，灰云记录的流量根本不经过 Cloudflare，写多少条规则都不会触发。
				</p>

				<h2 id="bypass">BYPASS：够格了，但响应本身不让存</h2>
				<p>
					看到 BYPASS，说明请求已经通过了资格审查，问题出在源站回来的那个响应上。官方
					<a href={DOCS_UNCACHED} target="_blank" rel="noopener noreferrer">
						排查文档
					</a>
					列的常见原因，按国内站点的踩坑频率排下来是这样：
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">源站发回了什么</th>
								<th scope="col">结果</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">
									<code>Set-Cookie</code>
								</th>
								<td>默认不缓存。PHP 框架默认给每个响应都种会话 Cookie，静态页也跟着遭殃</td>
							</tr>
							<tr>
								<th scope="row">
									<code>Cache-Control: no-store</code> 或 <code>private</code>
								</th>
								<td>明确禁止存储，两种模式下都不缓存</td>
							</tr>
							<tr>
								<th scope="row">
									<code>Vary: *</code>
								</th>
								<td>无条件绕过缓存</td>
							</tr>
							<tr>
								<th scope="row">响应体积超上限</th>
								<td>免费、Pro、Business 方案的可缓存上限是 512 MB</td>
							</tr>
							<tr>
								<th scope="row">
									<code>CDN-Cache-Control: no-store</code>
								</th>
								<td>
									优先级高于 <code>Cache-Control</code>，哪怕后者写着 public 也照样绕过
								</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					最后一行值得单拎出来。Cloudflare 读缓存指令的
					<a href={DOCS_CDNCC} target="_blank" rel="noopener noreferrer">
						优先级
					</a>
					是 <code>Cloudflare-CDN-Cache-Control</code> 高于 <code>CDN-Cache-Control</code>，再高于 <code>Cache-Control</code>。更麻烦的是第一个头不会转发给客户端，浏览器里看不见——BYPASS 得莫名其妙时，去源站确认它到底发了什么。
				</p>
				<p>
					还有一个反直觉的点：<code>no-cache</code> 和 <code>max-age=0</code> <strong>不会</strong>导致 BYPASS。在免费、Pro、Business 方案上，源站缓存控制默认是开启的，这几个指令的效果是「存下来，但每次回源确认一下」，对应 REVALIDATED 或 EXPIRED——那其实是已经缓存了。
				</p>

				<h2 id="miss">一直是 MISS：存进去了，下次却找不到</h2>
				<p>
					第一次请求返回 MISS 是正常的，那一次负责把内容填进缓存。反复 MISS 就不正常了，通常是两类原因。
				</p>
				<p>
					<strong>一是缓存键每次都在变。</strong>Cloudflare 默认用「源站协议 + 主机名 + 路径 + 查询字符串」组成缓存键，也就是说 <code>?utm_source=weixin</code> 和 <code>?utm_source=zhihu</code> 是两个完全独立的缓存条目。带时间戳、会话 ID、渠道参数的链接一多，缓存就被切得粉碎，每次都是新条目、每次都要回源。
					<a href={DOCS_KEYS} target="_blank" rel="noopener noreferrer">
						缓存键设置
					</a>
					里的「忽略查询字符串」免费方案就能用，但它是一刀切的；想按参数名挑着留，是企业方案才有的能力。
				</p>
				<p>
					<strong>二是压根不在同一个节点上。</strong>缓存是按数据中心各存一份的，你在公司网络测一次、用手机流量再测一次，很可能命中的是两个不同的机房，各自都得先回源一次。比对响应头 <code>cf-ray</code> 末尾那三个字母就能确认是不是同一个节点，不同就说明这两次 MISS 本来就该发生。冷门资源被挤出缓存也归在这一类。
				</p>

				<h2 id="china">在国内用，命中率天然更难看</h2>
				<p>
					上面那条「按数据中心各存一份」，放到国内的网络环境里会被显著放大。
				</p>
				<p>
					Cloudflare 在中国大陆确实有节点，但它属于单独的
					<a href={DOCS_CHINA} target="_blank" rel="noopener noreferrer">
						中国网络
					</a>
					，由京东云运营，只面向企业方案的额外订阅，并且每个根域名都需要有效的{" "}
					<a href={DOCS_ICP} target="_blank" rel="noopener noreferrer">
						ICP 备案或许可证
					</a>
					。免费、Pro、Business 方案的大陆访客一律走境外节点——具体落到香港、东京、新加坡还是更远的地方，取决于运营商在那个时段的路由，同一个用户换个网络就可能换一个机房。
				</p>
				<CacheFanout />
				<p>
					同样的流量规模，国内站点的请求会分散到更多数据中心，每个机房各自回源填一遍。这不是配置错了，是地理事实。
				</p>
				<p>
					能对症的一步是开
					<a href={DOCS_TIERED} target="_blank" rel="noopener noreferrer">
						分层缓存
					</a>
					。开启后下层节点未命中时先去问上层节点，只有上层才被允许回源。智能拓扑在免费方案上就可用，通用全球拓扑和区域分层才是企业方案专属。对访客分散的站点，这通常是投入产出比最高的一个开关。
				</p>
				<p>
					另一件要有心理准备的事：<strong>免费方案看不到命中率。</strong>
					<a href={DOCS_ANALYTICS} target="_blank" rel="noopener noreferrer">
						缓存分析
					</a>
					从 Pro 起才有，保留 7 天，Business 和企业是 30 天。免费方案想知道命中情况，只能靠 <code>cf-cache-status</code> 一条条抽样，或者去源站日志里数请求量的变化。
				</p>
				<p>
					最后，命中不等于快。跨境链路的抖动经常盖过缓存本身的收益，HIT 也可能等上几百毫秒。把首字节慢一律归因到「缓存没生效」是国内排查里最常见的误判——先看响应头确认状态，再谈优化，顺序别颠倒。
				</p>

				<h2 id="purge">「清了缓存，页面还是旧的」</h2>
				<p>这句话背后通常是三件互不相干的事：</p>
				<ul>
					<li>
						<strong>浏览器那份没清。</strong>
						<a href={DOCS_PURGE} target="_blank" rel="noopener noreferrer">
							清除缓存
						</a>
						只动 Cloudflare 边缘的副本，访客浏览器里存的那份它够不着。
						<a href={DOCS_TTL} target="_blank" rel="noopener noreferrer">
							浏览器缓存 TTL
						</a>
						默认 4 小时，这段时间里回头客不会来问你。用无痕窗口或者加个随机查询参数请求一次，就能把两者分开。
					</li>
					<li>
						<strong>还在回填。</strong>清完之后，每个数据中心的第一次请求都是 MISS，要重新回源填一遍。所谓预热，就是主动跑一遍把它们填回去。
					</li>
					<li>
						<strong>清得太频繁被限流了。</strong>整站清除按账号限速，免费方案是每分钟 5 次、桶容量 25。把「清全站」写进部署脚本每次发版跑一遍，很容易撞上；按 URL 精确清除的额度宽松得多，也是官方推荐的做法。
					</li>
				</ul>
				<p>
					反过来，如果你要的是「边缘只存几分钟」，免费方案做不到：最小
					<a href={DOCS_TTL} target="_blank" rel="noopener noreferrer">
						边缘缓存 TTL
					</a>
					是 2 小时，Pro 是 1 小时，Business 起才能设到秒级。频繁更新的内容，与其跟 TTL 较劲，不如靠版本化文件名或按 URL 清除。
				</p>

				<h2 id="checklist">排查顺序</h2>
				<ol>
					<li>
						<code>curl -sI</code> 拿到 <code>cf-cache-status</code>，先确定自己在哪条岔路上。
					</li>
					<li>DYNAMIC：查扩展名是否在白名单里、有没有绕过缓存的规则、开发模式是不是忘了关、记录是不是灰云。</li>
					<li>BYPASS：去源站看响应头，重点是 Set-Cookie、no-store、Vary 和体积。</li>
					<li>
						反复 MISS：比对两次请求的 <code>cf-ray</code> 末三位是不是同一个节点，再看查询字符串有没有每次都变的参数。
					</li>
					<li>
						HIT 但内容是旧的：看 <code>Age</code> 有多大，然后换无痕窗口确认是不是浏览器缓存。
					</li>
					<li>确认策略生效后，同一个客户端连请求两次，第二次应该是 HIT，且 <code>Age</code> 会递增。</li>
				</ol>
				<p>
					这套顺序把「缓存不生效」拆成了四种互斥的故障，每一种的修法都不一样。跳过第一步直接猜，基本都会绕远路。
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
