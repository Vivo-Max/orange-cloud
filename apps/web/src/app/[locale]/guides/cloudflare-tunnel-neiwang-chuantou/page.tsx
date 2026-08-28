import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { setRequestLocale } from "next-intl/server";
import GuideShell, { type RelatedLink } from "@/components/guides/GuideShell";
import TunnelFlow from "@/components/guides/TunnelFlow";
import { guideBySlug, guidePath, GUIDE_LOCALE_ZH } from "@/lib/guides/guides";

const SITE_URL = "https://oss.omail.us.kg";
const guide = guideBySlug("cloudflare-tunnel-neiwang-chuantou", GUIDE_LOCALE_ZH);
const PATH = guidePath(GUIDE_LOCALE_ZH, `/guides/${guide.slug}`);

const DOCS_TUNNEL = "https://developers.cloudflare.com/tunnel/";
const DOCS_SETUP = "https://developers.cloudflare.com/tunnel/setup/";
const DOCS_ROUTING = "https://developers.cloudflare.com/tunnel/routing/";
const DOCS_CONFIG = "https://developers.cloudflare.com/tunnel/configuration/";
const DOCS_TROUBLE = "https://developers.cloudflare.com/tunnel/troubleshooting/";
const DOCS_LOCAL = "https://developers.cloudflare.com/tunnel/advanced/local-management/create-local-tunnel/";
const DOCS_DOWNLOADS = "https://developers.cloudflare.com/tunnel/downloads/";
const DOCS_1033 =
	"https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-1xxx-errors/error-1033/";
const DOCS_413 =
	"https://developers.cloudflare.com/support/troubleshooting/http-status-codes/4xx-client-error/error-413/";
const DOCS_ACCESS =
	"https://developers.cloudflare.com/cloudflare-one/access-controls/applications/http-apps/self-hosted-public-app/";
const DOCS_CHINA = "https://developers.cloudflare.com/china-network/";
const TERMS_APP_SERVICES = "https://www.cloudflare.com/service-specific-terms-application-services/";

/** FAQ 一处定义：可见文本与 FAQPage JSON-LD 同源，保证逐字一致 */
const FAQ: Array<{ q: string; a: string }> = [
	{
		q: "Cloudflare Tunnel 是免费的吗？",
		a: "隧道本身在所有套餐上都可用，免费方案就能建、能跑、能绑自己的域名，流量也不额外计费。真正会拦住你的不是价格，而是套餐限制：免费与 Pro 方案单次请求体上限 100 MB，超过返回 413；同时 Cloudflare 的服务条款不允许在免费、Pro、Business 的 CDN 上提供视频，或提供比例失衡的图片、音频与大文件。",
	},
	{
		q: "用 Cloudflare Tunnel 做内网穿透需要备案吗？",
		a: "Cloudflare 的免费方案不使用中国大陆境内节点，大陆访客的请求会绕到境外数据中心，因此不涉及在 Cloudflare 侧办理 ICP 备案。但备案义务看的是你自己的服务器和业务：如果服务跑在境内主机上并对公众提供，相关法规仍然适用，不会因为流量经过境外 CDN 而消失。要用 Cloudflare 的大陆节点则需要 China Network，那是企业级方案，并且要求域名已完成 ICP 备案。",
	},
	{
		q: "Cloudflare Tunnel 和 frp、ngrok 有什么区别？",
		a: "frp 需要你自己准备一台有公网 IP 的服务器做中转，带宽、可用性、被扫描后的防护全归你管。Cloudflare Tunnel 把中转那一端换成了 Cloudflare 的全球网络，你不用买服务器，请求进来时还顺带过一遍缓存、WAF 和 DDoS 防护。代价是你要接受它的规则：域名必须托管在 Cloudflare，请求体大小、内容类型都有限制，非 HTTP 协议还要求访问端也装 cloudflared。",
	},
	{
		q: "访问域名报 1033 错误怎么办？",
		a: "1033 的意思是 Cloudflare 找不到一个健康的 cloudflared 实例来接收流量，问题几乎都在你这一侧，而不是域名或 DNS。先在仪表盘的 Networking → Tunnels 里看隧道状态：Inactive 表示隧道建好了但连接器从没跑起来，Down 表示进程停了，Degraded 表示还在服务但有连接失败。再看 cloudflared 日志里是不是卡在 7844 端口的连接上。",
	},
	{
		q: "能用 Cloudflare Tunnel 把 NAS 挂出去看电影吗？",
		a: "技术上能跑通，但这正是 Cloudflare 服务条款明确限制的用法——免费、Pro、Business 方案的 CDN 不得用于提供视频或比例失衡的大文件。个人偶尔取一份文件通常不会有人过问，把它当成长期的影音外链或公网网盘则是在赌，Cloudflare 保留停用 CDN 的权利。上传方向还另有一道 100 MB 的硬限制。",
	},
];

