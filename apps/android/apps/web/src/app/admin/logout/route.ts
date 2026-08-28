import { NextRequest, NextResponse } from "next/server";
import { SESSION_COOKIE, cookieSecure, sessionCookieOptions } from "@/lib/admin/auth";

export const dynamic = "force-dynamic";

export async function GET(request: NextRequest): Promise<NextResponse> {
	const res = NextResponse.redirect(new URL("/admin/login", request.url), { status: 303 });
	res.cookies.set(SESSION_COOKIE, "", { ...sessionCookieOptions(cookieSecure(request)), maxAge: 0 });
	return res;
}
