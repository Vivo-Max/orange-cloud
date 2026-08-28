/** 夜幕星星：固定坐标（避免 SSR/CSR 随机数不一致），CSS 错峰闪烁 */
const STARS: Array<[number, number, number, number]> = [
	// [left%, top%, size(px), delay(s)]
	[4, 12, 2, 0.2], [9, 38, 1.5, 1.1], [13, 8, 1.5, 2.3], [18, 52, 2, 0.7],
	[22, 22, 1.5, 1.8], [27, 64, 1.5, 3.0], [31, 15, 2, 0.4], [36, 41, 1.5, 2.6],
	[41, 6, 1.5, 1.4], [45, 30, 2, 0.9], [50, 58, 1.5, 2.0], [54, 11, 2, 3.2],
	[59, 44, 1.5, 0.5], [63, 24, 1.5, 1.6], [68, 60, 2, 2.8], [72, 9, 1.5, 0.1],
	[77, 35, 2, 1.9], [81, 17, 1.5, 2.4], [86, 49, 1.5, 0.8], [90, 27, 2, 3.4],
	[94, 7, 1.5, 1.3], [97, 55, 1.5, 2.1], [7, 70, 1.5, 0.6], [33, 76, 1.5, 1.7],
	[62, 78, 1.5, 2.9], [85, 72, 1.5, 0.3], [48, 82, 2, 1.0], [16, 86, 1.5, 2.5],
];

export default function Stars() {
	return (
		<div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
			{STARS.map(([left, top, size, delay], i) => (
				<span
					key={i}
					className="star"
					style={{
						left: `${left}%`,
						top: `${top}%`,
						width: size,
						height: size,
						animationDelay: `${delay}s`,
					}}
				/>
			))}
		</div>
	);
}
