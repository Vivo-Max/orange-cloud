import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { setRequestLocale } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import GuideShell, { type RelatedLink } from "@/components/guides/GuideShell";
import OriginTimeouts from "@/components/guides/OriginTimeouts";
import { guideBySlug, guidePath, GUIDE_LOCALE_ZH } from "@/lib/guides/guides";

const SITE_URL = "https://90dd.adsl8.workers.dev";
const guide = guideBySlug("cloudflare-522-error", GUIDE_LOCALE_ZH);
const PATH = guidePath(GUIDE_LOCALE_ZH, `/guides/${guide.slug}`);

const DOCS_522 =
	"https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-5xx-errors/error-522/";
const DOCS_5XX = "https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-5xx-errors/";
const DOCS_521 =
	"https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-5xx-errors/error-521/";
const DOCS_523 =
	"https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-5xx-errors/error-523/";
const DOCS_524 =
	"https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-5xx-errors/error-524/";
const DOCS_LIMITS = "https://developers.cloudflare.com/fundamentals/reference/connection-limits/";
const DOCS_IPS = "https://developers.cloudflare.com/fundamentals/concepts/cloudflare-ip-addresses/";
const IP_LIST = "https://www.cloudflare.com/ips/";
const DOCS_ORIGIN_ANALYTICS = "https://developers.cloudflare.com/speed/origin-analytics/";
const DOCS_ZONE_ANALYTICS = "https://developers.cloudflare.com/analytics/account-and-zone-analytics/zone-analytics/";
const DOCS_RAY_ID = "https://developers.cloudflare.com/fundamentals/reference/cloudflare-ray-id/";
const DOCS_PROXY_STATUS = "https://developers.cloudflare.com/dns/proxy-status/";
const DOCS_ORIGIN_RULES = "https://developers.cloudflare.com/rules/origin-rules/";
const DOCS_CHINA = "https://developers.cloudflare.com/china-network/";

/** FAQ 一处定义：可见文本与 FAQPage JSON-LD 同源，保证逐字一致 */
const FAQ: Array<{ q: string; a: string }> = [
	{
		q: "522 错误是我这边的问题还是 Cloudflare 的问题？",
		a: "几乎总是你这边。522 的判定条件是 Cloudflare 向源站发出 SYN 之后 19 秒内没收到回应，或者连接建立后 90 秒内等不到确认——两条都描述同一件事：源站没有回话。真正需要怀疑 Cloudflare 的场景很少，判断方法是绕开代理直连源站 IP 测一次，如果直连也连不上，问题百分之百在源站或它前面的防火墙。",
	},
	{
		q: "522 和 524 有什么区别？",
		a: "522 是连接压根没建起来，524 是连上了但源站迟迟不给响应。125 秒那条线很好记：Cloudflare 等源站响应的默认上限是 125 秒，超过就是 524，说明你的应用在跑一个很慢的查询或者卡死了。522 则连应用层都没走到，问题在网络和防火墙那一层，看应用日志通常什么都看不到。",
	},
	{
		q: "网站时好时坏，一会 522 一会正常，是什么原因？",
		a: "间歇性 522 通常有三个来源：源站过载导致连接队列打满、源站防火墙或安全软件对回源 IP 触发了限速规则、以及边缘到源站这段链路本身在丢包。国内源站配境外边缘节点时第三种尤其常见，晚高峰更明显。先在仪表盘按源站状态码筛出 522 看它的时间分布，如果集中在固定时段，多半是负载或链路，而不是配置。",
	},
	{
		q: "源站在国内、用 Cloudflare 免费版会更容易出 522 吗？",
		a: "会。免费方案不使用中国大陆境内的数据中心，所以边缘节点在境外，而源站在境内，回源这一段是一条跨境链路。19 秒的连接窗口在跨境高峰期的丢包和抖动面前并不总是够用，表现就是间歇性 522，而源站日志里干干净净——因为 SYN 根本没到。要用境内节点需要企业级的 China Network 方案，并且要求域名已完成 ICP 备案。",
	},
	{
		q: "出现 522 时把橙云关掉改成 DNS only 能解决吗？",
		a: "能让页面恢复，但那不是修好，是绕过。改成 DNS only 之后访客直连你的源站，Cloudflare 不再参与，522 自然消失，代价是源站真实 IP 暴露、缓存和 WAF 防护同时失效。把它当成排查手段和临时止血是合理的：如果灰云状态下访问正常，就证明源站活着，问题在回源那一段。确认之后还是要把橙云开回去。",
	},
];

