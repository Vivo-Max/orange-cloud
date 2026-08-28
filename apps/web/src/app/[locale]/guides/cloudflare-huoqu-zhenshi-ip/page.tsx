import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { setRequestLocale } from "next-intl/server";
import GuideShell, { type RelatedLink } from "@/components/guides/GuideShell";
import RealIpTrust from "@/components/guides/RealIpTrust";
import { guideBySlug, guidePath, GUIDE_LOCALE_ZH } from "@/lib/guides/guides";

const SITE_URL = "https://oss.omail.us.kg";
const guide = guideBySlug("cloudflare-huoqu-zhenshi-ip", GUIDE_LOCALE_ZH);
const PATH = guidePath(GUIDE_LOCALE_ZH, `/guides/${guide.slug}`);

const DOCS_RESTORE =
	"https://developers.cloudflare.com/support/troubleshooting/restoring-visitor-ips/restoring-original-visitor-ips/";
const DOCS_HEADERS = "https://developers.cloudflare.com/fundamentals/reference/http-headers/";
const DOCS_CF_IPS = "https://developers.cloudflare.com/fundamentals/concepts/cloudflare-ip-addresses/";
const IPS_LIST = "https://www.cloudflare.com/ips/";
const DOCS_API_IPS = "https://developers.cloudflare.com/api/resources/ips/methods/list/";
const DOCS_PSEUDO = "https://developers.cloudflare.com/network/pseudo-ipv4/";
const DOCS_MT = "https://developers.cloudflare.com/rules/transform/managed-transforms/reference/";
const DOCS_MT_CONFIG = "https://developers.cloudflare.com/rules/transform/managed-transforms/configure/";
const DOCS_AOP = "https://developers.cloudflare.com/ssl/origin-configuration/authenticated-origin-pull/";
const DOCS_CHINA = "https://developers.cloudflare.com/china-network/";
const DOCS_GEO = "https://developers.cloudflare.com/network/ip-geolocation/";
const DOCS_PROXY = "https://developers.cloudflare.com/dns/proxy-status/";
const DOCS_IP_SRC = "https://developers.cloudflare.com/ruleset-engine/rules-language/fields/reference/ip.src/";
const NGINX_REALIP = "https://nginx.org/en/docs/http/ngx_http_realip_module.html";
const APACHE_REMOTEIP = "https://httpd.apache.org/docs/2.4/mod/mod_remoteip.html";

/** FAQ 一处定义：可见文本与 FAQPage JSON-LD 同源，保证逐字一致 */
const FAQ: Array<{ q: string; a: string }> = [
	{
		q: "Cloudflare 怎么获取访客真实 IP？",
		a: "代理开着的时候，Cloudflare 会在转发给源站的请求上加一个 CF-Connecting-IP 头，值就是访客连到 Cloudflare 时用的那个地址，格式固定为单个 IP。源站这一侧要做的是让 Web 服务器读这个头，而不是读 TCP 连接的来源地址：Nginx 用 ngx_http_realip_module，Apache 2.4 用 mod_remoteip。",
	},
	{
		q: "Nginx 后面接 Cloudflare，日志里全是 Cloudflare 的 IP 怎么办？",
		a: "在 http 或 server 段里用 set_real_ip_from 把 Cloudflare 的全部 IP 段列出来，再加一行 real_ip_header CF-Connecting-IP，重载之后 $remote_addr 与访问日志就会变回访客地址。注意 set_real_ip_from 必须逐条列全，漏掉的段仍然会记成 Cloudflare 的 IP；也别图省事写成 0.0.0.0/0，那等于允许任何人伪造自己的地址。",
	},
	{
		q: "CF-Connecting-IP 会被伪造吗？",
		a: "会，只要源站还能被直接访问。HTTP 头是请求方随手就能写的东西，如果你的服务器对任何来源的连接都信任 CF-Connecting-IP，那么知道源站地址的人只要自己带上这个头，就能把自己伪装成任意 IP，绕过按 IP 做的封禁与限流。所以真实 IP 的恢复必须和信任边界一起做：只对来自 Cloudflare IP 段的连接采信这些头，其余的直接拒绝。",
	},
	{
		q: "X-Forwarded-For 和 CF-Connecting-IP 用哪个？",
		a: "用 CF-Connecting-IP。当请求到达 Cloudflare 之前没有经过其它代理时，两者的值确实一样；但只要访客那一侧还有别的代理或 CDN，X-Forwarded-For 就会变成一串用逗号分隔的地址列表，取第一个可能取到伪造值，取最后一个又会取到代理的地址。官方文档也明确建议日志和应用去读 CF-Connecting-IP。",
	},
	{
		q: "为什么开了 Cloudflare 之后，网站的 IP 封禁和限流都失效了？",
		a: "因为源站看到的来源地址已经全部变成 Cloudflare 的那十几个网段，你的封禁规则要么命中不了任何人，要么一命中就把所有访客一起挡在门外。恢复真实 IP 之后这些功能才会重新有意义。更省事的做法是把这类规则搬到 Cloudflare 那一侧去做：WAF 规则里的 ip.src 本来就是访客的真实地址，不需要任何额外配置。",
	},
];

