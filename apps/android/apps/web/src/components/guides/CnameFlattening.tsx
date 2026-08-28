/**
 * CNAME flattening 流程图：解析器问 apex → Cloudflare 自己把 CNAME 解到底 →
 * 用 apex 名字回一条 A 记录；代理与仅 DNS 两种答案分叉在底部。
 * 纯 SVG、无 JS；配色全走主题 token，亮/暗两套都成立；竖版布局，窄屏不溢出。
 */
export default function CnameFlattening() {
	const box = (y: number) => ({ x: 26, y, width: 308, height: 54, rx: 14 });

	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 360 470"
				role="img"
				aria-label="How CNAME flattening answers a query. A resolver asks for the A record of example.com. Inside Cloudflare DNS, the CNAME record at the zone apex is kept as configuration and never returned; Cloudflare resolves the target hostname itself down to an IP address, then rewrites the answer so it carries the apex name with record type A. The answer then splits two ways: a proxied record returns Cloudflare anycast IPs with a TTL of 300 seconds, while a DNS-only record returns the target's own IP address with a TTL equal to the lower of the two record TTLs."
				className="mx-auto block h-auto w-full max-w-[420px]"
			>
				<title>How Cloudflare answers a flattened CNAME</title>

				{/* 1 · 解析器提问 */}
				<rect {...box(16)} fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="40" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					A resolver asks
				</text>
				<text
					x="180"
					y="58"
					textAnchor="middle"
					fontSize="12"
					fill="var(--t-secondary)"
					fontFamily="ui-monospace, SFMono-Regular, Menlo, monospace"
				>
					example.com IN A
				</text>

				{/* Cloudflare DNS：flattening 发生的地方 */}
				<rect
					x="12"
					y="100"
					width="336"
					height="272"
					rx="20"
					fill="none"
					stroke="var(--oc-orange)"
					strokeOpacity="0.35"
					strokeDasharray="4 5"
				/>
				<text x="180" y="120" textAnchor="middle" fontSize="11" fill="var(--t-tertiary)" letterSpacing="0.6">
					INSIDE CLOUDFLARE DNS
				</text>

				{/* 2 · apex 上的 CNAME */}
				<rect {...box(132)} fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="180" y="156" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					CNAME at the apex
				</text>
				<text x="180" y="174" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					kept as configuration, never returned
				</text>

				{/* 3 · Cloudflare 自己把目标解到底 */}
				<rect {...box(216)} fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="180" y="240" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Cloudflare resolves the target
				</text>
				<text x="180" y="258" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					one lookup, or several if it chains
				</text>

				{/* 4 · 换成 apex 的名字 */}
				<rect {...box(300)} fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="180" y="324" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Answer rewritten to the apex
				</text>
				<text x="180" y="342" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					type A, name example.com
				</text>

				{/* 5 · 两种答案 */}
				<rect x="22" y="396" width="148" height="58" rx="14" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="96" y="418" textAnchor="middle" fontSize="14" fontWeight="600" fill="var(--t-primary)">
					Proxied
				</text>
				<text x="96" y="434" textAnchor="middle" fontSize="11" fill="var(--t-secondary)">
					Cloudflare anycast IPs
				</text>
				<text x="96" y="448" textAnchor="middle" fontSize="11" fill="var(--t-secondary)">
					TTL 300
				</text>

				<rect x="190" y="396" width="148" height="58" rx="14" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="264" y="418" textAnchor="middle" fontSize="14" fontWeight="600" fill="var(--t-primary)">
					DNS only
				</text>
				<text x="264" y="434" textAnchor="middle" fontSize="11" fill="var(--t-secondary)">
					the target&rsquo;s own IP
				</text>
				<text x="264" y="448" textAnchor="middle" fontSize="11" fill="var(--t-secondary)">
					lower of the two TTLs
				</text>

				<defs>
					<marker id="flat-arrow" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--oc-orange)" />
					</marker>
				</defs>
				<g stroke="var(--oc-orange)" strokeWidth="1.6" markerEnd="url(#flat-arrow)" fill="none">
					<path d="M180 76 L180 126" />
					<path d="M180 190 L180 210" />
					<path d="M180 274 L180 294" />
					<path d="M96 376 L96 390" />
					<path d="M264 376 L264 390" />
				</g>
				<g stroke="var(--oc-orange)" strokeWidth="1.6" fill="none">
					<path d="M180 354 L180 376 M96 376 L264 376" />
				</g>
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				The apex CNAME never reaches the wire. What leaves Cloudflare is an address record carrying your own
				domain name.
			</figcaption>
		</figure>
	);
}
