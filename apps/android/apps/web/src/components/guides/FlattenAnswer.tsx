/**
 * CNAME 展平后返回的是什么：Cloudflare 在权威侧替你把 CNAME 解析成 IP，
 * 返回的答案形态再按这条记录的代理状态分成两支（橙云 = 任播 IP + TTL 300，
 * 灰云 = 源站 IP + TTL 取较小值）。纯 SVG、无 JS；配色走主题 token，
 * 竖版布局，窄屏不溢出。
 */
export default function FlattenAnswer() {
	const mono = "ui-monospace, SFMono-Regular, Menlo, monospace";

	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 360 496"
				role="img"
				aria-label="CNAME 展平的过程：访客的解析器向 Cloudflare 查询 example.com 的 A 记录，Cloudflare 发现根域名上是一条 CNAME，强制展平，替你解析目标主机名拿到 192.0.2.1，再把答案的记录名改写回 example.com 返回。返回的形态取决于代理状态：开着代理时返回多条 Cloudflare 任播 IP、TTL 固定为 300；关着代理时返回源站 IP、TTL 取 CNAME 记录与目标记录里较小的那个。"
				className="mx-auto block h-auto w-full max-w-[420px]"
			>
				<title>CNAME 展平返回的是什么</title>

				{/* 1 · 访客的解析器 */}
				<rect x="30" y="14" width="300" height="52" rx="14" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="40" textAnchor="middle" fontSize="14.5" fontWeight="600" fill="var(--t-primary)">
					访客的解析器
				</text>
				<text x="180" y="57" textAnchor="middle" fontSize="11.5" fill="var(--t-secondary)" fontFamily={mono}>
					example.com IN A ?
				</text>

				{/* 2 · Cloudflare 权威侧 */}
				<rect
					x="14"
					y="96"
					width="332"
					height="152"
					rx="20"
					fill="none"
					stroke="var(--oc-orange)"
					strokeOpacity="0.35"
					strokeDasharray="4 5"
				/>
				<text x="30" y="116" fontSize="11" fill="var(--t-tertiary)" letterSpacing="0.6">
					Cloudflare 权威 DNS
				</text>

				<rect x="30" y="126" width="300" height="46" rx="12" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="154" textAnchor="middle" fontSize="12.5" fill="var(--t-primary)">
					根域名上是一条 CNAME，强制展平
				</text>

				<rect
					x="30"
					y="192"
					width="300"
					height="46"
					rx="12"
					fill="var(--glass-bg)"
					stroke="var(--oc-orange)"
					strokeOpacity="0.55"
				/>
				<text x="180" y="213" textAnchor="middle" fontSize="12.5" fill="var(--t-primary)">
					替你去解析目标主机名
				</text>
				<text x="180" y="229" textAnchor="middle" fontSize="11" fill="var(--t-secondary)" fontFamily={mono}>
					cdn.example.net → 192.0.2.1
				</text>

				{/* 3 · 两种答案 */}
				<text x="180" y="296" textAnchor="middle" fontSize="11.5" fill="var(--t-secondary)">
					答案的形态，取决于这条记录的代理状态
				</text>

				<rect
					x="30"
					y="308"
					width="300"
					height="76"
					rx="14"
					fill="var(--glass-bg)"
					stroke="var(--oc-orange)"
					strokeOpacity="0.55"
				/>
				<text x="180" y="331" textAnchor="middle" fontSize="13" fontWeight="600" fill="var(--t-primary)">
					代理开着 · 小黄云
				</text>
				<text x="180" y="351" textAnchor="middle" fontSize="10.5" fill="var(--t-secondary)" fontFamily={mono}>
					example.com 300 IN A [Cloudflare IP]
				</text>
				<text x="180" y="370" textAnchor="middle" fontSize="11" fill="var(--t-tertiary)">
					多条任播 IP，TTL 固定 300
				</text>

				<rect x="30" y="398" width="300" height="76" rx="14" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="421" textAnchor="middle" fontSize="13" fontWeight="600" fill="var(--t-primary)">
					代理关着 · 灰云
				</text>
				<text x="180" y="441" textAnchor="middle" fontSize="10.5" fill="var(--t-secondary)" fontFamily={mono}>
					example.com 3600 IN A 192.0.2.1
				</text>
				<text x="180" y="460" textAnchor="middle" fontSize="11" fill="var(--t-tertiary)">
					源站 IP，TTL 取两者中较小的那个
				</text>

				<defs>
					<marker id="flat-arrow" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--oc-orange)" />
					</marker>
				</defs>
				<g stroke="var(--oc-orange)" strokeWidth="1.6" markerEnd="url(#flat-arrow)" fill="none">
					<path d="M180 70 L180 118" />
					<path d="M180 174 L180 188" />
					<path d="M180 250 L180 276" />
				</g>
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				查询走到 Cloudflare 就停了：那一次对目标主机名的解析发生在 Cloudflare 一侧，访客的解析器从头到尾只拿到一条 A 记录。
			</figcaption>
		</figure>
	);
}
