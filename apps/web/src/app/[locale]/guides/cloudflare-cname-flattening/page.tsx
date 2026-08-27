import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { setRequestLocale } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import GuideShell, { type RelatedLink } from "@/components/guides/GuideShell";
import CnameFlattening from "@/components/guides/CnameFlattening";
import { guideBySlug, GUIDE_LOCALE } from "@/lib/guides/guides";

const SITE_URL = "https://90dd.adsl8.workers.dev";
const guide = guideBySlug("cloudflare-cname-flattening");
const PATH = `/guides/${guide.slug}`;

const DOCS_FLATTENING = "https://developers.cloudflare.com/dns/cname-flattening/";
const DOCS_SETUP = "https://developers.cloudflare.com/dns/cname-flattening/set-up-cname-flattening/";
const DOCS_DIAGRAM = "https://developers.cloudflare.com/dns/cname-flattening/cname-flattening-diagram/";
const DOCS_APEX = "https://developers.cloudflare.com/dns/concepts/#zone-apex";
const DOCS_CREATE_APEX = "https://developers.cloudflare.com/dns/manage-dns-records/how-to/create-zone-apex/";
const DOCS_CNAME = "https://developers.cloudflare.com/dns/manage-dns-records/reference/dns-record-types/#cname";
const DOCS_TTL = "https://developers.cloudflare.com/dns/manage-dns-records/reference/ttl/";
const DOCS_EMAIL = "https://developers.cloudflare.com/dns/troubleshooting/email-issues/";
const DOCS_1014 =
	"https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-1xxx-errors/error-1014/";
const DOCS_PROXY = "https://developers.cloudflare.com/dns/proxy-status/";
const DOCS_PAGES = "https://developers.cloudflare.com/pages/configuration/custom-domains/";
const DOCS_SETTINGS_API =
	"https://developers.cloudflare.com/api/resources/dns/subresources/settings/subresources/zone/methods/edit/";
const DOCS_ATTRIBUTES = "https://developers.cloudflare.com/dns/manage-dns-records/reference/record-attributes/";
const RFC_1034 = "https://www.rfc-editor.org/rfc/rfc1034#section-3.6.2";

/** FAQ 一处定义：可见文本与 FAQPage JSON-LD 同源，保证逐字一致 */
const FAQ: Array<{ q: string; a: string }> = [
	{
		q: "Can I use a CNAME record at my root domain in Cloudflare?",
		a: "Yes. Cloudflare flattens a CNAME at the zone apex automatically, on every plan, and there is no setting to turn that off. You add the record with the name @ and Cloudflare answers queries with the target's IP address instead of the CNAME.",
	},
	{
		q: "Why does dig return an A record when I configured a CNAME?",
		a: "Because the record was flattened. Flattening is not a redirect or a rewrite of your configuration — the CNAME stays in your zone, but Cloudflare resolves it and puts an address record on the wire. A dig for the CNAME type at an apex that is flattened returns nothing.",
	},
	{
		q: "Does CNAME flattening break DKIM or domain verification?",
		a: "It can. Some providers need to read the CNAME record itself, for DKIM keys, autodiscover, or proof that you control the domain. Once the record is flattened they see an address record instead and the check fails, which is why flattening every CNAME in a zone is riskier than flattening one.",
	},
	{
		q: "What is the difference between ALIAS, ANAME and CNAME flattening?",
		a: "They solve the same problem in different places. ALIAS and ANAME are provider-specific record types that resolve a hostname at the apex. Cloudflare has no such type: you recreate those records as ordinary CNAME records and flattening does the resolving.",
	},
	{
		q: "Why does my flattened CNAME return an empty answer?",
		a: "The target has no A or AAAA record to flatten to. Cloudflare documents this as a dangling CNAME, and the response is NODATA — an empty answer rather than an error, which reads like a record that never propagated.",
	},
];

