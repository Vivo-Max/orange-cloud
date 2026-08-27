import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { setRequestLocale } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import GuideShell, { type RelatedLink } from "@/components/guides/GuideShell";
import PurgeReach from "@/components/guides/PurgeReach";
import { guideBySlug, GUIDE_LOCALE } from "@/lib/guides/guides";

const SITE_URL = "https://90dd.adsl8.workers.dev";
const guide = guideBySlug("cloudflare-purge-cache-not-working");
const PATH = `/guides/${guide.slug}`;

const DOCS_PURGE = "https://developers.cloudflare.com/cache/how-to/purge-cache/";
const DOCS_SINGLE = "https://developers.cloudflare.com/cache/how-to/purge-cache/purge-by-single-file/";
const DOCS_EVERYTHING = "https://developers.cloudflare.com/cache/how-to/purge-cache/purge-everything/";
const DOCS_TAGS = "https://developers.cloudflare.com/cache/how-to/purge-cache/purge-by-tags/";
const DOCS_HOSTNAME = "https://developers.cloudflare.com/cache/how-to/purge-cache/purge-by-hostname/";
const DOCS_PREFIX = "https://developers.cloudflare.com/cache/how-to/purge-cache/purge_by_prefix/";
const DOCS_DEV_MODE = "https://developers.cloudflare.com/cache/reference/development-mode/";
const DOCS_BROWSER_TTL = "https://developers.cloudflare.com/cache/how-to/edge-browser-cache-ttl/";
const DOCS_STATUSES = "https://developers.cloudflare.com/cache/concepts/cache-responses/";
const DOCS_KEYS = "https://developers.cloudflare.com/cache/how-to/cache-keys/";
const DOCS_API = "https://developers.cloudflare.com/api/resources/cache/methods/purge/";
const DOCS_TIERED = "https://developers.cloudflare.com/cache/how-to/tiered-cache/";
const DOCS_RESERVE = "https://developers.cloudflare.com/cache/advanced-configuration/cache-reserve/";
const DOCS_RULES_SETTINGS = "https://developers.cloudflare.com/cache/how-to/cache-rules/settings/";
const DOCS_TRANSFORM = "https://developers.cloudflare.com/rules/transform/";

/** FAQ 一处定义：可见文本与 FAQPage JSON-LD 同源，保证逐字一致 */
const FAQ: Array<{ q: string; a: string }> = [
	{
		q: "How long does a Cloudflare cache purge take to work?",
		a: "The purge itself is instant — Cloudflare calls it Instant Purge, and it applies across every data centre rather than rolling out region by region. If content is still old a minute later, the purge did not match the cached object, or what you are looking at is not Cloudflare's copy.",
	},
	{
		q: "Does purging the Cloudflare cache clear my visitors' browser cache?",
		a: "No. Purging removes Cloudflare's copies only, and the documentation states plainly that it does not affect assets stored by a visitor's browser. Those copies expire on the Browser Cache TTL that was sent with them, which defaults to four hours.",
	},
	{
		q: "Why does my file still return cf-cache-status: HIT after I purged it?",
		a: "Because the object being served is stored under a different cache key than the one you purged. Custom cache keys built from headers or cookies cannot be purged from the dashboard, and objects cached with an Origin or X-Forwarded-Host header need an API purge that includes those header values.",
	},
	{
		q: "Is purge by cache tag only available on Enterprise plans?",
		a: "Not any more. Cloudflare's availability table lists URL, hostname, tag, prefix and purge everything on Free, Pro, Business and Enterprise alike. What changes by plan is the rate limit: Free allows five purge requests per minute, while Enterprise allows fifty per second.",
	},
	{
		q: "Should I use Purge Everything or purge by URL?",
		a: "Purge by URL, in nearly every case. Cloudflare explicitly recommends single-file purging, because purging everything sends every subsequent request back to your origin at once, which on a busy site turns a content update into a traffic spike.",
	},
	{
		q: "Does turning on Development Mode clear the cache?",
		a: "No. Development Mode suspends edge caching and Polish for three hours so you can see origin changes immediately, but it removes nothing that is already cached. When it switches off, the old objects are still there unless they expired in the meantime.",
	},
];

