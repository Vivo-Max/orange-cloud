/**
 * 小黄云开与关的对照图：同一条记录，代理关掉时 DNS 直接把源站 IP 交出去，
 * 打开后应答换成 Cloudflare 任播 IP、流量多走一跳边缘节点。
 * 纯 SVG、无 JS；配色走主题 token，竖版布局，窄屏不溢出。
 */
export default function ProxyToggle() {
	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 360 424"
				role="img"
				aria-label="小黄云关掉时（灰云、DNS only），域名解析返回源站真实 IP 203.0.113.10，访客直连源站；小黄云打开时（已代理），域名解析返回 Cloudflare 的任播 IP，访客先到达 Cloudflare 边缘节点，缓存、WAF、DDoS 防护和 SSL 都在这一层生效，再由边缘节点回源到 203.0.113.10。"
				className="mx-auto block h-auto w-full max-w-[420px]"
			>
				<title>小黄云开与关，链路差别在哪</title>

				<defs>
					<marker
						id="proxy-arrow"
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

				{/* —— 上半：灰云 —— */}
				<rect x="8" y="6" width="344" height="156" rx="18" fill="none" stroke="var(--divider)" />
				<text x="26" y="30" fontSize="12.5" fill="var(--t-tertiary)" letterSpacing="0.4">
					灰云 · DNS only · 解析出 203.0.113.10
				</text>

				<rect x="40" y="44" width="280" height="40" rx="12" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="69" textAnchor="middle" fontSize="14" fontWeight="600" fill="var(--t-primary)">
					访客
				</text>

				<rect x="40" y="112" width="280" height="40" rx="12" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="137" textAnchor="middle" fontSize="14" fontWeight="600" fill="var(--t-primary)">
					源站 203.0.113.10
				</text>

				<g stroke="var(--oc-orange)" strokeWidth="1.6" markerEnd="url(#proxy-arrow)" fill="none">
					<path d="M180 88 L180 106" />
				</g>
				<text x="192" y="103" fontSize="11.5" fill="var(--t-secondary)">
					直连
				</text>

				{/* —— 下半：小黄云 —— */}
				<rect x="8" y="180" width="344" height="236" rx="18" fill="none" stroke="var(--oc-orange)" strokeOpacity="0.35" />
				<text x="26" y="204" fontSize="12.5" fill="var(--t-tertiary)" letterSpacing="0.4">
					小黄云 · 已代理 · 解析出 Cloudflare 任播 IP
				</text>

				<rect x="40" y="218" width="280" height="40" rx="12" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="243" textAnchor="middle" fontSize="14" fontWeight="600" fill="var(--t-primary)">
					访客
				</text>

				<rect
					x="40"
					y="286"
					width="280"
					height="52"
					rx="14"
					fill="var(--glass-bg)"
					stroke="var(--oc-orange)"
					strokeOpacity="0.55"
				/>
				<text x="180" y="308" textAnchor="middle" fontSize="14" fontWeight="600" fill="var(--t-primary)">
					Cloudflare 边缘节点
				</text>
				<text x="180" y="326" textAnchor="middle" fontSize="11.5" fill="var(--t-secondary)">
					缓存 · WAF · DDoS 防护 · SSL 都在这层
				</text>

				<rect x="40" y="366" width="280" height="40" rx="12" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="391" textAnchor="middle" fontSize="14" fontWeight="600" fill="var(--t-primary)">
					源站 203.0.113.10
				</text>

				<g stroke="var(--oc-orange)" strokeWidth="1.6" markerEnd="url(#proxy-arrow)" fill="none">
					<path d="M180 262 L180 280" />
					<path d="M180 342 L180 360" />
				</g>
				<text x="192" y="277" fontSize="11.5" fill="var(--t-secondary)">
					走任播 IP
				</text>
				<text x="192" y="357" fontSize="11.5" fill="var(--t-secondary)">
					回源
				</text>
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				开关只动一件事：DNS 应答里给的是谁的地址。地址换成 Cloudflare 的，流量才有可能被它接住。
			</figcaption>
		</figure>
	);
}
