import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Link } from "@/i18n/navigation";
import { setRequestLocale } from "next-intl/server";
import GuideShell, { type RelatedLink } from "@/components/guides/GuideShell";
import ProxyToggle from "@/components/guides/ProxyToggle";
import { guideBySlug, guidePath, GUIDE_LOCALE_ZH } from "@/lib/guides/guides";

const SITE_URL = "https://oss.omail.us.kg";
const guide = guideBySlug("cloudflare-xiaohuangyun", GUIDE_LOCALE_ZH);
const PATH = guidePath(GUIDE_LOCALE_ZH, `/guides/${guide.slug}`);

const DOCS_PROXY = "https://developers.cloudflare.com/dns/proxy-status/";
const DOCS_LIMITS = "https://developers.cloudflare.com/dns/proxy-status/limitations/";
const DOCS_USECASES = "https://developers.cloudflare.com/dns/proxy-status/use-cases/";
const DOCS_PORTS = "https://developers.cloudflare.com/fundamentals/reference/network-ports/";
const DOCS_ANYCAST = "https://developers.cloudflare.com/fundamentals/concepts/cloudflare-ip-addresses/";
const DOCS_HEADERS = "https://developers.cloudflare.com/fundamentals/reference/http-headers/";
const DOCS_FLATTENING = "https://developers.cloudflare.com/dns/cname-flattening/";
const DOCS_SPECTRUM = "https://developers.cloudflare.com/spectrum/";
const DOCS_CHINA = "https://developers.cloudflare.com/china-network/";
const DOCS_ICP = "https://developers.cloudflare.com/china-network/concepts/icp/";
const DOCS_VENDOR = "https://developers.cloudflare.com/dns/manage-dns-records/reference/vendor-specific-records/";
const DOCS_STATUS = "https://developers.cloudflare.com/dns/zone-setups/reference/domain-status/";
const DOCS_CONNLIMITS = "https://developers.cloudflare.com/fundamentals/reference/connection-limits/";
const DOCS_1014 =
	"https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-1xxx-errors/error-1014/";

/** FAQ 一处定义：可见文本与 FAQPage JSON-LD 同源，保证逐字一致 */
const FAQ: Array<{ q: string; a: string }> = [
	{
		q: "小黄云和灰云有什么区别？",
		a: "区别只在一件事上：DNS 应答里返回谁的地址。小黄云（已代理）返回 Cloudflare 的任播 IP，浏览器连到的是边缘节点，缓存、WAF、DDoS 防护、SSL 证书都在那一层生效，源站真实 IP 不出现在应答里。灰云（DNS only）返回源站自己的 IP，Cloudflare 只当权威 DNS 用，HTTP 流量不经过它，也就没有防护和 HTTP 分析数据。",
	},
	{
		q: "开了小黄云网站打不开是怎么回事？",
		a: "四种情况最常见：服务跑在代理范围之外的端口上，比如宝塔面板默认的 8888；记录指向第三方建站平台或别家 CDN，两层代理互相打架；证书模式配成了源站不支持的那一档，握手失败或重定向循环；域名刚接入还没激活，记录暂时按仅 DNS 处理。把记录临时改回灰云，如果立刻正常，问题就在代理这一跳上。",
	},
	{
		q: "MX 记录为什么不能开小黄云？",
		a: "能开代理的只有 A、AAAA、CNAME 这三种用于地址解析的记录，MX、TXT、NS 压根没有这个开关。因为代理只处理 HTTP 和 HTTPS 流量，而邮件走的是 SMTP。如果 MX 指向的主机名和已开小黄云的网站是同一个，Cloudflare 会在应答里自动加上 _dc-mx 前缀让邮件绕开代理——这也意味着那个前缀会把源站 IP 暴露出去。",
	},
	{
		q: "开了小黄云还需要备案吗？",
		a: "备案义务看的是你的服务器和业务本身，不是流量经过了谁。Cloudflare 的免费方案不使用中国大陆境内的数据中心，开小黄云不会在 Cloudflare 这一侧产生备案手续。反过来，要用 Cloudflare 的大陆节点得上 China Network，那是 Enterprise 方案之外单独订阅的服务，并且要求每一个主域都持有有效的 ICP 备案或许可证。",
	},
	{
		q: "开了小黄云之后国内访问会变慢吗？",
		a: "有可能，这是国内用户该先算清楚的一笔账。免费方案的请求会被路由到境外数据中心，大陆访客等于多绕一段跨境链路；源站本来就在国内的话，这一绕就是净损失。反过来，访客分散在全球、或更看重挡掉扫描和攻击流量，多这一跳通常划算。",
	},
];

