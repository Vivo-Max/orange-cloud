import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const withNextIntl = createNextIntlPlugin();

const nextConfig: NextConfig = {
	// 单一数据源 changelog 包是 TS 源码（导出 ios.json/android.json + 类型），需 Next 转译。
	transpilePackages: ["@orange-cloud/changelog"],
	// 截图与图标都已按展示尺寸预缩放（2x JPEG/PNG），直接静态托管，
	// 不依赖 Cloudflare Images 做运行时优化。
	images: {
		unoptimized: true,
	},
};

export default withNextIntl(nextConfig);

// 仅在 `next dev` 阶段初始化 OpenNext Cloudflare 开发代理（用于 getCloudflareContext）。
// `next build` 阶段（含 CI）没有 Cloudflare 凭证，若在此处调用会启动 wrangler 远程代理
// 并因未登录导致构建失败。通过 NEXT_PHASE 判断，确保构建期不会触发远程代理。
// See https://opennext.js.org/cloudflare/bindings#local-access-to-bindings
if (process.env.NEXT_PHASE === "phase-development-server") {
	const { initOpenNextCloudflareForDev } = require("@opennextjs/cloudflare");
	initOpenNextCloudflareForDev();
}
