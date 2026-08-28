# Orange Cloud Web

The landing page and OAuth callback relay for [Orange Cloud](https://github.com/chen2he/orange-cloud), deployed at [oss.omail.us.kg](https://oss.omail.us.kg) on Cloudflare Workers.

## What this app does

- **Landing page** in five locales (en / zh-Hans / zh-Hant / zh-HK / ja) via `next-intl`, with `localePrefix: "as-needed"` — English lives at `/`, other locales at `/zh-Hans` etc. Also serves `/privacy`, `/terms`, and `/contact`, which the iOS app links to.
- **OAuth callback relay** at `/oauth/callback` ([route.ts](src/app/oauth/callback/route.ts)): Cloudflare's OAuth only accepts `https` redirect URIs, so this route 302-redirects the authorization `code` and `state` straight to the iOS app's custom scheme (`orangecloud://oauth/callback`). It stores nothing and never exchanges the code — the token exchange and `state` validation happen on-device, secured by PKCE.

## Stack

- Next.js 16 (App Router) + React 19
- `next-intl` for routing/messages — translations live in [`src/messages/`](src/messages/)
- Tailwind CSS 4
- Deployed to Cloudflare Workers with [`@opennextjs/cloudflare`](https://opennext.js.org/cloudflare); config in [`wrangler.jsonc`](wrangler.jsonc)

## Gotchas (read before touching routing or deploying)

- `/oauth/callback` sits **outside** the `[locale]` segment, and the middleware matcher in [`src/middleware.ts`](src/middleware.ts) explicitly excludes `oauth` — the callback must reach the route handler untouched. Don't let locale routing swallow it.
- On Cloudflare Workers, next-intl's middleware must be an **edge `middleware.ts`** — the Next 16 `proxy.ts` convention runs on the Node runtime, which Workers doesn't support.
- The screenshot gallery images live in `public/shots/<locale>/` (`01_dashboard.jpg` …); zh-HK reuses the zh-Hant set.

## SEO / GEO (AI visibility)

What's wired up:

- **`robots.ts`** tiers AI crawlers: search/retrieval bots (OAI-SearchBot, Claude-SearchBot, PerplexityBot) and user-triggered bots (ChatGPT-User, Claude-User, Perplexity-User) are allowed; training crawlers (GPTBot, ClaudeBot, Meta-ExternalAgent, CCBot), opt-out tokens (Google-Extended, Applebot-Extended), and undeclared crawlers (Bytespider) are blocked. `/oauth/` is excluded everywhere.
- **`public/llms.txt`** — Markdown overview of the product, key pages, pricing, and the GitHub repo for AI retrieval ([spec](https://llmstxt.org)).
- **`sitemap.ts`** emits all pages with full hreflang alternates; `generateMetadata` covers canonical, hreflang, Open Graph, Twitter card, and `itunes` app association.
- **JSON-LD** (`SoftwareApplication`) in the locale layout — only the Bing/Copilot index-enrichment path is known to consume it, so it stays minimal.
- **IndexNow** — changed pages are pushed to Bing (and Yandex / Seznam / Naver / Yep, which share the protocol) automatically:
  - the URL list is generated from [`src/lib/site/urls.ts`](src/lib/site/urls.ts), the same source `sitemap.ts` reads. Each entry carries an `updated` date — **bump it when you change a page**, that date is both the sitemap `lastmod` and the "has this changed?" test;
  - the daily cron in [`custom-worker.ts`](custom-worker.ts) (UTC 08:00) diffs that list against the `indexnow_urls` ledger in D1 and submits only what changed, then records it. Nothing changed → no outbound request;
  - submissions go to `bing.com/indexnow` first, falling back to `api.indexnow.org` then `yandex.com/indexnow` — the protocol shares a submission with every participating engine, so the first 2xx is enough. **The neutral `api.indexnow.org` endpoint 429s from Cloudflare Workers egress IPs** (measured 2026-08-21: 429 for a single URL with no `Retry-After`, while the identical body from a residential IP got 202) — that's why it isn't first;
  - the key file [`public/ae4368227a78d73327c42c34949e9075.txt`](public/ae4368227a78d73327c42c34949e9075.txt) is how search engines verify the domain. Don't delete or rename it.

  To push immediately after publishing (instead of waiting for the cron), with the admin password:

  ```bash
  curl -X POST "https://oss.omail.us.kg/api/indexnow" -H "Authorization: Bearer $ADMIN_PASSWORD"
  ```

  `GET /api/indexnow` previews what would be submitted without sending anything; add `?force=1` to either to ignore the ledger and resubmit everything.

One-time manual steps (dashboard accounts required): verify the domain in [Google Search Console](https://search.google.com/search-console) and [Bing Webmaster Tools](https://www.bing.com/webmasters), submit `https://oss.omail.us.kg/sitemap.xml` in both, and optionally list `llms.txt` at [directory.llmstxt.cloud](https://directory.llmstxt.cloud) / [llmstxt.site](https://llmstxt.site).

## Develop

From the repo root (pnpm workspace):

```bash
pnpm install
pnpm dev          # turbo dev --filter=web → next dev on http://localhost:3000
```

Or inside `apps/web/`:

```bash
pnpm dev          # Next.js dev server
pnpm preview      # build with OpenNext and preview on the Workers runtime
pnpm cf-typegen   # regenerate cloudflare-env.d.ts after changing wrangler.jsonc
```

## Deploy

```bash
pnpm deploy       # opennextjs-cloudflare build && deploy
```

The official deployment uses the custom domain `oss.omail.us.kg` (configured in `wrangler.jsonc` `routes`). For your own fork: change the `name` and `routes` in [`wrangler.jsonc`](wrangler.jsonc), deploy under your own Cloudflare account, then register `https://<your-domain>/oauth/callback` as the redirect URI of **your own** Cloudflare OAuth client (see [CONTRIBUTING.md](../../CONTRIBUTING.md)).
