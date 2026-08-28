/**
 * 缓存为什么在多节点场景下更容易未命中：同一个文件在每个数据中心各存一份，
 * 没开分层缓存时每个节点第一次都要各自回源；开了之后下层先问上层，
 * 只有上层去碰源站。纯 SVG、无 JS；配色走主题 token，竖版布局，窄屏不溢出。
 */
export default function CacheFanout() {
	const pops = ["香港", "东京", "新加坡", "洛杉矶"];
	const popX = [14, 100, 186, 272];

	const popRow = (y: number) =>
		pops.map((name, i) => (
			<g key={`${y}-${name}`}>
				<rect x={popX[i]} y={y} width="74" height="38" rx="11" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x={popX[i] + 37} y={y + 23} textAnchor="middle" fontSize="12" fill="var(--t-primary)">
					{name}
				</text>
			</g>
		));

	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 360 452"
				role="img"
				aria-label="同一个文件在每个数据中心各存一份。上半部分是没开分层缓存的情形：香港、东京、新加坡、洛杉矶四个节点第一次请求时各自向源站回源一次，源站被打四次，四条记录里都是未命中。下半部分是开了分层缓存的情形：四个下层节点先问同一个上层节点，只有上层节点在没有内容时才去碰源站，源站只被打一次。"
				className="mx-auto block h-auto w-full max-w-[420px]"
			>
				<title>分层缓存前后，回源次数的差别</title>

				<defs>
					<marker id="fanout-arrow" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--oc-orange)" />
					</marker>
					<marker id="fanout-arrow-dim" viewBox="0 0 8 8" refX="6" refY="4" markerWidth="6" markerHeight="6" orient="auto">
						<path d="M0 0 L8 4 L0 8 z" fill="var(--t-tertiary)" />
					</marker>
				</defs>

				{/* —— 场景一：没开分层缓存 —— */}
				<text x="14" y="16" fontSize="11.5" fill="var(--t-tertiary)" letterSpacing="0.6">
					没开分层缓存
				</text>
				{popRow(28)}
				<g stroke="var(--t-tertiary)" strokeWidth="1.4" markerEnd="url(#fanout-arrow-dim)" fill="none">
					<path d="M51 70 L140 132" />
					<path d="M137 70 L165 132" />
					<path d="M223 70 L195 132" />
					<path d="M309 70 L220 132" />
				</g>
				<rect x="100" y="140" width="160" height="42" rx="12" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="166" textAnchor="middle" fontSize="13" fontWeight="600" fill="var(--t-primary)">
					源站
				</text>
				<text x="180" y="200" textAnchor="middle" fontSize="11.5" fill="var(--t-secondary)">
					四个节点各回源一次，四次都记未命中
				</text>

				<path d="M14 224 L346 224" stroke="var(--divider)" strokeWidth="1" strokeDasharray="3 5" />

				{/* —— 场景二：开了分层缓存 —— */}
				<text x="14" y="252" fontSize="11.5" fill="var(--oc-orange)" letterSpacing="0.6">
					开了分层缓存
				</text>
				{popRow(264)}
				<g stroke="var(--oc-orange)" strokeWidth="1.4" markerEnd="url(#fanout-arrow)" fill="none">
					<path d="M51 306 L150 336" />
					<path d="M137 306 L170 336" />
					<path d="M223 306 L190 336" />
					<path d="M309 306 L210 336" />
				</g>
				<rect
					x="110"
					y="344"
					width="140"
					height="38"
					rx="11"
					fill="var(--glass-bg)"
					stroke="var(--oc-orange)"
					strokeOpacity="0.55"
				/>
				<text x="180" y="368" textAnchor="middle" fontSize="12.5" fill="var(--t-primary)">
					上层节点
				</text>
				<g stroke="var(--oc-orange)" strokeWidth="1.6" markerEnd="url(#fanout-arrow)" fill="none">
					<path d="M180 384 L180 398" />
				</g>
				<rect x="100" y="404" width="160" height="34" rx="11" fill="var(--glass-bg)" stroke="var(--divider)" />
				<text x="180" y="426" textAnchor="middle" fontSize="12.5" fontWeight="600" fill="var(--t-primary)">
					源站 · 只被打一次
				</text>
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				缓存是按数据中心各存一份的。访客越分散，同一个文件被重复回源的次数就越多——分层缓存治的正是这一段。
			</figcaption>
		</figure>
	);
}
