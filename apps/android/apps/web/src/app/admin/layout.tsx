import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import { PreferencesProvider } from "@/components/dashboard/prefs";
import "./dashboard.css";

// 后台账本自带一套根布局（独立 <html>/<body> 与晨昏落地页隔离）。
// 站点页面在 app/[locale]/ 下另有根布局；两者互不影响。
const geistSans = Geist({
	variable: "--font-geist-sans",
	subsets: ["latin"],
});

const geistMono = Geist_Mono({
	variable: "--font-geist-mono",
	subsets: ["latin"],
});

export const metadata: Metadata = {
	title: "Orange Cloud · IAP 数据看板",
	description: "Orange Cloud 应用的 Apple 内购与订阅数据看板",
	robots: { index: false, follow: false },
};

export default function AdminLayout({
	children,
}: Readonly<{
	children: React.ReactNode;
}>) {
	return (
		<html lang="zh-CN">
			<head>
				<link rel="icon" href="/icons/icon-32.png" type="image/png" sizes="32x32" />
				<link rel="icon" href="/icons/icon-64.png" type="image/png" sizes="64x64" />
				<link rel="apple-touch-icon" href="/icons/icon-180.png" />
			</head>
			<body className={`${geistSans.variable} ${geistMono.variable} antialiased`}>
				<PreferencesProvider>{children}</PreferencesProvider>
			</body>
		</html>
	);
}
