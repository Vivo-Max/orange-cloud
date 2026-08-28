/**
 * DNS 应答的四层缓存图：浏览器/系统 → 递归解析器 → 父区委派 → Cloudflare 权威。
 * 上三层都可能拿着旧答案，最底下那层永远是当前值——文章的核心论点。
 * 纯 SVG、无 JS；配色全走主题 token，亮/暗两套都成立；竖版布局，窄屏不溢出。
 */
export default function DnsCacheLayers() {
	const box = (y: number) => ({ x: 30, y, width: 300, height: 54, rx: 14 });

	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 360 400"
				role="img"
				aria-label="Four layers sit between a visitor and the answer to a DNS query. From the top: your browser and operating system, the recursive resolver used by the visitor, the parent zone delegation at the registry, and Cloudflare's authoritative nameservers at the bottom. Only the bottom layer always holds the current record; the three layers above it can all serve a stale answer."
				className="mx-auto block h-auto w-full max-w-[420px]"
			>
				<title>Where a stale DNS answer can be held</title>

				<defs>
					<marker id="dns-arrow" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--oc-orange)" />
					</marker>
				</defs>

				{/* 上三层：任何一层都可能压着旧答案 */}
				<rect
					x="14"
					y="8"
					width="332"
					height="286"
					rx="20"
					fill="none"
					stroke="var(--oc-orange)"
					strokeOpacity="0.35"
					strokeDasharray="4 5"
				/>
				<text x="180" y="28" textAnchor="middle" fontSize="11" fill="var(--t-tertiary)" letterSpacing="0.6">
					CAN HOLD A STALE ANSWER
				</text>

				{/* 1 · 浏览器与系统 */}
				<rect {...box(40)} fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="72" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Your browser and OS
				</text>
				<text x="180" y="88" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					flushable, but only for you
				</text>

				{/* 2 · 递归解析器：绝大多数「没生效」都停在这一层 */}
				<rect {...box(136)} fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="180" y="168" textAnchor="middle" fontSize="14" fontWeight="600" fill="var(--t-primary)">
					Recursive resolver (ISP, 1.1.1.1)
				</text>
				<text x="180" y="184" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					holds the TTL — or a cached NXDOMAIN
				</text>

				{/* 3 · 父区委派 */}
				<rect {...box(234)} fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="266" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Parent zone delegation
				</text>
				<text x="180" y="282" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					NS records; registrar edits can lag
				</text>

				{/* 4 · 权威：唯一永远是当前值的一层 */}
				<rect {...box(334)} fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="180" y="366" textAnchor="middle" fontSize="14" fontWeight="600" fill="var(--t-primary)">
					Cloudflare authoritative nameservers
				</text>
				<text x="180" y="382" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					updated globally within five minutes
				</text>

				<g stroke="var(--oc-orange)" strokeWidth="1.6" markerEnd="url(#dns-arrow)" fill="none">
					<path d="M180 98 L180 130" />
					<path d="M180 194 L180 228" />
					<path d="M180 292 L180 328" />
				</g>
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				A query stops at the first layer that already has an answer — which is why the fix depends on which layer
				is holding it.
			</figcaption>
		</figure>
	);
}
