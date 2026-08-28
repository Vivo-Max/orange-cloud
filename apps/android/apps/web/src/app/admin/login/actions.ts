"use server";

import { cookies, headers } from "next/headers";
import { redirect } from "next/navigation";
import { getCloudflareContext } from "@opennextjs/cloudflare";
import {
	SESSION_COOKIE,
	createSessionToken,
	passwordMatches,
	sessionCookieOptions,
} from "@/lib/admin/auth";

// 登录表单的 Server Action：沿用既有口令校验（常数时间比对）+ HMAC 签名会话 cookie。
// 校验失败 -> /admin/login?error=1；成功 -> 下发 cookie 后跳 /admin。
export async function login(formData: FormData): Promise<void> {
	const { env } = await getCloudflareContext({ async: true });
	const secret = env.ADMIN_PASSWORD ?? "";
	const password = String(formData.get("password") ?? "");

	if (!(await passwordMatches(password, secret))) {
		redirect("/admin/login?error=1");
	}

	const token = await createSessionToken(secret);
	// 无 NextRequest，secure 据转发协议判定（localhost http 下不能带 Secure，否则 cookie 不落）。
	const proto = (await headers()).get("x-forwarded-proto") ?? "http";
	(await cookies()).set(SESSION_COOKIE, token, sessionCookieOptions(proto === "https"));
	redirect("/admin");
}
