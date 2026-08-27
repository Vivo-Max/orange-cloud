import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { setRequestLocale } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import GuideShell, { type RelatedLink } from "@/components/guides/GuideShell";
import DnsCacheLayers from "@/components/guides/DnsCacheLayers";
import { guideBySlug, GUIDE_LOCALE } from "@/lib/guides/guides";

const SITE_URL = "https://90dd.adsl8.workers.dev";
const guide = guideBySlug("why-is-my-cloudflare-dns-change-not-working");
const PATH = `/guides/${guide.slug}`;

const DOCS_FAQ = "https://developers.cloudflare.com/dns/faq/";
const DOCS_TTL = "https://developers.cloudflare.com/dns/manage-dns-records/reference/ttl/";
const DOCS_ISSUES = "https://developers.cloudflare.com/dns/troubleshooting/dns-issues/";
const DOCS_PENDING = "https://developers.cloudflare.com/dns/zone-setups/troubleshooting/pending-nameservers/";
const DOCS_FULL_TROUBLE = "https://developers.cloudflare.com/dns/zone-setups/full-setup/troubleshooting/";
const DOCS_NS_TTL = "https://developers.cloudflare.com/dns/nameservers/nameserver-options/#nameserver-ttl";
const DOCS_PROXY = "https://developers.cloudflare.com/dns/proxy-status/";
const DOCS_STATUS = "https://developers.cloudflare.com/dns/zone-setups/reference/domain-status/";
const DOCS_DNSSEC = "https://developers.cloudflare.com/dns/dnssec/";
const PURGE_CF = "https://one.one.one.one/purge-cache/";
const PURGE_GOOGLE = "https://dns.google/cache";
const RFC_2308 = "https://datatracker.ietf.org/doc/html/rfc2308";

/** FAQ 一处定义：可见文本与 FAQPage JSON-LD 同源，保证逐字一致 */
const FAQ: Array<{ q: string; a: string }> = [
	{
		q: "How long does a Cloudflare DNS change take to take effect?",
		a: "Cloudflare publishes the change on its own nameservers globally within five minutes, and usually much less. Everything longer than that is a cache somewhere else expiring — most often a recursive resolver still holding the previous record for the length of its TTL.",
	},
	{
		q: "Why does my new DNS record still return NXDOMAIN?",
		a: "Because a resolver asked for that hostname before it existed and cached the empty answer. The length of that negative cache comes from the MINIMUM field of your zone's SOA record, not from the TTL of the record you just created, so lowering the record's TTL does not shorten the wait.",
	},
	{
		q: "Does lowering the TTL make a DNS change apply faster?",
		a: "Only if you lower it before making the change. Resolvers that already cached the old record keep it for the old TTL, so the new, shorter value is only honoured once that entry expires. Lower the TTL a day ahead of a planned migration, then change the record.",
	},
	{
		q: "How do I check whether Cloudflare has the new record?",
		a: "Query a Cloudflare authoritative nameserver directly with dig @hera.ns.cloudflare.com example.com A, substituting one of the nameservers assigned to your zone. That bypasses every resolver cache, so it shows what Cloudflare is actually publishing right now.",
	},
	{
		q: "Why does dig still show an old IP address after I changed a proxied record?",
		a: "It does not — a proxied record is answered with Cloudflare anycast addresses, never with your origin IP. If dig returns your server's real address, that hostname is set to DNS only, or the answer you are looking at came from a cache that predates the change.",
	},
	{
		q: "Does flushing my DNS cache fix a DNS change that has not applied?",
		a: "Only when your own machine is the layer holding the stale answer. Flushing clears your operating system and browser caches, but the recursive resolver upstream of you — your ISP's, or a public one — keeps its copy until its own TTL runs out.",
	},
];

