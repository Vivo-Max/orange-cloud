// 发码邮件：Cloudflare Email Service（send_email 绑定，无需 API key）。
// 发件域名须先 onboard（CF 控制台已为 oss.omail.us.kg 配好）。
// 未配置绑定（本地 dev）或无收件人则静默跳过。事务邮件，非营销。
//
// 三类邮件共用同一排版（购买 / 找回 / 评测邀请），仅 subject + intro 不同；
// 正文统一附「下载 Android App」按钮 + 官网 / 自助（找回·重置）链接。

import { formatCode } from "@/lib/codes/code";

export interface EmailBinding {
	send(message: {
		to: string;
		from: { email: string; name?: string };
		subject: string;
		html?: string;
		text?: string;
	}): Promise<unknown>;
}

const FROM = { email: "noreply@oss.omail.us.kg", name: "Orange Cloud" } as const;

// 激活码面向中国大陆 direct（sideload）渠道，故下载指向官网 APK。
const SITE = "https://oss.omail.us.kg";
const APK_URL = `${SITE}/orange-cloud.apk`;

function codesText(cores: string[]): string {
	return cores.map((c) => formatCode(c)).join("\n");
}

function codesHtml(cores: string[]): string {
	return cores
		.map(
			(c) =>
				`<div style="font-size:22px;font-weight:700;letter-spacing:2px;color:#F48120;background:#fff6ef;border:1px solid #f6d8c0;border-radius:12px;padding:14px;text-align:center;margin:0 0 10px">${formatCode(c)}</div>`,
		)
		.join("");
}

async function sendCodes(
	binding: EmailBinding | undefined,
	to: string | null | undefined,
	subject: string,
	intro: string,
	cores: string[],
): Promise<void> {
	if (!binding || !to || cores.length === 0) return;

	const text = [
		intro,
		"",
		codesText(cores),
		"",
		"还没装 App？下载 Android 版（APK）：",
		APK_URL,
		"",
		"装好后在 App 内「设置 → 激活 Pro」输入激活码即可解锁，一码最多激活 2 台设备。",
		"",
		`官网：${SITE}`,
		`找回激活码：${SITE}/buy/recover`,
		`重置已绑定设备：${SITE}/buy/reset`,
		`不满意，要退款?：${SITE}/buy/refund`,
		"",
		"请妥善保管本邮件，以便日后找回激活码。",
	].join("\n");

	const html = `<!doctype html>
<div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;max-width:480px;margin:0 auto;padding:32px 24px;color:#1c1c1e">
  <h1 style="font-size:20px;margin:0 0 14px">Orange Cloud Pro</h1>
  <p style="margin:0 0 14px;color:#3a3a3c">${intro}</p>
  ${codesHtml(cores)}
  <p style="margin:10px 0 18px;color:#3a3a3c">装好 App 后在「设置 → 激活 Pro」输入激活码即可解锁，一码最多激活 2 台设备。</p>
  <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 18px">
    <tr><td align="center" style="border-radius:12px;background:#F48120">
      <a href="${APK_URL}" style="display:inline-block;padding:13px 30px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none">下载 Android App</a>
    </td></tr>
  </table>
  <p style="margin:0 0 6px;color:#8e8e93;font-size:13px">官网 <a href="${SITE}" style="color:#F48120;text-decoration:none">oss.omail.us.kg</a></p>
  <p style="margin:0 0 16px;color:#8e8e93;font-size:13px"><a href="${SITE}/buy/recover" style="color:#F48120;text-decoration:none">找回激活码</a> · <a href="${SITE}/buy/reset" style="color:#F48120;text-decoration:none">重置已绑定设备</a></p>
  <p style="margin:0;color:#8e8e93;font-size:13px">请妥善保管本邮件以便日后找回。</p>
</div>`;

	await binding.send({ to, from: FROM, subject, html, text });
}

/** 购买成功后发码（Stripe webhook 用）。 */
export async function sendCodeEmail(
	binding: EmailBinding | undefined,
	to: string | null | undefined,
	core: string,
): Promise<void> {
	await sendCodes(binding, to, "你的 Orange Cloud Pro 激活码", "感谢支持 Orange Cloud！你的终身激活码：", [core]);
}

/** 找回：把某邮箱名下的全部激活码重新发送。 */
export async function sendCodesRecoveryEmail(
	binding: EmailBinding | undefined,
	to: string | null | undefined,
	cores: string[],
): Promise<void> {
	await sendCodes(binding, to, "你的 Orange Cloud 激活码（找回）", "这是你名下的 Orange Cloud 激活码：", cores);
}

/** 绑定：给某邮箱发送激活码。 */
export async function sendCodesBindEmail(
	binding: EmailBinding | undefined,
	to: string | null | undefined,
	cores: string[],
): Promise<void> {
	await sendCodes(binding, to, "你的 Orange Cloud 激活码绑定成功", "这是你名下的 Orange Cloud 激活码：", cores);
}

/** 评测邀请：后台手动发码、且填了邮箱时，给该地址发邀请邮件。 */
export async function sendInviteEmail(
	binding: EmailBinding | undefined,
	to: string | null | undefined,
	cores: string[],
	note: string | null,
): Promise<void> {
	const intro = note
		? `感谢评测 Orange Cloud（${note}）！这是你的激活码：`
		: "感谢评测 Orange Cloud！这是你的激活码：";
	await sendCodes(binding, to, "Orange Cloud Pro 评测邀请", intro, cores);
}
