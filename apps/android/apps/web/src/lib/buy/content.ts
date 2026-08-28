// 非 Play 中国大陆 Android 购买渠道的文案（自包含，不进 9 个 messages/*.json）。
// 单一市场 + CNY，仅需中/英两套：zh-* 取简体、其余取英文。

export interface BuySuccessStrings {
	confirmingTitle: string;
	confirmingSub: string;
	readyTitle: string;
	readySub: string;
	codeLabel: string;
	copy: string;
	copied: string;
	openApp: string;
	activateNote: string;
	emailedNote: string;
	timeoutTitle: string;
	timeoutSub: string;
	revokedNote: string;
}

export interface RefundStrings {
	title: string;
	sub: string;
	codeLabel: string;
	codePlaceholder: string;
	reasonLabel: string;
	reasonPlaceholder: string;
	submit: string;
	submitting: string;
	policyNote: string;
	back: string;
	okTitle: string;
	okSub: string;
	errNotFound: string;
	errNotPaid: string;
	errWindow: string;
	errAlready: string;
	errGeneric: string;
}

export interface ResetStrings {
	title: string;
	sub: string;
	codeLabel: string;
	codePlaceholder: string;
	emailLabel: string;
	emailPlaceholder: string;
	submit: string;
	submitting: string;
	policyNote: string;
	back: string;
	okTitle: string;
	okSub: string;
	errInvalid: string;
	errNoEmail: string;
	errRevoked: string;
	errRateLimited: string; // 含 {date} 占位
	errGeneric: string;
}

export interface BindStrings {
	title: string;
	sub: string;
	codeLabel: string;
	codePlaceholder: string;
	emailLabel: string;
	emailPlaceholder: string;
	submit: string;
	submitting: string;
	note: string;
	back: string;
	okTitle: string;
	okSub: string;
	errInvalid: string;
	errAlready: string;
	errRevoked: string;
	errGeneric: string;
}

export interface RecoverStrings {
	title: string;
	sub: string;
	emailLabel: string;
	emailPlaceholder: string;
	submit: string;
	submitting: string;
	note: string;
	back: string;
	okTitle: string;
	okSub: string;
	errGeneric: string;
}

export interface DownloadStrings {
	directTop: string;
	directMain: string;
	directAlt: string;
	playAlt: string;
	playComing: string;
}

export interface BuyContent {
	// 销售页
	kicker: string;
	title: string;
	sub: string;
	price: string;
	priceCaption: string;
	includesTitle: string;
	includes: string[];
	deviceNote: string;
	buyButton: string;
	buyLoading: string;
	buyError: string;
	payNote: string;
	howTitle: string;
	howSteps: string[];
	recoverNote: string;
	ossText: string;
	success: BuySuccessStrings;
	refundEntry: string;
	refund: RefundStrings;
	resetEntry: string;
	reset: ResetStrings;
	bindEntry: string;
	bind: BindStrings;
	recoverEntry: string;
	recover: RecoverStrings;
	download: DownloadStrings;
}

// 展示价（单一来源）。改价时改这里 + Stripe Price ID + 安卓 strings 的 paywall_buy_lifetime。
// 港币原币收款（Stripe 香港账户），避免 CNY 跨币种转换费。
const PRICE_DISPLAY = "HK$34.99";

