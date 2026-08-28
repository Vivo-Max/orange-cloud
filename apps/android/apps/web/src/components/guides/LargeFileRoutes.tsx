/**
 * 大文件在 Cloudflare 上的四条正路与一条踩线路径：
 * 网页与中小静态资源走 CDN，图片走 Images，视频走 Stream，
 * 安装包与备份走 R2 加自定义域；把视频或网盘压在源站靠代理分发则踩在条款上。
 * 纯 SVG、无 JS；配色走主题 token，竖版布局，窄屏不溢出。
 */
export default function LargeFileRoutes() {
	const rows = [
		{ kind: "网页、CSS、JS、中小图片", route: "小黄云开着，CDN 直接缓存", ok: true },
		{ kind: "大量图片、缩略图", route: "Images：每月 5,000 次转换免费", ok: true },
		{ kind: "视频、直播", route: "Stream：按分钟计费，不看文件大小", ok: true },
		{ kind: "安装包、备份、网盘", route: "R2 加自定义域：出网流量不计费", ok: true },
		{ kind: "视频、网盘压在源站靠代理分发", route: "条款红线，Cloudflare 有权停用 CDN", ok: false },
	];

	return (
		<figure className="my-8">
			<svg
				viewBox="0 0 360 458"
				role="img"
				aria-label="按内容类型分流：网页、CSS、JS 与中小图片开着小黄云由 CDN 直接缓存；大量图片走 Images，免费方案每月有 5,000 次唯一转换额度；视频走 Stream，按分钟计费而不看文件大小；安装包、备份与网盘文件走 R2 并绑定自定义域，出网流量不计费。这四条都在服务条款允许的范围内。而把视频或网盘直接压在源站、靠代理分发，是条款明确限制的用法，Cloudflare 有权停用 CDN。"
				className="mx-auto block h-auto w-full max-w-[420px]"
			>
				<title>一份文件在 Cloudflare 上该走哪条路</title>

				<text x="30" y="16" fontSize="11" fill="var(--t-tertiary)" letterSpacing="0.6">
					按内容类型分流
				</text>

				{rows.map((row, i) => {
					const y = 30 + i * 84;
					return (
						<g key={row.kind}>
							<rect
								x="30"
								y={y}
								width="300"
								height="68"
								rx="14"
								fill="var(--glass-bg)"
								stroke={row.ok ? "var(--divider)" : "var(--oc-orange)"}
								strokeOpacity={row.ok ? 1 : 0.85}
								strokeDasharray={row.ok ? undefined : "5 4"}
							/>
							<rect
								x="30"
								y={y + 14}
								width="3"
								height="40"
								rx="1.5"
								fill="var(--oc-orange)"
								fillOpacity={row.ok ? 0.55 : 0.95}
							/>
							<text x="46" y={y + 27} fontSize="13.5" fontWeight="600" fill="var(--t-primary)">
								{row.kind}
							</text>
							<text x="46" y={y + 48} fontSize="11.5" fill="var(--t-secondary)">
								{row.route}
							</text>
							<text
								x="316"
								y={y + 27}
								textAnchor="end"
								fontSize="10.5"
								letterSpacing="0.4"
								fill={row.ok ? "var(--t-tertiary)" : "var(--oc-orange)"}
							>
								{row.ok ? "条款内" : "踩线"}
							</text>
						</g>
					);
				})}
			</svg>
			<figcaption className="mt-3 text-center text-[13px] leading-relaxed t-tertiary">
				分界不在文件大小，而在你有没有用 Cloudflare 为这类内容准备的那个服务。
			</figcaption>
		</figure>
	);
}
