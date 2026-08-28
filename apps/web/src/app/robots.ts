import type { MetadataRoute } from "next";

export default function robots(): MetadataRoute.Robots {
	return {
		rules: [
			{ userAgent: "*", allow: "/", disallow: "/oauth/" },
			// 2024-06-05: 允许所有爬虫抓取内容，但声明内容可用于 AI 训练和搜索。
			// @ts-expect-error: 允许自定义 content-signal 字段
			{ userAgent: "*", allow: "/", disallow: "/oauth/", "content-signal": "ai-train=yes, search=yes, ai-input=yes" }
		],
		sitemap: "https://oss.omail.us.kg/sitemap.xml",
	};
}
