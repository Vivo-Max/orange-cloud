/**
 * 回源一次请求里的四道超时，以及各自超时后返回的错误码。
 * 纯 SVG、无 JS；配色走主题 token，竖版布局，窄屏不溢出。
 */
export default function OriginTimeouts() {
	const rows = [
		{
			y: 44,
			title: "① 建立 TCP 连接",
			sub: "边缘发 SYN，等源站的 SYN+ACK",
			limit: "19 秒",
			code: "522",
		},
		{
			y: 128,
			title: "② 发出请求，等确认",
			sub: "连接已建立，等源站 ACK",
			limit: "90 秒",
			code: "522",
		},
		{
			y: 212,
			title: "③ 等源站给出响应",
			sub: "Proxy Read Timeout",
			limit: "125 秒",
			code: "524",
		},
		{
			y: 296,
			title: "④ 读到响应内容",
			sub: "响应为空或不合法",
			limit: "—",
			code: "520",
		},
	];

	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 360 400"
				role="img"
				aria-label="Cloudflare 回源过程中的四道超时：第一步建立 TCP 连接，19 秒内收不到源站的 SYN 加 ACK 就返回 522；第二步连接建立后 90 秒内等不到请求的确认，同样返回 522；第三步等源站响应超过 125 秒返回 524；第四步拿到的响应为空或不合法则返回 520。"
				className="mx-auto block h-auto w-full max-w-[440px]"
			>
				<title>Cloudflare 回源过程中的四道超时</title>

				<text x="14" y="18" fontSize="11" fill="var(--t-tertiary)" letterSpacing="0.6">
					Cloudflare 边缘节点
				</text>
				<line x1="14" y1="26" x2="346" y2="26" stroke="var(--divider)" strokeDasharray="4 5" />

				{rows.map((row) => (
					<g key={row.title}>
						<rect
							x="14"
							y={row.y}
							width="236"
							height="62"
							rx="14"
							fill="var(--glass-bg)"
							stroke="var(--divider)"
						/>
						<text x="30" y={row.y + 26} fontSize="14" fontWeight="600" fill="var(--t-primary)">
							{row.title}
						</text>
						<text x="30" y={row.y + 45} fontSize="11.5" fill="var(--t-secondary)">
							{row.sub}
						</text>
						<rect
							x="264"
							y={row.y + 8}
							width="82"
							height="46"
							rx="12"
							fill="var(--glass-bg)"
							stroke="var(--oc-orange)"
							strokeOpacity="0.55"
						/>
						<text x="305" y={row.y + 26} textAnchor="middle" fontSize="11" fill="var(--t-secondary)">
							{row.limit}
						</text>
						<text
							x="305"
							y={row.y + 45}
							textAnchor="middle"
							fontSize="16"
							fontWeight="700"
							fill="var(--oc-orange)"
						>
							{row.code}
						</text>
					</g>
				))}

				<defs>
					<marker id="oto-arrow" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--oc-orange)" />
					</marker>
				</defs>
				<g stroke="var(--oc-orange)" strokeWidth="1.6" markerEnd="url(#oto-arrow)" fill="none">
					<path d="M132 30 L132 38" />
					<path d="M132 106 L132 122" />
					<path d="M132 190 L132 206" />
					<path d="M132 274 L132 290" />
					<path d="M132 358 L132 372" />
				</g>

				<line x1="14" y1="378" x2="346" y2="378" stroke="var(--divider)" strokeDasharray="4 5" />
				<text x="14" y="394" fontSize="11" fill="var(--t-tertiary)" letterSpacing="0.6">
					你的源站
				</text>
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				四道超时对应四种失败方式。522 卡在最前面两步——源站根本没回话；等很久才超时的是 524，回了话但内容不对的是 520。
			</figcaption>
		</figure>
	);
}
