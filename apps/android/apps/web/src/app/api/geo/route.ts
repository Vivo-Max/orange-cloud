import { NextRequest, NextResponse } from "next/server";
import { getCloudflareContext } from "@opennextjs/cloudflare";

// 访客所在国家/地区（两位 ISO 码，小写）。首页是 ISR 缓存页，无法内联按 IP 个性化的内容，
// 故客户端组件（AndroidBadge 等）挂载后请求本接口（边缘读 cf.country / CF-IPCountry），
// 仅中国大陆（cn）展示官网下载 badge（其余地区显示 Google Play）。注意：香港 hk / 台湾 tw / 澳门 mo 各自独立、不算大陆。
// ?country= 仅供本地验证覆盖；浏览器侧缓存 30 分钟，限制请求量。
export const dynamic = "force-dynamic";

export async function GET(request: NextRequest): Promise<NextResponse> {
	const { cf } = getCloudflareContext();
	const ipCountry = (cf?.country ?? request.headers.get("cf-ipcountry") ?? "").toLowerCase();
	const override = request.nextUrl.searchParams.get("country")?.toLowerCase();
	const country = (override ?? ipCountry).replace(/[^a-z]/g, "").slice(0, 2);
	return NextResponse.json({ country }, { headers: { "cache-control": "private, max-age=1800" } });
}