const RELATED: RelatedLink[] = [
	{
		href: "/guides/what-is-the-orange-cloud-in-cloudflare",
		label: "What does the orange cloud mean in Cloudflare?",
		note: "Why a proxied record never returns your origin IP — and why that changes what dig can tell you.",
	},
	{
		href: "/guides/cloudflare-cname-flattening",
		label: "What is CNAME flattening in Cloudflare?",
		note: "A flattened CNAME whose target has no address returns an empty answer — which reads exactly like a change that never propagated.",
	},
	{
		href: "/guides/why-is-cloudflare-not-caching-my-site",
		label: "Why is Cloudflare not caching my site?",
		note: "The other kind of stale answer: the one Cloudflare itself is serving from cache.",
	},
	{
		href: DOCS_ISSUES,
		label: "Cloudflare docs: General DNS issues",
		note: "The official walkthrough, including the negative-caching case and the dig commands for it.",
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

export default async function DnsChangeGuide({ params }: { params: Promise<{ locale: string }> }) {
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
				lede="Waiting is the standard advice, and it is usually the wrong advice — because the thing you are waiting for is not Cloudflare. Four layers can hold the old answer, and each one has a different fix."
				updated={guide.updated}
				readingTime={guide.readingTime}
				related={RELATED}
			>
				<div className="glass r-island note p-6 sm:p-7">
					<p>
						Cloudflare publishes zone changes globally in under five minutes. If a change still is not live,
						something is holding the old answer: a record TTL, a negatively cached <code>NXDOMAIN</code>, or
						a delegation that never moved.
					</p>
				</div>

				<p>
					&ldquo;DNS propagation&rdquo; describes something that does not happen. No record is pushed out to
					the world&rsquo;s resolvers. Authoritative nameservers hold the current answer and wait to be asked;
					every other machine in the chain keeps a copy for as long as it was told to. So the delay you are
					sitting through is never Cloudflare publishing the change — Cloudflare&rsquo;s own documentation
					puts that at{" "}
					<a href={DOCS_FAQ} target="_blank" rel="noopener noreferrer">
						within five minutes, usually much less
					</a>
					. It is a cache expiring, and which cache decides what you should do about it.
				</p>

				<h2 id="layers">Four places the old answer can be sitting</h2>
				<DnsCacheLayers />
				<p>
					A lookup walks down this stack and stops at the first layer that already has an answer. Restarting
					your browser fixes exactly one of these layers. Waiting fixes another. The bottom two are not caches
					at all and never fix themselves — a delegation that points at the wrong nameservers will still be
					wrong tomorrow.
				</p>

				<h2 id="ask-authoritative">Start by asking the authoritative nameserver</h2>
				<p>
					Before diagnosing anything, find out what Cloudflare is actually publishing. Querying a Cloudflare
					nameserver directly skips every cache above it:
				</p>
				<pre>
					<code>dig @1.1.1.1 example.com NS +short{"\n"}dig @hera.ns.cloudflare.com www.example.com A</code>
				</pre>
				<p>
					Use whichever nameserver names the first command returns for your zone. The answer splits the
					problem cleanly in two:
				</p>
				<ul>
					<li>
						<strong>The new value is there.</strong> Cloudflare has done its part. The remaining wait is a
						cache above it, and the sections below cover which one.
					</li>
					<li>
						<strong>The new value is not there, or the query fails.</strong> The record was not saved as you
						think it was, the hostname does not match exactly, or your zone is not delegated to these
						nameservers at all. Skip to{" "}
						<a href="#delegation">the delegation section</a>.
					</li>
				</ul>

				<h2 id="ttl">Case one: the TTL has not run out</h2>
				<p>
					A resolver holding the previous record keeps it for the TTL it was handed <em>at the time it
					cached it</em>. That is the single most misunderstood point here: editing the TTL now has no effect
					on a copy that is already cached. The values Cloudflare allows:
				</p>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">Record</th>
								<th scope="col">TTL</th>
								<th scope="col">Worst-case wait</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">Proxied (orange cloud)</th>
								<td>Auto, fixed at 300 seconds, not editable</td>
								<td>5 minutes</td>
							</tr>
							<tr>
								<th scope="row">DNS only, Auto</th>
								<td>300 seconds</td>
								<td>5 minutes</td>
							</tr>
							<tr>
								<th scope="row">DNS only, custom</th>
								<td>60 seconds to 1 day (30 seconds on Enterprise)</td>
								<td>Whatever you set — up to 24 hours</td>
							</tr>
							<tr>
								<th scope="row">Nameserver (NS) TTL</th>
								<td>86,400 seconds by default</td>
								<td>24 hours</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					Two consequences worth internalising. First, proxied records are{" "}
					<a href={DOCS_TTL} target="_blank" rel="noopener noreferrer">
						pinned to five minutes
					</a>{" "}
					precisely so Cloudflare can move its anycast addresses without stranding anyone — which means an
					orange-clouded hostname can never be more than five minutes stale. Second, the{" "}
					<a href={DOCS_NS_TTL} target="_blank" rel="noopener noreferrer">
						nameserver TTL
					</a>{" "}
					is a separate 24-hour figure, so a zone that has just moved to Cloudflare can look inconsistent for
					a full day even when every record is correct.
				</p>

				<h2 id="negative-cache">Case two: the resolver cached &ldquo;this does not exist&rdquo;</h2>
				<p>
					This is the failure that sends people in circles. You add a brand-new subdomain, it returns{" "}
					<code>NXDOMAIN</code>, you lower the TTL, you flush your cache, you wait — and nothing changes.
				</p>
				<p>
					The reason is <strong>negative caching</strong>. When a resolver is asked for a hostname that has no
					records, it caches the absence, so it does not have to ask again immediately. If anything queried
					your hostname before you created it — a monitoring check, a certificate probe, your own impatient
					browser — that empty answer is now stored. Per{" "}
					<a href={RFC_2308} target="_blank" rel="noopener noreferrer">
						RFC 2308
					</a>
					, its lifetime comes from the <code>MINIMUM</code> field of your zone&rsquo;s <code>SOA</code>{" "}
					record, and Cloudflare&rsquo;s{" "}
					<a href={DOCS_ISSUES} target="_blank" rel="noopener noreferrer">
						troubleshooting guidance
					</a>{" "}
					spells out what follows from that:
				</p>
				<ul>
					<li>Lowering the new record&rsquo;s TTL changes nothing, because the entry being served is not your record.</li>
					<li>Flushing locally changes nothing, because the entry lives in the resolver upstream of you.</li>
					<li>
						Different resolvers queried the name at different moments and apply different limits, which is
						exactly why the result looks like it is &ldquo;propagating&rdquo; unevenly.
					</li>
				</ul>
				<p>You can watch the countdown rather than guess at it:</p>
				<pre>
					<code>dig +noall +answer +authority newhost.example.com</code>
				</pre>
				<p>
					If the answer section is empty and the authority section returns your <code>SOA</code>, the number
					beside it is how many seconds remain before that resolver will ask again. To stop waiting on the two
					big public resolvers, purge the name directly at{" "}
					<a href={PURGE_CF} target="_blank" rel="noopener noreferrer">
						1.1.1.1
					</a>{" "}
					and{" "}
					<a href={PURGE_GOOGLE} target="_blank" rel="noopener noreferrer">
						8.8.8.8
					</a>
					. The habit that prevents all of this: create the record first, and only then point anything at the
					hostname.
				</p>

				<h2 id="delegation">Case three: the zone is not really delegated to Cloudflare</h2>
				<p>
					If the authoritative query in the first step came back empty or failed, no amount of waiting will
					help. Check what the parent zone publishes, rather than what your registrar&rsquo;s control panel
					claims:
				</p>
				<pre>
					<code>dig +trace example.com NS +noall +authority +nodnssec</code>
				</pre>
				<p>The usual causes, all of them things you have to go and fix:</p>
				<ol>
					<li>
						<strong>The nameservers do not match exactly.</strong> Each zone is assigned a specific pair,
						and re-adding a deleted domain gets a fresh pair. A previously working set may no longer be the
						right one, and typos like <code>cloudfare.com</code> are common enough that Cloudflare{" "}
						<a href={DOCS_PENDING} target="_blank" rel="noopener noreferrer">
							calls them out by name
						</a>
						.
					</li>
					<li>
						<strong>Extra nameservers are still listed.</strong> The registrar should list the Cloudflare
						nameservers and nothing else.
					</li>
					<li>
						<strong>A stale DS record is still published.</strong> DNSSEC from your previous provider blocks
						the delegation and produces <code>SERVFAIL</code> until{" "}
						<a href={DOCS_DNSSEC} target="_blank" rel="noopener noreferrer">
							the old DS record is removed
						</a>{" "}
						and its TTL at the parent has expired — commonly 24 to 48 hours for most top-level domains.
					</li>
					<li>
						<strong>The registrar has not published the change yet.</strong> Some take{" "}
						<a href={DOCS_FULL_TROUBLE} target="_blank" rel="noopener noreferrer">
							up to 24 hours
						</a>
						. This one really is a wait — but confirm it with <code>dig +trace</code> rather than assuming.
					</li>
				</ol>
				<p>
					A zone sitting in <strong>Pending Nameserver Update</strong> is Cloudflare telling you it cannot see
					the delegation either; the{" "}
					<a href={DOCS_STATUS} target="_blank" rel="noopener noreferrer">
						zone status reference
					</a>{" "}
					explains what each state means, including the <strong>Moved</strong> state that eventually deletes
					the zone.
				</p>

				<h2 id="proxied">When DNS is correct and the site is still wrong</h2>
				<p>
					One case looks like a DNS delay and is not one at all. A{" "}
					<Link href="/guides/what-is-the-orange-cloud-in-cloudflare">proxied record</Link> is answered with
					Cloudflare anycast addresses, never with your server&rsquo;s address — so if you change the origin
					IP behind a proxied hostname, <em>the DNS answer does not change</em>. There is nothing for a
					resolver to re-fetch, and <code>dig</code> cannot confirm the change either way. The new origin
					takes effect at the edge within the same few minutes Cloudflare needs to publish it.
				</p>
				<p>
					Toggling proxy status is the opposite case, and it is asymmetric. Turning the proxy{" "}
					<strong>off</strong> replaces a record whose TTL was pinned at 300 seconds, so it settles in about
					five minutes. Turning it <strong>on</strong> has to wait out the DNS-only TTL that was in force
					before — which, if someone set it to a day, means a day of visitors still reaching your origin
					directly. Lower that TTL first, then flip the switch. The{" "}
					<a href={DOCS_PROXY} target="_blank" rel="noopener noreferrer">
						proxy status reference
					</a>{" "}
					covers what else changes with it.
				</p>

				<h2 id="planned">Making the next change painless</h2>
				<ol>
					<li>
						Lower the TTL on the records you are about to change to 60 or 300 seconds, and do it at least
						one old-TTL period in advance.
					</li>
					<li>Wait out the old TTL. Until it expires, resolvers are still handing out the long one.</li>
					<li>Make the change, then verify against an authoritative nameserver before testing anywhere else.</li>
					<li>
						Create records before anything queries their hostnames, so no negative cache entry is ever
						created.
					</li>
					<li>Once the change is stable, raise the TTL back up — long TTLs are good for everyone the rest of the time.</li>
				</ol>
				<p>
					None of this needs a fourth tool: <code>dig</code>, the record itself, and knowing which of the four
					layers you are talking to will resolve nearly every &ldquo;my DNS change is not working&rdquo;
					report.
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