const RELATED: RelatedLink[] = [
	{
		href: "/guides/cloudflare-xiaohuangyun",
		label: "Cloudflare 的小黄云到底是什么？什么时候该关掉？",
		note: "真实 IP 会变成 Cloudflare 的地址，起点就是这个代理开关——顺带看清哪些记录该关掉。",
	},
	{
		href: "/guides/cloudflare-yuanzhan-ip-xielou",
		label: "套了 Cloudflare，源站真实 IP 还会泄露吗？",
		note: "本文那条信任边界能不能立住，前提是别人拿不到你的源站地址——先看看它有几条路会漏出去。",
	},
	{
		href: "/guides/cloudflare-522-error",
		label: "Cloudflare 为什么会报 522 错误？",
		note: "把源站收紧到只放行 Cloudflare 之后最常见的翻车方式：IP 段列漏了一条，回源直接被自己挡掉。",
	},
	{
		href: "/guides",
		label: "全部中文指南",
		note: "Cloudflare 的其它设置项：代理状态、回源、缓存、证书与内网穿透。",
	},
	{
		href: DOCS_RESTORE,
		label: "Cloudflare 官方文档：恢复访客原始 IP",
		note: "各种 Web 服务器与面板的具体配置写法，本文的配置部分都出自这一页。",
		external: true,
	},
	{
		href: DOCS_HEADERS,
		label: "Cloudflare 官方文档：Cloudflare HTTP 头",
		note: "每个 CF- 开头的请求头分别装什么、在哪些场景下取值会变，以这一页为准。",
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

export default async function RealIpGuideZh({ params }: { params: Promise<{ locale: string }> }) {
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
				lede="日志里那一片 104.16 开头的地址不是配错了，是代理生效的正常结果。真实地址还在，只是换了个位置放——以及，取它之前有一步不能省。"
				updated={guide.updated}
				readingTime={guide.readingTime}
				related={RELATED}
			>
				<div className="glass r-island note p-6 sm:p-7">
					<p>
						访客真实 IP 在 <code>CF-Connecting-IP</code> 请求头里，让 Web
						服务器读这个头即可。但必须先限定只信任来自 Cloudflare IP 段的连接，否则任何人都能伪造它。
					</p>
				</div>

				<p>
					把域名切成橙云之后，第一个被打乱的往往不是网站本身，而是那些依赖访客地址的功能：登录风控开始误判、评论限频形同虚设、后台的封 IP
					按钮点了没反应、统计里所有人都来自美国。翻开访问日志一看，来源地址翻来覆去就那么十几个网段。
				</p>
				<p>
					这不是配错了，是反向代理的必然结果。下面按「为什么会这样 — 真实地址在哪 — 怎么取 — 取之前要先做什么」的顺序过一遍，最后那一步最常被跳过，也最容易出事。
				</p>

				<h2 id="why">源站看到的是 Cloudflare，这是对的</h2>
				<p>
					<a href={DOCS_PROXY} target="_blank" rel="noopener noreferrer">
						代理状态
					</a>{" "}
					打开之后，访客的 DNS 查询拿到的是 Cloudflare 的地址，TCP 连接也建立在离访客最近的 Cloudflare
					节点上。节点处理完缓存、WAF 这些活儿，再由它自己向你的源站发起一条新的连接。对源站的操作系统来说，这条连接的对端就是那个节点，没有别的可能。
				</p>
				<p>
					所以一切从连接层取地址的代码都会失真：Nginx 的 <code>$remote_addr</code>、PHP 的{" "}
					<code>$_SERVER[&#39;REMOTE_ADDR&#39;]</code>、各种框架里的 <code>request.remote_ip</code>，拿到的都是
					Cloudflare 的 IP。这些地址来自一份公开的清单，目前是 15 个 IPv4 网段加 7 个 IPv6 网段，全体 Cloudflare
					用户共用，列在{" "}
					<a href={IPS_LIST} target="_blank" rel="noopener noreferrer">
						Cloudflare IP 地址页
					</a>
					。
				</p>
				<RealIpTrust />
				<p>
					要强调的是：失真只发生在源站这一侧。Cloudflare 自己那一层看到的一直是访客的真实地址——WAF 规则里的{" "}
					<a href={DOCS_IP_SRC} target="_blank" rel="noopener noreferrer">
						<code>ip.src</code>
					</a>
					、安全事件列表、流量分析，都不需要任何额外配置。
				</p>

				<h2 id="headers">真实地址在哪个头里</h2>
				<p>
					Cloudflare 在转发时会往请求上加几个头，各有各的用途。下面这张表按「你到底该读哪个」排序，细节以官方的{" "}
					<a href={DOCS_HEADERS} target="_blank" rel="noopener noreferrer">
						HTTP 头文档
					</a>{" "}
					为准。
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">请求头</th>
								<th scope="col">装的是什么</th>
								<th scope="col">该不该用</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">
									<code>CF-Connecting-IP</code>
								</th>
								<td>访客连到 Cloudflare 时用的地址，永远是单个 IP</td>
								<td>首选。所有套餐都有，格式恒定</td>
							</tr>
							<tr>
								<th scope="row">
									<code>X-Forwarded-For</code>
								</th>
								<td>访客地址加上沿途每一跳代理的地址，逗号分隔</td>
								<td>能用但别用，见下一节</td>
							</tr>
							<tr>
								<th scope="row">
									<code>True-Client-IP</code>
								</th>
								<td>内容与 CF-Connecting-IP 完全相同，只是换了个名字</td>
								<td>企业版专属，且要在托管转换里开启</td>
							</tr>
							<tr>
								<th scope="row">
									<code>CF-Connecting-IPv6</code>
								</th>
								<td>开启伪 IPv4 的覆盖模式时，访客原本的 IPv6 地址</td>
								<td>只在那一种配置下出现</td>
							</tr>
							<tr>
								<th scope="row">
									<code>CF-IPCountry</code>
								</th>
								<td>访客所在国家或地区的两位代码</td>
								<td>做地域判断时用它，别自己反查 IP 库</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					如果源站压根收不到 <code>CF-Connecting-IP</code>，先去检查{" "}
					<a href={DOCS_MT_CONFIG} target="_blank" rel="noopener noreferrer">
						托管转换
					</a>{" "}
					里的「移除访客 IP 头」有没有被打开——这个开关会一并去掉 <code>cf-connecting-ip</code>、
					<code>true-client-ip</code> 和 <code>x-forwarded-for</code> 里的访客地址；其次检查有没有哪条转换规则把它改掉了。
				</p>

				<h3 id="xff">X-Forwarded-For 为什么不建议直接用</h3>
				<p>
					当请求在到达 Cloudflare 之前没有经过其它代理时，<code>X-Forwarded-For</code> 的值确实和{" "}
					<code>CF-Connecting-IP</code> 一模一样，很多人就是这么用过来的，也一直没出事。问题出在有中间代理的时候：
					Cloudflare 会把它收到的那一跳地址追加到已有的列表后面，于是这个头变成一串地址。取第一个，那一段完全由客户端提供，可以随便填；取最后一个，取到的是代理而不是访客。
				</p>
				<p>
					官方文档在这一点上说得很直白：日志和应用应当去读 <code>CF-Connecting-IP</code> 或{" "}
					<code>True-Client-IP</code>，因为它们的格式一致、只含一个地址。
				</p>

				<h3 id="ipv6">IPv6 访客与伪 IPv4</h3>
				<p>
					Cloudflare 对所有域名默认支持 IPv6，这意味着 <code>CF-Connecting-IP</code>{" "}
					里可能出现一个 IPv6 地址。老一些的风控和统计程序只认得点分十进制，遇到冒号就崩，这时可以用{" "}
					<a href={DOCS_PSEUDO} target="_blank" rel="noopener noreferrer">
						伪 IPv4
					</a>
					：它把 IPv6 地址哈希成一个 E 类 IPv4 地址（形如 <code>240.16.0.1</code>），有三档可选——关闭、加一个{" "}
					<code>CF-Pseudo-IPv4</code> 头、或者直接覆盖 <code>CF-Connecting-IP</code> 与{" "}
					<code>X-Forwarded-For</code>，并把真正的 IPv6 地址挪到 <code>CF-Connecting-IPv6</code> 里。
				</p>
				<p>
					选覆盖模式的话源站一行代码都不用改，代价是日志里记下的不再是真实地址，而是一个只在你这套系统里有意义的映射值。
				</p>

				<h2 id="config">配置：把真实地址写回日志</h2>
				<p>
					思路对所有 Web 服务器都一样：告诉它「这些来源是可信的代理」，再告诉它「去哪个头里取真实地址」。以 Nginx 为例：
				</p>
				<pre>
					<code>{`# 逐条列出 Cloudflare 的 IP 段（这里只截取开头几条）
set_real_ip_from 173.245.48.0/20;
set_real_ip_from 103.21.244.0/22;
set_real_ip_from 2400:cb00::/32;
# ……其余网段照抄 cloudflare.com/ips

real_ip_header CF-Connecting-IP;`}</code>
				</pre>
				<p>
					重载之后 <code>$remote_addr</code> 与访问日志就换回访客地址了。想同时保留原始头做对账，可以在{" "}
					<code>log_format</code> 里加上 <code>$http_cf_connecting_ip</code> 和 <code>$http_x_forwarded_for</code>
					。模块本身的参数说明见{" "}
					<a href={NGINX_REALIP} target="_blank" rel="noopener noreferrer">
						ngx_http_realip_module 文档
					</a>
					。
				</p>
				<p>其它常见环境的对应做法：</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">环境</th>
								<th scope="col">怎么做</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">Apache 2.4</th>
								<td>
									启用{" "}
									<a href={APACHE_REMOTEIP} target="_blank" rel="noopener noreferrer">
										mod_remoteip
									</a>
									，配 <code>RemoteIPHeader CF-Connecting-IP</code> 与逐条的{" "}
									<code>RemoteIPTrustedProxy</code>，再把日志格式里的 <code>%h</code> 换成 <code>%a</code>
								</td>
							</tr>
							<tr>
								<th scope="row">Caddy</th>
								<td>
									在 <code>reverse_proxy</code> 里用 <code>header_up</code> 把{" "}
									<code>X-Forwarded-For</code> 覆盖成 <code>CF-Connecting-IP</code> 的值，并只放行 Cloudflare
									的来源
								</td>
							</tr>
							<tr>
								<th scope="row">HAProxy</th>
								<td>
									关掉 <code>option forwardfor</code>，用一条 acl 匹配 Cloudflare 来源后再重写{" "}
									<code>X-Forwarded-For</code>
								</td>
							</tr>
							<tr>
								<th scope="row">LiteSpeed</th>
								<td>后台勾上「Use Client IP in Header」，日志与 PHP 变量会一起恢复</td>
							</tr>
							<tr>
								<th scope="row">IIS 8.5 及以上</th>
								<td>
									在日志里加一个 <code>CF-Connecting-IP</code> 自定义字段。注意它只影响日志，不会恢复应用层拿到的地址
								</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					另外，Apache 的 <code>mod_cloudflare</code>{" "}
					早已停止维护，从 Debian 9 与 Ubuntu 18.04 起官方就不再更新它，改推 <code>mod_remoteip</code>；网上那些老教程仍在教装它，照做只会白折腾。完整清单见官方的{" "}
					<a href={DOCS_RESTORE} target="_blank" rel="noopener noreferrer">
						恢复访客原始 IP
					</a>{" "}
					一页。
				</p>

				<h2 id="trust">最关键的一步：先划出信任边界</h2>
				<p>
					上面每一段配置里都有「可信来源」这半边，很多教程会把它一笔带过，甚至写成{" "}
					<code>set_real_ip_from 0.0.0.0/0</code> 图个省事。这一步偷懒的后果比不恢复真实 IP 严重得多。
				</p>
				<p>
					道理很简单：HTTP 头是请求方随手就能写的东西。如果服务器对任何来源的连接都采信{" "}
					<code>CF-Connecting-IP</code>，那么任何一个知道你源站地址的人，只要在请求里自己带上这个头，就能把自己伪装成任意地址——你按 IP
					做的封禁、限流、白名单、审计日志，全部一起作废。Cloudflare 的文档在讲 Caddy 配置时特意点了这一条：不限定来源，头就是可以被伪造的。
				</p>
				<p>所以恢复真实 IP 这件事，要和下面两件一起做：</p>
				<ol>
					<li>
						<strong>把可信代理列表限定为 Cloudflare 的网段</strong>，并且逐条列全。清单在{" "}
						<a href={IPS_LIST} target="_blank" rel="noopener noreferrer">
							cloudflare.com/ips
						</a>
						；想写脚本定期同步，用{" "}
						<a href={DOCS_API_IPS} target="_blank" rel="noopener noreferrer">
							<code>GET /client/v4/ips</code>
						</a>{" "}
						这个接口，它不需要鉴权，返回里带 <code>etag</code> 方便判断有没有变。脚本要处理好取不到的情况，别在请求失败时把列表清空。
					</li>
					<li>
						<strong>在防火墙上只放行 Cloudflare 的网段</strong>，让直连源站的请求根本到不了 Web
						服务器。官方也是这么建议的，具体的 iptables 写法在{" "}
						<a href={DOCS_CF_IPS} target="_blank" rel="noopener noreferrer">
							Cloudflare IP 地址
						</a>{" "}
						这一页。
					</li>
				</ol>
				<p>
					想更进一步，可以开{" "}
					<a href={DOCS_AOP} target="_blank" rel="noopener noreferrer">
						Authenticated Origin Pulls
					</a>
					，让源站校验 Cloudflare 出示的客户端证书。它在所有套餐上都能用，但有两个前提要知道：全局模式用的是所有账号共享的证书，只能证明「来自
					Cloudflare 网络」而不是「来自你的账号」，要更强的保证得自己上传证书；另外它和 Tunnel
					不兼容，因为隧道场景下源站根本没有对外监听的端口。
				</p>

				<h2 id="china">国内环境里额外要注意的几件事</h2>
				<h3 id="china-jd">走中国网络的话，回源不来自那 15 个网段</h3>
				<p>
					这一条会让不少人排查半天。{" "}
					<a href={DOCS_CHINA} target="_blank" rel="noopener noreferrer">
						Cloudflare 中国网络
					</a>{" "}
					的境内节点由京东云运营，它们回源时用的是京东云的地址，不在公开那份列表里。只按那 15 个网段配白名单，境内节点的回源会被你自己的防火墙全部挡掉，恢复真实 IP
					也一并失效。这些网段要用 <code>GET /client/v4/ips?networks=jdcloud</code>{" "}
					单独取，返回字段是 <code>jdcloud_cidrs</code>，数量比全球那份多得多。
				</p>
				<p>
					用得上这条的人不多：中国网络是企业版的单独订阅项，且要求每个顶级域都已完成 ICP 备案。免费和 Pro
					方案的大陆访客仍走境外节点，回源地址还是那 15 个网段。
				</p>
				<h3 id="china-panel">面板环境常见的双层代理</h3>
				<p>
					国内主机上装的一体化面板，很多是 Nginx 在前、Apache 或 PHP-FPM 在后。这种结构下真实 IP 要在最外层那一跳恢复：Nginx
					配 <code>set_real_ip_from</code> 认 Cloudflare，后端再信任来自 Nginx 的本地地址。顺序反了，后端日志里就会整齐地记满{" "}
					<code>127.0.0.1</code>，看上去像是恢复失败，其实是恢复了两次。
				</p>
				<h3 id="china-geo">地域判断别再对着连接地址反查</h3>
				<p>
					国内业务常用 IP 库判断省份、做分流或者限制注册。代理开启后这类逻辑必须改成读{" "}
					<code>CF-Connecting-IP</code>，否则查到的全是 Cloudflare 节点，结论一律是境外。只要国家或地区代码就够的话，更省事的是直接读{" "}
					<code>CF-IPCountry</code>——开启{" "}
					<a href={DOCS_GEO} target="_blank" rel="noopener noreferrer">
						IP 地理位置
					</a>{" "}
					后 Cloudflare 就会带上它，需要城市、经纬度等更细的字段则要在{" "}
					<a href={DOCS_MT} target="_blank" rel="noopener noreferrer">
						托管转换
					</a>{" "}
					里打开「添加访客位置头」。
				</p>
				<h3 id="china-log">留存的日志得记得住人</h3>
				<p>
					境内运营的站点普遍有网络日志留存的要求。一份把所有访客都记成同十几个地址的访问日志，事后追溯时基本没有价值。恢复真实 IP 在国内因此多了一层现实理由。
				</p>

				<h2 id="verify">怎么确认配好了</h2>
				<p>
					最直接的办法：从一台你知道出口地址的机器上访问一次网站，再去看服务器的访问日志，第一列应该是那台机器的地址，不是{" "}
					<code>104.16</code> 开头的。
				</p>
				<p>
					信任边界那一半要单独验。如果源站还能被直连（比如你只做了头的配置、没做防火墙），试着直接对源站地址发一个带伪造{" "}
					<code>CF-Connecting-IP</code> 的请求，看日志里记下的是什么。记成了你伪造的那个值，说明可信来源列表写得太宽，等于把封禁开关交给了对方。理想结果是这个请求压根连不上。
				</p>
				<p>
					另外有个容易踩的边角：如果站点前面还挂着 Worker，同区域的子请求里 <code>CF-Connecting-IP</code> 的取值会跟随{" "}
					<code>x-real-ip</code>，而后者是脚本里可以改的；跨区域的子请求则会被替换成一个固定地址。
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
