import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Link } from "@/i18n/navigation";
import { setRequestLocale } from "next-intl/server";
import GuideShell, { type RelatedLink } from "@/components/guides/GuideShell";
import DnsAuthorityBranch from "@/components/guides/DnsAuthorityBranch";
import { guideBySlug, guidePath, GUIDE_LOCALE_ZH } from "@/lib/guides/guides";

const SITE_URL = "https://o-c.do";
const guide = guideBySlug("cloudflare-dns-jiexi-bu-shengxiao", GUIDE_LOCALE_ZH);
const PATH = guidePath(GUIDE_LOCALE_ZH, `/guides/${guide.slug}`);

const DOCS_TTL = "https://developers.cloudflare.com/dns/manage-dns-records/reference/ttl/";
const DOCS_STATUS = "https://developers.cloudflare.com/dns/zone-setups/reference/domain-status/";
const DOCS_PENDING = "https://developers.cloudflare.com/dns/zone-setups/troubleshooting/pending-nameservers/";
const DOCS_NS = "https://developers.cloudflare.com/dns/nameservers/update-nameservers/";
const DOCS_FULL_TS = "https://developers.cloudflare.com/dns/zone-setups/full-setup/troubleshooting/";
const DOCS_STALE = "https://developers.cloudflare.com/dns/manage-dns-records/troubleshooting/stale-response/";
const DOCS_PROXY = "https://developers.cloudflare.com/dns/proxy-status/";
const DOCS_ANYCAST = "https://developers.cloudflare.com/fundamentals/concepts/cloudflare-ip-addresses/";
const DOCS_PURGE = "https://developers.cloudflare.com/cache/how-to/purge-cache/";
const DOCS_DNSSEC = "https://developers.cloudflare.com/dns/dnssec/";
const DOCS_NS_OPTIONS = "https://developers.cloudflare.com/dns/nameservers/nameserver-options/";
const DOCS_FLATTENING = "https://developers.cloudflare.com/dns/cname-flattening/";
const DOCS_ACTIVATION =
	"https://developers.cloudflare.com/api/resources/zones/subresources/activation_check/methods/trigger/";
const TOOL_PURGE = "https://one.one.one.one/purge-cache/";

/** FAQ 一处定义：可见文本与 FAQPage JSON-LD 同源，保证逐字一致 */
const FAQ: Array<{ q: string; a: string }> = [
	{
		q: "Cloudflare 修改 DNS 记录多久生效？",
		a: "前提是域名状态已经是 Active。已代理（小黄云）的记录 TTL 固定为 Auto，即 300 秒，改不了；仅 DNS 的记录可在 60 秒（企业方案 30 秒）到 1 天之间选。官方还补了一句：你自己感受到变化可能比 TTL 更久，因为本机缓存不一定这么快跟上。",
	},
	{
		q: "域名一直卡在 Pending Nameserver Update 怎么办？",
		a: "按顺序查四件事：那两个 NS 有没有被逐字复制过去，注册商那边有没有留着别家的 NS 没删（完整接入模式下多一条都不行），改动有没有真的被推送出去，以及有没有残留上一家的 DS 记录。查委派用 dig +trace example.com NS，查 DS 用 dig DS example.com。",
	},
	{
		q: "怎么确认 DNS 已经生效了？",
		a: "别用浏览器，也别用 ping——这两个都吃本机缓存。用 dig 分别向几个解析器提问，再加一次 dig www.example.com @<你的 Cloudflare NS> 直接问权威。权威返回新值而公共解析器返回旧值，说明只是缓存还没过期；权威那一次也是旧值，问题才在配置本身。",
	},
	{
		q: "改了 DNS 但网站打开的还是旧页面，是解析没生效吗？",
		a: "多半不是。DNS 决定请求发到哪个地址，不决定那个地址返回什么内容。小黄云开着时页面可能直接来自 Cloudflare 的边缘缓存，需要单独清一次。先确认解析已经指到新地址，再去清缓存，两件事别混在一起试。",
	},
	{
		q: "国内运营商的 DNS 缓存能强制刷新吗？",
		a: "别人的递归解析器不归你管，你能做的只有等 TTL 过期。Cloudflare 为自家的 1.1.1.1 提供了刷新缓存的小工具，但它只刷 1.1.1.1 那一份，对电信、联通、移动的解析器没有作用。真正有用的是提前量：切换前一两天先把 TTL 调到 60 秒，等旧值在各处过期之后再动记录。",
	},
];

