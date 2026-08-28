import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Link } from "@/i18n/navigation";
import { setRequestLocale } from "next-intl/server";
import GuideShell, { type RelatedLink } from "@/components/guides/GuideShell";
import TlsModeLadder from "@/components/guides/TlsModeLadder";
import { guideBySlug, guidePath, GUIDE_LOCALE_ZH } from "@/lib/guides/guides";

const SITE_URL = "https://oss.omail.us.kg";
const guide = guideBySlug("cloudflare-ssl-jiami-moshi", GUIDE_LOCALE_ZH);
const PATH = guidePath(GUIDE_LOCALE_ZH, `/guides/${guide.slug}`);

const DOCS_MODES = "https://developers.cloudflare.com/ssl/origin-configuration/ssl-modes/";
const DOCS_OFF = "https://developers.cloudflare.com/ssl/origin-configuration/ssl-modes/off/";
const DOCS_FLEXIBLE = "https://developers.cloudflare.com/ssl/origin-configuration/ssl-modes/flexible/";
const DOCS_FULL = "https://developers.cloudflare.com/ssl/origin-configuration/ssl-modes/full/";
const DOCS_STRICT = "https://developers.cloudflare.com/ssl/origin-configuration/ssl-modes/full-strict/";
const DOCS_ORIGIN_PULL = "https://developers.cloudflare.com/ssl/origin-configuration/ssl-modes/ssl-only-origin-pull/";
const DOCS_ORIGIN_CA = "https://developers.cloudflare.com/ssl/origin-configuration/origin-ca/";
const DOCS_TRUST_STORE = "https://developers.cloudflare.com/ssl/origin-configuration/custom-origin-trust-store/";
const DOCS_AOP = "https://developers.cloudflare.com/ssl/origin-configuration/authenticated-origin-pull/";
const DOCS_REDIRECTS = "https://developers.cloudflare.com/ssl/troubleshooting/too-many-redirects/";
const DOCS_CACHE_KEYS = "https://developers.cloudflare.com/cache/how-to/cache-keys/";
const DOCS_ALWAYS_HTTPS = "https://developers.cloudflare.com/ssl/edge-certificates/additional-options/always-use-https/";
const DOCS_HSTS =
	"https://developers.cloudflare.com/ssl/edge-certificates/additional-options/http-strict-transport-security/";
const DOCS_ACM = "https://developers.cloudflare.com/ssl/edge-certificates/advanced-certificate-manager/";
const DOCS_525 =
	"https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-5xx-errors/error-525/";
const DOCS_526 =
	"https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-5xx-errors/error-526/";

/** FAQ 一处定义：可见文本与 FAQPage JSON-LD 同源，保证逐字一致 */
const FAQ: Array<{ q: string; a: string }> = [
	{
		q: "Cloudflare 的 SSL 模式该选灵活还是完全？",
		a: "只要源站能装证书，就别选灵活。灵活会让 Cloudflare 到源站那一段完全走明文，中间任何一跳都能看到并篡改内容，官方也明确建议涉及登录或个人数据的站点改用完全或完全（严格）。源站只有自签或过期证书就选完全，有公开 CA 或 Cloudflare 源服务器 CA 签发的有效证书就选完全（严格）。两样都没有，去签一张源服务器证书，比停在灵活划算得多。",
	},
	{
		q: "开了 Cloudflare 之后重定向次数过多怎么办？",
		a: "先看加密模式和源站的跳转规则是不是在对着干。灵活模式下 Cloudflare 用 HTTP 回源，源站如果强制跳 HTTPS 就会来回成环——把模式升到完全，或者去掉源站的强制跳转，二选一。反过来，用完全系列而源站把 HTTPS 往 HTTP 跳，同样成环。还要检查始终使用 HTTPS 是否与源站跳转冲突、HSTS 是否与关闭模式同时开着，以及多条重定向规则之间有没有互相指。",
	},
	{
		q: "525 和 526 有什么区别？",
		a: "525 是 SSL 握手就没成，Cloudflare 连证书都没拿到，通常是源站 443 端口没开、没装证书、不支持 SNI，或者加密套件与 Cloudflare 对不上。526 是握手成了、证书也拿到了，但没通过校验：过期、自签、被吊销、CN 或 SAN 与主机名不匹配，或者中间证书没配全。525 在完全和完全（严格）下都可能出现，526 只出现在完全（严格）。",
	},
	{
		q: "Cloudflare 源服务器证书和公开 CA 的证书该用哪个？",
		a: "看这个主机名的流量是不是永远经过 Cloudflare。是的话用源服务器证书更省事：全方案免费、签完就能用、有效期可以设得很长，也不用为续期折腾。但它只被 Cloudflare 信任，一旦这个主机名改成灰云直连或者暂停了 Cloudflare，访客就会撞上证书警告。存在直连可能的主机名，还是用公开 CA 签发的证书稳妥。",
	},
	{
		q: "改了加密模式要多久生效？会影响网站吗？",
		a: "设置本身几乎立刻全网生效，真正的影响在缓存上。默认缓存键里含回源协议，从关闭或灵活改到完全系列，回源协议由 HTTP 变成 HTTPS，原有缓存会整体作废，需要重新回源填充。所以别在流量高峰或活动期间改，尤其源站在国内、边缘节点在境外的时候，那段重建期的跨境回源量不小。",
	},
];

