// shields 风格的排名徽章 SVG（左标签 + 右值），供 /api/ranks/badge 输出、README 内嵌。
// 无名次时返回 1×1 透明 SVG（空白图），让 README 不出现裂图。

const BRAND_ORANGE = "#F48120";

/** 1×1 透明 SVG：无名次 / 无数据时的「空白图片」。 */
export const BLANK_SVG = '<svg xmlns="http://www.w3.org/2000/svg" width="1" height="1"/>';

function escapeXml(s: string): string {
	return s
		.replace(/&/g, "&amp;")
		.replace(/</g, "&lt;")
		.replace(/>/g, "&gt;")
		.replace(/"/g, "&quot;")
		.replace(/'/g, "&apos;");
}

/** 粗略估算 11px sans 文本宽度（标签均为 ASCII，足够排版）。 */
function textWidth(s: string): number {
	let w = 0;
	for (const ch of s) w += /[A-Z0-9#]/.test(ch) ? 7.4 : /[ .·]/.test(ch) ? 3.6 : 6.3;
	return w;
}

/**
 * 生成左右两段式徽章 SVG。value 为空 → 返回空白图。
 * 经典 shields 双行文本（阴影 + 主体），作为 <img> 被浏览器/GitHub camo 渲染。
 */
export function renderRankBadge(opts: { label: string; value: string | null; color?: string }): string {
	const { label, value, color = BRAND_ORANGE } = opts;
	if (!value) return BLANK_SVG;

	const PAD = 7;
	const H = 20;
	const leftW = Math.round(textWidth(label) + PAD * 2);
	const rightW = Math.round(textWidth(value) + PAD * 2);
	const W = leftW + rightW;
	const lMid = leftW / 2;
	const rMid = leftW + rightW / 2;
	const L = escapeXml(label);
	const V = escapeXml(value);
	const aria = escapeXml(`${label}: ${value}`);

	return (
		`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" role="img" aria-label="${aria}">` +
		`<title>${aria}</title>` +
		`<linearGradient id="s" x2="0" y2="100%"><stop offset="0" stop-color="#bbb" stop-opacity=".1"/><stop offset="1" stop-opacity=".1"/></linearGradient>` +
		`<clipPath id="r"><rect width="${W}" height="${H}" rx="3" fill="#fff"/></clipPath>` +
		`<g clip-path="url(#r)">` +
		`<rect width="${leftW}" height="${H}" fill="#555"/>` +
		`<rect x="${leftW}" width="${rightW}" height="${H}" fill="${color}"/>` +
		`<rect width="${W}" height="${H}" fill="url(#s)"/>` +
		`</g>` +
		`<g fill="#fff" text-anchor="middle" font-family="Verdana,Geneva,DejaVu Sans,sans-serif" font-size="11">` +
		`<text x="${lMid}" y="15" fill="#010101" fill-opacity=".3">${L}</text>` +
		`<text x="${lMid}" y="14">${L}</text>` +
		`<text x="${rMid}" y="15" fill="#010101" fill-opacity=".3">${V}</text>` +
		`<text x="${rMid}" y="14">${V}</text>` +
		`</g>` +
		`</svg>`
	);
}