const RELATED: RelatedLink[] = [
	{
		href: "/guides/cloudflare-cname-zhanping",
		label: "Cloudflare 的 CNAME 展平（拉平）到底做了什么？",
		note: "代理开关也决定了展平后返回什么：开着返回任播 IP，关着返回展平出来的真实 IP。",
	},
	{
		href: "/guides/cloudflare-ssl-jiami-moshi",
		label: "Cloudflare 的 SSL/TLS 加密模式该选哪一个？",
		note: "小黄云一开，证书就在 Cloudflare 那一层终止了。回源那一段加不加密、验不验证，由加密模式决定。",
	},
	{
		href: "/guides/cloudflare-huoqu-zhenshi-ip",
		label: "开了 Cloudflare 之后，怎么在源站拿到访客真实 IP？",
		note: "小黄云打开后日志里全是 Cloudflare 的地址，真实 IP 在哪个头、Nginx 与 Apache 怎么配。",
	},
	{
		href: "/guides/cloudflare-yuanzhan-ip-xielou",
		label: "套了 Cloudflare，源站真实 IP 还会泄露吗？",
		note: "小黄云挡住的只是这一条记录，灰云子域、MX 应答、SPF 与历史解析仍然会把地址交出去。",
	},
	{
		href: "/guides/cloudflare-522-error",
		label: "Cloudflare 为什么会报 522 错误？",
		note: "开了小黄云才会遇到的报错：边缘节点连不上源站，19 秒之后返回 522。",
	},
	{
		href: "/guides/cloudflare-dns-jiexi-bu-shengxiao",
		label: "改了 DNS 解析，为什么一直不生效？",
		note: "开着小黄云改源站 IP，dig 出来的结果一个字都不会变——这不是没生效，是它本来就不返回源站地址。",
	},
	{
		href: "/guides/cloudflare-huancun-mingzhonglv",
		label: "Cloudflare 到底缓存了什么？为什么命中率一直上不去",
		note: "缓存和缓存规则只对代理流量生效——灰云记录的请求根本不经过 Cloudflare。",
	},
	{
		href: DOCS_PROXY,
		label: "Cloudflare 官方文档：代理状态",
		note: "本文所有行为描述的原始出处，含固定 TTL、同名记录混用与 CNAME 链的规则。",
		external: true,
	},
	{
		href: DOCS_USECASES,
		label: "Cloudflare 官方文档：该代理与不该代理的场景",
		note: "邮件、域名验证、SaaS 建站、非 HTTP 协议各自为什么必须保持仅 DNS。",
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

export default async function ProxyStatusGuideZh({ params }: { params: Promise<{ locale: string }> }) {
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
				lede="橙云、橙色云朵、代理开关，说的都是同一个东西。它比看上去要「重」——拨过去的不只是一个图标，而是整条链路的走向。"
				updated={guide.updated}
				readingTime={guide.readingTime}
				related={RELATED}
			>
				<div className="glass r-island note p-6 sm:p-7">
					<p>
						小黄云是 DNS 记录上的代理开关。开着，域名解析到 Cloudflare 的任播 IP，流量先过 Cloudflare
						再回源站；关掉（灰云）就直接解析到源站真实 IP。
					</p>
				</div>

				<p>
					在 Cloudflare 的 DNS 记录列表里，每条 A、AAAA、CNAME 记录后面都跟着一个云朵图标。橙色的那个，中文圈习惯叫它「小黄云」——严格说是橙色，但叫顺了口就没人改了；官方文档里它的名字是{" "}
					<strong>Proxied</strong>，灰色那个是 <strong>DNS only</strong>。
				</p>
				<p>
					不少人接触 Cloudflare 就是冲着它来的：打开就能隐藏源站 IP、能挡 CC、还能白嫖 CDN
					加速。这话不算错，但都少说了一半。下面把它真正做了什么、哪些记录必须关、国内环境要额外算哪笔账，一次讲清楚。
				</p>

				<h2 id="what-changes">拨过去，变的其实是 DNS 应答</h2>
				<p>
					这个开关唯一直接改变的东西是：别人查询你的域名时，Cloudflare 给出的是谁的 IP 地址。
				</p>
				<ProxyToggle />
				<p>
					关着（灰云）的时候，Cloudflare 只是你的权威 DNS 服务器。有人查{" "}
					<code>www.example.com</code>，它老老实实返回你填的那个 <code>203.0.113.10</code>，浏览器直接连过去。整个 HTTP
					过程 Cloudflare 完全不参与，你在仪表盘上打开的缓存、WAF、页面规则，对这条记录一律无效——它甚至看不到这些请求，所以 HTTP
					侧的分析数据也是空的。
				</p>
				<p>
					开着（小黄云）的时候，同一个查询返回的是 Cloudflare 的{" "}
					<a href={DOCS_ANYCAST} target="_blank" rel="noopener noreferrer">
						任播 IP
					</a>
					——一批全球共用的共享地址，访客的请求会落在离他最近的那个数据中心。请求先在边缘节点上过一遍缓存、WAF 规则、DDoS
					防护和证书，再由 Cloudflare 用你填的那个 <code>203.0.113.10</code> 回源。源站的真实地址就此从 DNS 应答里消失。
				</p>
				<p>
					所以「开了小黄云就隐藏了源站 IP」这句话要加个限定词：隐藏的是<strong>这一条记录</strong>上的地址。同域名下的其它记录、历史解析、证书透明度日志，都不归这个开关管。
				</p>

				<h2 id="which-records">哪些记录能开，哪些天生没这个开关</h2>
				<p>
					能开代理的只有三种记录类型：<strong>A、AAAA、CNAME</strong>。这三种是用来解析地址的，Cloudflare 才有东西可换。MX、TXT、NS、SRV
					这些类型的记录旁边根本不会出现云朵图标，它们永远是仅 DNS。
				</p>
				<p>再往下还有几条规则，容易在改记录的时候踩到：</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">情况</th>
								<th scope="col">实际行为</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">同名多条 A 记录，一橙一灰</th>
								<td>只要有一条是橙的，这个主机名下的所有 A / AAAA 记录都按已代理处理。想灰就得全灰</td>
							</tr>
							<tr>
								<th scope="row">CNAME 链上某一环是橙的</th>
								<td>整条链的流量都被代理。同账号内的多个域名互相 CNAME 时尤其容易出现这种「隔空生效」</td>
							</tr>
							<tr>
								<th scope="row">CNAME 指向别人 Cloudflare 账号下的域名</th>
								<td>
									被禁止，访客会看到{" "}
									<a href={DOCS_1014} target="_blank" rel="noopener noreferrer">
										1014 错误
									</a>
								</td>
							</tr>
							<tr>
								<th scope="row">已代理的 CNAME 记录</th>
								<td>
									默认被
									<a href={DOCS_FLATTENING} target="_blank" rel="noopener noreferrer">
										展平
									</a>
									，因为返回的本来就是任播 IP，不再返回目标主机名
								</td>
							</tr>
							<tr>
								<th scope="row">已代理记录的 TTL</th>
								<td>固定为 Auto（300 秒），不可修改。因为任播 IP 可能变，Cloudflare 不允许解析器缓存超过五分钟</td>
							</tr>
							<tr>
								<th scope="row">某些 CNAME 目标</th>
								<td>
									Cloudflare 直接不让你开。<code>dkim.amazonses.com</code>、<code>acm-validations.aws</code>、
									<code>*.onmicrosoft.com</code> 这类邮件签名与证书验证用的目标在
									<a href={DOCS_LIMITS} target="_blank" rel="noopener noreferrer">
										名单
									</a>
									里，开了必坏，所以干脆拦住
								</td>
							</tr>
						</tbody>
					</table>
				</div>

				<h2 id="on-or-off">该开还是该关：按记录用途对照</h2>
				<p>
					官方给的原则很简单：<strong>凡是承载 HTTP / HTTPS 网页流量的 A、AAAA、CNAME 记录，都应该开</strong>
					；其余的保持灰云。难点在于「其余」具体有哪些。下面这张表按国内常见的服务列一遍。
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">这条记录用来做什么</th>
								<th scope="col">开关</th>
								<th scope="col">为什么</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">主域、www、博客、API 接口</th>
								<td>小黄云</td>
								<td>标准网页流量，缓存、WAF、DDoS 防护全都指望它</td>
							</tr>
							<tr>
								<th scope="row">宝塔面板、群晖 DSM 等后台</th>
								<td>看端口</td>
								<td>端口落在代理支持范围内才行，宝塔默认的 8888 就不在其中</td>
							</tr>
							<tr>
								<th scope="row">SSH、FTP、远程桌面、游戏服</th>
								<td>灰云</td>
								<td>代理只处理 HTTP / HTTPS，非 HTTP 连接会被直接丢弃</td>
							</tr>
							<tr>
								<th scope="row">企业邮箱的 mail 主机记录</th>
								<td>灰云</td>
								<td>SMTP 走 25 端口，不在代理范围内，开了收不到信</td>
							</tr>
							<tr>
								<th scope="row">域名所有权验证的 CNAME</th>
								<td>灰云</td>
								<td>验证方要比对应答里的目标值，代理后返回的是任播 IP，比对不上</td>
							</tr>
							<tr>
								<th scope="row">第三方建站平台（Wix、Squarespace、Webflow 等）</th>
								<td>灰云</td>
								<td>平台自己也终止 TLS、自己做跳转，两层代理叠加会证书报错或重定向循环</td>
							</tr>
							<tr>
								<th scope="row">指向别家 CDN 的 CNAME</th>
								<td>灰云</td>
								<td>两个代理互相转发，轻则握手失败，重则路由成环</td>
							</tr>
							<tr>
								<th scope="row">对方按 IP 白名单校验的接口 / 回调</th>
								<td>灰云</td>
								<td>对方看到的是 Cloudflare 的地址，白名单一律校验失败</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					一类例外值得记：部分 SaaS 平台和 Cloudflare 做过对接，它们的记录开小黄云是正常的。哪家可以，官方维护了一份{" "}
					<a href={DOCS_VENDOR} target="_blank" rel="noopener noreferrer">
						厂商记录清单
					</a>
					，接第三方服务前翻一眼比自己试省事。
				</p>

				<h2 id="ports">端口这道暗门</h2>
				<p>
					「开了小黄云但网页打不开」这类问题里，端口占了相当大一部分。代理并不是接管这个主机名的全部流量，它只处理落在固定几个 HTTP /
					HTTPS 端口上的请求：
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">协议</th>
								<th scope="col">代理覆盖的端口</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">HTTP</th>
								<td>
									<code>80</code>、<code>8080</code>、<code>8880</code>、<code>2052</code>、<code>2082</code>、
									<code>2086</code>、<code>2095</code>
								</td>
							</tr>
							<tr>
								<th scope="row">HTTPS</th>
								<td>
									<code>443</code>、<code>2053</code>、<code>2083</code>、<code>2087</code>、<code>2096</code>、
									<code>8443</code>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					两件事要留意。其一，这批端口里除了 <code>80</code> 和 <code>443</code>，其余的在非企业方案上<strong>不做缓存</strong>
					——代理照走、防护照有，但别指望它加速。其二，宝塔面板的默认端口 <code>8888</code>{" "}
					不在名单里，看着和 <code>8880</code> 只差一位，行为上却是天壤之别：请求到了 Cloudflare 却没有对应的处理规则，页面就是打不开。
				</p>
				<p>
					非标准端口和 TCP / UDP 应用要走{" "}
					<a href={DOCS_SPECTRUM} target="_blank" rel="noopener noreferrer">
						Spectrum
					</a>
					，全端口支持是 Enterprise 的能力。个人站点更现实的做法是把后台挪到 <code>8443</code>{" "}
					这类被支持的端口，或者让这个主机名保持灰云。端口清单以{" "}
					<a href={DOCS_PORTS} target="_blank" rel="noopener noreferrer">
						官方文档
					</a>
					为准。
				</p>

				<h2 id="origin-side">开了之后，源站会察觉到的三件事</h2>
				<p>代理这一跳不是透明的。源站上的程序会实实在在感觉到变化，有些不处理就会出问题。</p>
				<h3 id="origin-ip">所有请求都来自 Cloudflare 的 IP</h3>
				<p>
					这是最先被发现的一条：访问日志里全是 Cloudflare 的地址，按 IP 做的限流、封禁、地理位置判断统统失灵。真实地址在{" "}
					<code>CF-Connecting-IP</code> 请求头里，需要在 Web 服务器上配置读取，并且必须先把信任边界限定在 Cloudflare 的 IP
					段内，否则这个头谁都能伪造。具体配法见{" "}
					<Link href="/guides/cloudflare-huoqu-zhenshi-ip">怎么在源站拿到访客真实 IP</Link>。
				</p>
				<h3 id="mtls">客户端证书到不了源站</h3>
				<p>
					代理开着的时候 TLS 在 Cloudflare 终止，Cloudflare 再和源站另建一条 TLS 连接。这意味着源站在握手阶段永远拿不到访客的客户端证书。依赖
					mTLS 做设备认证的系统，要么把校验挪到 Cloudflare 侧，要么把证书信息通过请求头转发给源站，不能指望原来那套逻辑继续工作。
				</p>
				<h3 id="headers">请求头被增删改，请求体有上限</h3>
				<p>
					Cloudflare 会往请求里加一批自己的头（含访客 IP、诊断信息、连接管理），也会改动一些既有的头。按位置而不是按名字解析头部的老程序，可能会因此出错，完整清单在{" "}
					<a href={DOCS_HEADERS} target="_blank" rel="noopener noreferrer">
						HTTP 请求头文档
					</a>
					里。同时，代理层对请求体大小和源站响应时间都有硬性限制：源站在超时窗口内不给响应就返回 524，请求体超限则直接被拒，各方案的具体数值见{" "}
					<a href={DOCS_CONNLIMITS} target="_blank" rel="noopener noreferrer">
						连接限制
					</a>
					。这些限制在代理开着时无法绕过。
				</p>
				<p>
					还有个冷门但会卡死人的点：Windows 集成认证（NTLM、Kerberos）在 TCP 连接层做认证，而 Cloudflare
					不保证连续请求复用同一条回源连接，结果就是反复弹认证框。这类系统只能走灰云。
				</p>

				<h2 id="china">国内环境下，这个开关的账要重新算</h2>
				<p>
					前面都是通用行为，这一段是大陆用户特有的权衡，也是多数教程不会提的部分。
				</p>
				<p>
					<strong>免费方案的流量不落在境内节点。</strong>Cloudflare 在中国大陆的节点属于{" "}
					<a href={DOCS_CHINA} target="_blank" rel="noopener noreferrer">
						China Network
					</a>
					，是与京东云合作、在 Enterprise 方案之外单独订阅的服务。普通用户开了小黄云之后，大陆访客的请求会被路由到香港、日本、新加坡等地的数据中心，再从那里回源。如果源站本来就在国内，这一来一回等于凭空多了两段跨境链路，延迟和抖动都会写在实际体验上。
				</p>
				<p>
					这笔账怎么算取决于你在乎什么：访客主要在国内又对延迟敏感，开小黄云很可能是净亏；访客分散在全球、或正被扫描和刷流量困扰，多这一跳换来的防护通常是值的。折中做法也常见——展示站点开小黄云，国内高频访问的后台留灰云直连。
				</p>
				<p>
					<strong>境内节点只支持 80 和 443。</strong>即便用上了 China Network，前面那批 <code>2052</code>、
					<code>8443</code> 之类的备用端口在大陆数据中心也不生效，只有标准的两个端口能走。
				</p>
				<p>
					<strong>备案这件事和开关无关。</strong>域名的备案义务取决于服务器位置和业务性质，不会因为流量绕了境外 CDN 而消失，也不会因为开了小黄云而产生。反过来，要把域名接进 China Network，Cloudflare
					明确要求每一个主域都持有有效的{" "}
					<a href={DOCS_ICP} target="_blank" rel="noopener noreferrer">
						ICP 备案或许可证
					</a>
					。
				</p>

				<h2 id="switching">怎么切，多久生效，以及刚接入时的空窗期</h2>
				<p>
					切换本身没什么难度：在 DNS 记录列表里点一下那个云朵图标，或者编辑记录时改代理状态，保存即生效。由于已代理记录的 TTL 固定在 300
					秒，反向操作（橙改灰）最多五分钟就会被解析器换掉——不过你本机的 DNS 缓存可能更慢一些，验证时用{" "}
					<code>dig www.example.com @1.1.1.1</code> 直接问公共解析器更准。
				</p>
				<p>
					真正要留意的是刚把域名接进 Cloudflare 的那段时间。激活前域名处于{" "}
					<a href={DOCS_STATUS} target="_blank" rel="noopener noreferrer">
						pending 状态
					</a>
					，这期间即使记录标成了已代理，也仍按仅 DNS 返回源站真实 IP，最长可能持续 24
					小时——你以为藏好的地址其实公开挂了一整天。官方建议很直接：激活之后去主机商那里把源站 IP 换一次。
				</p>
				<p>
					最后是排查顺序。遇到「开了就坏」，最快的判断是把记录临时改回灰云等五分钟：立刻恢复，问题就在代理这一跳（端口、证书模式、双层代理、请求体超限）；照样不通，那是源站自己的事。
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