const RELATED: RelatedLink[] = [
	{
		href: "/guides/cloudflare-ssl-jiami-moshi",
		label: "Cloudflare 的 SSL/TLS 加密模式该选哪一个？",
		note: "522 是 TCP 都没连上；连上了却握手失败是 525、证书没过校验是 526，那两个归加密模式管。",
	},
	{
		href: "/guides/cloudflare-yuanzhan-ip-xielou",
		label: "套了 Cloudflare，源站真实 IP 还会泄露吗？",
		note: "把源站锁成「只放行 Cloudflare」正是 522 最常见的诱因之一；这篇讲清楚为什么值得冒这个险。",
	},
	{
		href: "/guides/cloudflare-tunnel-neiwang-chuantou",
		label: "没有公网 IP，怎么用 Cloudflare Tunnel 做内网穿透？",
		note: "让源站主动向外建连，回源不再需要开放任何入站端口，也就没有防火墙挡回源 IP 这类问题。",
	},
	{
		href: DOCS_522,
		label: "Cloudflare 官方文档：522 错误",
		note: "本文所有判定条件与成因清单的原始出处。",
		external: true,
	},
	{
		href: DOCS_LIMITS,
		label: "Cloudflare 官方文档：连接超时限制",
		note: "19 秒、90 秒、125 秒这些数字的完整表格，含哪些可调、哪些不可调。",
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

export default async function Error522GuideZh({ params }: { params: Promise<{ locale: string }> }) {
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
				lede="先别忙着重启服务器。522 卡在 Cloudflare 到源站这一段，跟访客到 Cloudflare 那一段无关——分清这一点，排查范围立刻少一半。"
				updated={guide.updated}
				readingTime={guide.readingTime}
				related={RELATED}
			>
				<div className="glass r-island note p-6 sm:p-7">
					<p>
						522 的意思是 Cloudflare 在 19 秒内没能和你的源站建立 TCP 连接。最常见的原因是源站防火墙挡掉了 Cloudflare 的回源 IP。
					</p>
				</div>

				<p>
					522 的错误页把三个环节画成一排：访客那一格是绿的，Cloudflare 那一格也是绿的，只有最右边的源站那一格写着 Host Error。这张图其实已经把答案说了七成——请求成功到了 Cloudflare，是从 Cloudflare 走向你的服务器时断的。
				</p>
				<p>
					所以刷新页面、清缓存、换个网络再试，对 522 一点用都没有。下面按官方文档的判定条件把它拆开，给一条从最可能到最少见的排查路线，最后单独讲国内源站特有的那种。
				</p>

				<h2 id="what-happens">522 到底卡在哪一步</h2>
				<p>
					Cloudflare 是反向代理，一次代理请求实际上是两段独立的 TCP 连接：访客到 Cloudflare 是一段，Cloudflare 到源站是另一段。两段各有各的超时表，522 只跟第二段有关。
				</p>
				<p>
					官方给的判定条件很具体，有两条，满足任意一条就返回 522：Cloudflare 发出 SYN 之后 19 秒内没有收到源站的 SYN+ACK；或者 TCP 连接已经建立，但 Cloudflare 在 90 秒内没有收到自己那个资源请求的确认。19 秒不是一次死等，中间会按 1、1、1、1、1、2、4、8 秒的间隔重试 SYN，全部落空才算超时。
				</p>
				<p>
					这两条说的是同一件事：源站没有回话。不是回得慢，是根本没回——慢有另外的错误码等着。
				</p>
				<OriginTimeouts />
				<p>
					这几个数字都写在官方的{" "}
					<a href={DOCS_LIMITS} target="_blank" rel="noopener noreferrer">
						连接超时限制
					</a>
					页上，除了 125 秒那条对企业方案可调，其余都是固定值，改不了。
				</p>

				<h2 id="which-code">先用错误码把范围缩小</h2>
				<p>52x 这一族看着都像「服务器挂了」，但它们卡在完全不同的位置，指向的排查对象也不一样。</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">错误码</th>
								<th scope="col">卡在哪</th>
								<th scope="col">典型原因</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">520</th>
								<td>源站回了话，但内容是空的或不合法</td>
								<td>应用崩溃、响应头超过 128 KB、源站的 HTTP/2 配置不对</td>
							</tr>
							<tr>
								<th scope="row">521</th>
								<td>源站明确拒绝了连接</td>
								<td>服务没起来、端口没监听、防火墙用 REJECT 而不是 DROP</td>
							</tr>
							<tr>
								<th scope="row">522</th>
								<td>连接建不起来，超时</td>
								<td>防火墙丢包、源站过载、DNS 记录里的 IP 是旧的</td>
							</tr>
							<tr>
								<th scope="row">523</th>
								<td>到不了源站，没有路由</td>
								<td>IP 不可达；云上路由表把 Cloudflare 用的网段引向了内网</td>
							</tr>
							<tr>
								<th scope="row">524</th>
								<td>连上了，但 125 秒内没给出响应</td>
								<td>慢查询、大数据导出、进程卡死</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					记一个对比就够了：<strong>521 是敲门有人回你「不行」，522 是敲门根本没人应</strong>。前者说明源站的网络栈活着、只是拒绝，后者连拒绝都没有。这个差别决定了你该去看服务进程，还是去看防火墙。各错误码的完整说明见{" "}
					<a href={DOCS_521} target="_blank" rel="noopener noreferrer">
						521
					</a>
					、
					<a href={DOCS_523} target="_blank" rel="noopener noreferrer">
						523
					</a>
					、
					<a href={DOCS_524} target="_blank" rel="noopener noreferrer">
						524
					</a>{" "}
					各自的文档页。
				</p>

				<h2 id="checklist">按这个顺序查，多数人在第二步就停了</h2>

				<h3 id="step-scope">第一步：先分清是全站、间歇，还是只有某些路径</h3>
				<p>
					这一步能省掉后面一大半功夫。全站持续 522，指向源站整体故障——机器关了，或防火墙把回源整个挡了。时好时坏，指向过载、连接数打满或链路丢包。只有某几个路径出错，多半是那几个接口把源站拖垮了。
				</p>
				<p>
					看分布别靠自己刷新。仪表盘的 HTTP Traffic 页可以按「源站状态码」筛出 522，配合{" "}
					<a href={DOCS_ZONE_ANALYTICS} target="_blank" rel="noopener noreferrer">
						域名分析
					</a>{" "}
					看时间分布和涉及的数据中心，注意这份错误分析是 1% 采样，小流量站点要多等一会。想按路径定位，用{" "}
					<a href={DOCS_ORIGIN_ANALYTICS} target="_blank" rel="noopener noreferrer">
						源站分析
					</a>{" "}
					看各个端点的 TCP 失败率。
				</p>

				<h3 id="step-firewall">第二步：防火墙有没有挡住回源 IP</h3>
				<p>
					官方把这一条列在 522 成因的第一位，实际排查中它的命中率也确实最高。原因不难理解：开了代理之后，源站看到的来源不再是成千上万个访客 IP，而是 Cloudflare 那几个共享网段，在防火墙眼里就是「少数几个 IP 在打大量请求」，自动封禁和限速规则很容易被触发。
				</p>
				<p>
					处理方式是把{" "}
					<a href={IP_LIST} target="_blank" rel="noopener noreferrer">
						官方 IP 段列表
					</a>{" "}
					整体放行，注意 IPv4 和 IPv6 是两份，源站有 AAAA 记录时回源可能走 v6，只放行 v4 会留下一半的坑。具体到 iptables、.htaccess 怎么写，
					<a href={DOCS_IPS} target="_blank" rel="noopener noreferrer">
						官方这一页
					</a>{" "}
					给了可以直接照抄的例子。这些网段会变，别把某次抄下来的清单当永久配置，最好定期同步。
				</p>
				<p>
					还有两件容易忽略的事。其一，要按地区限制访问，别在源站防火墙上做——那会连带把回源掐掉，用边缘上的 WAF 自定义规则才对。其二，应用日志里没有请求记录，不等于请求没到你的机房，中间的负载均衡、缓存层、云安全组各有各的日志，官方也提醒要一并翻。
				</p>

				<h3 id="step-ip">第三步：DNS 记录里的源站 IP 还对吗</h3>
				<p>
					换机器、重建实例、弹性 IP 被回收再分配，这些都会让 DNS 记录里的 A 记录指向一个已经不属于你的地址。请求发过去石沉大海，就是 522；如果那个地址完全不可达，则更可能是 523。核对一遍代理记录背后的真实 IP，比盯着服务器日志猜要快得多。
				</p>
				<p>
					用{" "}
					<a href={DOCS_ORIGIN_RULES} target="_blank" rel="noopener noreferrer">
						源站规则
					</a>{" "}
					改写过回源目标的，也要检查改写后的主机名能不能解析——官方举的例子是规则指向一个 A 记录为保留地址的主机名，结果就是 522。
				</p>

				<h3 id="step-origin">第四步：源站自己撑不住</h3>
				<p>
					连接队列满了、内存打爆了、数据库把工作进程占死了，源站都会开始丢掉新进来的 SYN。这类 522 的特征是跟着流量走：低谷正常，高峰出错。
				</p>
				<p>
					顺带看一眼源站有没有关掉 HTTP keep-alive。Cloudflare 会复用已建立的 TCP 连接，最长保持到 900 秒的空闲上限；官方明确建议源站开着 keep-alive，否则每个请求都要重新握手，连接重置会明显变多。
				</p>

				<h3 id="step-workers">第五步：两种看着莫名其妙的情况</h3>
				<p>
					一是 Workers。给 Worker 绑了自定义域之后，在这个 Worker 里 fetch 它自己的主机名会直接返回 522——请求绕回了自己。改用路由、换一个目标主机名，或者启用 global_fetch_strictly_public 兼容性标志都能解决。二是 Pages，要确认自定义域已配好、CNAME 指向的是 Pages 的域名。这两种情况下源站根本不是一台你能登录的机器，前面四步全都用不上。
				</p>

				<h2 id="china">国内源站的 522，常常不是同一回事</h2>

				<h3 id="china-crossborder">回源是一条跨境链路</h3>
				<p>
					Cloudflare 的免费方案不使用中国大陆境内的数据中心，大陆访客会被路由到香港、日本、新加坡等地的节点。如果你的服务器在境内，那么「边缘节点到源站」这一段就是一条实实在在的跨境链路，而 19 秒的连接窗口是按正常网络设计的，遇上晚高峰的丢包和抖动，八次 SYN 重试全部落空并不稀奇。
				</p>
				<p>
					这种 522 有个很好认的特征：源站日志里干干净净，什么异常都没有——因为 SYN 压根没送到。此时去查应用、查数据库、重启服务，全是白费力气。可选的路只有几条：把源站挪到香港或境外、接受这份抖动、上{" "}
					<a href={DOCS_CHINA} target="_blank" rel="noopener noreferrer">
						China Network
					</a>{" "}
					用境内节点（企业级方案，且要求域名已完成 ICP 备案），或者干脆不让 Cloudflare 主动回源。
				</p>

				<h3 id="china-firewall">安全组、系统防火墙、面板防火墙是三层</h3>
				<p>
					国内主机上放行回源 IP 的坑在于层数。云厂商控制台里有一层安全组，系统里有 iptables 或 firewalld，装了面板的话面板还自带一层，三层任意一层没放行，症状都一模一样。特别是那种「只允许国内 IP 访问」的收紧策略，一加上去就会把全部回源流量掐死，而它在很多人的印象里属于安全加固，不属于会出事的改动。
				</p>
				<p>
					与其逐层翻规则，不如直接在源站抓包看有没有来自 Cloudflare 网段的 SYN：抓不到，堵在机器外面；抓得到却没有回包，堵在机器里面。
				</p>

				<h3 id="china-tunnel">要不要干脆不开入站端口</h3>
				<p>
					还有一条思路是把连接方向反过来：让源站上的 cloudflared 主动向 Cloudflare 建立出站连接，请求沿着这条已有的连接下发。这样源站不需要开放任何入站端口，也就不存在「防火墙挡了回源 IP」「源站 IP 变了忘了改记录」这一整类问题，顺带还不需要固定公网 IP。它自己的限制另有一套，写在{" "}
					<Link href="/guides/cloudflare-tunnel-neiwang-chuantou">内网穿透那一篇</Link>
					里，动手前值得先看一眼。
				</p>

				<h2 id="verify">改完之后怎么确认真的好了</h2>
				<p>
					刷新一下能打开不算验证——522 常常是间歇的。几个更靠谱的做法：
				</p>
				<ul>
					<li>
						记下错误页上的 <code>cf-ray</code>，也就是{" "}
						<a href={DOCS_RAY_ID} target="_blank" rel="noopener noreferrer">
							Ray ID
						</a>
						，它能把某一次具体的请求定位到具体的数据中心，比描述「大概几点出错」有用得多。
					</li>
					<li>
						访问 <code>/cdn-cgi/trace</code>，确认自己这次请求落在哪个节点上，跨地区对比时尤其有用。
					</li>
					<li>在 HTTP Traffic 页按源站状态码持续观察一段时间，看 522 的曲线是不是真的归零，而不是恰好被采样漏掉了。</li>
					<li>用源站分析盯 TCP 失败率和 P95 响应时间，前者接近零、后者离 125 秒还远，才算稳。</li>
				</ul>
				<p>
					如果需要立刻止血，可以把对应的 DNS 记录临时改成{" "}
					<a href={DOCS_PROXY_STATUS} target="_blank" rel="noopener noreferrer">
						DNS only（灰云）
					</a>
					，让访客直连源站。这同时也是个干净的判断手段：灰云下能正常访问，就说明源站活着。代价见文末问答，确认完记得改回去。
				</p>
				<p>
					要联系托管商或提交工单，官方列了必须一起给出的信息：错误码、出错时间与时区、完整 URL，见{" "}
					<a href={DOCS_5XX} target="_blank" rel="noopener noreferrer">
						5xx 错误总览
					</a>
					。少了这三样，对方基本查不动。
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
