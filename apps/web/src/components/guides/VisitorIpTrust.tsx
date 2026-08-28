/**
 * 「访客真实 IP 在哪、什么时候可信」——英文版指南用图。
 * 上半部分：一次代理请求到达源站时，TCP 对端是 Cloudflare 节点，访客地址在请求头里；
 * 下半部分：这些头唯一的可信条件是连接本身来自 Cloudflare 已发布的 IP 段。
 * 纯 SVG、无 JS；配色全走主题 token，亮/暗两套都成立；竖版布局，窄屏靠整体缩放不溢出。
 */
export default function VisitorIpTrust() {
	const mono = "ui-monospace, SFMono-Regular, Menlo, monospace";

	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 380 400"
				role="img"
				aria-label="When a proxied request reaches your origin server, the TCP peer address belongs to Cloudflare, while the visitor's own address travels in the CF-Connecting-IP request header. Your web server then has to decide whether to believe that header: if the connection came from one of Cloudflare's published IP ranges, the header is the visitor and can be logged; if the connection came from anywhere else, the header was set by whoever opened the connection and must be discarded."
				className="mx-auto block h-auto w-full max-w-[440px]"
			>
				<title>Where the visitor IP travels, and the one condition that makes it trustworthy</title>

				<defs>
					<marker id="vip-arrow" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--oc-orange)" />
					</marker>
					<marker id="vip-arrow-quiet" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--t-tertiary)" />
					</marker>
				</defs>

				{/* 1 · 源站实际收到的东西 */}
				<rect x="24" y="12" width="332" height="98" rx="14" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="44" y="36" fontSize="11" letterSpacing="0.7" fill="var(--t-tertiary)">
					ONE PROXIED REQUEST, AT YOUR ORIGIN
				</text>
				<text x="44" y="61" fontSize="13" fontFamily={mono} fill="var(--t-secondary)">
					172.71.0.1
				</text>
				<text x="140" y="61" fontSize="12" fill="var(--t-tertiary)">
					TCP peer — a Cloudflare address
				</text>
				<text x="44" y="88" fontSize="13" fontFamily={mono} fill="var(--t-primary)">
					203.0.113.7
				</text>
				<text x="140" y="88" fontSize="12" fill="var(--t-secondary)">
					cf-connecting-ip — the visitor
				</text>

				<path d="M190 110 L190 132" stroke="var(--oc-orange)" strokeWidth="1.6" fill="none" markerEnd="url(#vip-arrow)" />

				{/* 2 · 唯一的判断 */}
				<rect
					x="24"
					y="138"
					width="332"
					height="58"
					rx="14"
					fill="none"
					stroke="var(--oc-orange)"
					strokeOpacity="0.6"
					strokeDasharray="5 4"
				/>
				<text x="190" y="163" textAnchor="middle" fontSize="13.5" fontWeight="600" fill="var(--t-primary)">
					Did this connection arrive from
				</text>
				<text x="190" y="182" textAnchor="middle" fontSize="13.5" fontWeight="600" fill="var(--t-primary)">
					a published Cloudflare IP range?
				</text>

				{/* 3 · 分叉：从判断框底部下探，转向左侧脊线 */}
				<path d="M190 196 L190 210 L44 210 L44 336" stroke="var(--divider)" strokeWidth="1.4" fill="none" />

				<path d="M44 246 L74 246" stroke="var(--oc-orange)" strokeWidth="1.6" fill="none" markerEnd="url(#vip-arrow)" />
				<rect x="82" y="222" width="274" height="62" rx="12" fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.5" />
				<text x="98" y="245" fontSize="12" fontWeight="600" fill="var(--oc-orange)">
					Yes
				</text>
				<text x="98" y="265" fontSize="12.5" fill="var(--t-secondary)">
					The header is the visitor. Log it,
				</text>
				<text x="98" y="280" fontSize="12.5" fill="var(--t-secondary)">
					rate limit on it, geolocate it.
				</text>

				<path d="M44 336 L74 336" stroke="var(--divider)" strokeWidth="1.6" fill="none" markerEnd="url(#vip-arrow-quiet)" />
				<rect x="82" y="312" width="274" height="62" rx="12" fill="none" stroke="var(--divider)" strokeDasharray="5 4" />
				<text x="98" y="335" fontSize="12" fontWeight="600" fill="var(--t-tertiary)">
					No
				</text>
				<text x="98" y="355" fontSize="12.5" fill="var(--t-tertiary)">
					Whoever opened the connection
				</text>
				<text x="98" y="370" fontSize="12.5" fill="var(--t-tertiary)">
					chose that value. Discard it.
				</text>
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				A request header is only as trustworthy as the connection that carried it, which is why every real-IP
				module asks you for a list of trusted proxies.
			</figcaption>
		</figure>
	);
}