const RELATED: RelatedLink[] = [
	{
		href: "/guides/cloudflare-yuanzhan-ip-xielou",
		label: "套了 Cloudflare，源站真实 IP 还会泄露吗？",
		note: "隧道之所以是最彻底的一档，是因为源站压根没有入站端口——地址泄露了也无处可打。",
	},
	{
		href: "/guides/cloudflare-mianfeiban-shipin-tucang",
		label: "Cloudflare 免费版能拿来放视频、当图床吗？",
		note: "把 NAS 挂出去之后最容易踩的那条线：条款怎么写的，100 MB 与 512 MB 各卡在哪一步。",
	},
	{
		href: "/guides/cloudflare-522-error",
		label: "Cloudflare 为什么会报 522 错误？",
		note: "隧道之外的另一种回源方式会遇到的问题：防火墙挡了回源 IP、跨境链路丢包，以及 19 秒的连接窗口。",
	},
	{
		href: "/guides",
		label: "全部中文指南",
		note: "Cloudflare 的其它设置项：代理状态、回源、缓存与证书。",
	},
	{
		href: DOCS_SETUP,
		label: "Cloudflare 官方文档：Tunnel 快速上手",
		note: "本文所有步骤的原始出处，含仪表盘与 API 两条路径。",
		external: true,
	},
	{
		href: DOCS_TROUBLE,
		label: "Cloudflare 官方文档：Tunnel 故障排查",
		note: "连接类报错的完整清单，含各种日志原文与对应处理。",
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

export default async function TunnelGuideZh({ params }: { params: Promise<{ locale: string }> }) {
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
				lede="不用公网 IP，不用端口映射，也不用 DDNS。代价是要接受 Cloudflare 的几条硬限制——先把它们看清楚，再决定要不要把家里那台机器挂上去。"
				updated={guide.updated}
				readingTime={guide.readingTime}
				related={RELATED}
			>
				<div className="glass r-island note p-6 sm:p-7">
					<p>
						Cloudflare Tunnel 靠内网里的 <code>cloudflared</code>{" "}
						主动向 Cloudflare 建立一条出站连接，公网请求沿着这条连接回到你的服务。所以它不需要公网 IP，也不需要在路由器上开放任何入站端口。前提只有一个：域名托管在
						Cloudflare。
					</p>
				</div>

				<p>
					家宽拿不到公网 IP、拿到了也是随时会变的动态地址、路由器在运营商的大内网后面——这是国内做内网穿透绕不开的起点。传统解法是租一台有公网
					IP 的小服务器跑 frp，把中转这件事自己扛下来。Cloudflare Tunnel 换了个思路：中转交给 Cloudflare
					的全球网络，你这边只跑一个客户端进程。
				</p>
				<p>
					这条路确实省事，但它不是万能的替代品。下面按「怎么通 — 怎么配 — 有哪些坑」的顺序过一遍，重点放在那些通常要踩过才知道的边界上。
				</p>

				<h2 id="how-it-works">连接方向反过来了，所以不需要公网 IP</h2>
				<p>
					关键差别只有一个字：方向。端口映射也好、frp 也好，本质都是让公网能主动「敲」到你家的某个入口；而{" "}
					<code>cloudflared</code> 是从内网往外敲 Cloudflare 的门。
				</p>
				<TunnelFlow />
				<p>
					按官方文档的说法，<code>cloudflared</code> 会向 Cloudflare 建立四条长连接，分布在至少两个数据中心上，任意一条断掉服务都不中断。这些连接走
					7844 端口，优先用 QUIC（UDP），握手不成功会自动回落到 HTTP/2（TCP）。绝大多数家用网络默认放行出站流量，所以这一步通常什么都不用配。
				</p>
				<p>
					连接建立之后，流量是双向的：公网访客的请求先落在 Cloudflare 的边缘节点，过一遍缓存、WAF 和 DDoS 防护，再沿着这条已有的隧道下发给{" "}
					<code>cloudflared</code>，由它转交给本机（或内网里另一台机器）上的服务。你的服务器从头到尾没有对外暴露过一个端口，扫描器也就无从下手。细节见{" "}
					<a href={DOCS_TUNNEL} target="_blank" rel="noopener noreferrer">
						Cloudflare Tunnel 文档
					</a>
					。
				</p>

				<h2 id="three-shapes">先选形态：三种隧道差别很大</h2>
				<p>「Cloudflare Tunnel」这个名字底下其实有三种用法，选错了会白折腾半天。</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">形态</th>
								<th scope="col">怎么建</th>
								<th scope="col">适合</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">Quick Tunnel</th>
								<td>
									一条命令 <code>cloudflared tunnel --url http://localhost:8080</code>，不用账号、不用域名，随机分配一个{" "}
									<code>trycloudflare.com</code> 子域
								</td>
								<td>临时给人看一眼本地开发环境。官方明确说它只用于测试：并发上限 200，且不支持 SSE</td>
							</tr>
							<tr>
								<th scope="row">远程管理</th>
								<td>仪表盘里建，配置存在 Cloudflare，机器上只跑一条带 token 的服务</td>
								<td>绝大多数人该选这个。换机器、加路由都在网页上点，本机不留配置文件</td>
							</tr>
							<tr>
								<th scope="row">本地管理</th>
								<td>
									<code>cloudflared tunnel create</code> 加一份 <code>config.yml</code>，凭证落在本机
								</td>
								<td>要把配置纳入版本管理或用 Ansible、Terraform 批量下发时</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					下面的步骤按远程管理隧道来写。想走配置文件那条路，参考官方的{" "}
					<a href={DOCS_LOCAL} target="_blank" rel="noopener noreferrer">
						本地管理隧道文档
					</a>
					，两者的网络行为完全一样，只是配置存放位置不同。
				</p>

				<h2 id="setup">五分钟配完一条隧道</h2>
				<p>动手前确认三件事：域名的 DNS 已经托管在 Cloudflare；有一台能上网的机器（NAS、软路由、树莓派、云主机都行）；这台机器能出站访问 7844 端口。</p>
				<ol>
					<li>
						仪表盘进 <strong>Networking → Tunnels</strong>，点 <strong>Create Tunnel</strong>，起个名字。
					</li>
					<li>
						选好机器的操作系统和架构，把页面给出的安装命令复制到机器上执行。Linux 上通常就是{" "}
						<code>sudo cloudflared service install &lt;TOKEN&gt;</code>；Docker 用户可以直接跑官方镜像。各平台的安装包见{" "}
						<a href={DOCS_DOWNLOADS} target="_blank" rel="noopener noreferrer">
							下载页
						</a>
						。
					</li>
					<li>
						回到页面，隧道状态变成 <strong>Healthy</strong> 就说明连上了。
					</li>
					<li>
						在 <strong>Routes</strong> 里点 <strong>Add route</strong>，选 <strong>Published application</strong>，填公开域名（比如{" "}
						<code>nas.example.com</code>）和本地服务地址（比如 <code>http://localhost:8080</code>）。服务在内网另一台机器上就填那台机器的内网
						IP。
					</li>
				</ol>
				<p>
					保存之后 Cloudflare 会自动建好 DNS 记录：一条指向{" "}
					<code>&lt;隧道 UUID&gt;.cfargotunnel.com</code> 的 CNAME。这个子域只对同一个 Cloudflare
					账号下的 DNS 记录生效，别人就算知道了你的隧道 UUID，也没法在自己账号里建记录来蹭这条隧道。
				</p>
				<p>
					一条隧道可以挂多个域名，各自映射到不同的本地服务，全部共用同一条连接。相关规则见{" "}
					<a href={DOCS_ROUTING} target="_blank" rel="noopener noreferrer">
						路由文档
					</a>
					。
				</p>

				<h3 id="non-http">SSH、远程桌面这些非 HTTP 协议</h3>
				<p>
					隧道能转的不止网页。下面这些服务类型都可以映射到公开域名，但有一条共同前提：访问端也要装{" "}
					<code>cloudflared</code>，用 <code>cloudflared access</code> 建立连接——浏览器直接打开是不行的。
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">类型</th>
								<th scope="col">写法</th>
								<th scope="col">访问端要求</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">HTTP / HTTPS</th>
								<td>
									<code>http://localhost:8000</code>
								</td>
								<td>浏览器直接访问</td>
							</tr>
							<tr>
								<th scope="row">SSH</th>
								<td>
									<code>ssh://localhost:22</code>
								</td>
								<td>
									客户端跑 <code>cloudflared access ssh</code>
								</td>
							</tr>
							<tr>
								<th scope="row">RDP</th>
								<td>
									<code>rdp://localhost:3389</code>
								</td>
								<td>客户端装 cloudflared</td>
							</tr>
							<tr>
								<th scope="row">SMB</th>
								<td>
									<code>smb://localhost:445</code>
								</td>
								<td>客户端装 cloudflared</td>
							</tr>
							<tr>
								<th scope="row">任意 TCP</th>
								<td>
									<code>tcp://localhost:2222</code>
								</td>
								<td>
									客户端跑 <code>cloudflared access tcp</code>，长连接场景官方建议改用私有网络方案
								</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					注意这张表里没有 UDP。游戏联机、部分 VoIP 这类纯 UDP 的场景，公网发布这条路走不通，得改用 Cloudflare One 的私有网络加 WARP
					客户端，那是另一套东西了。
				</p>

				<h2 id="limits">动手之前，先认下这几条硬限制</h2>
				<p>这一节是本文最值得先读的部分。绝大多数「用了半年突然出问题」的故事，根源都在这里。</p>
				<h3 id="limit-upload">单次上传 100 MB</h3>
				<p>
					免费和 Pro 方案的请求体上限是 100 MB，Business 是 200 MB，超过就返回{" "}
					<code>413</code>。这个限制作用在整个 Cloudflare 代理层上，隧道自然也跑不掉。挂 NAS 想往上传大文件、或者自建 Git 仓库推大提交，都会撞上它。规避办法只有分片上传、把域名改成灰云直连（那就失去了穿透的意义），或者升级套餐。细节见{" "}
					<a href={DOCS_413} target="_blank" rel="noopener noreferrer">
						413 错误说明
					</a>
					。
				</p>
				<h3 id="limit-terms">视频与大文件受服务条款限制</h3>
				<p>
					Cloudflare 的{" "}
					<a href={TERMS_APP_SERVICES} target="_blank" rel="noopener noreferrer">
						应用服务专项条款
					</a>
					里写明：免费、Pro、Business 方案的 CDN 不得用于提供视频，或提供比例失衡的图片、音频及其他大文件，否则 Cloudflare
					有权停用 CDN 服务。把隧道当影音站或公网网盘用，就是踩在这一条上。自用偶尔取个文件是一回事，长期跑流量是另一回事。
				</p>
				<h3 id="limit-china">大陆访客走的是境外节点</h3>
				<p>
					这是国内用户最容易失望的一点：免费方案不使用中国大陆境内的数据中心，大陆访客的请求会被路由到香港、日本、新加坡等地的节点，延迟和稳定性都受跨境链路影响。要用境内节点得上{" "}
					<a href={DOCS_CHINA} target="_blank" rel="noopener noreferrer">
						Cloudflare China Network
					</a>
					，那是与京东云合作的企业级方案，并且要求域名已完成 ICP 备案。所以：如果你的访客全在国内、又对延迟敏感，一台国内小服务器加 frp
					可能仍然更合适。反过来，如果只是自己在外面偶尔连回家，绕一圈境外完全能接受。
				</p>
				<h3 id="limit-compliance">合规这件事跟隧道无关</h3>
				<p>
					流量绕经境外不等于业务出境。服务跑在境内主机上、面向公众提供，该守的规矩一条都不会少。个人自用（连自己的 NAS、远程桌面回家）和对公众开放的站点，是两件性质不同的事，别混为一谈。
				</p>

				<h2 id="access">别让 NAS 裸奔：加一道 Access</h2>
				<p>
					隧道解决的是「能连上」，不解决「谁能连」。域名一旦生效，全世界都能打开它，剩下的防线只有你那个服务自己的登录页——而家用 NAS
					和各种自建面板的登录页，通常经不起自动化扫描。
				</p>
				<p>
					Cloudflare Access 可以在流量到达你的服务之前先拦一道：在 <strong>Zero Trust → Access controls → Applications</strong>{" "}
					里新建一个 self-hosted 应用，填上刚才那个域名，然后加一条 Allow 策略，比如只允许某个邮箱后缀、或者某几个具体邮箱。没有匹配到策略的请求根本走不进隧道。
				</p>
				<p>
					要留意的是：一个没有任何策略的应用会拒绝所有请求。加了应用一定记得配至少一条 Allow 策略，否则你自己也进不去。配置方法见{" "}
					<a href={DOCS_ACCESS} target="_blank" rel="noopener noreferrer">
						self-hosted 应用文档
					</a>
					。
				</p>

				<h2 id="errors">四个最常撞上的报错</h2>
				<h3 id="error-1033">1033：找不到健康的连接器</h3>
				<p>
					这是隧道类问题里最常见的一个，意思是 Cloudflare 收到了请求，却找不到活着的 <code>cloudflared</code>{" "}
					来接。先去仪表盘看隧道状态，对照处理：
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">状态</th>
								<th scope="col">含义</th>
								<th scope="col">怎么办</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">Healthy</th>
								<td>四条连接都在，正常服务</td>
								<td>问题不在隧道，查路由映射和服务本身</td>
							</tr>
							<tr>
								<th scope="row">Inactive</th>
								<td>隧道建好了，但连接器从来没跑起来过</td>
								<td>去机器上装并启动 cloudflared</td>
							</tr>
							<tr>
								<th scope="row">Down</th>
								<td>之前连过，现在进程停了</td>
								<td>看机器是否关机、服务是否崩溃、网络是否变动</td>
							</tr>
							<tr>
								<th scope="row">Degraded</th>
								<td>还在服务，但有连接失败</td>
								<td>查 cloudflared 日志和防火墙对 7844 的放行</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					还有一种情况：隧道删了或停了，DNS 记录却还留着。官方路由文档里说这时访客会看到{" "}
					<code>1016</code>，而 <a href={DOCS_1033} target="_blank" rel="noopener noreferrer">1033 的说明页</a>{" "}
					描述的是找不到健康实例。两个错误码指向的是同一件事的两个阶段，看到哪个都先查隧道状态。
				</p>
				<h3 id="error-7844">连不上 7844 端口</h3>
				<p>
					日志里出现 <code>failed to dial to edge with quic</code> 或者 <code>DialContext error: dial tcp ... i/o timeout</code>
					，说明出站被挡了。只有 UDP 被挡时，<code>cloudflared</code> 会自己退回 HTTP/2，服务还能用，只是性能差一些；UDP 和 TCP
					都被挡则完全连不上。排查用 <code>nc -vz -w 3 198.41.200.43 7844</code> 试一下（IP 换成日志里那个），必要时用{" "}
					<code>--protocol http2</code> 强制走 TCP。完整清单在{" "}
					<a href={DOCS_CONFIG} target="_blank" rel="noopener noreferrer">
						配置文档
					</a>
					里。
				</p>
				<h3 id="error-dns">edge discovery 解析失败</h3>
				<p>
					报错写着 <code>error looking up Cloudflare edge IPs</code>，是本机的 DNS 解析器返不回{" "}
					<code>cloudflared</code> 用来发现边缘节点的 SRV 记录。用{" "}
					<code>dig SRV _v2-origintunneld._tcp.argotunnel.com</code> 验一下，再加 <code>@1.1.1.1</code>{" "}
					对比：如果只有公共解析器能出结果，把机器的 DNS 换掉即可。这种情况在容器里尤其常见。
				</p>
				<h3 id="error-installed">cloudflared service is already installed</h3>
				<p>
					一台机器上只能有一个 <code>cloudflared</code> 系统服务。想再发布一个服务，正确做法是给现有隧道加一条路由，而不是再建一条隧道。确实要重来就先{" "}
					<code>sudo cloudflared service uninstall</code>。同理，保存公开域名时提示{" "}
					<code>An A, AAAA, or CNAME record with that host already exists</code>，是这个子域已经有 DNS 记录了，换个子域或先删掉旧记录。
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
