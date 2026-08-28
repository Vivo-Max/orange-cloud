/**
 * 「源站到底看到了什么」的解剖图：TCP 层的来源地址是 Cloudflare 的节点，
 * 访客真实地址在 HTTP 头里；下半部分是那条决定这些头可不可信的边界。
 * 纯 SVG、无 JS；配色走主题 token，竖版布局，窄屏不溢出。
 */
export default function RealIpTrust() {
	const mono = "ui-monospace, SFMono-Regular, Menlo, monospace";

	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 360 424"
				role="img"
				aria-label="一次经过 Cloudflare 代理的请求到达源站时，源站在 TCP 层看到的来源地址是 Cloudflare 节点的 IP，访客的真实地址放在 CF-Connecting-IP 与 X-Forwarded-For 这两个 HTTP 请求头里；只有当这条连接确实来自 Cloudflare 的 IP 段时，这些头才可信，否则应当被拒绝。"
				className="mx-auto block h-auto w-full max-w-[420px]"
			>
				<title>源站收到一次代理请求时看到的内容，以及信任边界</title>

				{/* 1 · TCP 层看到的来源 */}
				<rect x="30" y="16" width="300" height="70" rx="14" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="46" y="40" fontSize="11.5" fill="var(--t-tertiary)" letterSpacing="0.6">
					TCP 层 · 连接来源
				</text>
				<text x="46" y="60" fontSize="14" fontFamily={mono} fill="var(--t-primary)">
					104.16.0.1
				</text>
				<text x="46" y="77" fontSize="12" fill="var(--t-secondary)">
					Cloudflare 节点，不是访客
				</text>

				{/* 2 · HTTP 头里的真实地址 */}
				<rect
					x="30"
					y="102"
					width="300"
					height="112"
					rx="14"
					fill="var(--glass-bg)"
					stroke="var(--oc-orange)"
					strokeOpacity="0.55"
				/>
				<text x="46" y="126" fontSize="11.5" fill="var(--t-tertiary)" letterSpacing="0.6">
					HTTP 头 · 访客真实地址在这里
				</text>
				<text x="46" y="149" fontSize="12" fontFamily={mono} fill="var(--t-primary)">
					CF-Connecting-IP: 203.0.113.9
				</text>
				<text x="46" y="170" fontSize="12" fontFamily={mono} fill="var(--t-primary)">
					X-Forwarded-For: 203.0.113.9
				</text>
				<text x="46" y="191" fontSize="12" fontFamily={mono} fill="var(--t-secondary)">
					CF-IPCountry: CN
				</text>

				{/* 3 · 信任边界 */}
				<line
					x1="14"
					y1="248"
					x2="346"
					y2="248"
					stroke="var(--oc-orange)"
					strokeOpacity="0.5"
					strokeDasharray="4 5"
				/>
				<text x="14" y="240" fontSize="11.5" fill="var(--t-tertiary)" letterSpacing="0.6">
					信任边界：先判断来源，再决定信不信这些头
				</text>

				{/* 4 · 两种判定 */}
				<rect x="30" y="266" width="300" height="66" rx="14" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="46" y="292" fontSize="13.5" fontWeight="600" fill="var(--t-primary)">
					来源属于 Cloudflare IP 段
				</text>
				<text x="46" y="313" fontSize="12" fill="var(--t-secondary)">
					取 CF-Connecting-IP 当访客地址
				</text>
				<text x="300" y="299" fontSize="19" textAnchor="middle" fill="var(--oc-orange)">
					✓
				</text>

				<rect x="30" y="344" width="300" height="66" rx="14" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="46" y="370" fontSize="13.5" fontWeight="600" fill="var(--t-primary)">
					来源是其它任意地址
				</text>
				<text x="46" y="391" fontSize="12" fill="var(--t-secondary)">
					头可能是伪造的，一律不采信
				</text>
				<text x="300" y="377" fontSize="19" textAnchor="middle" fill="var(--t-tertiary)">
					✕
				</text>

				<defs>
					<marker id="realip-arrow" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--oc-orange)" />
					</marker>
				</defs>
				<g stroke="var(--oc-orange)" strokeWidth="1.6" markerEnd="url(#realip-arrow)" fill="none">
					<path d="M180 88 L180 98" />
				</g>
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				真实地址一直都在，只是从连接层挪到了请求头里。头本身谁都能写，所以取用之前必须先确认这条连接来自 Cloudflare。
			</figcaption>
		</figure>
	);
}
