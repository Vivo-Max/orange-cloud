/**
 * 「你改的那份记录，到底算不算数」的判定图：
 * 一次查询的答案由父区委派决定——委派指向旧服务商，Cloudflare 面板里改什么都不影响结果；
 * 只有委派指向 Cloudflare、且域名已激活，改动才是权威答案。
 * 纯 SVG、无 JS；配色全走主题 token，亮/暗两套都成立；竖版布局，窄屏不溢出。
 */
export default function DnsAuthorityBranch() {
	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 360 430"
				role="img"
				aria-label="判定一次 DNS 查询的答案由谁给出：解析器先向父区（注册局）问委派，父区返回的 NS 指向哪家，答案就由哪家的权威服务器给出。委派仍指向旧 DNS 服务商时，在 Cloudflare 面板里改的记录不参与解析；委派已指向 Cloudflare 且域名状态为 Active 时，改动才是权威答案。"
				className="mx-auto block h-auto w-full max-w-[420px]"
			>
				<title>你改的记录算不算权威答案</title>

				<defs>
					<marker id="auth-arrow" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--oc-orange)" />
					</marker>
					<marker id="auth-arrow-dim" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--t-tertiary)" />
					</marker>
				</defs>

				{/* 1 · 解析器发问 */}
				<rect x="60" y="10" width="240" height="50" rx="14" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="34" textAnchor="middle" fontSize="14.5" fontWeight="600" fill="var(--t-primary)">
					访客的解析器
				</text>
				<text x="180" y="50" textAnchor="middle" fontSize="11.5" fill="var(--t-secondary)">
					手上没有缓存，向上追问
				</text>

				{/* 2 · 父区委派：决定谁有资格回答 */}
				<rect
					x="45"
					y="96"
					width="270"
					height="54"
					rx="14"
					fill="var(--glass-bg)"
					stroke="var(--oc-orange)"
					strokeOpacity="0.55"
				/>
				<text x="180" y="120" textAnchor="middle" fontSize="14.5" fontWeight="600" fill="var(--t-primary)">
					父区（.com 等注册局）
				</text>
				<text x="180" y="137" textAnchor="middle" fontSize="11.5" fill="var(--t-secondary)">
					这里的 NS 决定了谁有资格回答
				</text>

				<g strokeWidth="1.6" fill="none">
					<path d="M180 62 L180 92" stroke="var(--oc-orange)" markerEnd="url(#auth-arrow)" />
					<path d="M120 152 L120 176 L96 176 L96 200" stroke="var(--t-tertiary)" markerEnd="url(#auth-arrow-dim)" />
					<path d="M240 152 L240 176 L264 176 L264 200" stroke="var(--oc-orange)" markerEnd="url(#auth-arrow)" />
				</g>

				{/* 左路 · 委派还在旧服务商：面板改动进不了解析链路 */}
				<rect
					x="14"
					y="204"
					width="164"
					height="76"
					rx="14"
					fill="var(--glass-bg)"
					stroke="var(--divider)"
					strokeDasharray="4 5"
				/>
				<text x="96" y="228" textAnchor="middle" fontSize="13" fontWeight="600" fill="var(--t-primary)">
					指向旧 DNS 服务商
				</text>
				<text x="96" y="247" textAnchor="middle" fontSize="11.5" fill="var(--t-secondary)">
					答案由旧服务商给出
				</text>
				<text x="96" y="264" textAnchor="middle" fontSize="11.5" fill="var(--t-secondary)">
					面板改动不参与解析
				</text>

				{/* 右路 · 委派已指向 Cloudflare */}
				<rect
					x="182"
					y="204"
					width="164"
					height="76"
					rx="14"
					fill="var(--glass-bg)"
					stroke="var(--oc-orange)"
					strokeOpacity="0.55"
				/>
				<text x="264" y="228" textAnchor="middle" fontSize="13" fontWeight="600" fill="var(--t-primary)">
					指向 Cloudflare
				</text>
				<text x="264" y="247" textAnchor="middle" fontSize="11.5" fill="var(--t-secondary)">
					答案由 Cloudflare 给出
				</text>
				<text x="264" y="264" textAnchor="middle" fontSize="11.5" fill="var(--t-secondary)">
					状态须已到 Active
				</text>

				{/* 3 · 只有走右路，才轮得到缓存这一段 */}
				<path d="M264 282 L264 316" stroke="var(--oc-orange)" strokeWidth="1.6" fill="none" markerEnd="url(#auth-arrow)" />
				<rect x="45" y="320" width="270" height="54" rx="14" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="344" textAnchor="middle" fontSize="14.5" fontWeight="600" fill="var(--t-primary)">
					TTL 到期前，沿途都存着旧答案
				</text>
				<text x="180" y="361" textAnchor="middle" fontSize="11.5" fill="var(--t-secondary)">
					递归解析器 · 系统 · 浏览器各存一份
				</text>

				<text x="180" y="400" textAnchor="middle" fontSize="11.5" fill="var(--t-tertiary)">
					走左边 = 改错了地方，等多久都没用
				</text>
				<text x="180" y="418" textAnchor="middle" fontSize="11.5" fill="var(--t-tertiary)">
					走右边 = 改对了，剩下的只是等缓存过期
				</text>
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				「不生效」分两种：一种是改动压根没进解析链路，一种是进了但还被缓存挡着。先分清是哪一种，再决定是去改配置还是去等。
			</figcaption>
		</figure>
	);
}
