/**
 * 「purge 能碰到哪一层」示意：浏览器缓存在 purge 触及范围之外，
 * 边缘缓存 / 上层节点 / Cache Reserve 在范围之内，源站永远是新的。
 * 纯 SVG、无 JS；配色全走主题 token，亮/暗两套都成立；竖版布局，窄屏不溢出。
 */
export default function PurgeReach() {
	const box = (y: number) => ({ x: 26, y, width: 308, height: 54, rx: 14 });

	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 360 428"
				role="img"
				aria-label="Where a stale copy of a file can sit. A purge request reaches three layers inside Cloudflare: the edge cache in each data centre, the upper tier used by Tiered Cache, and Cache Reserve. It does not reach the copy stored in a visitor's browser, which expires on its own browser cache TTL. The origin server below always holds the current file."
				className="mx-auto block h-auto w-full max-w-[420px]"
			>
				<title>What a Cloudflare cache purge reaches</title>

				{/* 浏览器：purge 够不到的一层 */}
				<rect {...box(18)} fill="var(--glass-bg)" stroke="var(--divider)" strokeDasharray="5 4" />
				<text x="180" y="42" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Visitor&rsquo;s browser
				</text>
				<text x="180" y="60" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					out of reach &mdash; expires on browser TTL
				</text>

				{/* Cloudflare 网络：purge 覆盖的三层 */}
				<rect
					x="12"
					y="100"
					width="336"
					height="222"
					rx="20"
					fill="none"
					stroke="var(--oc-orange)"
					strokeOpacity="0.35"
					strokeDasharray="4 5"
				/>
				<text x="180" y="120" textAnchor="middle" fontSize="11" fill="var(--t-tertiary)" letterSpacing="0.6">
					WHAT A PURGE CLEARS
				</text>

				<rect {...box(132)} fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="180" y="156" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Edge cache
				</text>
				<text x="180" y="174" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					every data centre, all at once
				</text>

				<rect {...box(194)} fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="180" y="218" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Upper tier
				</text>
				<text x="180" y="236" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					only if Tiered Cache is on
				</text>

				<rect {...box(256)} fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="180" y="280" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Cache Reserve
				</text>
				<text x="180" y="298" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					deleted by URL purge, revalidated by the rest
				</text>

				{/* 源站 */}
				<rect {...box(348)} fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="372" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Your origin server
				</text>
				<text x="180" y="390" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					always has the current file
				</text>

				<defs>
					<marker id="purge-arrow" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--oc-orange)" />
					</marker>
				</defs>
				<g stroke="var(--oc-orange)" strokeWidth="1.6" markerEnd="url(#purge-arrow)" fill="none">
					<path d="M180 322 L180 342" />
				</g>
				<text x="192" y="334" fontSize="11" fill="var(--t-secondary)">
					refetched on next request
				</text>
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				A purge empties Cloudflare&rsquo;s copies. The copy already sitting in a browser is not Cloudflare&rsquo;s
				to remove.
			</figcaption>
		</figure>
	);
}
