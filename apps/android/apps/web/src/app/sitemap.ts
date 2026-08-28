import type { MetadataRoute } from "next";
import { siteUrls } from "@/lib/site/urls";

// URL 清单与 IndexNow 推送同源，见 src/lib/site/urls.ts。
export default function sitemap(): MetadataRoute.Sitemap {
	return siteUrls().map((entry) => ({
		url: entry.url,
		lastModified: new Date(entry.updated),
		...(entry.languages ? { alternates: { languages: entry.languages } } : {}),
	}));
}
