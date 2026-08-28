/**
 * 缓存判定图：请求先过「请求时是否可缓存」（否 → DYNAMIC），
 * 再过「响应本身能不能存」（否 → BYPASS），最后才查缓存（MISS / HIT）。
 * 纯 SVG、无 JS；配色全走主题 token，亮/暗两套都成立；竖版布局，窄屏不溢出。
 */
export default function CacheDecision() {
	const decision = (y: number) => ({ x: 8, y, width: 214, height: 62, rx: 14 });
	const pill = (y: number) => ({ x: 252, y, width: 100, height: 40, rx: 12 });

	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 360 412"
				role="img"
				aria-label="Cloudflare makes three checks in order. First, is the request eligible for cache at request time? If not, the status is DYNAMIC. Second, is the origin response cacheable? If not, the status is BYPASS. Third, is the asset already in this data center's cache? If not, the status is MISS; if it is, the status is HIT."
				className="mx-auto block h-auto w-full max-w-[420px]"
			>
				<title>How Cloudflare arrives at each cf-cache-status value</title>

				<defs>
					<marker id="cache-arrow" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--oc-orange)" />
					</marker>
				</defs>

				{/* 请求入口 */}
				<rect x="8" y="10" width="214" height="44" rx="14" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="115" y="37" textAnchor="middle" fontSize="14" fontWeight="600" fill="var(--t-primary)">
					Request reaches Cloudflare
				</text>

				{/* 判定一 · 请求时是否可缓存 */}
				<rect {...decision(84)} fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="115" y="110" textAnchor="middle" fontSize="13" fill="var(--t-primary)">
					Eligible for cache
				</text>
				<text x="115" y="128" textAnchor="middle" fontSize="13" fill="var(--t-primary)">
					at request time?
				</text>
				<rect {...pill(95)} fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="302" y="120" textAnchor="middle" fontSize="13" fontWeight="700" fill="var(--t-primary)">
					DYNAMIC
				</text>

				{/* 判定二 · 响应本身能不能存 */}
				<rect {...decision(176)} fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="115" y="202" textAnchor="middle" fontSize="13" fill="var(--t-primary)">
					Origin response
				</text>
				<text x="115" y="220" textAnchor="middle" fontSize="13" fill="var(--t-primary)">
					cacheable?
				</text>
				<rect {...pill(187)} fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="302" y="212" textAnchor="middle" fontSize="13" fontWeight="700" fill="var(--t-primary)">
					BYPASS
				</text>

				{/* 判定三 · 缓存里有没有 */}
				<rect {...decision(268)} fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="115" y="294" textAnchor="middle" fontSize="13" fill="var(--t-primary)">
					Already in this
				</text>
				<text x="115" y="312" textAnchor="middle" fontSize="13" fill="var(--t-primary)">
					data center&#39;s cache?
				</text>
				<rect {...pill(279)} fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="302" y="304" textAnchor="middle" fontSize="13" fontWeight="700" fill="var(--t-primary)">
					MISS
				</text>

				{/* 命中 */}
				<rect x="8" y="360" width="214" height="44" rx="14" fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="115" y="387" textAnchor="middle" fontSize="14" fontWeight="700" fill="var(--t-primary)">
					HIT
				</text>

				<g stroke="var(--oc-orange)" strokeWidth="1.6" markerEnd="url(#cache-arrow)" fill="none">
					<path d="M115 54 L115 80" />
					<path d="M115 146 L115 172" />
					<path d="M115 238 L115 264" />
					<path d="M115 330 L115 356" />
					<path d="M222 115 L248 115" />
					<path d="M222 207 L248 207" />
					<path d="M222 299 L248 299" />
				</g>

				<g fontSize="10.5" fill="var(--t-tertiary)">
					<text x="124" y="164">yes</text>
					<text x="124" y="256">yes</text>
					<text x="124" y="348">yes</text>
					<text x="235" y="108" textAnchor="middle">no</text>
					<text x="235" y="200" textAnchor="middle">no</text>
					<text x="235" y="292" textAnchor="middle">no</text>
				</g>
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				DYNAMIC and BYPASS look alike in a browser, but they are decided at different moments and have
				different fixes.
			</figcaption>
		</figure>
	);
}