const RELATED: RelatedLink[] = [
	{
		href: "/guides/cloudflare-xiaohuangyun",
		label: "Cloudflare 的小黄云到底是什么？什么时候该关掉？",
		note: "代理开着的时候 DNS 应答返回的是任播 IP——这正是改了源站 IP 却在 dig 里看不出变化的原因。",
	},
	{
		href: "/guides/cloudflare-cname-zhanping",
		label: "Cloudflare 的 CNAME 展平（拉平）到底做了什么？",
		note: "展平会把上游的答案缓存下来，上游抽风时 Cloudflare 会拿过期结果顶上，看起来就像「改了不生效」。",
	},
	{
		href: "/guides/cloudflare-huancun-mingzhonglv",
		label: "Cloudflare 到底缓存了什么？为什么命中率一直上不去",
		note: "解析已经指到新地址、页面还是旧的，那就不是 DNS 的事，该去看边缘缓存这一层。",
	},
	{
		href: "/guides/cloudflare-522-error",
		label: "Cloudflare 为什么会报 522 错误？",
		note: "把源站 IP 改错之后最常见的下场：解析生效得很快，边缘节点却连不上新地址。",
	},
	{
		href: DOCS_PENDING,
		label: "Cloudflare 官方文档：域名卡在 Pending Nameserver Update",
		note: "本文第三节的原始出处，含逐步核对委派与排查 DS 残留的完整命令。",
		external: true,
	},
	{
		href: DOCS_TTL,
		label: "Cloudflare 官方文档：TTL",
		note: "已代理与仅 DNS 两种记录各自的 TTL 取值范围，以及本地缓存滞后的那条说明。",
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

export default async function DnsNotWorkingGuideZh({ params }: { params: Promise<{ locale: string }> }) {
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
				lede="「改了 DNS 不生效」这句话里藏着两个完全不同的问题。一个要去改配置，一个只需要等——先分清楚，能省下一整晚的瞎折腾。"
				updated={guide.updated}
				readingTime={guide.readingTime}
				related={RELATED}
			>
				<div className="glass r-island note p-6 sm:p-7">
					<p>
						先分清是哪一种「不生效」：域名的 NS 还没指到 Cloudflare，你改的记录压根不参与解析；已经指过来了，剩下的就只是
						TTL 和沿途缓存在拖时间。
					</p>
				</div>

				<p>
					绝大多数情况下，改动本身没有任何问题。真正要判断的只有一件事：
					<strong>此刻回答这个域名查询的，到底是不是 Cloudflare</strong>
					。答案不同，动作完全相反——一种要立刻改配置，另一种恰恰是什么都别做。
				</p>

				<h2 id="two-kinds">两种「不生效」，别混在一起查</h2>
				<p>
					一次查询是自上而下的：解析器没有缓存就去问父区（<code>.com</code>、<code>.cn</code> 这些注册局），父区返回一组 NS
					指明谁有资格回答；解析器再向那组 NS 提问，拿到答案后按 TTL 存一段时间。
				</p>
				<DnsAuthorityBranch />
				<p>
					所以「改了不生效」只有两种可能：要么父区的委派还指着旧服务商，你的操作根本没进这条链路，等多久都没用；要么委派已经指向
					Cloudflare，权威侧早就是新值，只是沿途某层缓存还没到期。
				</p>
				<p>区分这两种情况只要一条命令——直接向 Cloudflare 分配给你的 NS 提问，绕开所有缓存：</p>
				<pre>
					<code>dig www.example.com @vera.ns.cloudflare.com</code>
				</pre>
				<p>返回新值，配置这一侧就没问题，剩下的纯粹是等；返回旧值或查不到，问题在配置本身。</p>

				<h2 id="zone-status">先看域名状态：三种状态，三种行为</h2>
				<p>
					面板首页那个状态标签不是装饰，它直接决定 Cloudflare 对外怎么回答查询。完整的
					<a href={DOCS_STATUS} target="_blank" rel="noopener noreferrer">
						域名状态
					</a>
					有六种，和「不生效」相关的是前三种。
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">状态</th>
								<th scope="col">Cloudflare 怎么回答查询</th>
								<th scope="col">你改的记录算不算数</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">完成设置（Initializing）</th>
								<td>完全不回答这个域名的任何 DNS 查询</td>
								<td>不算。方案都没选完，域名等于没接上</td>
							</tr>
							<tr>
								<th scope="row">待更新 NS（Pending）</th>
								<td>会在分配给你的那组 NS 上回答，但域名尚未激活，也不能代理流量</td>
								<td>看父区委派指向谁。委派没切过来，外界问不到这组 NS</td>
							</tr>
							<tr>
								<th scope="row">已激活（Active）</th>
								<td>正常回答，代理、缓存、WAF 等能力全部可用</td>
								<td>算。此后的「不生效」只剩缓存这一个原因</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					Pending 是误会的温床：这个状态下 Cloudflare 会回答——前提是有人真的来问它。父区委派还指着旧服务商时，解析器根本走不到 Cloudflare
					这一侧，你改的每一条记录都不参与解析。
				</p>

				<h2 id="pending">NS 改了却一直 Pending：四个卡点</h2>
				<p>
					这是中文社区出现频率最高的一类。官方那篇
					<a href={DOCS_PENDING} target="_blank" rel="noopener noreferrer">
						排查文档
					</a>
					给的顺序很清楚，照着走就行；各家注册商改 NS 的具体位置见
					<a href={DOCS_NS} target="_blank" rel="noopener noreferrer">
						更新 NS 文档
					</a>
					。
				</p>
				<h3 id="check-delegation">看父区实际发布的是什么</h3>
				<p>
					注册商面板显示的是你「要求」它发布的内容，不等于注册局实际对外返回的。两者可能因为没保存、改错了域名、改在另一个账号下或还没推送而不一致。查真相的命令是：
				</p>
				<pre>
					<code>dig +trace example.com NS +noall +authority +nodnssec</code>
				</pre>
				<p>
					<code>+trace</code> 从根开始一级级跟着委派往下走，最后一段非空输出就是父区实际给出的委派，它应该<strong>只</strong>包含分配给这个域名的那组 NS。
				</p>
				<h3 id="exact-match">必须是「这个域名的那一组」，而且只有它们</h3>
				<p>
					两个细节咬人特别狠。其一，完整接入模式下注册商那里<strong>只能</strong>留 Cloudflare 的 NS，多留一条别家的都会导致激活失败，除非显式开了
					<a href={DOCS_NS_OPTIONS} target="_blank" rel="noopener noreferrer">
						多提供商 DNS
					</a>
					。其二，NS 按域名分配，不是账号通用的一对，别把别的域名那组照抄过来；官方还提醒要从面板直接复制别手打，
					<code>cloudlfare.com</code> 这类拼错很常见。
				</p>
				<p>
					面板分配的 NS 和预期不一样，通常是因为创建域名时委派就已指向 Cloudflare——为防劫持它会另外分配一组，删掉重加同理。所以顺序永远是：先建好域名，再拿概览页上的值去注册商那里改。
				</p>
				<h3 id="ds-record">注册商那里有没有残留的 DS 记录</h3>
				<p>
					委派查着完全正确、状态却纹丝不动，十有八九是这一条。DS 记录存放在注册商而非 DNS
					服务商那里，换服务商时必须手动删除；留着不管，DNSSEC 信任链就断了，解析器对你的域名一律返回 SERVFAIL。
				</p>
				<pre>
					<code>dig DS example.com</code>
				</pre>
				<p>
					有返回、而你又没在 Cloudflare 这边主动配过
					<a href={DOCS_DNSSEC} target="_blank" rel="noopener noreferrer">
						DNSSEC
					</a>
					，那就是上一家留下的。去注册商的 DNSSEC 设置（常在「高级 DNS」或「安全」下面）删干净，官方说要留最多 24
					小时让它在各处缓存里过期。这一条在
					<a href={DOCS_FULL_TS} target="_blank" rel="noopener noreferrer">
						完整接入的排查页
					</a>
					上也重复了一遍。
				</p>
				<h3 id="recheck">等激活检查，或者手动催一次</h3>
				<p>
					上面三条都没问题就只剩等。Cloudflare 会按计划反复检查 NS 是否已更新，<strong>第一次在 60 秒后</strong>
					，之后间隔逐步拉长；你可以在概览页手动触发，也可以
					<a href={DOCS_ACTIVATION} target="_blank" rel="noopener noreferrer">
						走 API
					</a>
					。但别指望点一下就立刻激活——接口有限流，触发成功只是把域名放进优先队列，实际激活可能要几分钟到几小时。免费方案在 Pending
					超过 28 天会被自动删除。
				</p>

				<h2 id="ttl">已经 Active 了：那就只是 TTL 在走</h2>
				<p>
					状态是 Active、问权威也返回新值，那么剩下的全部是等待。等多久由
					<a href={DOCS_TTL} target="_blank" rel="noopener noreferrer">
						TTL
					</a>
					决定。
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">记录类型</th>
								<th scope="col">TTL 可选范围</th>
								<th scope="col">说明</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">已代理（小黄云）</th>
								<td>固定 Auto = 300 秒，不可修改</td>
								<td>返回的是任播 IP，可能变动，所以不允许解析器缓存超过五分钟</td>
							</tr>
							<tr>
								<th scope="row">仅 DNS（灰云）</th>
								<td>60 秒（企业方案 30 秒）到 1 天</td>
								<td>Auto 同样是 300 秒；自己填的话，切换前调低、切换后调回</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					TTL 文档里那句提示值得抄下来：实际感受到变化可能比 TTL
					更久，因为本机缓存不一定跟得上。五分钟是解析器那一层的上限，不是你打开浏览器就能看到新结果的保证——系统缓存、浏览器缓存、路由器转发，每一层都可能再压一会儿。
				</p>
				<p>
					这也解释了「降低 TTL」为什么只在<strong>改动之前</strong>有用：TTL 随上一次应答一起发出，等你意识到不对再去调，各处缓存里存着的仍是旧那份的过期时间。
				</p>

				<h2 id="proxied-illusion">开着小黄云时，dig 根本看不出变化</h2>
				<p>
					<Link href="/guides/cloudflare-xiaohuangyun">小黄云</Link>开着时对外返回的是 Cloudflare 的
					<a href={DOCS_ANYCAST} target="_blank" rel="noopener noreferrer">
						任播 IP
					</a>
					，不是你填在记录里的源站地址。所以把源站 IP 从 <code>203.0.113.10</code> 改成 <code>203.0.113.20</code> 之后，
					<code>dig</code> 出来的结果<strong>一个字都不会变</strong>——它本来就没在返回你的源站地址。
				</p>
				<p>
					这不是没生效，恰恰相反：下一个请求 Cloudflare 就会用新地址回源。想验证只能从回源那一侧看——查新源站的访问日志，或临时把记录改成灰云再
					<code>dig</code> 一次。
				</p>
				<p>
					反过来，
					<a href={DOCS_PROXY} target="_blank" rel="noopener noreferrer">
						代理状态
					</a>
					本身的切换是能被 <code>dig</code> 看出来的：橙改灰，返回值从任播 IP 变成源站 IP；灰改橙则相反。
				</p>

				<h2 id="not-dns">还有两种情况，根本不是 DNS 的问题</h2>
				<h3 id="edge-cache">页面还是旧的，但地址已经对了</h3>
				<p>
					DNS 只负责把请求送到哪个地址，不负责那个地址返回什么。解析已指向新服务器、打开还是老页面，那多半是内容在 Cloudflare
					的边缘缓存里，需要单独
					<a href={DOCS_PURGE} target="_blank" rel="noopener noreferrer">
						清一次缓存
					</a>
					，判断方法见
					<Link href="/guides/cloudflare-huancun-mingzhonglv">Cloudflare 到底缓存了什么</Link>。
				</p>
				<h3 id="stale">CNAME 上游抽风时，Cloudflare 会拿过期答案顶上</h3>
				<p>
					用了
					<a href={DOCS_FLATTENING} target="_blank" rel="noopener noreferrer">
						CNAME 展平
					</a>
					（已代理的 CNAME 默认就是展平的）时，Cloudflare 要去解析上游那个主机名。官方在
					<a href={DOCS_STALE} target="_blank" rel="noopener noreferrer">
						过期应答
					</a>
					文档里写明：上游响应太慢或返回 SERVFAIL 时，Cloudflare 会先用缓存里已过期的那份顶上，再异步更新。你改的是上游的记录，看到的却是它手里的旧值——这种情况要去催上游。
				</p>

				<h2 id="china">国内环境：这一层的账要单独算</h2>
				<p>前面的规则通用，下面几条是大陆用户会额外遇到的。</p>
				<p>
					<strong>TTL 是建议，不是约束。</strong>递归解析器缓存多久由它自己决定。官方那句「本地缓存可能更慢」放到国内的多层转发环境里会被放大：家用路由器转发一层、运营商解析器再存一层，实际滞后经常明显超过
					300 秒。这不是配置错了，只是你恰好在一条缓存比较厚的链路上。
				</p>
				<p>
					<strong>验证要横着比。</strong>判断扩散进度最有效的办法是同时问几个不同的解析器：
				</p>
				<pre>
					<code>
						dig www.example.com @1.1.1.1{"\n"}
						dig www.example.com @8.8.8.8{"\n"}
						dig www.example.com @223.5.5.5{"\n"}
						dig www.example.com @119.29.29.29
					</code>
				</pre>
				<p>
					几家返回不一致，说明正在扩散，等就行；全是旧值但直接问权威是新值，那只是缓存还没过期；问权威也是旧值，回前面几节查配置。
				</p>
				<p>
					<strong>
						别用 <code>ping</code> 判断解析。
					</strong>
					它走系统解析缓存，本身就滞后一层；小黄云开着时它 ping 的还是任播地址，跟源站 IP 毫无关系。<code>nslookup</code>{" "}
					默认也问本机配的解析器，同样吃缓存。要看真相就明确指定要问谁。
				</p>
				<p>
					<strong>别人的缓存刷不动。</strong>Cloudflare 给自家的 1.1.1.1 有一个
					<a href={TOOL_PURGE} target="_blank" rel="noopener noreferrer">
						刷新缓存的小工具
					</a>
					，但只对 1.1.1.1 自己有效，电信、联通、移动的解析器不在此列。对国内访客，唯一可靠的手段还是提前量。
				</p>
				<p>
					<strong>注册商推送的节奏各不相同。</strong>改完 NS 推到注册局从几分钟到数小时不等，官方给的通用窗口是最长 24
					小时。窗口内 <code>dig +trace</code> 还是旧 NS 属于正常，别急着来回改。
				</p>

				<h2 id="checklist">排查顺序清单</h2>
				<p>压成一条流水线，从上往下走，通常前三步就能定位。</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">顺序</th>
								<th scope="col">查什么</th>
								<th scope="col">结论</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">1</th>
								<td>面板上的域名状态是不是 Active</td>
								<td>不是，就先解决接入问题，改记录没有意义</td>
							</tr>
							<tr>
								<th scope="row">2</th>
								<td>
									<code>dig +trace example.com NS</code>
								</td>
								<td>父区返回的不是分配给你的那组 NS，问题在注册商侧</td>
							</tr>
							<tr>
								<th scope="row">3</th>
								<td>
									<code>dig DS example.com</code>
								</td>
								<td>有残留 DS 记录，去注册商删掉，等它在缓存里过期</td>
							</tr>
							<tr>
								<th scope="row">4</th>
								<td>直接向 Cloudflare 的 NS 提问</td>
								<td>返回新值，剩下的是等；返回旧值，回面板核对记录</td>
							</tr>
							<tr>
								<th scope="row">5</th>
								<td>这条记录是不是开着小黄云</td>
								<td>开着的话，源站 IP 的改动本来就看不出来，去回源侧验证</td>
							</tr>
							<tr>
								<th scope="row">6</th>
								<td>地址对了但内容不对</td>
								<td>那是边缘缓存的事，去清缓存，不是 DNS 的问题</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>这套判断里最花时间的是「等」。把 TTL 的提前量安排好，大部分所谓的不生效根本不会发生。</p>

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
