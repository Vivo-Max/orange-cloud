/**
 * Error 1000 的成因图：请求进了 Cloudflare 代理，代理去取源站地址，
 * 却发现该地址还是指回 Cloudflare —— 于是在闭环形成前中止，返回 1000。
 * 纯 SVG、无 JS；配色全走主题 token，亮/暗两套都成立；竖版布局，窄屏不溢出。
 */
export default function Error1000Loop() {
	const box = (y: number) => ({ x: 20, y, width: 290, height: 56, rx: 14 });

	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 400 386"
				role="img"
				aria-label="How Cloudflare error 1000 happens: a visitor request reaches the Cloudflare proxy, the proxy looks up the origin address for the hostname and finds a Cloudflare address, which would send the request back into the proxy it just came from, so Cloudflare halts the request and returns error 1000."
				className="mx-auto block h-auto w-full max-w-[440px]"
			>
				<title>Why Cloudflare returns error 1000</title>

				{/* 1 · 访客 */}
				<rect {...box(16)} fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="165" y="41" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Visitor
				</text>
				<text x="165" y="58" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					requests www.example.com
				</text>

				{/* 2 · Cloudflare 代理 */}
				<rect {...box(118)} fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="165" y="143" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Cloudflare proxy
				</text>
				<text x="165" y="160" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					needs an origin address to forward to
				</text>

				{/* 3 · 查到的源站地址仍在 Cloudflare 内 */}
				<rect {...box(220)} fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="165" y="245" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Origin address
				</text>
				<text x="165" y="262" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					resolves back to Cloudflare
				</text>

				<defs>
					<marker id="e1000-arrow" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--oc-orange)" />
					</marker>
				</defs>

				{/* 正向流转 */}
				<g stroke="var(--oc-orange)" strokeWidth="1.6" markerEnd="url(#e1000-arrow)" fill="none">
					<path d="M165 74 L165 112" />
					<path d="M165 176 L165 214" />
				</g>

				{/* 闭环：源站地址把请求送回它刚离开的代理 */}
				<path
					d="M310 248 C 366 248, 366 146, 316 146"
					fill="none"
					stroke="var(--oc-orange)"
					strokeWidth="1.6"
					strokeDasharray="4 5"
					markerEnd="url(#e1000-arrow)"
				/>
				<text x="330" y="200" textAnchor="middle" fontSize="11" fill="var(--t-tertiary)">
					<tspan x="330" dy="0">loop</tspan>
				</text>

				{/* 中止 */}
				<path d="M165 280 L165 306" stroke="var(--oc-orange)" strokeWidth="1.6" markerEnd="url(#e1000-arrow)" fill="none" />
				<rect
					x="20"
					y="312"
					width="290"
					height="56"
					rx="14"
					fill="none"
					stroke="var(--oc-orange)"
					strokeOpacity="0.7"
					strokeDasharray="5 4"
				/>
				<text x="165" y="337" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Request halted
				</text>
				<text x="165" y="354" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					Error 1000: DNS points to prohibited IP
				</text>
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				Cloudflare refuses to forward a request to itself, so it stops before the loop closes.
			</figcaption>
		</figure>
	);
}
