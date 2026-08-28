/**
 * 内网穿透的连接方向图：连接由内网的 cloudflared 主动向外建立，
 * 公网侧从头到尾没有一个入站端口。纯 SVG、无 JS；配色走主题 token，
 * 竖版布局，窄屏不溢出。
 */
export default function TunnelFlow() {
	const box = (y: number) => ({ x: 30, y, width: 300, height: 54, rx: 14 });

	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 360 400"
				role="img"
				aria-label="Cloudflare Tunnel 的连接方向：公网访客先到达 Cloudflare 全球网络，内网里的 cloudflared 主动从 7844 端口向 Cloudflare 建立出站连接，再把请求转给本机上的服务，整个过程不需要公网 IP，也不需要开放任何入站端口。"
				className="mx-auto block h-auto w-full max-w-[420px]"
			>
				<title>Cloudflare Tunnel 的连接方向</title>

				{/* 1 · 公网访客 */}
				<rect {...box(16)} fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="48" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					公网访客
				</text>
				<text x="180" y="64" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					打开 nas.example.com
				</text>

				{/* 2 · Cloudflare 网络 */}
				<rect {...box(112)} fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="180" y="144" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Cloudflare 全球网络
				</text>
				<text x="180" y="160" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					缓存 · WAF · DDoS 防护在这一层生效
				</text>

				{/* 3 · 你的内网 */}
				<rect
					x="14"
					y="196"
					width="332"
					height="196"
					rx="20"
					fill="none"
					stroke="var(--oc-orange)"
					strokeOpacity="0.35"
					strokeDasharray="4 5"
				/>
				<text x="30" y="216" fontSize="11" fill="var(--t-tertiary)" letterSpacing="0.6">
					你的内网 · 没有公网 IP
				</text>

				<rect {...box(228)} fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="180" y="260" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					cloudflared
				</text>
				<text x="180" y="276" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					装在 NAS / 软路由 / 任意一台机器上
				</text>

				<rect {...box(328)} fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="360" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					内网服务
				</text>
				<text x="180" y="376" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					http://localhost:8080
				</text>

				<defs>
					<marker
						id="tunnel-arrow"
						viewBox="0 0 8 8"
						refX="6"
						refY="4"
						markerWidth="6"
						markerHeight="6"
						orient="auto"
					>
						<path d="M0 0 L8 4 L0 8 z" fill="var(--oc-orange)" />
					</marker>
				</defs>
				<g stroke="var(--oc-orange)" strokeWidth="1.6" markerEnd="url(#tunnel-arrow)" fill="none">
					<path d="M180 74 L180 106" />
					{/* 关键的一笔：箭头朝上，连接是从内网往外发起的 */}
					<path d="M180 224 L180 172" />
					<path d="M180 288 L180 322" />
				</g>

				<text x="194" y="186" fontSize="11.5" fill="var(--t-secondary)">
					主动出站建连
				</text>
				<text
					x="194"
					y="202"
					fontSize="11.5"
					fill="var(--t-secondary)"
					fontFamily="ui-monospace, SFMono-Regular, Menlo, monospace"
				>
					TCP/UDP 7844
				</text>
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				连接只有一个方向：从内网往外。防火墙不必放行任何入站流量，也就不需要端口映射、DDNS 或公网 IP。
			</figcaption>
		</figure>
	);
}
