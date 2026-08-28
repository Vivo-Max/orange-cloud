import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { setRequestLocale } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import GuideShell, { type RelatedLink } from "@/components/guides/GuideShell";
import VisitorIpTrust from "@/components/guides/VisitorIpTrust";
import { guideBySlug, GUIDE_LOCALE } from "@/lib/guides/guides";

const SITE_URL = "https://o-c.do";
const guide = guideBySlug("cloudflare-real-visitor-ip-cf-connecting-ip");
const PATH = `/guides/${guide.slug}`;

const DOCS_HEADERS = "https://developers.cloudflare.com/fundamentals/reference/http-headers/";
const DOCS_CF_CONNECTING_IP = `${DOCS_HEADERS}#cf-connecting-ip`;
const DOCS_TRUE_CLIENT_IP = `${DOCS_HEADERS}#true-client-ip-enterprise-plan-only`;
const DOCS_XFF = `${DOCS_HEADERS}#x-forwarded-for`;
const DOCS_RESTORE =
	"https://developers.cloudflare.com/support/troubleshooting/restoring-visitor-ips/restoring-original-visitor-ips/";
const DOCS_IP_ADDRESSES = "https://developers.cloudflare.com/fundamentals/concepts/cloudflare-ip-addresses/";
const DOCS_MANAGED_TRANSFORMS = "https://developers.cloudflare.com/rules/transform/managed-transforms/reference/";
const DOCS_PSEUDO_IPV4 = "https://developers.cloudflare.com/network/pseudo-ipv4/";
const DOCS_PROXY_STATUS = "https://developers.cloudflare.com/dns/proxy-status/";
const CF_IPS = "https://www.cloudflare.com/ips/";
const NGINX_REALIP = "https://nginx.org/en/docs/http/ngx_http_realip_module.html";
const APACHE_REMOTEIP = "https://httpd.apache.org/docs/2.4/mod/mod_remoteip.html";

/** FAQ 一处定义：可见文本与 FAQPage JSON-LD 同源，保证逐字一致 */
const FAQ: Array<{ q: string; a: string }> = [
	{
		q: "How do I get the real visitor IP behind Cloudflare?",
		a: "Read the CF-Connecting-IP request header instead of the address of the connection itself. Every proxied request Cloudflare forwards to your origin carries it, and it always holds exactly one address.",
	},
	{
		q: "What is the CF-Connecting-IP header?",
		a: "It is the header Cloudflare adds to a proxied request to give the origin the client IP address that connected to Cloudflare. Cloudflare only sends it on traffic from its own edge to your origin, so it never appears on a request that reached your server some other way.",
	},
	{
		q: "Should I use CF-Connecting-IP or X-Forwarded-For?",
		a: "Cloudflare recommends CF-Connecting-IP or True-Client-IP over X-Forwarded-For, because both contain a single address in a consistent format. X-Forwarded-For is a list that grows by one entry for every proxy in front of Cloudflare, so parsing it correctly is harder than it looks.",
	},
	{
		q: "Can the CF-Connecting-IP header be spoofed?",
		a: "Not through Cloudflare, but a request that reaches your origin directly can carry any header its sender likes. That is why real-IP modules take a trusted proxy list, and why Cloudflare recommends blocking traffic at the origin that does not come from its published IP ranges.",
	},
	{
		q: "Why is CF-Connecting-IP missing from my requests?",
		a: "The three documented reasons are that the record is not proxied, that the Remove visitor IP headers Managed Transform is enabled on the zone, or that Pseudo IPv4 is set to Overwrite Headers, which replaces the value with a Class E address and moves the real one into CF-Connecting-IPv6.",
	},
];

const RELATED: RelatedLink[] = [
	{
		href: "/guides/what-is-the-orange-cloud-in-cloudflare",
		label: "What does the orange cloud mean in Cloudflare?",
		note: "The proxy status on the record is what puts Cloudflare between the visitor and your server in the first place — and what makes this header appear.",
	},
	{
		href: "/guides/cloudflare-error-1000-dns-points-to-prohibited-ip",
		label: "Cloudflare error 1000: DNS points to prohibited IP",
		note: "The other place origin addresses and request headers collide, this time by sending Cloudflare back to itself.",
	},
	{
		href: "/guides/cloudflare-error-522-connection-timed-out",
		label: "Cloudflare error 522: connection timed out",
		note: "The same list of Cloudflare IP ranges, used for the opposite purpose: letting the proxy reach your origin at all.",
	},
	{
		href: DOCS_RESTORE,
		label: "Cloudflare docs: Restoring original visitor IPs",
		note: "The per-server instructions this guide summarises, including Lighttpd, LiteSpeed, IIS and Tomcat.",
		external: true,
	},
	{
		href: "/contact",
		label: "Something wrong on this page?",
		note: "Corrections and questions are welcome — we read every message.",
	},
];