const RELATED: RelatedLink[] = [
	{
		href: "/guides/cloudflare-522-error",
		label: "Cloudflare 为什么会报 522 错误？",
		note: "525 是握手失败，522 是连都没连上——两种回源故障卡在不同的步骤，排查路径也不一样。",
	},
	{
		href: "/guides/cloudflare-xiaohuangyun",
		label: "Cloudflare 的小黄云到底是什么？什么时候该关掉？",
		note: "加密模式只对开着代理的记录生效。灰云记录的流量压根不经过 Cloudflare，这个设置对它没有意义。",
	},
	{
		href: "/guides/cloudflare-yuanzhan-ip-xielou",
		label: "套了 Cloudflare，源站真实 IP 还会泄露吗？",
		note: "把回源这一段加密之外，还得堵住那些绕开代理直接指向源站的路径。",
	},
	{
		href: DOCS_MODES,
		label: "Cloudflare 官方文档：加密模式",
		note: "本文所有行为描述的原始出处，含自动 SSL/TLS 的灰度规则与各档的 API 取值。",
		external: true,
	},
	{
		href: DOCS_REDIRECTS,
		label: "Cloudflare 官方文档：ERR_TOO_MANY_REDIRECTS",
		note: "重定向循环的四种成因，每种都配了环路示意图。",
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

export default async function SslModesGuideZh({ params }: { params: Promise<{ locale: string }> }) {
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
				lede="这几个选项的名字听着像安全等级，其实是几种截然不同的连接方式。选错了不一定报错——地址栏那把锁照样是好的，中间那段路却在明文里跑。"
				updated={guide.updated}
				readingTime={guide.readingTime}
				related={RELATED}
			>
				<div className="glass r-island note p-6 sm:p-7">
					<p>
						加密模式管的只是 Cloudflare
						到源站这一段。源站有公开受信任的证书就选「完全（严格）」，只有自签证书选「完全」，两样都没有才退到「灵活」。
					</p>
				</div>

				<p>
					「SSL/TLS」→「概览」页面上那一组单选框，是 Cloudflare
					里最容易选错、也最容易一直错下去的设置。错了往往不报错：网站照常打开，浏览器地址栏的锁头也是好的，只有 Cloudflare
					到你服务器那一段在明文里跑，页面上留不下任何痕迹。
				</p>
				<p>
					另一种错法则相反，吵得很：一改就是「重定向次数过多」，或者满屏 525、526。这两类症状看着毫不相干，根却在同一处。
				</p>

				<h2 id="two-hops">一个设置，两段连接</h2>
				<p>
					一次请求从访客走到你的服务器，被 Cloudflare 切成了两段（前提是这条 DNS
					记录开着代理，也就是<Link href="/guides/cloudflare-xiaohuangyun">小黄云</Link>；灰云记录的 HTTP 流量压根不经过
					Cloudflare，这个设置对它没有意义）。
				</p>
				<p>
					<strong>第一段</strong>是访客的浏览器到 Cloudflare 边缘节点，由 Cloudflare 签发并托管的边缘证书负责，接入即有，你基本不用管它。
				</p>
				<p>
					<strong>第二段</strong>是 Cloudflare 回源到你的服务器。加密模式这个设置，从头到尾只在管这一段：用不用 HTTPS
					回源、用了之后验不验证你的证书。
				</p>
				<TlsModeLadder />
				<p>
					所以那句话得反过来听：锁是绿的，只说明第一段没问题。第二段长什么样，访客看不见，你不去查也看不见。
				</p>

				<h2 id="five-modes">五档模式，各自到底是什么意思</h2>
				<p>
					Cloudflare 一共给了五档，其中一档是 Enterprise 专属。名字里的「灵活」「完全」并不构成一条从松到紧的连续刻度，它们是四种不同的回源方式。
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">模式</th>
								<th scope="col">Cloudflare 怎么回源</th>
								<th scope="col">什么时候用</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">
									<a href={DOCS_OFF} target="_blank" rel="noopener noreferrer">
										关闭
									</a>
									（Off）
								</th>
								<td>全程明文，访客发来的 HTTPS 请求会被跳回 HTTP</td>
								<td>没有该用它的场景，官方也不推荐</td>
							</tr>
							<tr>
								<th scope="row">
									<a href={DOCS_FLEXIBLE} target="_blank" rel="noopener noreferrer">
										灵活
									</a>
									（Flexible）
								</th>
								<td>访客那段加密，回源固定走明文 HTTP，源站不需要装任何证书</td>
								<td>源站实在装不了证书时的过渡手段</td>
							</tr>
							<tr>
								<th scope="row">
									<a href={DOCS_FULL} target="_blank" rel="noopener noreferrer">
										完全
									</a>
									（Full）
								</th>
								<td>按访客用的协议回源，访客用 HTTPS 就用 HTTPS，但源站证书不做任何校验</td>
								<td>源站只有自签证书或已过期的证书</td>
							</tr>
							<tr>
								<th scope="row">
									<a href={DOCS_STRICT} target="_blank" rel="noopener noreferrer">
										完全（严格）
									</a>
									（Full strict）
								</th>
								<td>同上，但证书必须未过期、由受信任的 CA 或 Cloudflare 源服务器 CA 签发、CN 或 SAN 与主机名对得上</td>
								<td>默认就该选这一档</td>
							</tr>
							<tr>
								<th scope="row">
									<a href={DOCS_ORIGIN_PULL} target="_blank" rel="noopener noreferrer">
										严格（仅 SSL 回源）
									</a>
								</th>
								<td>不管访客用什么协议，回源一律 HTTPS 并验证证书</td>
								<td>仅 Enterprise 方案可选</td>
							</tr>
						</tbody>
					</table>
				</div>

				<h3 id="full-vs-strict">「完全」和「完全（严格）」之间那道坎</h3>
				<p>两者对连接的处理方式完全一样，差别只有一件事：验不验证源站递过来的那张证书。</p>
				<p>
					「完全」这一档，官方文档写得毫不含糊——源站的证书<strong>不做任何形式的校验</strong>：可以过期，可以自签，甚至 CN / SAN
					跟被请求的主机名对不上，照收不误。
				</p>
				<p>
					这意味着加密是真加密，但「在跟谁加密」没人确认。真有人在中间劫持了这条连接、递上自己的一张证书，Cloudflare
					一样会接受。所以「完全」买到的是防窃听，不是防冒充。
				</p>
				<p>
					「完全（严格）」补上的正是这一环，要求三条同时成立：证书未过期；由公开受信任的 CA 或 Cloudflare 的源服务器 CA 签发；证书里的 CN 或
					SAN 覆盖被请求的那个主机名。三条缺一，回源直接失败，访客拿到 526。
				</p>

				<h3 id="why-flexible-hurts">「灵活」的三层代价</h3>
				<p>「灵活」的卖点是省事：源站什么都不用装，网站立刻就有了 HTTPS。代价有三层，一层比一层实。</p>
				<p>
					<strong>第一层在明面上。</strong>Cloudflare 到你服务器那一段是明文 HTTP，中间任何一跳都能看到完整内容，也能改。表单里的密码、登录
					Cookie、后台接口，全在里面。官方的措辞很直接：涉及个人数据或用户登录的应用请改用「完全」或「完全（严格）」。
				</p>
				<p>
					<strong>第二层是它并不像名字那样「统一」。</strong>灵活模式只对 443 端口上的 HTTPS 连接生效，其它 HTTPS
					端口会自动回落成「完全」的行为——你以为全站一个规则，实际上不是。另外，
					<a href={DOCS_AOP} target="_blank" rel="noopener noreferrer">
						源站认证回源
					</a>
					在「关闭」和「灵活」这两档下不工作，想用它得先把模式升上去。
				</p>
				<p>
					<strong>第三层是最常撞上的：重定向循环。</strong>Cloudflare 用 HTTP
					回源，而你的服务器——Nginx 里的 301 跳转、面板上的「强制 HTTPS」、WordPress 里设成 <code>https://</code>{" "}
					的站点地址——一看到 HTTP 请求就往 HTTPS 跳，跳回 Cloudflare，Cloudflare 再降成 HTTP 发过去。一圈一圈，浏览器数够次数就报{" "}
					<code>ERR_TOO_MANY_REDIRECTS</code>。
				</p>

				<h2 id="three-errors">三个报错，各自卡在哪一步</h2>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">现象</th>
								<th scope="col">说明什么</th>
								<th scope="col">先查哪里</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">重定向次数过多</th>
								<td>加密模式与源站的跳转规则打架</td>
								<td>「灵活 + 源站强制 HTTPS」是最常见的组合；反过来，用「完全」系列而源站把 HTTPS 往 HTTP 跳，一样成环</td>
							</tr>
							<tr>
								<th scope="row">
									<a href={DOCS_525} target="_blank" rel="noopener noreferrer">
										525
									</a>{" "}
									SSL 握手失败
								</th>
								<td>已经在用「完全」或「完全（严格）」，但握手没做成</td>
								<td>源站 443 端口没开、没装证书、不支持 SNI，或者加密套件与 Cloudflare 对不上</td>
							</tr>
							<tr>
								<th scope="row">
									<a href={DOCS_526} target="_blank" rel="noopener noreferrer">
										526
									</a>{" "}
									证书无效
								</th>
								<td>已经在用「完全（严格）」，握手成了但证书没通过校验</td>
								<td>过期、自签、被吊销、CN 或 SAN 不匹配，或者中间证书没配全</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					关于重定向循环，还有两个不属于加密模式、但同样会成环的开关值得一并检查：
					<a href={DOCS_ALWAYS_HTTPS} target="_blank" rel="noopener noreferrer">
						始终使用 HTTPS
					</a>
					打开而源站把 HTTPS 往 HTTP 跳；以及
					<a href={DOCS_HSTS} target="_blank" rel="noopener noreferrer">
						HSTS
					</a>
					打开的同时加密模式停在「关闭」。多条重定向规则互相指也会成环，改完一处不通就顺手把规则列表扫一遍。
				</p>
				<p>
					526 那行里的「中间证书没配全」最容易被忽略：源站必须把叶子证书连同所需的中间 CA 证书一起发出来，Cloudflare
					才拼得出一条到根 CA 的信任链。用本机浏览器打开没问题不算数——浏览器会自己想办法补链，Cloudflare 不会。
				</p>

				<h2 id="origin-ca">源站没有受信任的证书？别急着退回「灵活」</h2>
				<p>
					这是卡住最多人的一处：面板生成的是自签证书，选「完全（严格）」就 526，于是退回「灵活」，然后开始跟重定向循环搏斗。
				</p>
				<p>
					其实还有第三条路：Cloudflare 的
					<a href={DOCS_ORIGIN_CA} target="_blank" rel="noopener noreferrer">
						源服务器证书
					</a>
					（Origin CA）。在「SSL/TLS」→「源服务器」页面签一张，装到你的 Web 服务器上，「完全（严格）」就通了。几个要点：
				</p>
				<ul>
					<li>全方案免费，Free 也能用；签发时要求账号具备 API 访问权限，没有的话会直接报错。</li>
					<li>
						一张最多 200 个 SAN，支持通配符，但通配符只覆盖一级——<code>*.example.com</code> 管不到{" "}
						<code>a.b.example.com</code>，需要的话再加一条 <code>*.b.example.com</code>。
					</li>
					<li>不能把 IP 地址写进 SAN。</li>
					<li>它只被 Cloudflare 信任，浏览器不认，所以只能装在流量确实经过 Cloudflare 的主机名上。</li>
					<li>Cloudflare 不会为源服务器证书发到期提醒。有效期签得长的话，得自己记进监控。</li>
				</ul>
				<p>倒数第二条是它最锋利的地方，也正是下一节要说的坑。</p>

				<h2 id="china">在大陆环境下，还要多算这几笔</h2>
				<p>
					<strong>灰云子域会当场翻车。</strong>国内常见的做法是分流：对外的页面走 Cloudflare，后台或者国内高频访问的子域改回灰云直连，图个低延迟。但源服务器证书只被
					Cloudflare 信任——一旦某个主机名改成灰云，或者你把整个域名暂停了 Cloudflare，访客直连到源站，看到的就是一张浏览器不认识的证书，红色警告糊满全屏。要这么分流，那些直连的主机名必须单独装一张公开
					CA 签发的证书。
				</p>
				<p>
					<strong>面板自签证书对应的是「完全」，不是「完全（严格）」。</strong>
					面板默认生成的多是自签证书，直接选「完全（严格）」必然 526。想升上去，要么换成公开 CA 签发的证书，要么用上面那张源服务器证书。官方文档里还给了第三个办法——把自己的
					CA 传进
					<a href={DOCS_TRUST_STORE} target="_blank" rel="noopener noreferrer">
						自定义源站信任存储
					</a>
					让 Cloudflare 认——但这项能力要求域名开通
					<a href={DOCS_ACM} target="_blank" rel="noopener noreferrer">
						高级证书管理器
					</a>
					，是付费加购项，免费方案用不了，别照着文档白折腾一趟。
				</p>
				<p>
					<strong>改这个设置会清空缓存。</strong>在默认缓存键下，Cloudflare 的 <code>$scheme</code>{" "}
					变量指的是回源协议。从「关闭」或「灵活」升到「完全」系列，回源协议由 HTTP 变成 HTTPS，
					<a href={DOCS_CACHE_KEYS} target="_blank" rel="noopener noreferrer">
						缓存键
					</a>
					随之改变，原来那批缓存整体作废，得重新回源填充。源站在国内、边缘节点在境外的组合下，这段重建期的跨境回源量会明显上一个台阶——挑低峰时段改，别在活动当天动它。
				</p>

				<h2 id="automatic">现在这个设置，可能已经不归你管了</h2>
				<p>
					Cloudflare 正在分批把域名切到「自动 SSL/TLS」，并且把它作为默认档。它会用 <code>Cloudflare-SSLDetector</code>{" "}
					这个用户代理定期爬你的站点（目前大约每月一次），分别用 HTTP 和 HTTPS
					取一遍内容做相似度比对，判断源站到底能撑到哪一档，然后自动把设置升上去。
				</p>
				<p>几条行为值得先知道：</p>
				<ul>
					<li>
						<strong>只升不降。</strong>源站证书过期了它也不会把「完全（严格）」降回「完全」，该 526 还是 526——证书的有效性得你自己守着。
					</li>
					<li>
						<strong>升级是灰度的。</strong>先放 1% 的流量试，没问题再按 10% 递增到全量；中途回源失败会立即整体回滚到原来的模式并记录下来。
					</li>
					<li>
						<strong>从「灵活」往上升会走得更慢。</strong>因为回源协议一变缓存键就变，Cloudflare 会留时间给缓存预热再继续递增。
					</li>
					<li>
						<strong>每周发一封汇总邮件</strong>，列出哪些域名被升过档，收件人目前只有超级管理员。
					</li>
				</ul>
				<p>
					不想让它自己动手，切到「自定义 SSL/TLS」手动指定即可；域名多的话，把 <code>ssl_automatic_mode</code> 这个设置 PATCH 成{" "}
					<code>custom</code> 批量退出。
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