const zhHans: BuyContent = {
	kicker: "Android · 中国大陆",
	title: "Orange Cloud Pro 终身",
	sub: "用不了 Google Play？在这里一次买断，解锁全部 Pro 功能，永久使用，不是订阅。",
	price: PRICE_DISPLAY,
	priceCaption: "一次买断 · 永久 · 非订阅",
	includesTitle: "解锁全部 Pro",
	includes: [
		"多账号切换",
		"存储模块：R2 / D1 / KV",
		"Workers 实时日志 tail",
		"WAF 规则 · Cloudflare Tunnel",
		"7 天 / 30 天流量分析",
		"后续新功能持续解锁",
	],
	deviceNote: "一个激活码最多可激活 2 台设备。",
	buyButton: "微信 / 支付宝购买",
	buyLoading: "正在前往支付…",
	buyError: "下单失败，请重试。",
	payNote: "由 Stripe 安全收单 · 支持微信支付 / 支付宝 / 银行卡",
	howTitle: "怎么用",
	howSteps: [
		"完成支付，立即拿到激活码（同时发到你的邮箱）",
		"打开 App：设置 → 激活 Pro",
		"输入激活码，解锁全部功能",
	],
	recoverNote: "激活码会发送到你的邮箱，丢失可凭邮箱找回。",
	ossText: "开源项目 · 也可自行编译免费解锁",
	success: {
		confirmingTitle: "正在确认支付…",
		confirmingSub: "微信 / 支付宝支付可能需要几秒确认。激活码生成后会显示在这里，并发送到你的邮箱。",
		readyTitle: "支付成功，感谢支持！",
		readySub: "这是你的终身激活码：",
		codeLabel: "激活码",
		copy: "复制",
		copied: "已复制",
		openApp: "在 App 中激活",
		activateNote: "或手动打开 App：设置 → 激活 Pro，输入上面的激活码。",
		emailedNote: "激活码也已发送到你的邮箱，请妥善保管。",
		timeoutTitle: "还没拿到激活码",
		timeoutSub: "如果你已完成支付，激活码稍后会发到你的邮箱；也可以刷新本页重试。",
		revokedNote: "此激活码已失效（可能因退款被撤销）。如有疑问请联系我们。",
	},
	refundEntry: "不满意？申请退款",
	refund: {
		title: "申请退款",
		sub: "购买后 30 天内，如不满意可在此申请退款。提交后我们会尽快人工处理，退款原路退回。",
		codeLabel: "激活码",
		codePlaceholder: "OC-XXXXX-XXXXX",
		reasonLabel: "退款原因（选填）",
		reasonPlaceholder: "告诉我们哪里不满意，帮助我们改进",
		submit: "提交退款申请",
		submitting: "提交中…",
		policyNote: "退款政策：购买后 30 天内可申请，经人工审核后原路退回，对应激活码将失效。",
		back: "← 返回购买页",
		okTitle: "申请已提交",
		okSub: "我们会尽快处理。退款将原路退回到你的支付方式，激活码届时失效。",
		errNotFound: "未找到该激活码，请检查后重试。",
		errNotPaid: "该激活码不是付费购买所得，无法申请退款。",
		errWindow: "已超过 30 天退款期限，无法在线申请。如有特殊情况请联系我们。",
		errAlready: "该激活码已提交过申请或已退款。如有疑问请联系我们。",
		errGeneric: "提交失败，请稍后重试。",
	},
	resetEntry: "设备换了？重置已绑定设备",
	reset: {
		title: "重置已绑定设备",
		sub: "在新设备上提示「设备已达上限」？用购买时的邮箱验证后，可解除该激活码的全部设备绑定，再到需要的设备上重新激活。",
		codeLabel: "激活码",
		codePlaceholder: "OC-XXXXX-XXXXX",
		emailLabel: "购买时填写的邮箱",
		emailPlaceholder: "you@example.com",
		submit: "验证并重置",
		submitting: "处理中…",
		policyNote: "为防止滥用，每枚激活码每 30 天最多自助重置 1 次。",
		back: "← 返回购买页",
		okTitle: "已重置",
		okSub: "该激活码的设备绑定已全部解除。现在可在需要的设备上重新激活（上限 2 台）。",
		errInvalid: "激活码或邮箱不匹配，请检查后重试。",
		errNoEmail: "此激活码没有登记邮箱，无法自助重置，请联系我们。",
		errRevoked: "此激活码已失效（可能已退款）。",
		errRateLimited: "距上次重置不足 30 天，可在 {date} 之后再试。",
		errGeneric: "处理失败，请稍后重试。",
	},
	bindEntry: "给激活码绑定邮箱",
	bind: {
		title: "绑定邮箱",
		sub: "给还没绑定邮箱的激活码绑定一个邮箱，方便日后找回激活码、自助重置设备。",
		codeLabel: "激活码",
		codePlaceholder: "OC-XXXXX-XXXXX",
		emailLabel: "要绑定的邮箱",
		emailPlaceholder: "you@example.com",
		submit: "绑定",
		submitting: "处理中…",
		note: "仅未绑定邮箱的激活码可自助绑定；已绑定的如需变更请联系我们。",
		back: "← 返回购买页",
		okTitle: "已绑定",
		okSub: "邮箱已绑定到该激活码。日后可凭此邮箱找回激活码或自助重置设备。",
		errInvalid: "激活码无效，请检查后重试。",
		errAlready: "该激活码已绑定邮箱，如需变更请联系我们。",
		errRevoked: "此激活码已失效（可能已退款）。",
		errGeneric: "处理失败，请稍后重试。",
	},
	recoverEntry: "忘了激活码？重发到邮箱",
	recover: {
		title: "找回激活码",
		sub: "输入购买或绑定时用的邮箱，我们会把你名下的激活码重新发送到该邮箱。",
		emailLabel: "邮箱",
		emailPlaceholder: "you@example.com",
		submit: "发送到邮箱",
		submitting: "发送中…",
		note: "如该邮箱名下有激活码，邮件会很快送达；请一并检查垃圾箱。",
		back: "← 返回购买页",
		okTitle: "已发送",
		okSub: "如果该邮箱名下有激活码，我们已发送过去，请查收（含垃圾箱）。",
		errGeneric: "发送失败，请稍后重试。",
	},
	download: {
		directTop: "中国大陆 · Android",
		directMain: "官网下载",
		directAlt: "官网下载 Orange Cloud 安卓版",
		playAlt: "Google Play",
		playComing: "即将上线",
	},
};