export const metadata: Metadata = {
	metadataBase: new URL(SITE_URL),
	title: guide.title,
	description: guide.description,
	alternates: {
		canonical: PATH,
		languages: { en: PATH, "x-default": PATH },
	},
	openGraph: {
		title: guide.title,
		description: guide.description,
		url: PATH,
		siteName: "Orange Cloud",
		type: "article",
		locale: "en_US",
		images: [{ url: "/og/en.jpg", width: 1280, height: 640, alt: guide.h1 }],
	},
	twitter: {
		card: "summary_large_image",
		title: guide.title,
		description: guide.description,
		images: ["/og/en.jpg"],
	},
};

export default async function VisitorIpGuide({ params }: { params: Promise<{ locale: string }> }) {
	const { locale } = await params;
	if (locale !== GUIDE_LOCALE) notFound();
	setRequestLocale(locale);

	const jsonLd = [
		{
			"@context": "https://schema.org",
			"@type": "TechArticle",
			headline: guide.h1,
			description: guide.description,
			url: `${SITE_URL}${PATH}`,
			inLanguage: "en",
			datePublished: guide.updated,
			dateModified: guide.updated,
			image: `${SITE_URL}/og/en.jpg`,
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
				{ "@type": "ListItem", position: 1, name: "Guides", item: `${SITE_URL}/guides` },
				{ "@type": "ListItem", position: 2, name: guide.h1, item: `${SITE_URL}${PATH}` },
			],
		},
	];

	return (
		<>
			<script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />
			<GuideShell
				title={guide.h1}
				lede="Every entry in the access log is a Cloudflare address, the rate limiter is throttling continents at a time, and the ban list has stopped meaning anything. Here is where the visitor went, and how to get them back."
				updated={guide.updated}
				readingTime={guide.readingTime}
				related={RELATED}
			>
				<div className="glass r-island note p-6 sm:p-7">
					<p>
						<strong>
							The visitor&rsquo;s address is in the <code>CF-Connecting-IP</code> request header.
						</strong>{" "}
						Configure your web server to read that instead of the connection&rsquo;s source address — and to
						believe it only on connections from Cloudflare&rsquo;s published IP ranges.
					</p>
				</div>

				<p>
					Nothing is broken. Once a DNS record is{" "}
					<a href={DOCS_PROXY_STATUS} target="_blank" rel="noopener noreferrer">
						proxied
					</a>
					, Cloudflare is a reverse proxy in front of your server, so Cloudflare is the client as far as your
					origin is concerned. The TCP connection genuinely originates at a Cloudflare data centre, and{" "}
					<code>REMOTE_ADDR</code> — along with everything that derives from it — genuinely describes that
					connection. The visitor has not disappeared; they have moved from the connection into a header.
				</p>

				<VisitorIpTrust />

				<h2 id="headers">The headers that carry the address</h2>
				<p>
					Cloudflare adds several, and they are not interchangeable. The differences matter mostly when you get
					to parsing:
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">Header</th>
								<th scope="col">Contains</th>
								<th scope="col">Availability</th>
								<th scope="col">Watch out for</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">
									<a href={DOCS_CF_CONNECTING_IP} target="_blank" rel="noopener noreferrer">
										<code>CF-Connecting-IP</code>
									</a>
								</th>
								<td>Exactly one address: the client that connected to Cloudflare</td>
								<td>All plans, added automatically</td>
								<td>Only ever sent from Cloudflare&rsquo;s edge to your origin</td>
							</tr>
							<tr>
								<th scope="row">
									<code>CF-Connecting-IPv6</code>
								</th>
								<td>The original IPv6 address</td>
								<td>With Pseudo IPv4 in <em>Overwrite Headers</em> mode</td>
								<td>Only relevant if you use Pseudo IPv4 at all</td>
							</tr>
							<tr>
								<th scope="row">
									<a href={DOCS_TRUE_CLIENT_IP} target="_blank" rel="noopener noreferrer">
										<code>True-Client-IP</code>
									</a>
								</th>
								<td>The same single address, under a different name</td>
								<td>Enterprise, and only once the Managed Transform is enabled</td>
								<td>Not added by default; a stacked CDN can forge it</td>
							</tr>
							<tr>
								<th scope="row">
									<a href={DOCS_XFF} target="_blank" rel="noopener noreferrer">
										<code>X-Forwarded-For</code>
									</a>
								</th>
								<td>A comma-separated chain, visitor first</td>
								<td>All plans</td>
								<td>Grows by one entry per proxy ahead of Cloudflare</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					Cloudflare documents <code>True-Client-IP</code> as differing from <code>CF-Connecting-IP</code> in
					name alone. It exists for Enterprise customers whose load balancers or firewalls already read that
					header name and cannot be reconfigured.
				</p>

				<h2 id="xff">Why X-Forwarded-For is the tempting wrong answer</h2>
				<p>
					If nothing sits in front of Cloudflare, <code>X-Forwarded-For</code> has the same value as{" "}
					<code>CF-Connecting-IP</code>, which is exactly why so many setups appear to work and then quietly
					stop. The moment a request passes through another proxy first, Cloudflare appends rather than
					replaces. Cloudflare&rsquo;s own worked example: a visitor at <code>203.0.113.1</code> going through
					proxy A (<code>198.51.100.101</code>) and proxy B (<code>198.51.100.102</code>) arrives at your
					origin as
				</p>
				<pre>
					<code>x-forwarded-for: 203.0.113.1,198.51.100.101,198.51.100.102</code>
				</pre>
				<p>
					Read the last entry and you ban proxy B. Read the first and you trust whatever the earliest hop
					claimed. Cloudflare&rsquo;s recommendation is unambiguous — use <code>CF-Connecting-IP</code> or{" "}
					<code>True-Client-IP</code> for logging and applications, because both hold a single address in a
					consistent format. Treat XFF as a diagnostic, not as an identity.
				</p>

				<h2 id="configure">Configuring your web server</h2>
				<p>
					The pattern for restoring the real IP is the same everywhere: name the header, then name the
					addresses you will accept it from. Both halves are required.
				</p>

				<h3 id="nginx">Nginx</h3>
				<p>
					Use the built-in{" "}
					<a href={NGINX_REALIP} target="_blank" rel="noopener noreferrer">
						<code>ngx_http_realip_module</code>
					</a>
					. One <code>set_real_ip_from</code> line per Cloudflare range, then the header:
				</p>
				<pre>
					<code>{`set_real_ip_from 198.51.100.0/24;   # repeat for every published range
real_ip_header CF-Connecting-IP;`}</code>
				</pre>
				<p>
					To keep the address in the access log, Cloudflare notes that you add{" "}
					<code>$http_cf_connecting_ip</code> — and, if you want it, <code>$http_x_forwarded_for</code> — to
					your <code>log_format</code> directive. It is the same header throughout, spelled three ways
					depending on where you are looking: <code>CF-Connecting-IP</code> in Cloudflare&rsquo;s reference,{" "}
					<code>cf-connecting-ip</code> on the wire, and <code>$http_cf_connecting_ip</code> once Nginx has
					turned it into a variable.
				</p>

				<h3 id="apache">Apache</h3>
				<p>
					Cloudflare stopped updating and supporting <code>mod_cloudflare</code> as of Debian 9 and Ubuntu
					18.04 LTS, and now points to Apache&rsquo;s own{" "}
					<a href={APACHE_REMOTEIP} target="_blank" rel="noopener noreferrer">
						<code>mod_remoteip</code>
					</a>
					. Enable it with <code>a2enmod remoteip</code>, then:
				</p>
				<pre>
					<code>{`RemoteIPHeader CF-Connecting-IP
RemoteIPTrustedProxy 198.51.100.0/24   # repeat for every published range`}</code>
				</pre>
				<p>
					The step people miss is the log format itself. <code>%h</code> logs the connection&rsquo;s address
					and will keep printing Cloudflare&rsquo;s; swap it for <code>%a</code> in your{" "}
					<code>combined</code> <code>LogFormat</code> line so the restored address is what lands in the file.
				</p>
				<p>
					Cloudflare&rsquo;s{" "}
					<a href={DOCS_RESTORE} target="_blank" rel="noopener noreferrer">
						restoring original visitor IPs
					</a>{" "}
					page carries the equivalent recipes for Lighttpd, LiteSpeed, IIS and Tomcat, plus a one-line PHP
					fallback that reassigns <code>REMOTE_ADDR</code> for scripts without touching the server logs.
				</p>

				<h2 id="trust">The trust boundary, which is the whole point</h2>
				<p>
					A header is a string that someone put in a request. Through Cloudflare it is reliable; sent straight
					to your server it is whatever the sender typed. If your origin address is discoverable — and{" "}
					<a href={DOCS_IP_ADDRESSES} target="_blank" rel="noopener noreferrer">
						Cloudflare notes it often is
					</a>
					, through historical DNS records or mail configuration — then an application that reads{" "}
					<code>CF-Connecting-IP</code> unconditionally has handed its rate limiter, its ban list and its
					geolocation to anyone willing to set one header.
				</p>
				<p>Two measures, and the second is the one that actually holds:</p>
				<ol>
					<li>
						<strong>Give the module a trusted proxy list.</strong> That is what{" "}
						<code>set_real_ip_from</code> and <code>RemoteIPTrustedProxy</code> are for. Cloudflare publishes
						the ranges at{" "}
						<a href={CF_IPS} target="_blank" rel="noopener noreferrer">
							cloudflare.com/ips
						</a>{" "}
						and says plainly that the list needs updating regularly — a config written three years ago is
						probably incomplete now, so pull it from the published source on a schedule rather than pasting
						it once.
					</li>
					<li>
						<strong>Refuse the connections in the first place.</strong> Cloudflare recommends blocking all
						traffic to ports 80 and 443 that does not come from its ranges or from partners you trust. Then a
						forged header never reaches the parser, and the same allowlist doubles as the fix for the
						firewall-shaped causes of{" "}
						<Link href="/guides/cloudflare-error-522-connection-timed-out">error 522</Link>.
					</li>
				</ol>

				<h2 id="missing">When the header is not there at all</h2>
				<p>Four documented reasons, in the order worth checking:</p>
				<ul>
					<li>
						<strong>The record is grey-clouded.</strong> Cloudflare only sends the header on traffic from its
						edge to your origin. On a DNS-only record there is no edge on the path, and the connection
						address is already the visitor. See{" "}
						<Link href="/guides/what-is-the-orange-cloud-in-cloudflare">
							what the orange cloud means in Cloudflare
						</Link>{" "}
						if you are not sure which records are proxied.
					</li>
					<li>
						<strong>A Managed Transform is stripping it.</strong>{" "}
						<a href={DOCS_MANAGED_TRANSFORMS} target="_blank" rel="noopener noreferrer">
							Remove visitor IP headers
						</a>{" "}
						removes <code>cf-connecting-ip</code>, <code>true-client-ip</code> and the visitor entry from{" "}
						<code>x-forwarded-for</code> together. It is a privacy feature, and it is mutually exclusive with
						the Enterprise transform that adds <code>True-Client-IP</code>.
					</li>
					<li>
						<strong>
							<a href={DOCS_PSEUDO_IPV4} target="_blank" rel="noopener noreferrer">
								Pseudo IPv4
							</a>{" "}
							is set to Overwrite Headers.
						</strong>{" "}
						The header is present but holds a Class E address such as <code>240.16.0.1</code>, hashed from the
						visitor&rsquo;s IPv6 address. The real one is in <code>CF-Connecting-IPv6</code>.
					</li>
					<li>
						<strong>A Worker is in the path.</strong> In same-zone subrequests the value reflects{" "}
						<code>x-real-ip</code>, which a Worker script can alter; in cross-zone subrequests Cloudflare
						deliberately sets it to <code>2a06:98c0:3600::103</code> instead of the client address. If your
						traffic passes through a Worker, verify the value rather than assuming it.
					</li>
				</ul>
				<p>
					One last trap worth knowing about, because it produces a header that vanishes for no visible reason:
					Cloudflare may drop request headers whose <em>names</em> it considers invalid by Nginx&rsquo;s rules
					— a custom header with a dot in the name, for instance. If you are forwarding a visitor address under
					a name of your own devising, that is a good place to look before you suspect the proxy.
				</p>

				<h2 id="faq">FAQ</h2>
				{FAQ.map((item) => (
					<div key={item.q}>
						<h3>{item.q}</h3>
						<p>{item.a}</p>
					</div>
				))}
			</GuideShell>
		</>
	);
}
