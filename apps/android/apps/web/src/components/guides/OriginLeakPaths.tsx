/**
 * 源站 IP 的泄露面：橙云代理只覆盖 A/AAAA/CNAME 上的 HTTP/HTTPS 那一层，
 * 其余几条路径在代理之外，各自都会把源站真实 IP 交出去。
 * 纯 SVG、无 JS；配色走主题 token，竖版布局，窄屏不溢出。
 */
export default function OriginLeakPaths() {
	const rows = [
		"灰云子域：mail、ftp、面板",
		"MX 记录的 _dc-mx 应答",
		"SPF / TXT 里写死的 ip4:",
		"接入 Cloudflare 前的历史解析",
		"证书透明度日志里的子域名",
	];

	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 360 462"
				role="img"
				aria-label="橙云代理只覆盖 A、AAAA、CNAME 记录上的 HTTP 与 HTTPS 流量，这一层的 DNS 应答给出的是 Cloudflare 的 anycast IP。而灰云子域、MX 记录的 _dc-mx 应答、SPF 或 TXT 里写死的 ip4 地址、接入 Cloudflare 之前的历史解析记录、证书透明度日志里暴露的子域名这五条路径都在代理之外，任意一条都会把源站真实 IP 交出去。"
				className="mx-auto block h-auto w-full max-w-[420px]"
			>
				<title>源站真实 IP 的五条泄露路径</title>

				{/* 代理覆盖到的那一层 */}
				<text x="30" y="16" fontSize="11" fill="var(--t-tertiary)" letterSpacing="0.6">
					橙云代理覆盖的范围
				</text>
				<rect
					x="30"
					y="28"
					width="300"
					height="62"
					rx="14"
					fill="var(--glass-bg)"
					stroke="var(--oc-orange)"
					strokeOpacity="0.55"
				/>
				<text x="180" y="54" textAnchor="middle" fontSize="14" fontWeight="600" fill="var(--t-primary)">
					A / AAAA / CNAME 上的 HTTP·HTTPS
				</text>
				<text x="180" y="74" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					解析结果是 Cloudflare 的 anycast IP
				</text>

				<line x1="30" y1="110" x2="330" y2="110" stroke="var(--divider)" strokeDasharray="4 5" />

				{/* 代理覆盖不到的五条 */}
				<text x="30" y="134" fontSize="11" fill="var(--t-tertiary)" letterSpacing="0.6">
					代理之外，直接交出源站 IP
				</text>

				<defs>
					<marker id="leak-arrow" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--oc-orange)" />
					</marker>
				</defs>

				{rows.map((label, i) => {
					const y = 148 + i * 48;
					return (
						<g key={label}>
							<rect x="30" y={y} width="250" height="40" rx="12" fill="var(--glass-bg)" stroke="var(--divider)" />
							<text x="44" y={y + 24.5} fontSize="12" fill="var(--t-primary)">
								{label}
							</text>
							<path
								d={`M280 ${y + 20} L306 ${y + 20}`}
								stroke="var(--oc-orange)"
								strokeWidth="1.4"
								strokeOpacity="0.7"
								fill="none"
							/>
						</g>
					);
				})}

				<path
					d="M312 168 L312 390"
					stroke="var(--oc-orange)"
					strokeWidth="1.6"
					fill="none"
					markerEnd="url(#leak-arrow)"
				/>

				<rect
					x="30"
					y="400"
					width="300"
					height="48"
					rx="14"
					fill="var(--glass-bg)"
					stroke="var(--oc-orange)"
					strokeOpacity="0.55"
				/>
				<text x="180" y="429" textAnchor="middle" fontSize="14" fontWeight="600" fill="var(--t-primary)">
					源站真实 IP
				</text>
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				代理挡住的是最显眼的那一条路。只要还有一条路径能问出源站 IP，前面这层就等于没设防。
			</figcaption>
		</figure>
	);
}
