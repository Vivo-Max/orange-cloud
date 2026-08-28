// 发起 Stripe 退款（手写 form-POST，不引 SDK）。全额退款：只传 payment_intent。
// 退款成功后 Stripe 会推 charge.refunded webhook -> 自动撤销对应激活码。

const STRIPE_REFUNDS = "https://api.stripe.com/v1/refunds";

export async function createRefund(
	secretKey: string,
	paymentIntent: string,
): Promise<{ id: string; status: string }> {
	const form = new URLSearchParams();
	form.set("payment_intent", paymentIntent);

	const res = await fetch(STRIPE_REFUNDS, {
		method: "POST",
		headers: {
			authorization: `Bearer ${secretKey}`,
			"content-type": "application/x-www-form-urlencoded",
		},
		body: form.toString(),
	});
	if (!res.ok) {
		throw new Error(`stripe refund failed ${res.status}: ${await res.text()}`);
	}
	return (await res.json()) as { id: string; status: string };
}
