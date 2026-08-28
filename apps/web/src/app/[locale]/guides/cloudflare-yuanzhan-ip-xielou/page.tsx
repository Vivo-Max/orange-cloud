import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { setRequestLocale } from "next-intl/server";
import GuideShell, { type RelatedLink } from "@/components/guides/GuideShell";
import OriginLeakPaths from "@/components/guides/OriginLeakPaths";
import { guideBySlug, guidePath, GUIDE_LOCALE_ZH } from "@/lib/guides/guides";

const SITE_URL = "https://oss.omail.us.kg";
const guide = guideBySlug("cloudflare-yuanzhan-ip-xielou", GUIDE_LOCALE_ZH);
const PATH = guidePath(GUIDE_LOCALE_ZH, `/guides/${guide.slug}`);

const DOCS_PROXY = "https://developers.cloudflare.com/dns/proxy-status/";
const DOCS_PROXY_LIMITS = "https://developers.cloudflare.com/dns/proxy-status/limitations/";
const DOCS_PROXY_USECASES = "https://developers.cloudflare.com/dns/proxy-status/use-cases/";
const DOCS_CF_IPS = "https://developers.cloudflare.com/fundamentals/concepts/cloudflare-ip-addresses/";
const IPS_LIST = "https://www.cloudflare.com/ips/";
const DOCS_PROTECT = "https://developers.cloudflare.com/fundamentals/security/protect-your-origin-server/";
const DOCS_AOP = "https://developers.cloudflare.com/ssl/origin-configuration/authenticated-origin-pull/";
const DOCS_AOP_GLOBAL = "https://developers.cloudflare.com/ssl/origin-configuration/authenticated-origin-pull/set-up/global/";
const DOCS_PORTS = "https://developers.cloudflare.com/fundamentals/reference/network-ports/";
const DOCS_CT = "https://developers.cloudflare.com/ssl/edge-certificates/additional-options/certificate-transparency-monitoring/";
const DOCS_TRANSFORM = "https://developers.cloudflare.com/rules/transform/request-header-modification/";
const DOCS_TUNNEL = "https://developers.cloudflare.com/tunnel/";
const DOCS_CHINA = "https://developers.cloudflare.com/china-network/";
const DOCS_SSL_MODES = "https://developers.cloudflare.com/ssl/origin-configuration/ssl-modes/";
const DOCS_DC_MX =
	"https://developers.cloudflare.com/dns/manage-dns-records/troubleshooting/unexpected-dns-records/";

/** FAQ 一处定义：可见文本与 FAQPage JSON-LD 同源，保证逐字一致 */
const FAQ: Array<{ q: string; a: string }> = [
	{
		q: "换了 Cloudflare 之后，源站真实 IP 还能被查到吗？",
		a: "能。代理只改变 A、AAAA、CNAME 这三类记录在 HTTP 与 HTTPS 上的解析结果，域名下其余的东西照旧。一个忘了切橙云的子域、一条 MX 记录、一段写死 ip4: 的 SPF、一份接入之前留下的历史解析，任意一条都足以把源站地址交出去。别只看仪表盘里那几朵橙色云。",
	},
	{
		q: "怎么确认自己的源站 IP 有没有泄露？",
		a: "先跑 dig 你的域名 mx +short，看返回里有没有 _dc-mx 开头的主机名，有就再解析它一次，出来的多半就是源站真实地址。再用 dig 你的域名 txt +short 看 SPF 里有没有写死的 ip4: 段。再把 DNS 记录表翻一遍，数还剩几条灰云的 A 记录。",
	},
	{
		q: "在源站只放行 Cloudflare 的 IP 段，够安全吗？",
		a: "够用，但不是最强的一档。Cloudflare 自己把这种做法归为「中等安全」，理由是它依赖源 IP 判断，而源 IP 可以伪造，并且这些地址段由全体代理客户共用。想再紧一档，就叠加 Authenticated Origin Pulls，或者改用 Tunnel 让源站不再有对外监听的端口。",
	},
	{
		q: "Authenticated Origin Pulls 免费版能用吗？",
		a: "能，官方可用性表格里免费、Pro、Business、企业四档全是可用。前提是 SSL/TLS 加密模式必须为完全或完全（严格），设成关闭或灵活时不生效，源站也要改配置去校验证书。注意默认那张全局证书由所有账号共用，只能证明请求来自 Cloudflare 网络，证明不了来自你的账号。",
	},
	{
		q: "源站 IP 已经泄露了，必须换 IP 吗？",
		a: "要防住针对性攻击就必须换，因为历史解析是公开的、留在第三方存档里删不掉，堵住泄露路径也改变不了那个已经被记下的地址，Cloudflare 的文档里也把接入后轮换源站 IP 写成了推荐动作。但换 IP 只是把计时器归零，路径没堵上，新地址过不了多久照样会被问出来。",
	},
];

