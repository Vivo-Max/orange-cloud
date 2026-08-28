/**
 * 四档加密模式对两段连接的处理方式：访客 → Cloudflare 一段，Cloudflare → 源站一段。
 * 纯 SVG、无 JS；配色走主题 token，亮/暗两套都成立；竖版布局，窄屏不溢出。
 */
export default function TlsModeLadder() {
	const rows = [
		{
			y: 46,
			zh: "关闭",
			en: "Off",
			hop1: ["明文", "HTTPS 被跳回 HTTP"],
			hop2: ["明文", ""],
			secure1: false,
			secure2: false,
		},
		{
			y: 116,
			zh: "灵活",
			en: "Flexible",
			hop1: ["加密", ""],
			hop2: ["明文", "源站不需要证书"],
			secure1: true,
			secure2: false,
		},
		{
			y: 186,
			zh: "完全",
			en: "Full",
			hop1: ["加密", ""],
			hop2: ["加密", "证书不做任何校验"],
			secure1: true,
			secure2: true,
		},
		{
			y: 256,
			zh: "完全（严格）",
			en: "Full (strict)",
			hop1: ["加密", ""],
			hop2: ["加密并验证", "证书必须真实有效"],
			secure1: true,
			secure2: true,
		},
	];

	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 360 336"
				role="img"
				aria-label="四档 Cloudflare 加密模式对两段连接的处理：关闭模式下访客到 Cloudflare、Cloudflare 到源站都是明文；灵活模式下访客到 Cloudflare 加密、Cloudflare 到源站明文，源站不需要证书；完全模式下两段都加密，但源站证书不做任何校验；完全（严格）模式下两段都加密，并且源站证书必须真实有效。"
				className="mx-auto block h-auto w-full max-w-[460px]"
			>
				<title>四档加密模式分别怎么对待两段连接</title>

				<text x="122" y="30" fontSize="11" fill="var(--t-tertiary)" letterSpacing="0.4">
					访客 → Cloudflare
				</text>
				<text x="240" y="30" fontSize="11" fill="var(--t-tertiary)" letterSpacing="0.4">
					Cloudflare → 源站
				</text>
				<line x1="14" y1="38" x2="346" y2="38" stroke="var(--divider)" strokeDasharray="4 5" />

				{rows.map((row) => (
					<g key={row.en}>
						<text x="14" y={row.y + 24} fontSize="13.5" fontWeight="600" fill="var(--t-primary)">
							{row.zh}
						</text>
						<text x="14" y={row.y + 41} fontSize="10.5" fill="var(--t-tertiary)">
							{row.en}
						</text>

						<rect
							x="118"
							y={row.y}
							width="110"
							height="54"
							rx="13"
							fill="var(--glass-bg)"
							stroke={row.secure1 ? "var(--oc-orange)" : "var(--divider)"}
							strokeOpacity={row.secure1 ? 0.55 : 1}
							strokeDasharray={row.secure1 ? undefined : "4 5"}
						/>
						<text
							x="173"
							y={row.hop1[1] ? row.y + 24 : row.y + 32}
							textAnchor="middle"
							fontSize="12.5"
							fontWeight="600"
							fill={row.secure1 ? "var(--oc-orange)" : "var(--t-tertiary)"}
						>
							{row.hop1[0]}
						</text>
						{row.hop1[1] ? (
							<text x="173" y={row.y + 40} textAnchor="middle" fontSize="10" fill="var(--t-tertiary)">
								{row.hop1[1]}
							</text>
						) : null}

						<rect
							x="236"
							y={row.y}
							width="110"
							height="54"
							rx="13"
							fill="var(--glass-bg)"
							stroke={row.secure2 ? "var(--oc-orange)" : "var(--divider)"}
							strokeOpacity={row.secure2 ? 0.55 : 1}
							strokeDasharray={row.secure2 ? undefined : "4 5"}
						/>
						<text
							x="291"
							y={row.hop2[1] ? row.y + 24 : row.y + 32}
							textAnchor="middle"
							fontSize="12.5"
							fontWeight="600"
							fill={row.secure2 ? "var(--oc-orange)" : "var(--t-tertiary)"}
						>
							{row.hop2[0]}
						</text>
						{row.hop2[1] ? (
							<text x="291" y={row.y + 40} textAnchor="middle" fontSize="10" fill="var(--t-tertiary)">
								{row.hop2[1]}
							</text>
						) : null}
					</g>
				))}

				<line x1="14" y1="326" x2="346" y2="326" stroke="var(--divider)" strokeDasharray="4 5" />
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				浏览器地址栏那把锁，证明的只是左边这一列。右边那一列长什么样，只有这个设置说了算。
			</figcaption>
		</figure>
	);
}