const RELATED: RelatedLink[] = [
	{
		href: "/guides/why-is-cloudflare-not-caching-my-site",
		label: "Why is Cloudflare not caching my site?",
		note: "The opposite problem, and the header that tells them apart: DYNAMIC, BYPASS and a MISS that never becomes a HIT.",
	},
	{
		href: "/guides/what-is-the-orange-cloud-in-cloudflare",
		label: "What does the orange cloud mean in Cloudflare?",
		note: "Nothing is cached — and nothing can be purged — on a hostname that is set to DNS only.",
	},
	{
		href: DOCS_PURGE,
		label: "Cloudflare docs: Purge cache",
		note: "The official reference, including the per-plan rate limits and every purge method.",
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

export default async function PurgeGuide({ params }: { params: Promise<{ locale: string }> }) {
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
				lede="The purge almost certainly worked. The question is what it worked on — because a purge removes one exact cache key, and there are several ways to end up looking at a different one."
				updated={guide.updated}
				readingTime={guide.readingTime}
				related={RELATED}
			>
				<div className="glass r-island note p-6 sm:p-7">
					<p>
						A purge clears one exact cache key. If the key you purged is not the key that is cached — a
						custom cache key, a transformed URL, a copy already sitting in a browser — the old file
						survives, and nothing looks wrong.
					</p>
				</div>

				<p>
					Cloudflare&rsquo;s purge is not a background job that slowly rolls out. It is instant and global: by
					the time the dashboard confirms it, every data centre has dropped its copy. So &ldquo;purge is not
					working&rdquo; is nearly always one of two other things — the purge matched nothing, or the stale
					copy you are looking at was never Cloudflare&rsquo;s to delete.
				</p>

				<h2 id="verify">First, read the header rather than the page</h2>
				<p>
					A refreshed browser tab is the worst diagnostic available. Ask the edge directly instead:
				</p>
				<pre>
					<code>curl -sSI https://example.com/app.css | grep -i &apos;^cf-cache-status\|^age&apos;</code>
				</pre>
				<p>
					Each purge method leaves a documented fingerprint in{" "}
					<a href={DOCS_STATUSES} target="_blank" rel="noopener noreferrer">
						<code>cf-cache-status</code>
					</a>
					, and that fingerprint tells you whether the object was touched at all:
				</p>
				<ul>
					<li>
						<strong>
							<code>MISS</code>
						</strong>{" "}
						— the object was deleted and refetched. This is what a URL, hostname, prefix or tag purge
						produces.
					</li>
					<li>
						<strong>
							<code>EXPIRED</code>
						</strong>{" "}
						— the object was found but treated as stale, so it was revalidated against your origin. This is
						what <em>Purge Everything</em> produces.
					</li>
					<li>
						<strong>
							<code>HIT</code> with a large <code>Age</code>
						</strong>{" "}
						— nothing happened. The object you are hitting is not the object you purged, which is the case
						the rest of this page is about.
					</li>
				</ul>

				<h2 id="methods">The five purge methods, and what each one covers</h2>
				<div className="table-wrap">
					<table>
						<thead>
							<tr>
								<th scope="col">Method</th>
								<th scope="col">Clears</th>
								<th scope="col">Status after</th>
								<th scope="col">Watch out for</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">
									<a href={DOCS_SINGLE} target="_blank" rel="noopener noreferrer">
										By URL
									</a>
								</th>
								<td>One exact URL</td>
								<td>
									<code>MISS</code>
								</td>
								<td>No wildcards; case-sensitive path; misses custom cache keys from the dashboard</td>
							</tr>
							<tr>
								<th scope="row">
									<a href={DOCS_PREFIX} target="_blank" rel="noopener noreferrer">
										By prefix
									</a>
								</th>
								<td>Everything under a path</td>
								<td>
									<code>MISS</code>
								</td>
								<td>Max 31 path separators, 100 prefixes per request; query strings not supported</td>
							</tr>
							<tr>
								<th scope="row">
									<a href={DOCS_HOSTNAME} target="_blank" rel="noopener noreferrer">
										By hostname
									</a>
								</th>
								<td>Every asset on one host</td>
								<td>
									<code>MISS</code>
								</td>
								<td>Up to 100 hostnames per request</td>
							</tr>
							<tr>
								<th scope="row">
									<a href={DOCS_TAGS} target="_blank" rel="noopener noreferrer">
										By cache tag
									</a>
								</th>
								<td>Everything tagged at the origin</td>
								<td>
									<code>MISS</code>
								</td>
								<td>
									Requires a <code>Cache-Tag</code> response header; up to 100 tags per request
								</td>
							</tr>
							<tr>
								<th scope="row">Purge Everything</th>
								<td>The whole zone</td>
								<td>
									<code>EXPIRED</code>
								</td>
								<td>Sends every subsequent request back to your origin at once</td>
							</tr>
						</tbody>
					</table>
				</div>
				<p>
					One widely repeated claim is worth correcting: purge by tag, hostname and prefix are{" "}
					<a href={DOCS_PURGE} target="_blank" rel="noopener noreferrer">
						available on every plan
					</a>
					, Free included. What differs by plan is the rate limit — five purge requests per minute on Free,
					up to fifty per second on Enterprise, counted per account rather than per zone.
				</p>

				<h2 id="reach">Where a stale copy can still be sitting</h2>
				<PurgeReach />

				<h2 id="mismatch">Reason one: the purge never matched the object</h2>
				<p>
					Cloudflare stores an object under a cache key, and a purge request is matched against that key. If
					your{" "}
					<a href={DOCS_KEYS} target="_blank" rel="noopener noreferrer">
						cache key
					</a>{" "}
					includes headers or cookies, the dashboard cannot reproduce it — the form has nowhere to put those
					values — so a single-file purge from the dashboard silently matches nothing. The{" "}
					<a href={DOCS_API} target="_blank" rel="noopener noreferrer">
						purge API
					</a>{" "}
					accepts a <code>headers</code> object for exactly this reason, and any header you omit is treated as
					empty. The same applies to objects cached with an <code>Origin</code>, <code>X-Forwarded-Host</code>,{" "}
					<code>X-Host</code>, <code>X-Forwarded-Scheme</code>, <code>X-Original-URL</code>,{" "}
					<code>X-Rewrite-URL</code> or <code>Forwarded</code> request header: the dashboard cannot clear
					them, the API can.
				</p>
				<p>Three more mismatches that produce the same silent no-op:</p>
				<ol>
					<li>
						<strong>Transform Rules, purged from the wrong side.</strong> If a{" "}
						<a href={DOCS_TRANSFORM} target="_blank" rel="noopener noreferrer">
							Transform Rule
						</a>{" "}
						rewrites the path, a single-file purge needs the <em>end-user</em> URL, while a prefix purge
						needs the <em>post-transform</em> origin URL. Using one convention for both is a common way to
						purge nothing twice.
					</li>
					<li>
						<strong>A Cache Rule that only matches GET.</strong> A purge arrives as a{" "}
						<code>PURGE</code> request, so an expression such as{" "}
						<code>http.request.method eq &quot;GET&quot;</code> excludes it. Widen the expression to accept{" "}
						<code>PURGE</code> as well, or purge by prefix or tag instead. Rules matching on fields that do
						not exist during a purge — a bot score, for instance — cannot be single-file purged at all.
					</li>
					<li>
						<strong>Wildcards.</strong> They are not supported in single-file purge. A URL with a query
						string has to be purged exactly as cached, or reached with prefix, hostname or tag purge.
					</li>
				</ol>

				<h2 id="browser">Reason two: the edge is clean, the browser is not</h2>
				<p>
					This is the most common false alarm, and the documentation is unambiguous about it: purging
					Cloudflare&rsquo;s cache{" "}
					<a href={DOCS_BROWSER_TTL} target="_blank" rel="noopener noreferrer">
						does not affect assets stored by a visitor&rsquo;s browser
					</a>
					. Those copies live under the <code>Cache-Control</code> lifetime they were served with, and
					Cloudflare&rsquo;s default Browser Cache TTL is four hours. You purge, you reload, you see the old
					file — from your own disk.
				</p>
				<p>
					Confirm it with <code>curl</code>, or a private window, before touching anything at the edge again.
					The durable fix is not a shorter browser TTL but a changing filename: fingerprinted assets such as{" "}
					<code>app.9f2c1d.css</code> are never stale, because a new build is a new URL that no cache has
					ever seen.
				</p>

				<h2 id="below">Reason three: a layer below the edge still holds it</h2>
				<p>
					Two features move copies out from under a straightforward purge. With{" "}
					<a href={DOCS_TIERED} target="_blank" rel="noopener noreferrer">
						Tiered Cache
					</a>{" "}
					enabled, a hostname, prefix or tag purge can return <code>EXPIRED</code> rather than{" "}
					<code>MISS</code>, because the lower tier revalidates against the upper tier instead of going
					straight to your origin. That is working as designed — but if your origin change was not picked up,
					the tier you reached is the one to check.
				</p>
				<p>
					<a href={DOCS_RESERVE} target="_blank" rel="noopener noreferrer">
						Cache Reserve
					</a>{" "}
					is the sharper edge. A purge by URL removes the object from Cache Reserve along with the edge cache.
					Every other method — tag, hostname, prefix, and Purge Everything — only forces a revalidation
					attempt against what Cache Reserve holds, and Purge Everything is documented as a soft purge that
					does not refresh metadata such as cache tags. If you are on Cache Reserve and you need an object
					genuinely gone, purge it by URL.
				</p>

				<h2 id="dev-mode">Development Mode is not a purge</h2>
				<p>
					<a href={DOCS_DEV_MODE} target="_blank" rel="noopener noreferrer">
						Development Mode
					</a>{" "}
					suspends edge caching and Polish for three hours. It is the right tool while you are iterating on
					CSS, and the wrong tool for shipping a change: it deletes nothing, so when it expires, the objects
					cached beforehand are still there. If you need a longer bypass, use the bypass cache setting in{" "}
					<a href={DOCS_RULES_SETTINGS} target="_blank" rel="noopener noreferrer">
						Cache Rules
					</a>{" "}
					instead, which is scoped to an expression rather than to the whole zone.
				</p>

				<h2 id="habit">The habit that avoids all of this</h2>
				<p>
					Cloudflare recommends single-file purging over{" "}
					<a href={DOCS_EVERYTHING} target="_blank" rel="noopener noreferrer">
						Purge Everything
					</a>{" "}
					for a practical reason: after a full purge, every request in flight goes back to your origin at
					once. On a quiet site that is invisible; on a busy one it turns a copy edit into a traffic spike,
					and the slow origin responses that follow look like Cloudflare being broken.
				</p>
				<p>A deploy sequence that never needs a purge at all:</p>
				<ol>
					<li>Fingerprint anything versioned — CSS, JS, images — so new builds get new URLs.</li>
					<li>
						Tag what cannot be fingerprinted with a <code>Cache-Tag</code> response header, then purge by
						tag when it changes.
					</li>
					<li>
						Reserve <em>Purge Everything</em> for configuration mistakes, not for releases.
					</li>
					<li>
						Verify with <code>cf-cache-status</code> rather than with a reload, and check from a private
						window when the header says <code>MISS</code> but the page still looks old.
					</li>
				</ol>
				<p>
					If the header keeps saying <code>DYNAMIC</code> or <code>BYPASS</code> instead, the object was never
					cached and there is nothing to purge — that is{" "}
					<Link href="/guides/why-is-cloudflare-not-caching-my-site">a different problem</Link> with a
					different fix.
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