const RELATED: RelatedLink[] = [
	{
		href: "/guides/cloudflare-cname-zhanping",
		label: "Cloudflare 的 CNAME 展平（拉平）到底做了什么？",
		note: "顶点上的灰云 CNAME 会被展平成一条普通 A 记录，源站 IP 就这么摆到了台面上。",
	},
	{
		href: "/guides/cloudflare-xiaohuangyun",
		label: "Cloudflare 的小黄云到底是什么？什么时候该关掉？",
		note: "泄露的前提是这条记录本来该被代理挡住。小黄云能挡什么、挡不住什么，先从开关本身说起。",
	},
	{
		href: "/guides/cloudflare-huoqu-zhenshi-ip",
		label: "开了 Cloudflare 之后，怎么在源站拿到访客真实 IP？",
		note: "把源站收进 Cloudflare 之后的另一半功课：访客地址挪到了请求头里，取它之前要先划好信任边界。",
	},
	{
		href: "/guides/cloudflare-522-error",
		label: "Cloudflare 为什么会报 522 错误？",
		note: "把源站锁到只放行 Cloudflare 之后最容易撞上的副作用：回源被自己的防火墙挡掉。",
	},
	{
		href: "/guides/cloudflare-tunnel-neiwang-chuantou",
		label: "没有公网 IP，怎么用 Cloudflare Tunnel 做内网穿透？",
		note: "本文里最彻底的那条方案展开讲：源站主动出站建连，压根不留对外监听的端口。",
	},
	{
		href: DOCS_PROTECT,
		label: "Cloudflare 官方文档：保护源站服务器",
		note: "本文四种加固手段的安全强度与可用套餐，都出自这一页的对照表。",
		external: true,
	},
	{
		href: DOCS_AOP,
		label: "Cloudflare 官方文档：Authenticated Origin Pulls",
		note: "三种配置层级的差别、与加密模式的关系，以及和 Tunnel 不兼容的说明。",
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

export default async function OriginIpLeakGuideZh({ params }: { params: Promise<{ locale: string }> }) {
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
				lede="橙色云朵给人一种「藏好了」的错觉。实际上它只盖住了最显眼的那一条路，域名下还有好几个地方仍然在原样回答「我的服务器在哪」。"
				updated={guide.updated}
				readingTime={guide.readingTime}
				related={RELATED}
			>
				<div className="glass r-island note p-6 sm:p-7">
					<p>
						会。橙云挡住的只有 A、AAAA、CNAME 记录上的 HTTP 流量；灰云子域、MX 应答、SPF 记录、历史解析和证书日志都在这一层之外，任何一条都能问出源站
						IP。
					</p>
				</div>

				<p>
					「网站被 CC 打了，套个 Cloudflare 就好了」——这话成立的前提是攻击者只知道域名。一旦对方手里有源站真实 IP，请求可以直接绕过 Cloudflare
					打到你的机器上，你配的那些 WAF 规则、速率限制、DDoS 防护，一条都不会触发。
				</p>
				<p>而泄露往往不是被谁「破解」的，是你自己在别处原样公布的。</p>

				<h2 id="what-proxy-hides">代理盖住的，只是其中一层</h2>
				<p>
					把一条记录切成橙云之后，向这个名字发起的 DNS 查询会拿到 Cloudflare 的 anycast 地址，而不是你填在记录里的那个 IP。
					<a href={DOCS_PROXY} target="_blank" rel="noopener noreferrer">
						代理状态文档
					</a>
					把这件事写得很直白：它控制的是 HTTP 与 HTTPS 流量走不走 Cloudflare 的网络。这句话里有三个限定词。
				</p>
				<p>
					一是记录类型，只有 A、AAAA、CNAME 能被代理，MX、TXT、SRV 这些
					<a href={DOCS_PROXY_LIMITS} target="_blank" rel="noopener noreferrer">
						根本开不了
					</a>
					。二是端口，默认只代理那十几个 HTTP/HTTPS 端口（HTTP 侧 80、8080、8880、2052、2082、2086、2095，HTTPS 侧
					443、2053、2083、2087、2096、8443），数据库、SSH、面板挂在非标端口上，那个子域就一定是灰的，清单见{" "}
					<a href={DOCS_PORTS} target="_blank" rel="noopener noreferrer">
						网络端口文档
					</a>
					。三是「解析结果」——代理改的是别人问你要地址时你给的答案，机器还在那个 IP 上，端口还开着，谁猜中了照样连得上。
				</p>
				<OriginLeakPaths />

				<h2 id="leak-paths">绕开代理的五条路</h2>

				<h3 id="leak-gray">灰云子域：最常见，也最容易忘</h3>
				<p>
					一个域名下往往不止一个 A 记录，<code>mail</code>、<code>ftp</code>、<code>cpanel</code>、<code>bt</code>、<code>db</code>
					——要么协议不兼容代理，要么因为「代理了会出问题」被手工切成灰云，指向的通常就是同一台机器。官方的措辞是「暴露源站 IP
					地址，去掉了针对定向攻击的一层防护」。麻烦的是很多人是排查故障时临时切灰的，切完就忘了。分类见{" "}
					<a href={DOCS_PROXY_USECASES} target="_blank" rel="noopener noreferrer">
						使用场景文档
					</a>
					。
				</p>

				<h3 id="leak-mail">邮件：一条你没建过的记录会替你回答</h3>
				<p>
					这条最阴，因为它在记录表里看不见。如果 MX 指向的主机名正好是那个已经切成橙云的网站主机名，Cloudflare
					不会让邮件走代理（那会直接打断收信），于是在应答里动态插入一条你从没建过的记录：前缀 <code>_dc-mx</code>{" "}
					的子域，解析结果就是源站真实 IP。SRV 记录同理，用 <code>dc-</code> 前缀。
				</p>
				<p>
					也就是说，一条 <code>dig 你的域名 mx +short</code> 就能把地址钓出来。机制说明见{" "}
					<a href={DOCS_DC_MX} target="_blank" rel="noopener noreferrer">
						官方的记录异常排查页
					</a>
					。邮件还有第二条路径和 DNS 无关：往不存在的地址发信，退信里带着邮件服务器地址。所以官方专门建议过，别把邮件服务和想保护的网站放同一台机器。
				</p>

				<h3 id="leak-history">历史解析：接入之前的那份记录还留着</h3>
				<p>
					域名接入 Cloudflare 之前，A 记录指向哪里是公开信息，早被各种被动 DNS
					存档抄走过。你今天切成橙云，改不了别人三年前抄走的那一份。官方文档说得很干脆：DNS 记录属于公开信息，历史记录会被保留——对策不是「藏起来」而是「换掉」。
				</p>

				<h3 id="leak-ct">证书透明度日志：你申请过的每张证书都记着</h3>
				<p>
					任何一张公共 CA 签发的证书都会写进公开的证书透明度日志。如果你给源站单独申请过证书——比如为了配{" "}
					<a href={DOCS_SSL_MODES} target="_blank" rel="noopener noreferrer">
						完全（严格）加密模式
					</a>
					，或者某个内部子域用了自动续期的免费证书——那个主机名就进了日志。日志本身不含 IP，但它把「有哪些子域存在」变成了公开清单，剩下的只是逐个解析一遍。
				</p>
				<p>
					Cloudflare 的{" "}
					<a href={DOCS_CT} target="_blank" rel="noopener noreferrer">
						证书透明度监控
					</a>
					默认是关的，打开并填上邮箱，之后有覆盖你域名的证书进了公共日志就会收到提醒。文档里也点了 <code>crt.sh</code> 这类工具，可以先拿它自查一遍。
				</p>

				<h3 id="leak-pending">刚接入的那 24 小时</h3>
				<p>
					这条几乎没人知道。域名刚加进 Cloudflare 时区域处于待激活状态，最长可能持续 24
					小时；这段时间里即便记录已经标成代理，实际仍按灰云响应，查到的都是源站真实 IP。这正是官方建议「激活后再轮换一次源站 IP」的直接原因，说明在{" "}
					<a href={DOCS_PROXY_LIMITS} target="_blank" rel="noopener noreferrer">
						代理限制文档
					</a>
					里。
				</p>

				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">路径</th>
								<th scope="col">为什么在代理之外</th>
								<th scope="col">怎么堵</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">灰云子域</th>
								<td>协议或端口不兼容代理，只能直连</td>
								<td>能改橙云的改回去；改不了的换机器，或用隧道接入</td>
							</tr>
							<tr>
								<th scope="row">MX 的 _dc-mx 应答</th>
								<td>邮件不能走代理，查询时插入直连记录</td>
								<td>邮件用独立主机名、独立机器</td>
							</tr>
							<tr>
								<th scope="row">SPF / TXT 里的 ip4:</th>
								<td>文本记录永远是灰云，内容原样公开</td>
								<td>把写死的地址段换成发信服务商的 include</td>
							</tr>
							<tr>
								<th scope="row">历史解析记录</th>
								<td>接入之前的公开数据，第三方已存档</td>
								<td>只能换 IP，删不掉</td>
							</tr>
							<tr>
								<th scope="row">证书透明度日志</th>
								<td>公共 CA 签发的证书全部公开可查</td>
								<td>源站改用 Cloudflare 源站证书；开 CT 监控</td>
							</tr>
						</tbody>
					</table>
				</div>

				<h2 id="self-check">花五分钟自查一遍</h2>
				<ol>
					<li>
						<code>dig 你的域名 mx +short</code>：返回里出现 <code>_dc-mx</code> 开头的主机名，就再解析它一次，出来的多半就是源站。
					</li>
					<li>
						<code>dig 你的域名 txt +short</code>：看 SPF 里有没有写死的 <code>ip4:</code> 段，那通常就是发信机器。
					</li>
					<li>打开 DNS 记录表，数还剩几条灰云的 A 和 AAAA，每条都问一句「它为什么是灰的」。</li>
					<li>从外网直接对源站 IP 发一条带正确 Host 头的请求。原样返回了站点内容，说明源站对任何来源照单全收。</li>
				</ol>
				<p>顺带一句：扫到 Cloudflare 地址段上开着一堆非标端口是正常的，那些地址由全体客户共用，官方文档解释过。</p>

				<h2 id="rotate-ip">换一次 IP，是唯一能把计时器归零的动作</h2>
				<p>
					前面五条路里有两条是「已经发生过」的——历史解析和 24 小时激活窗口。它们堵不住，泄露发生在过去、数据已经在别人手里，唯一能让它们失效的办法就是换掉那个地址。
				</p>
				<p>
					顺序很重要：先把还在漏的路径处理干净，再换 IP，最后把源站锁上。倒过来做，新地址当天就会顺着没堵的那条路被重新记下来。国内主机换公网 IP
					往往还得走服务商工单，成本要单独算。
				</p>

				<h2 id="lock-origin">真正的防线在源站上，不在 DNS 里</h2>
				<p>
					藏 IP 是概率问题，锁源站是确定性问题。只要源站只接受来自 Cloudflare
					的连接，泄不泄露就从「会不会被打穿」降级成了「会不会被无效流量骚扰」。官方加固清单里，非企业版能用的是这几种。
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">做法</th>
								<th scope="col">官方给的安全强度</th>
								<th scope="col">代价</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">只放行 Cloudflare IP 段</th>
								<td>中等</td>
								<td>要维护地址段清单；依赖源 IP 判断，可被伪造</td>
							</tr>
							<tr>
								<th scope="row">密钥请求头校验</th>
								<td>中等</td>
								<td>两端都要改配置；覆盖 Host 头的规则会带来例外</td>
							</tr>
							<tr>
								<th scope="row">Authenticated Origin Pulls</th>
								<td>很强</td>
								<td>需要完全或完全（严格）模式；源站要装证书</td>
							</tr>
							<tr>
								<th scope="row">Cloudflare Tunnel</th>
								<td>很强</td>
								<td>源站要跑 cloudflared 守护进程</td>
							</tr>
						</tbody>
					</table>
				</div>

				<h3 id="lock-iptables">只放行 Cloudflare 的 IP 段</h3>
				<p>
					门槛最低的一档：在源站防火墙上放行{" "}
					<a href={IPS_LIST} target="_blank" rel="noopener noreferrer">
						Cloudflare 公布的地址段
					</a>
					的 80 和 443，其余来源全部丢弃。官方 iptables 例子就是「逐段 ACCEPT，最后一条 DROP 兜底」这个形态；IPv4 和 IPv6
					两套规则都要配，只配一套等于留后门。
				</p>
				<p>
					它被归为中等强度有两个原因：源 IP 可以伪造；这些地址由全体代理客户共用，放行之后别人的 Cloudflare
					账号理论上也能打到你这里。配完如果站点直接 522，多半是规则写反了或漏了某个段，
					<a href={DOCS_CF_IPS} target="_blank" rel="noopener noreferrer">
						地址段文档
					</a>
					和延伸阅读里那篇 522 排查值得一起看。
				</p>

				<h3 id="lock-aop">Authenticated Origin Pulls：让源站验证书</h3>
				<p>
					紧一档的做法，是 TLS 握手时让 Cloudflare 出示一张客户端证书，源站验过才放行。四个套餐都能用，含免费版。两个前提：
					<a href={DOCS_SSL_MODES} target="_blank" rel="noopener noreferrer">
						加密模式
					</a>
					必须是完全或完全（严格），设成关闭或灵活时不生效；源站也要改配置校验这张证书。
				</p>
				<p>
					一个细节：
					<a href={DOCS_AOP_GLOBAL} target="_blank" rel="noopener noreferrer">
						默认那张全局证书
					</a>
					由所有账号共用，只能证明「请求来自 Cloudflare 网络」，证明不了「来自你的账号」，要那层保证得自己上传证书。它也和 Tunnel
					不兼容——隧道是出站连接，源站上没有监听端口可以让 Cloudflare 递证书。
				</p>

				<h3 id="lock-tunnel">Tunnel：干脆不留对外监听的端口</h3>
				<p>
					最彻底的一档，也是官方清单里唯一一个「源站不需要可路由的公网地址」的方案：源站跑 <code>cloudflared</code>{" "}
					主动向 Cloudflare 建立出站连接。没有入站端口，也就没有「泄露之后被直连」这回事——地址泄了也无处可打。展开在{" "}
					<a href={DOCS_TUNNEL} target="_blank" rel="noopener noreferrer">
						官方文档
					</a>
					和延伸阅读里那篇内网穿透指南。
				</p>

				<h3 id="lock-header">密钥请求头：改动最小的折中</h3>
				<p>
					源站不方便动 TLS 配置的话，还有一条路：用{" "}
					<a href={DOCS_TRANSFORM} target="_blank" rel="noopener noreferrer">
						请求头转换规则
					</a>
					给所有经过 Cloudflare 的请求加一个只有你知道的头，源站上没带这个头的一律拒掉。同样是中等强度：两边改动都小，但这个头本质上是个长期不变的共享密钥。
				</p>

				<h2 id="china">国内环境下的几个额外坑</h2>
				<p>
					最常见的一个：把源站锁成「只放行 Cloudflare」之后站点直接不通。原因是免费方案不使用中国大陆境内的数据中心，回源请求从境外节点发过来，而国内主机上一种很流行的安全策略正好是「封掉境外
					IP」。两条规则撞在一起，表现就是 522。配完加固规则务必立刻验证一次。
				</p>
				<p>
					其次是一键面板的全家桶：网站、邮件、数据库、phpMyAdmin 装在同一台机器上，正好同时踩中前面三条路径。真要认真藏，第一步是把邮件搬出去。
				</p>
				<p>
					还有个关于境内节点的常见误解：想让大陆访客走境内节点，需要的是{" "}
					<a href={DOCS_CHINA} target="_blank" rel="noopener noreferrer">
						Cloudflare China Network
					</a>
					，它是企业版之上的单独订阅，由京东云运营境内数据中心，且要求每个主域都有有效的 ICP 备案。免费方案里没有这个选项。
				</p>
				<p>
					最后一点提醒：源站锁上之后，你自己那些直连 IP 的监控、CI、备份脚本也会一起被挡在门外，记得把它们一并列进白名单。
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
