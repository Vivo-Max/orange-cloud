import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { setRequestLocale } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import GuideShell, { type RelatedLink } from "@/components/guides/GuideShell";
import O2OFlow from "@/components/guides/O2OFlow";
import { guideBySlug, GUIDE_LOCALE } from "@/lib/guides/guides";

const SITE_URL = "https://oss.omail.us.kg";
const guide = guideBySlug("cloudflare-orange-to-orange");
const PATH = `/guides/${guide.slug}`;

const DOCS_O2O = "https://developers.cloudflare.com/cloudflare-for-platforms/cloudflare-for-saas/saas-customers/how-it-works/";
const DOCS_COMPAT = "https://developers.cloudflare.com/cloudflare-for-platforms/cloudflare-for-saas/saas-customers/product-compatibility/";

/** FAQ 一处定义：可见文本与 FAQPage JSON-LD 同源，保证逐字一致 */
const FAQ: Array<{ q: string; a: string }> = [
	{
		q: "What does orange to orange mean in Cloudflare?",
		a: "It describes a request that passes through two Cloudflare zones instead of one: your own proxied hostname routes into a SaaS provider’s zone that also runs on Cloudflare. Both zones are “orange”, hence orange-to-orange.",
	},
	{
		q: "Does O2O work with an A record?",
		a: "No. O2O is triggered by a proxied CNAME record that matches a custom hostname on the provider’s zone. Apex proxying, which uses A records pointing at the provider’s addresses, does not enable O2O.",
	},
	{
		q: "Which zone’s settings win in an O2O setup?",
		a: "Your zone runs first and its settings generally override the provider’s. The provider’s zone then applies its own configuration to whatever your zone forwarded, so a request has to survive both.",
	},
];

