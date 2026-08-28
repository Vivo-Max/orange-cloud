import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { setRequestLocale } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import GuideShell, { type RelatedLink } from "@/components/guides/GuideShell";
import { guideBySlug, GUIDE_LOCALE } from "@/lib/guides/guides";

const SITE_URL = "https://oss.omail.us.kg";
const guide = guideBySlug("what-is-the-orange-cloud-in-cloudflare");
const PATH = `/guides/${guide.slug}`;

/** FAQ 一处定义：可见文本与 FAQPage JSON-LD 同源，保证逐字一致 */
const FAQ: Array<{ q: string; a: string }> = [
	{
		q: "What does the orange cloud mean in Cloudflare?",
		a: "It means that DNS record is proxied. Cloudflare answers queries for the hostname with its own anycast IP addresses, so HTTP and HTTPS requests reach Cloudflare first and your Cloudflare settings — caching, WAF, rules — apply before the request is passed to your origin server.",
	},
	{
		q: "What’s the difference between the orange cloud and the grey cloud?",
		a: "It is the proxied vs DNS only choice. The orange cloud proxies traffic through Cloudflare; the grey cloud — spelled gray in Cloudflare’s own documentation, and labelled DNS only in the dashboard — does not. DNS only means the record returns your origin IP address, so visitors connect straight to your server, and no caching, WAF, or HTTP analytics apply to those requests.",
	},
	{
		q: "Should the orange cloud be on or off?",
		a: "On for any A, AAAA, or CNAME record that serves your website or API over HTTP/HTTPS. Off for mail hostnames, domain-verification records, SSH and other non-HTTP services, and endpoints a third party has to reach at your real IP address.",
	},
	{
		q: "Why can’t I turn on the orange cloud for my MX record?",
		a: "Only A, AAAA, and CNAME records can be proxied. MX, TXT, NS, SRV, and every other type stay DNS only, because Cloudflare’s proxy handles HTTP and HTTPS traffic and has no anycast address to substitute for those record types.",
	},
	{
		q: "Does the orange cloud hide my origin IP?",
		a: "For that hostname, yes — queries return Cloudflare anycast addresses instead of your server’s address. It does not help if another record in the same zone, such as a mail, FTP, or legacy subdomain record, still publishes the same origin IP.",
	},
];