const RELATED: RelatedLink[] = [
	{
		href: "/guides/cloudflare-error-1000-dns-points-to-prohibited-ip",
		label: "Cloudflare error 1000: DNS points to prohibited IP",
		note: "Where a flattened record quietly resolves to Cloudflare instead of your origin.",
	},
	{
		href: "/guides/what-is-the-orange-cloud-in-cloudflare",
		label: "What does the orange cloud mean in Cloudflare?",
		note: "Every proxied record is flattened by definition — this is what the proxy returns instead of your origin.",
	},
	{
		href: "/guides/why-is-my-cloudflare-dns-change-not-working",
		label: "Why isn't my Cloudflare DNS change working yet?",
		note: "The other reason a record looks broken when it is not: something downstream is still holding the old answer.",
	},
	{
		href: DOCS_FLATTENING,
		label: "Cloudflare docs: CNAME flattening",
		note: "The official reference, including the caveats about third-party verification and dangling targets.",
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

export default async function CnameFlatteningGuide({ params }: { params: Promise<{ locale: string }> }) {
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
				lede="It is the reason your root domain can point at a hostname at all — and the reason a record you can see in the dashboard sometimes refuses to show up in dig."
				updated={guide.updated}
				readingTime={guide.readingTime}
				related={RELATED}
			>
				<div className="glass r-island note p-6 sm:p-7">
					<p>
						CNAME flattening means Cloudflare resolves the CNAME itself and answers with the target&rsquo;s IP
						address instead of the CNAME record. It is what lets a root domain such as{" "}
						<code>example.com</code> point at a hostname.
					</p>
				</div>

				<p>
					Every other DNS provider makes you choose between a root domain and a hostname target. Cloudflare
					does not, and the mechanism behind that is worth understanding — because the same mechanism quietly
					removes records that some services need to read.
				</p>

				<h2 id="why">Why a root domain cannot hold a CNAME</h2>
				<p>
					The restriction is older than any CDN. RFC 1034 states that{" "}
					<a href={RFC_1034} target="_blank" rel="noopener noreferrer">
						if a CNAME record is present at a node, no other data should be present
					</a>
					, so that an alias and its canonical name can never disagree. Your{" "}
					<a href={DOCS_APEX} target="_blank" rel="noopener noreferrer">
						zone apex
					</a>{" "}
					— the domain itself, the record you write as <code>@</code> — always carries other data, because
					that is where the SOA record and the nameserver records live. A literal CNAME there would be a
					standards violation, and resolvers are entitled to behave badly when they meet one.
				</p>
				<p>
					That is a problem, because plenty of things you might want your bare domain to point at do not have
					a stable IP address: a load balancer, an object storage endpoint, a platform that reassigns
					addresses whenever it likes. Other providers invented non-standard record types for this — ALIAS
					and ANAME. Cloudflare has neither. Its documentation tells you to{" "}
					<a href={DOCS_CREATE_APEX} target="_blank" rel="noopener noreferrer">
						recreate ALIAS or ANAME records as ordinary CNAME records
					</a>
					, and lets flattening do the work.
				</p>

				<h2 id="how">What Cloudflare puts on the wire</h2>
				<CnameFlattening />
				<p>
					When a query arrives for a name whose CNAME is flattened, Cloudflare follows the target itself —{" "}
					<a href={DOCS_FLATTENING} target="_blank" rel="noopener noreferrer">
						one lookup, or several if the target is another CNAME
					</a>{" "}
					— and returns the final IP address under your own name. The CNAME never leaves the network. Two
					details of that answer are documented in Cloudflare&rsquo;s{" "}
					<a href={DOCS_DIAGRAM} target="_blank" rel="noopener noreferrer">
						flattening example
					</a>{" "}
					and catch people out:
				</p>
				<ul>
					<li>
						<strong>If the record is proxied</strong>, the answer is made of Cloudflare anycast IPs with a
						TTL of <code>300</code> — the target&rsquo;s address is never disclosed, and{" "}
						<a href={DOCS_TTL} target="_blank" rel="noopener noreferrer">
							the TTL of a proxied record cannot be edited
						</a>
						.
					</li>
					<li>
						<strong>If the record is DNS only</strong>, the answer is the target&rsquo;s own IP address, and
						its TTL is the <em>lower</em> of the target&rsquo;s TTL and your CNAME&rsquo;s TTL. Setting a
						long TTL on your record does not raise it; the target&rsquo;s cache lifetime caps yours.
					</li>
				</ul>
				<p>
					Because what leaves Cloudflare is an address record rather than an alias, the apex can still carry
					the MX and TXT records your mail and verification depend on. The conflict RFC 1034 forbids never
					appears on the wire, which is exactly the point. The same trick is what allows a bare domain to be
					used as a{" "}
					<a href={DOCS_PAGES} target="_blank" rel="noopener noreferrer">
						root custom domain on Cloudflare Pages
					</a>
					.
				</p>

				<h2 id="when">When flattening happens, and when you choose</h2>
				<p>
					Three of the five cases below are not settings at all — they happen whether you want them or not.
					Only the last two are yours to configure, and both are{" "}
					<a href={DOCS_SETUP} target="_blank" rel="noopener noreferrer">
						limited to paid zones
					</a>
					.
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">Case</th>
								<th scope="col">What is flattened</th>
								<th scope="col">Plans</th>
								<th scope="col">Can you turn it off?</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">
									CNAME at the apex (<code>@</code>)
								</th>
								<td>That record, always</td>
								<td>All, by default</td>
								<td>No</td>
							</tr>
							<tr>
								<th scope="row">Target in the same zone</th>
								<td>That record, always</td>
								<td>All</td>
								<td>No — the zone setting is ignored</td>
							</tr>
							<tr>
								<th scope="row">
									<a href={DOCS_PROXY} target="_blank" rel="noopener noreferrer">
										Proxied
									</a>{" "}
									CNAME
								</th>
								<td>Every proxied record</td>
								<td>All</td>
								<td>No — proxying returns anycast IPs</td>
							</tr>
							<tr>
								<th scope="row">
									All CNAME records (<code>flatten_all_cnames</code>)
								</th>
								<td>Every CNAME in the zone</td>
								<td>Paid</td>
								<td>Yes — off by default</td>
							</tr>
							<tr>
								<th scope="row">
									Per record (<code>flatten_cname</code>)
								</th>
								<td>One record you pick</td>
								<td>Paid</td>
								<td>Yes — off by default</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					The zone-wide switch lives on the DNS Settings page and maps to{" "}
					<a href={DOCS_SETTINGS_API} target="_blank" rel="noopener noreferrer">
						<code>flatten_all_cnames</code> on the zone DNS settings endpoint
					</a>
					. The per-record option appears as a <strong>Flatten</strong> toggle when you edit a record, and it
					is hidden in the cases where it would mean nothing: at the apex, on a proxied record, or when the
					zone-wide switch is already on. Records flattened individually are marked with a{" "}
					<a href={DOCS_ATTRIBUTES} target="_blank" rel="noopener noreferrer">
						<code>cf-flatten-cname</code> tag
					</a>{" "}
					so the setting survives a zone-file export and import.
				</p>

				<h2 id="breaks">Three ways flattening bites</h2>
				<p>
					Turning it on for every CNAME in a zone sounds tidy — faster resolution, one less thing to think
					about. It is the setting most likely to cause an outage you cannot see in the dashboard, because
					your records still look exactly right there.
				</p>
				<ol>
					<li>
						<strong>Services that need to read the CNAME itself.</strong> DKIM keys, autodiscover endpoints
						and domain-verification records are frequently published as CNAMEs pointing into a
						provider&rsquo;s zone. Flatten them and the provider gets an address record instead, so{" "}
						<a href={DOCS_EMAIL} target="_blank" rel="noopener noreferrer">
							the check fails
						</a>{" "}
						even though the underlying value is correct. Cloudflare&rsquo;s own guidance is to turn
						flattening off when a provider requires the CNAME.
					</li>
					<li>
						<strong>Targets with nothing to flatten to.</strong> If the final target has no A or AAAA
						record, there is no address to return, and Cloudflare answers with NODATA — an empty answer, not
						an error. It looks precisely like{" "}
						<Link href="/guides/why-is-my-cloudflare-dns-change-not-working">
							a record that never propagated
						</Link>
						, which sends people hunting in the wrong place for hours.
					</li>
					<li>
						<strong>Targets in someone else&rsquo;s Cloudflare account.</strong> Flattening does not get you
						around this: a CNAME pointing at a hostname in a different Cloudflare account returns{" "}
						<a href={DOCS_1014} target="_blank" rel="noopener noreferrer">
							Error 1014: CNAME Cross-User Banned
						</a>
						. Within one account, or across zones you own, it is fine; otherwise the target&rsquo;s owner has
						to onboard you through Cloudflare for SaaS.
					</li>
				</ol>

				<h2 id="check">Checking what your zone actually returns</h2>
				<p>Ask the nameservers rather than the dashboard. At an apex that is working normally:</p>
				<pre>
					<code>{"dig +noall +answer example.com A\ndig +noall +answer example.com CNAME"}</code>
				</pre>
				<p>
					The first command returns an address record even though you configured a CNAME — that is flattening
					working. The second returns nothing at all, which is not a bug and not a missing record: at a
					flattened name there is no CNAME to hand out. A TTL of <code>300</code> in the first answer tells
					you the record is proxied; anything else means it is DNS only and you are looking at the
					target&rsquo;s address.
				</p>
				<p>
					If you are auditing a whole zone, read the records back from the API instead of clicking through
					them. Each{" "}
					<a href={DOCS_CNAME} target="_blank" rel="noopener noreferrer">
						CNAME record
					</a>{" "}
					carries its own <code>settings.flatten_cname</code> value, and the zone-level{" "}
					<code>flatten_all_cnames</code> flag sits on the DNS settings endpoint — worth checking before you
					blame a provider for a verification failure.
				</p>

				<h2 id="choose">A reasonable default</h2>
				<p>
					At the apex you have no decision to make. For everything else, the ordering that causes the fewest
					surprises is: leave the zone-wide switch off, flatten individual DNS-only records when you have a
					specific reason to — a slow multi-hop chain, a target that resolves through two or three
					intermediaries — and never flatten a record a third party is going to read. If a provider ever tells
					you your DNS record is missing while the dashboard shows it plainly, flattening is the first thing
					to check.
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
