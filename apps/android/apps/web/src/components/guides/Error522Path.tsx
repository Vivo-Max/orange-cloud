/**
 * Error 522 的位置图：请求到了 Cloudflare 边缘，边缘向源站发起 TCP 连接，
 * 但源站那一侧静默（丢包 / 防火墙 drop），19 秒内等不到 SYN+ACK —— 于是返回 522。
 * 与 521 的分界画在同一张图上：被拒绝（RST）是 521，被丢弃（静默）才是 522。
 * 纯 SVG、无 JS；配色全走主题 token，亮/暗两套都成立；竖版布局，窄屏不溢出。
 */
export default function Error522Path() {
	const col = { x: 30, width: 260, rx: 14 };

	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 400 404"
				role="img"
				aria-label="Where Cloudflare error 522 happens: a visitor reaches the Cloudflare edge, the edge opens a TCP connection to the origin server, but nothing comes back because the packets are silently dropped. After 19 seconds of retried SYN packets with no SYN and ACK reply, Cloudflare gives up and returns error 522, connection timed out. If the origin had actively refused the connection instead of dropping it, the result would be error 521 rather than 522."
				className="mx-auto block h-auto w-full max-w-[440px]"
			>
				<title>Where a Cloudflare 522 error happens on the path to your origin</title>

				{/* 1 · 访客 → 边缘：这一段是通的 */}
				<rect {...col} y="12" height="52" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="160" y="36" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Visitor
				</text>
				<text x="160" y="53" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					reaches Cloudflare normally
				</text>

				<defs>
					<marker id="e522-arrow" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--oc-orange)" />
					</marker>
				</defs>

				<path
					d="M160 68 L160 96"
					stroke="var(--oc-orange)"
					strokeWidth="1.6"
					fill="none"
					markerEnd="url(#e522-arrow)"
				/>

				{/* 2 · Cloudflare 边缘：向源站发 SYN */}
				<rect {...col} y="102" height="56" fill="var(--glass-bg)" stroke="var(--oc-orange)" strokeOpacity="0.55" />
				<text x="160" y="127" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Cloudflare edge
				</text>
				<text x="160" y="144" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					opens a TCP connection to the origin
				</text>

				{/* 3 · 静默的一段：SYN 重试，没有任何回应 */}
				<path
					d="M160 162 L160 232"
					stroke="var(--oc-orange)"
					strokeWidth="1.6"
					strokeDasharray="4 6"
					fill="none"
					markerEnd="url(#e522-arrow)"
				/>
				<text x="172" y="187" fontSize="11" fill="var(--t-tertiary)">
					SYN, retried for 19 s
				</text>
				<text x="172" y="204" fontSize="11" fill="var(--t-tertiary)">
					nothing comes back
				</text>

				{/* 4 · 源站侧：包被静默丢弃 */}
				<rect {...col} y="238" height="56" fill="none" stroke="var(--divider)" strokeDasharray="5 4" />
				<text x="160" y="263" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-secondary)">
					Origin server
				</text>
				<text x="160" y="280" textAnchor="middle" fontSize="12" fill="var(--t-tertiary)">
					silent: packets dropped, not refused
				</text>

				{/* 5 · 结果 */}
				<path
					d="M160 300 L160 326"
					stroke="var(--oc-orange)"
					strokeWidth="1.6"
					fill="none"
					markerEnd="url(#e522-arrow)"
				/>
				<rect {...col} y="332" height="56" fill="none" stroke="var(--oc-orange)" strokeOpacity="0.7" strokeDasharray="5 4" />
				<text x="160" y="357" textAnchor="middle" fontSize="15" fontWeight="600" fill="var(--t-primary)">
					Error 522
				</text>
				<text x="160" y="374" textAnchor="middle" fontSize="12" fill="var(--t-secondary)">
					connection timed out
				</text>
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				A 522 is silence, not rejection. Had the origin answered with a refusal, the visitor would see 521 instead.
			</figcaption>
		</figure>
	);
}
