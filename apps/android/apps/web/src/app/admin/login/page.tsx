import type { Metadata } from "next";
import Image from "next/image";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import { SESSION_COOKIE, verifySessionToken } from "@/lib/admin/auth";
import { login } from "./actions";

// 后台账本依赖 Cloudflare 环境变量（ADMIN_PASSWORD），必须每请求实时渲染。
// 显式 force-dynamic：避免 next build 预渲染探测期执行 getCloudflareContext()
// 时尝试启动 wrangler 远程代理（CI 无 Cloudflare 凭证会导致构建失败）。
export const dynamic = "force-dynamic";

export const metadata: Metadata = {
	title: "登录 · Orange Cloud 后台账本",
	robots: { index: false, follow: false },
};

export default async function LoginPage({
	searchParams,
}: {
	searchParams: Promise<{ error?: string }>;
}) {
	const { env } = await getCloudflareContext({ async: true });
	const token = (await cookies()).get(SESSION_COOKIE)?.value;
	if (token && (await verifySessionToken(token, env.ADMIN_PASSWORD ?? ""))) {
		redirect("/admin");
	}
	const { error } = await searchParams;

	return (
		<div className="flex min-h-screen items-center justify-center px-4">
			<div className="w-full max-w-sm">
				<div className="mb-6 flex items-center gap-2.5">
					<Image
						src="/icons/icon-64.png"
						alt=""
						width={20}
						height={20}
						className="h-5 w-5 rounded-[5px] shadow-sm"
					/>
					<div className="leading-tight">
						<p className="text-sm font-semibold tracking-tight">Orange Cloud</p>
						<p className="text-xs text-muted">收入账本</p>
					</div>
				</div>
				<form action={login} className="rounded-xl border border-border bg-surface p-6 shadow-sm">
					<label htmlFor="password" className="text-xs font-medium text-muted">
						管理口令
					</label>
					<input
						id="password"
						name="password"
						type="password"
						autoFocus
						autoComplete="current-password"
						className="mt-1.5 w-full rounded-lg border border-border bg-surface-2 px-3 py-2 text-sm outline-none transition-colors focus:border-accent"
					/>
					{error === "1" ? (
						<p className="mt-2 text-xs text-negative">口令不正确，请重试。</p>
					) : null}
					<button
						type="submit"
						className="mt-4 w-full rounded-lg bg-accent px-3 py-2 text-sm font-medium text-white transition-opacity hover:opacity-90"
					>
						登录
					</button>
				</form>
				<p className="mt-4 text-center text-[11px] text-muted">
					App Store Server Notifications V2 · 仅 Production
				</p>
			</div>
		</div>
	);
}
