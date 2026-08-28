/**
 * O2O 请求流转图：访客 → 客户 zone → SaaS 提供方 zone → 提供方源站。
 * 纯 SVG、无 JS；配色全走主题 token，亮/暗两套都成立；竖版布局，窄屏不溢出。
 */
export default function O2OFlow() {
	const box = (y: number) => ({ x: 30, y, width: 300, height: 54, rx: 14 });

	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 360 408"
				role="img"
				aria-label="Request flow in an Orange-to-Orange setup: a visitor request reaches the customer's Cloudflare zone first, is routed inside Cloudflare to the SaaS provider's zone with the cf-connecting-o2o header set, and is then sent to the SaaS provider's origin server."
				className="mx-auto block h-auto w-full max-w-[420px]"
			>
				<title>Orange-to-Orange request flow</title>

				{/* Cloudflare 网络：两个 zone 在同一张网内，请求不出网再回来 */}
				<rect
					x="14"
					y="98"
					width="332"
					height="212"
					rx="20"
					fill="none"
					stroke="var(--oc-orange)"
					strokeOpacity="0.35"
					strokeDasharray="4 5"
				/>
				<text x="180" y="118" textAnchor="middle" fontSize="11" fill="var(--t-tertiary)" letterSpacing="0.6">
					INSIDE CLOUDFLARE
				</text>

				{/* 1 · 访客 */}
				<rect {...box(20)} fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="52" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Website visitor
				</text>
				<text x="180" y="68" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					resolves shop.example.com
				</text>

				{/* 2 · 客户 zone */}
				<rect {...box(130)} fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="180" y="162" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Your zone (example.com)
				</text>
				<text x="180" y="178" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					your settings apply first
				</text>

				{/* 3 · SaaS 提供方 zone */}
				<rect {...box(232)} fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="180" y="264" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					SaaS provider zone
				</text>
				<text x="180" y="280" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					provider settings apply second
				</text>

				{/* 4 · 提供方源站 */}
				<rect {...box(342)} fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="374" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					SaaS provider origin
				</text>
				<text x="180" y="390" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					the platform that serves your store
				</text>

				<defs>
					<marker id="o2o-arrow" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--oc-orange)" />
					</marker>
				</defs>
				<g stroke="var(--oc-orange)" strokeWidth="1.6" markerEnd="url(#o2o-arrow)" fill="none">
					<path d="M180 74 L180 124" />
					<path d="M180 184 L180 226" />
					<path d="M180 286 L180 336" />
				</g>

				{/* 进入提供方 zone 时被打上的标记头 */}
				<text x="188" y="212" fontSize="11.5" fill="var(--t-secondary)" fontFamily="ui-monospace, SFMono-Regular, Menlo, monospace">
					cf-connecting-o2o: 1
				</text>
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				Both hops happen inside Cloudflare’s network — the request does not leave and come back.
			</figcaption>
		</figure>
	);
}
