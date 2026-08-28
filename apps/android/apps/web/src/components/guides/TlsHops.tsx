/**
 * SSL/TLS 加密模式作用范围图：访客 → Cloudflare → 源站两段连接，
 * 加密模式只管第二段。纯 SVG、无 JS；配色全走主题 token，亮/暗两套都成立；
 * 竖版布局，窄屏不溢出。
 */
export default function TlsHops() {
	const box = (y: number) => ({ x: 20, y, width: 320, height: 52, rx: 14 });

	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 360 384"
				role="img"
				aria-label="A request passes over two separate connections: connection one runs from the visitor to Cloudflare and is secured by the edge certificate, and connection two runs from Cloudflare to the origin server. The SSL/TLS encryption mode setting controls only the second connection."
				className="mx-auto block h-auto w-full max-w-[420px]"
			>
				<title>What the SSL/TLS encryption mode controls</title>

				<defs>
					<marker id="tls-arrow" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--oc-orange)" />
					</marker>
				</defs>

				{/* 1 · 访客 */}
				<rect {...box(14)} fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="46" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Website visitor
				</text>

				{/* 第一段连接：边缘证书 */}
				<rect x="44" y="92" width="272" height="44" rx="12" fill="none" stroke="var(--divider)" />
				<text x="180" y="110" textAnchor="middle" fontSize="12.5" fontWeight="600" fill="var(--t-primary)">
					Connection 1
				</text>
				<text x="180" y="126" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					secured by the edge certificate
				</text>

				{/* 2 · Cloudflare */}
				<rect {...box(162)} fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="180" y="194" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Cloudflare
				</text>

				{/* 第二段连接：加密模式唯一作用的地方 */}
				<rect
					x="44"
					y="240"
					width="272"
					height="52"
					rx="12"
					fill="none"
					stroke="var(--oc-orange)"
					strokeOpacity="0.6"
					strokeDasharray="4 5"
				/>
				<text x="180" y="261" textAnchor="middle" fontSize="12.5" fontWeight="600" fill="var(--t-primary)">
					Connection 2
				</text>
				<text x="180" y="278" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					the encryption mode controls this hop
				</text>

				{/* 3 · 源站 */}
				<rect {...box(318)} fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="350" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Your origin server
				</text>

				<g stroke="var(--oc-orange)" strokeWidth="1.6" markerEnd="url(#tls-arrow)" fill="none">
					<path d="M180 70 L180 86" />
					<path d="M180 140 L180 156" />
					<path d="M180 218 L180 234" />
					<path d="M180 296 L180 312" />
				</g>
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				A padlock in the browser only proves connection 1 is encrypted. It says nothing about connection 2.
			</figcaption>
		</figure>
	);
}