const en: BuyContent = {
	kicker: "Android · Mainland China",
	title: "Orange Cloud Pro Lifetime",
	sub: "No Google Play? Buy once here to unlock every Pro feature forever. Not a subscription.",
	price: PRICE_DISPLAY,
	priceCaption: "One-time · Lifetime · Not a subscription",
	includesTitle: "Unlock all of Pro",
	includes: [
		"Multi-account switching",
		"Storage: R2 / D1 / KV",
		"Workers live log tail",
		"WAF rules · Cloudflare Tunnel",
		"7-day / 30-day analytics",
		"Future features included",
	],
	deviceNote: "One code activates up to 2 devices.",
	buyButton: "Buy with WeChat / Alipay",
	buyLoading: "Redirecting to payment…",
	buyError: "Checkout failed, please retry.",
	payNote: "Secured by Stripe · WeChat Pay / Alipay / card",
	howTitle: "How it works",
	howSteps: [
		"Pay and get your code instantly (also emailed to you)",
		"Open the app: Settings → Activate Pro",
		"Enter the code to unlock everything",
	],
	recoverNote: "Your code is emailed to you; recover it anytime via your email.",
	ossText: "Open source · you can also self-compile for free",
	success: {
		confirmingTitle: "Confirming payment…",
		confirmingSub: "WeChat / Alipay can take a few seconds to confirm. Your code will appear here and be emailed to you.",
		readyTitle: "Payment complete. Thank you!",
		readySub: "Here is your lifetime activation code:",
		codeLabel: "Activation code",
		copy: "Copy",
		copied: "Copied",
		openApp: "Activate in app",
		activateNote: "Or open the app manually: Settings → Activate Pro, then enter the code above.",
		emailedNote: "The code has also been emailed to you. Keep it safe.",
		timeoutTitle: "Code not ready yet",
		timeoutSub: "If you already paid, your code will arrive by email shortly. You can also refresh this page.",
		revokedNote: "This code is no longer valid (it may have been revoked after a refund). Contact us if this is unexpected.",
	},
	refundEntry: "Not satisfied? Request a refund",
	refund: {
		title: "Request a refund",
		sub: "Within 30 days of purchase, request a refund here if you're not satisfied. We review each request and refund to your original payment method.",
		codeLabel: "Activation code",
		codePlaceholder: "OC-XXXXX-XXXXX",
		reasonLabel: "Reason (optional)",
		reasonPlaceholder: "Tell us what went wrong, it helps us improve",
		submit: "Submit refund request",
		submitting: "Submitting…",
		policyNote: "Refund policy: request within 30 days of purchase. Reviewed manually and returned to your original payment method; the code will be revoked.",
		back: "← Back to purchase",
		okTitle: "Request submitted",
		okSub: "We'll process it shortly. The refund goes back to your original payment method and the code will then be revoked.",
		errNotFound: "Code not found. Please check and try again.",
		errNotPaid: "This code was not generated from a paid purchase, so it can't be refunded.",
		errWindow: "The 30-day refund window has passed, so online requests aren't available. Contact us for special cases.",
		errAlready: "This code already has a request or has been refunded. Contact us if you have questions.",
		errGeneric: "Submission failed, please try again later.",
	},
	resetEntry: "Changed devices? Reset bound devices",
	reset: {
		title: "Reset bound devices",
		sub: "Seeing \"device limit reached\" on a new device? Verify with your purchase email to release all device bindings for this code, then re-activate on the devices you need.",
		codeLabel: "Activation code",
		codePlaceholder: "OC-XXXXX-XXXXX",
		emailLabel: "Email used at purchase",
		emailPlaceholder: "you@example.com",
		submit: "Verify and reset",
		submitting: "Processing…",
		policyNote: "To prevent abuse, each code can be self-reset at most once every 30 days.",
		back: "← Back to purchase",
		okTitle: "Reset done",
		okSub: "All device bindings for this code are cleared. You can now re-activate on the devices you need (up to 2).",
		errInvalid: "Code or email doesn't match. Please check and retry.",
		errNoEmail: "This code has no registered email, so self-reset isn't available. Please contact us.",
		errRevoked: "This code is no longer valid (possibly refunded).",
		errRateLimited: "Less than 30 days since the last reset. Try again after {date}.",
		errGeneric: "Something went wrong, please try again later.",
	},
	bindEntry: "Bind an email to your code",
	bind: {
		title: "Bind an email",
		sub: "Attach an email to a code that doesn't have one yet, so you can recover the code and self-reset devices later.",
		codeLabel: "Activation code",
		codePlaceholder: "OC-XXXXX-XXXXX",
		emailLabel: "Email to bind",
		emailPlaceholder: "you@example.com",
		submit: "Bind",
		submitting: "Processing…",
		note: "Only codes without a bound email can be self-bound; to change a bound email, contact us.",
		back: "← Back to purchase",
		okTitle: "Bound",
		okSub: "The email is now bound to this code. Use it to recover the code or self-reset devices later.",
		errInvalid: "Invalid code. Please check and retry.",
		errAlready: "This code already has a bound email. To change it, contact us.",
		errRevoked: "This code is no longer valid (possibly refunded).",
		errGeneric: "Something went wrong, please try again later.",
	},
	recoverEntry: "Forgot your code? Resend by email",
	recover: {
		title: "Recover your code",
		sub: "Enter the email used at purchase or binding, and we'll resend the codes under that email.",
		emailLabel: "Email",
		emailPlaceholder: "you@example.com",
		submit: "Send to email",
		submitting: "Sending…",
		note: "If there are codes under that email, the message will arrive shortly. Check your spam folder too.",
		back: "← Back to purchase",
		okTitle: "Sent",
		okSub: "If there are codes under that email, we've sent them. Please check your inbox (and spam).",
		errGeneric: "Sending failed, please try again later.",
	},
	download: {
		directTop: "Android · Mainland China",
		directMain: "Download APK",
		directAlt: "Download Orange Cloud for Android",
		playAlt: "Google Play",
		playComing: "Coming soon",
	},
};

export function getBuyContent(locale: string): BuyContent {
	return locale.startsWith("zh") ? zhHans : en;
}