const RELATED: RelatedLink[] = [
	{
		href: "/guides/cloudflare-orange-to-orange",
		label: "Cloudflare Orange-to-Orange (O2O), explained",
		note: "What happens when your proxied hostname points at another Cloudflare zone — the Cloudflare for SaaS case.",
	},
	{
		href: "/guides/cloudflare-ssl-tls-encryption-modes",
		label: "Which Cloudflare SSL/TLS encryption mode should you use?",
		note: "Once a record is proxied, this setting decides whether the Cloudflare-to-origin hop is encrypted at all.",
	},
	{
		href: "/guides/why-is-cloudflare-not-caching-my-site",
		label: "Why is Cloudflare not caching my site?",
		note: "Proxying is what makes caching possible — here is why most pages still are not cached.",
	},
	{
		href: "/guides/why-is-my-cloudflare-dns-change-not-working",
		label: "Why isn\u2019t my Cloudflare DNS change working yet?",
		note: "Flipping the cloud is a record change like any other \u2014 here is which cache decides how long it takes.",
	},
	{
		href: "/guides/cloudflare-error-1000-dns-points-to-prohibited-ip",
		label: "Cloudflare error 1000: DNS points to prohibited IP",
		note: "What happens when a proxied record hands the proxy an address that is Cloudflare’s own.",
	},
	{
		href: "https://developers.cloudflare.com/dns/proxy-status/",
		label: "Cloudflare docs: Proxy status",
		note: "The official reference for proxied and DNS-only records, including limitations and use cases.",
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

const API_EXAMPLE = `curl "https://api.cloudflare.com/client/v4/zones/$ZONE_ID/dns_records/$RECORD_ID" \\
  --request PATCH \\
  --header "Authorization: Bearer $CLOUDFLARE_API_TOKEN" \\
  --json '{"proxied": true}'`;

export default async function OrangeCloudGuide({ params }: { params: Promise<{ locale: string }> }) {
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
				lede="One toggle in the Cloudflare dashboard decides whether your traffic goes through Cloudflare at all. Here is exactly what it changes."
				updated={guide.updated}
				readingTime={guide.readingTime}
				related={RELATED}
			>
				<div className="glass r-island note p-6 sm:p-7">
					<p>
						<strong>The orange cloud means a DNS record is proxied:</strong> Cloudflare answers with its
						own anycast IP addresses, and HTTP traffic passes through Cloudflare. The grey cloud means
						DNS only — queries return your origin IP, and visitors connect straight to your server.
					</p>
				</div>

				<p>
					In the Cloudflare dashboard, every A, AAAA, and CNAME record has a small cloud icon next to it.
					Orange means <em>proxied</em>. Gray — spelled grey in much of the world, and often searched that
					way — means <em>DNS only</em>. So the meaning of the orange cloud comes down to one question about
					that record: <strong>proxied vs DNS only</strong>. It looks like a cosmetic toggle and it
					is not: it decides whether Cloudflare is a CDN and firewall in front of your site, or merely the
					place where your DNS records happen to live.
				</p>

				<h2 id="orange-vs-gray">Orange cloud vs grey cloud: proxied vs DNS only</h2>
				<p>
					The difference between the orange cloud and the grey cloud starts one step earlier than most
					people expect — at the DNS answer itself, before any HTTP request exists.
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">&nbsp;</th>
								<th scope="col">Orange cloud (proxied)</th>
								<th scope="col">Gray cloud (DNS only)</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">DNS answer</th>
								<td>Cloudflare anycast IP addresses</td>
								<td>Your origin IP address</td>
							</tr>
							<tr>
								<th scope="row">Traffic path</th>
								<td>Visitor → Cloudflare → origin</td>
								<td>Visitor → origin</td>
							</tr>
							<tr>
								<th scope="row">Origin IP</th>
								<td>Not published in DNS for that name</td>
								<td>Published to anyone who queries</td>
							</tr>
							<tr>
								<th scope="row">CDN caching</th>
								<td>Yes</td>
								<td>No</td>
							</tr>
							<tr>
								<th scope="row">WAF, DDoS mitigation, bot rules</th>
								<td>Applied at the edge</td>
								<td>Not applied to HTTP traffic</td>
							</tr>
							<tr>
								<th scope="row">Cloudflare TLS certificate</th>
								<td>Cloudflare terminates TLS for visitors</td>
								<td>Your origin serves its own certificate</td>
							</tr>
							<tr>
								<th scope="row">Rules, Redirects, Workers routes</th>
								<td>Run on the request</td>
								<td>Never see the request</td>
							</tr>
							<tr>
								<th scope="row">Analytics</th>
								<td>Full HTTP/HTTPS analytics</td>
								<td>DNS query analytics only</td>
							</tr>
							<tr>
								<th scope="row">TTL</th>
								<td>Fixed at Auto (300 s)</td>
								<td>Whatever you set</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					One consequence is worth spelling out: a gray-clouded record publishes your server’s real address,
					which is what makes “I moved to Cloudflare and still got hit directly” possible. Cloudflare can
					only protect requests it actually receives.
				</p>

				<h2 id="which-records">Which records can be orange-clouded</h2>
				<p>
					Only <strong>A</strong>, <strong>AAAA</strong>, and <strong>CNAME</strong> records — the record
					types that resolve a name to an address. MX, TXT, NS, SRV, CAA and the rest are always DNS only,
					and the dashboard will not offer you a toggle for them. The reason is structural rather than
					arbitrary: proxying works by handing out Cloudflare anycast addresses instead of yours, and there
					is nothing to substitute in a TXT or MX record.
				</p>
				<p>Three behaviours that surprise people:</p>
				<ul>
					<li>
						<strong>Mixed records on the same name are treated as proxied.</strong> If one A record for{" "}
						<code>www</code> is orange and another is gray, Cloudflare proxies both.
					</li>
					<li>
						<strong>Proxying is inherited along a CNAME chain.</strong> If a name anywhere in the chain is
						proxied, the request is proxied.
					</li>
					<li>
						<strong>Some CNAME targets are blocked from proxying on purpose.</strong> DKIM and validation
						targets such as <code>dkim.amazonses.com</code>, <code>acm-validations.aws</code>, or
						subdomains of <code>onmicrosoft.com</code> cannot be orange-clouded, because a proxied answer
						would break the very check they exist for.
					</li>
				</ul>
				<p>
					Pointing a proxied record at a hostname in a <em>different</em> Cloudflare account is also
					refused — that is{" "}
					<a
						href="https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-1xxx-errors/error-1014/"
						target="_blank"
						rel="noopener noreferrer"
					>
						Error 1014, CNAME Cross-User Banned
					</a>
					, unless the owner of the target has onboarded your hostname through Cloudflare for SaaS. That
					sanctioned version is{" "}
					<Link href="/guides/cloudflare-orange-to-orange">Orange-to-Orange routing</Link>.
				</p>

				<h2 id="ports">The proxy only covers certain HTTP and HTTPS ports</h2>
				<p>
					This is the single biggest trap for newcomers. The orange cloud is an HTTP/HTTPS reverse proxy,
					not a general network tunnel. By default it handles these ports:
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">Protocol</th>
								<th scope="col">Ports proxied by default</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">HTTP</th>
								<td>
									<code>80</code>, <code>8080</code>, <code>8880</code>, <code>2052</code>,{" "}
									<code>2082</code>, <code>2086</code>, <code>2095</code>
								</td>
							</tr>
							<tr>
								<th scope="row">HTTPS</th>
								<td>
									<code>443</code>, <code>2053</code>, <code>2083</code>, <code>2087</code>,{" "}
									<code>2096</code>, <code>8443</code>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					Everything outside that list — SSH on 22, RDP on 3389, SMTP on 25, a database port, a game
					server, a dev server on 3000 — is not proxied. The hostname now resolves to Cloudflare, and
					Cloudflare has no reason to forward a connection on those ports to your machine, so the
					connection simply fails. Gray-cloud that hostname, or put it behind{" "}
					<a
						href="https://developers.cloudflare.com/spectrum/"
						target="_blank"
						rel="noopener noreferrer"
					>
						Cloudflare Spectrum
					</a>
					, which proxies non-HTTP ports — all TCP and UDP ports on the Enterprise plan.
				</p>
				<p>
					Note also that the alternate ports (<code>2052</code>, <code>2053</code>, <code>2082</code>,{" "}
					<code>2083</code>, <code>2086</code>, <code>2087</code>, <code>2095</code>, <code>2096</code>,{" "}
					<code>8880</code>, <code>8443</code>) are proxied but <em>not cached</em> by default. If you serve
					a site on 8443 and wonder why the cache hit rate is zero, that is why.
				</p>

				<h2 id="when-gray">When the gray cloud is the right answer</h2>
				<p>Reach for DNS only whenever the service on the other end is not plain HTTP/HTTPS to your own origin:</p>
				<ul>
					<li>
						<strong>Mail.</strong> MX records cannot be proxied at all, and a hostname used for mail
						delivery (<code>mail.example.com</code>) should stay gray. Cloudflare does not proxy SMTP on
						port 25, so proxying the mail host points senders at Cloudflare and delivery stops.
					</li>
					<li>
						<strong>Domain verification.</strong> Verification CNAMEs and TXT records must return the
						exact value the third party expects; a proxied answer returns Cloudflare addresses and
						verification fails.
					</li>
					<li>
						<strong>Non-HTTP services.</strong> SSH, RDP, FTP, game servers, anything on a port outside
						the list above.
					</li>
					<li>
						<strong>Sites hosted on a SaaS platform</strong> that terminates its own TLS — Wix,
						Squarespace, Webflow and similar — unless that platform is explicitly integrated with
						Cloudflare. Two proxies both terminating TLS and both redirecting to HTTPS is how you get
						certificate errors and redirect loops.
					</li>
					<li>
						<strong>Endpoints validated by IP.</strong> If a partner allowlists your server’s address for
						webhooks or API calls, proxying replaces it with Cloudflare’s.
					</li>
				</ul>
				<p>
					A rule of thumb: <em>if the hostname serves web traffic on a standard port and you control the
					origin, orange-cloud it. Everything else stays gray.</em>
				</p>

				<h2 id="troubleshooting">When the toggle causes an error</h2>
				<p>Most orange-cloud incidents look like one of these.</p>
				<h3>SSH or a custom port stopped working right after you switched</h3>
				<p>
					Expected. That port is not proxied. Move the service to its own hostname and gray-cloud that
					hostname, keeping the web hostname orange.
				</p>
				<h3>Mail stopped being delivered</h3>
				<p>
					Check whether the hostname your MX record points to is proxied. Give mail its own DNS-only
					hostname rather than sharing the web hostname. (Cloudflare does try to save you here: if an MX
					record points at a proxied name, it dynamically prepends <code>_dc-mx</code> to the answer so mail
					bypasses the proxy — a safety net, not a design to rely on.)
				</p>
				<h3>522, 521, or 524 errors appeared</h3>
				<p>
					These only exist because traffic is now proxied: Cloudflare is reaching your origin and not
					getting a usable answer. A 521 means the origin refused the connection, 522 means it timed out —
					both usually a firewall that does not allow{" "}
					<a href="https://www.cloudflare.com/ips/" target="_blank" rel="noopener noreferrer">
						Cloudflare’s IP ranges
					</a>
					, or an origin that is down. A 524 means the origin accepted the connection but took longer than
					the proxy read timeout to respond.
				</p>
				<h3>Redirect loop or certificate error appeared</h3>
				<p>
					Usually the{" "}
					<a
						href="https://developers.cloudflare.com/ssl/origin-configuration/ssl-modes/"
						target="_blank"
						rel="noopener noreferrer"
					>
						SSL/TLS encryption mode
					</a>
					: with <strong>Flexible</strong>, Cloudflare talks to your origin over plain HTTP, and an origin
					that redirects HTTP to HTTPS will{" "}
					<a
						href="https://developers.cloudflare.com/ssl/troubleshooting/too-many-redirects/"
						target="_blank"
						rel="noopener noreferrer"
					>
						loop forever
					</a>
					. Use <strong>Full (strict)</strong> with a valid certificate on the origin — the differences
					between the modes, and which error each one produces, are in{" "}
					<Link href="/guides/cloudflare-ssl-tls-encryption-modes">
						the guide to Cloudflare’s encryption modes
					</Link>
					.
				</p>
				<h3>Your origin IP leaked anyway</h3>
				<p>
					Look for the other records in the same zone — <code>mail</code>, <code>ftp</code>, <code>cpanel</code>,
					a forgotten staging subdomain — that still point at the same server. Also remember that while a
					newly added domain is still pending activation (up to 24 hours), records behave as DNS only even
					when marked proxied; rotating the origin IP after activation closes that window.
				</p>
				<h3>Your app now sees Cloudflare addresses as the visitor IP</h3>
				<p>
					Expected: the origin’s peer is Cloudflare. Read the real client address from the{" "}
					<code>CF-Connecting-IP</code> header (or <code>X-Forwarded-For</code>) instead of the socket.
				</p>

				<h2 id="how-to-switch">How to switch it</h2>
				<p>
					<strong>In the dashboard:</strong> open your domain, go to <strong>DNS → Records</strong>, select{" "}
					<strong>Edit</strong> on the record, and click the cloud under <strong>Proxy status</strong>. It
					takes effect at Cloudflare immediately, though resolvers may hold the previous answer for up to
					five minutes.
				</p>
				<p>
					<strong>Over the API:</strong> proxy status is the <code>proxied</code> boolean on the DNS record.
					A partial update is enough:
				</p>
				<pre>
					<code>{API_EXAMPLE}</code>
				</pre>
				<p>
					<strong>From your phone:</strong> that toggle is the reason this site exists —{" "}
					<Link href="/">Orange Cloud</Link> is a native iOS and Android client for Cloudflare, and flipping
					proxy status is one tap in the DNS list.
				</p>

				<h2 id="both-orange">What if both ends are orange?</h2>
				<p>
					If your proxied record points at a hostname that is itself proxied by Cloudflare — typically a
					SaaS platform using Cloudflare for SaaS, such as a Shopify store on your own domain — the request
					crosses two Cloudflare zones, and both sets of settings apply in a defined order. Cloudflare calls
					this Orange-to-Orange, or O2O, and it has its own rules about which zone wins:{" "}
					<Link href="/guides/cloudflare-orange-to-orange">
						Cloudflare Orange-to-Orange (O2O), explained
					</Link>
					.
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