const RELATED: RelatedLink[] = [
	{
		href: "/guides/what-is-the-orange-cloud-in-cloudflare",
		label: "What does the orange cloud mean in Cloudflare?",
		note: "The prerequisite: proxied vs DNS only, which records qualify, and which ports the proxy covers.",
	},
	{
		href: "/guides/cloudflare-ssl-tls-encryption-modes",
		label: "Which Cloudflare SSL/TLS encryption mode should you use?",
		note: "Flexible, Full, Full (strict): what each does to the origin connection, and which error each one produces.",
	},
	{
		href: "/guides/cloudflare-error-1000-dns-points-to-prohibited-ip",
		label: "Cloudflare error 1000: DNS points to prohibited IP",
		note: "The failure mode when a SaaS platform on Cloudflare has not onboarded your hostname yet.",
	},
	{
		href: DOCS_O2O,
		label: "Cloudflare docs: How O2O works",
		note: "The official reference for the routing behaviour, prerequisites, and the cf-connecting-o2o header.",
		external: true,
	},
	{
		href: DOCS_COMPAT,
		label: "Cloudflare docs: O2O product compatibility",
		note: "The authoritative per-product table this guide summarises. Check it before changing anything.",
		external: true,
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

export default async function OrangeToOrangeGuide({ params }: { params: Promise<{ locale: string }> }) {
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
				lede="Two Cloudflare zones, one request. Here is what triggers it, how the request actually travels, and which zone’s configuration wins."
				updated={guide.updated}
				readingTime={guide.readingTime}
				related={RELATED}
			>
				<div className="glass r-island note p-6 sm:p-7">
					<p>
						<strong>Orange-to-Orange (O2O) is a request that crosses two Cloudflare zones.</strong> Your
						proxied hostname points at a SaaS platform that also runs on Cloudflare, so your settings are
						applied first and the provider’s second, before the request reaches their origin.
					</p>
				</div>

				<p>
					If you run a Shopify store, a Webflow site, or any platform built on Cloudflare for SaaS, and your
					own domain is on Cloudflare too, you are probably in an orange to orange setup — whether or not
					anyone told you. It explains a class of confusing behaviour: a cache rule that does nothing, a
					redirect that loops, a WAF rule that fires in a dashboard you cannot see.
				</p>

				<h2 id="prerequisite">First, the orange cloud</h2>
				<p>
					In Cloudflare, a DNS record marked with the orange cloud is <em>proxied</em>: queries return
					Cloudflare anycast addresses and HTTP traffic passes through Cloudflare before it reaches an
					origin. A gray cloud means DNS only. O2O is simply what happens when both ends of that path are
					orange. If any of that is unfamiliar, start with{" "}
					<Link href="/guides/what-is-the-orange-cloud-in-cloudflare">
						what the orange cloud means in Cloudflare
					</Link>
					.
				</p>

				<h2 id="when">When O2O happens</h2>
				<p>All of the following have to be true at once:</p>
				<ul>
					<li>
						Your SaaS provider uses{" "}
						<a
							href="https://developers.cloudflare.com/cloudflare-for-platforms/cloudflare-for-saas/"
							target="_blank"
							rel="noopener noreferrer"
						>
							Cloudflare for SaaS
						</a>{" "}
						and has created your hostname as a <strong>custom hostname</strong> in their zone.
					</li>
					<li>Your domain’s authoritative DNS is Cloudflare — you have your own zone.</li>
					<li>
						That zone holds a <strong>CNAME record matching the custom hostname</strong>, pointing at the
						target the provider gave you, and it is <strong>proxied</strong>.
					</li>
					<li>The two zones sit in <strong>different Cloudflare accounts</strong>.</li>
				</ul>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">Type</th>
								<th scope="col">Name</th>
								<th scope="col">Target</th>
								<th scope="col">Proxy status</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<td>CNAME</td>
								<td>shop</td>
								<td>customers.saasprovider.example</td>
								<td>Proxied</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					Two things that do <em>not</em> produce O2O: an A record pointing at the provider’s addresses
					(apex proxying is not CNAME-based, so it never triggers O2O), and a gray-clouded CNAME — that is
					an ordinary custom hostname, where only the provider’s zone has any say. O2O also refuses to chain
					further: traffic that has entered one custom-hostname zone will not be routed onward into
					another.
				</p>

				<h2 id="flow">How the request actually travels</h2>
				<O2OFlow />
				<ol>
					<li>
						A visitor resolves your hostname. Because your record is proxied, they get Cloudflare anycast
						addresses and connect to the nearest Cloudflare data centre.
					</li>
					<li>
						<strong>Your zone processes the request first.</strong> Your WAF rules, cache configuration,
						redirects, and Workers run here, on your plan, visible in your analytics.
					</li>
					<li>
						The request is handed to the <strong>provider’s zone inside Cloudflare’s network</strong> — it
						does not exit to the public internet and come back. On the way in, Cloudflare stamps the
						request with the header <code>cf-connecting-o2o: 1</code>, which is how the provider (in their
						origin or a Worker) can tell O2O traffic apart.
					</li>
					<li>
						<strong>The provider’s zone processes it second</strong>, applying their security and caching
						configuration, and then forwards it to their origin — the servers that actually render your
						store or site.
					</li>
				</ol>

				<h2 id="who-wins">Which zone’s configuration wins</h2>
				<p>
					The governing rule from Cloudflare is short: <strong>settings on your zone override settings on
					the provider’s zone</strong>. In practice a request has to survive both, and each product falls
					into one of four buckets.
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">Where it takes effect</th>
								<th scope="col">Products</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">Both zones — yours first, theirs second</th>
								<td>
									Access, WAF custom rules, WAF managed rules, Bot Management, Browser Integrity Check,
									Security Level, Waiting Room, Client-side Security (Page Shield), Image resizing, IPv6
								</td>
							</tr>
							<tr>
								<th scope="row">Your zone only</th>
								<td>API Shield, Zaraz</td>
							</tr>
							<tr>
								<th scope="row">Provider’s zone only</th>
								<td>
									Argo Smart Routing, Load Balancing, Origin Rules — your zone can still use Argo and
									Load Balancing for hostnames that are not on O2O
								</td>
							</tr>
							<tr>
								<th scope="row">Neither</th>
								<td>Spectrum, WebSockets, Rocket Loader, China Network</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>Several products work on your side but deserve care before you touch them:</p>
				<ul>
					<li>
						<strong>Cache.</strong> Supported, and generally discouraged for HTML. Your provider very
						likely caches outside Cloudflare, so caching in your zone invites stale or out-of-sync
						content. Caching hostnames that do <em>not</em> route to the provider is fine.
					</li>
					<li>
						<strong>Workers, Page Rules, Transform Rules, Rate Limiting.</strong> All run in your zone, and
						all can block or distort traffic on the O2O hostname if the rule matches it. Scope rules to
						hostnames you actually own end to end.
					</li>
					<li>
						<strong>Polish.</strong> Only optimises cached assets, so if your zone bypasses cache for
						provider-bound traffic there is nothing for it to optimise.
					</li>
					<li>
						<strong>DNS.</strong> Keep the records that make the setup work. Deleting the CNAME does not
						just break routing — it tells the provider you are gone, and the custom hostname moves to a
						removed state.
					</li>
					<li>
						<strong>HTTP/2 prioritization and IPv6 compatibility</strong> depend on matching settings
						across the two zones to behave as expected.
					</li>
				</ul>
				<p>
					This summary compresses Cloudflare’s per-product table; before you rely on any single row, check{" "}
					<a href={DOCS_COMPAT} target="_blank" rel="noopener noreferrer">
						the official compatibility list
					</a>
					, which is updated as products change.
				</p>

				<h2 id="detect">How to tell whether a hostname is on O2O</h2>
				<p>
					There is no zone setting or API field that reports “O2O is on” — it is per-request routing
					behaviour that falls out of your DNS configuration. So you check the inputs:
				</p>
				<ul>
					<li>
						<strong>Your DNS.</strong> Is the record a CNAME, pointing at a target the provider gave you,
						and proxied? For some providers the Cloudflare dashboard even shows their icon next to the
						record.
					</li>
					<li>
						<strong>Resolution.</strong> <code>dig +short shop.example.com</code> returns Cloudflare
						anycast addresses rather than the provider’s — expected, and confirms the record is proxied.
					</li>
					<li>
						<strong>The provider’s side.</strong> If you are the SaaS provider, look for{" "}
						<code>cf-connecting-o2o: 1</code> on inbound requests, at your origin or in a Worker. That
						header is set only on requests entering your zone through O2O.
					</li>
					<li>
						<strong>Your analytics.</strong> Traffic for that hostname shows up in your own zone’s HTTP
						analytics, because your zone genuinely sees the request first.
					</li>
				</ul>

				<h2 id="failures">What goes wrong</h2>
				<h3>Certificates stop renewing after you enable Always Use HTTPS</h3>
				<p>
					A classic, and documented for{" "}
					<a
						href="https://developers.cloudflare.com/cloudflare-for-platforms/cloudflare-for-saas/saas-customers/provider-guides/shopify/"
						target="_blank"
						rel="noopener noreferrer"
					>
						Shopify in particular
					</a>
					. Providers often validate certificates over HTTP-01, which requires{" "}
					<code>/.well-known/acme-challenge/*</code> to be reachable over plain HTTP. Always Use HTTPS
					redirects that path too, so validation fails and the certificate is never issued or renewed. Use a
					redirect rule that enforces HTTPS while excluding the ACME path instead.
				</p>
				<h3>Redirect loops or blank pages after adding a rule</h3>
				<p>
					Look at what your own zone applies to that hostname: a Page Rule, Transform Rule, Redirect, or
					Worker that matches the O2O subdomain. Your zone runs first, so a rewrite or redirect there is
					applied to traffic the provider expects to receive untouched.
				</p>
				<h3>Cache hit rate is far lower than expected</h3>
				<p>
					If the provider’s zone has Bot Management enabled, its <code>__cf_bm</code> cookie comes back in a{" "}
					<code>Set-Cookie</code> response header — and a response with <code>Set-Cookie</code> is not
					cached by default in your zone. The result is an eyeball-facing zone that caches far less than you
					planned.
				</p>
				<h3>A feature you enabled seems to do nothing</h3>
				<p>
					Check the bucket table above. Load Balancing, Origin Rules, Argo Smart Routing, WebSockets and
					Spectrum are not yours to control on an O2O hostname; that configuration belongs to the provider.
				</p>
				<h3>The provider cannot activate your custom hostname</h3>
				<p>
					If your zone is on an Enterprise plan with a zone hold enabled, activation is refused and the
					provider sees an error saying the hostname belongs to a held zone. Release the hold temporarily
					(including the subdomain option, if you are onboarding a subdomain) and retry.
				</p>
				<h3>Error 1014</h3>
				<p>
					A cross-account CNAME to another Cloudflare zone is banned unless the target owner is using
					Cloudflare for SaaS. If you see{" "}
					<a
						href="https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-1xxx-errors/error-1014/"
						target="_blank"
						rel="noopener noreferrer"
					>
						Error 1014
					</a>
					, the hostname was never onboarded on the provider’s side — that is not an O2O configuration
					problem, it is a missing custom hostname.
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
