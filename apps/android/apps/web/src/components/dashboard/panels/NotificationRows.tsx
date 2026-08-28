"use client";

import { type ReactNode, useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { TimeText } from "@/components/dashboard/prefs";
import { Badge, PlatformBadge } from "@/components/dashboard/ui";
import { formatMoney } from "@/lib/dashboard/format";
import type { NotificationRow } from "@/lib/dashboard/queries";
import {
	notificationLabel,
	notificationTone,
	offerTypeLabel,
	productLabel,
	revocationReasonLabel,
	subtypeLabel,
	txTypeLabel,
} from "@/lib/dashboard/types";

export function NotificationRows({ rows }: { rows: NotificationRow[] }) {
	const [selected, setSelected] = useState<NotificationRow | null>(null);

	return (
		<>
			{rows.map((n) => {
				const sub = subtypeLabel(n.subtype);
				return (
					<tr
						key={n.notification_uuid}
						data-testid="notif-row"
						onClick={() => setSelected(n)}
						className="cursor-pointer border-b border-border/50 transition-colors last:border-0 hover:bg-foreground/[0.03]"
					>
						<td className="px-5 py-2.5 whitespace-nowrap text-muted">
							<TimeText ms={n.received_at} />
						</td>
						<td className="px-5 py-2.5 whitespace-nowrap">
							<PlatformBadge platform={n.platform} />
						</td>
						<td className="px-5 py-2.5 whitespace-nowrap">
							<Badge tone={notificationTone(n.notification_type)}>
								{notificationLabel(n.notification_type)}
							</Badge>
						</td>
						<td className="px-5 py-2.5 whitespace-nowrap">
							{sub ? <Badge tone="muted">{sub}</Badge> : <span className="text-muted">—</span>}
						</td>
						<td className="px-5 py-2.5 font-mono text-[11px] whitespace-nowrap text-muted">
							{/* Play 的 purchaseToken 很长（几百字符），截断显示、hover 看全量 */}
							<span
								className="inline-block max-w-56 truncate align-bottom"
								title={n.original_transaction_id ?? undefined}
							>
								{n.original_transaction_id ?? "—"}
							</span>
						</td>
						<td className="px-5 py-2.5 text-right whitespace-nowrap tabular-nums">
							{n.txn?.price_millis != null ? (
								formatMoney(n.txn.price_millis, n.txn.currency ?? "USD")
							) : (
								<span className="text-muted">—</span>
							)}
						</td>
						<td className="px-3 py-2.5 text-right whitespace-nowrap text-muted">›</td>
					</tr>
				);
			})}
			{selected && typeof document !== "undefined"
				? createPortal(
						<NotificationModal notification={selected} onClose={() => setSelected(null)} />,
						document.body,
					)
				: null}
		</>
	);
}

function NotificationModal({
	notification,
	onClose,
}: {
	notification: NotificationRow;
	onClose: () => void;
}) {
	useEffect(() => {
		const onKey = (e: KeyboardEvent) => {
			if (e.key === "Escape") onClose();
		};
		document.addEventListener("keydown", onKey);
		const prevOverflow = document.body.style.overflow;
		document.body.style.overflow = "hidden";
		return () => {
			document.removeEventListener("keydown", onKey);
			document.body.style.overflow = prevOverflow;
		};
	}, [onClose]);

	const n = notification;
	const t = n.txn;
	const refunded = t?.revocation_date != null;

	return (
		<div className="fixed inset-0 z-50 flex items-center justify-center p-4">
			<button
				type="button"
				aria-label="关闭"
				onClick={onClose}
				className="absolute inset-0 bg-black/50 backdrop-blur-sm"
			/>
			<div
				role="dialog"
				aria-modal="true"
				className="relative z-10 w-full max-w-lg overflow-hidden rounded-xl border border-border bg-surface shadow-xl"
			>
				<div className="flex items-start justify-between gap-3 border-b border-border px-5 py-4">
					<div className="min-w-0">
						<div className="flex flex-wrap items-center gap-1.5">
							<PlatformBadge platform={n.platform} />
							<Badge tone={notificationTone(n.notification_type)}>
								{notificationLabel(n.notification_type)}
							</Badge>
							{subtypeLabel(n.subtype) ? <Badge tone="muted">{subtypeLabel(n.subtype)}</Badge> : null}
						</div>
						<p className="mt-1.5 text-xs text-muted">
							<TimeText ms={n.received_at} />
						</p>
					</div>
					<button
						type="button"
						onClick={onClose}
						aria-label="关闭"
						className="rounded-md px-2 py-1 text-muted transition-colors hover:bg-foreground/[0.06] hover:text-foreground"
					>
						✕
					</button>
				</div>

				<div className="px-5 py-4">
					{t ? (
						<>
							{refunded ? (
								<div className="mb-4 rounded-lg bg-negative/10 px-3 py-2 text-xs text-negative">
									已退款 · <TimeText ms={t.revocation_date} />
									{revocationReasonLabel(t.revocation_reason)
										? ` · ${revocationReasonLabel(t.revocation_reason)}`
										: ""}
								</div>
							) : null}
							<dl className="grid grid-cols-2 gap-x-4 gap-y-3">
								<Field label="产品">{productLabel(t.product_id)}</Field>
								<Field label="类型">{txTypeLabel(t.type)}</Field>
								<Field label="金额">
									{formatMoney(t.price_millis, t.currency ?? "USD")}{" "}
									<span className="text-muted">{t.currency}</span>
								</Field>
								<Field label="优惠">{offerTypeLabel(t.offer_type) ?? "无"}</Field>
								<Field label="购买时间">
									<TimeText ms={t.purchase_date} />
								</Field>
								{t.expires_date ? (
									<Field label="到期时间">
										<TimeText ms={t.expires_date} />
									</Field>
								) : null}
								<Field label="状态">
									{refunded ? (
										<span className="text-negative">已退款</span>
									) : (
										<span className="text-positive">正常</span>
									)}
								</Field>
							</dl>
							<div className="mt-4 space-y-1.5 border-t border-border pt-3">
								<IdRow label="交易 ID" id={t.transaction_id} />
								<IdRow label="原始交易 ID" id={n.original_transaction_id} />
								<IdRow label="通知 UUID" id={n.notification_uuid} />
							</div>
						</>
					) : (
						<div className="text-sm text-muted">
							<p>
								该通知未关联到本地交易记录
								{n.transaction_id
									? `（交易 ID ${n.transaction_id} 不在交易表中）`
									: "（通知不含交易 ID）"}
								。
							</p>
							<div className="mt-3 space-y-1.5">
								<IdRow label="原始交易 ID" id={n.original_transaction_id} />
								<IdRow label="通知 UUID" id={n.notification_uuid} />
							</div>
						</div>
					)}
				</div>
			</div>
		</div>
	);
}

function Field({ label, children }: { label: string; children: ReactNode }) {
	return (
		<div>
			<dt className="text-[11px] text-muted">{label}</dt>
			<dd className="mt-0.5 text-sm font-medium tabular-nums">{children}</dd>
		</div>
	);
}

function IdRow({ label, id }: { label: string; id: string | null }) {
	return (
		<div className="flex items-start justify-between gap-3 text-[11px]">
			<span className="shrink-0 text-muted">{label}</span>
			{/* Play 的 purchaseToken 是长串，允许换行以免撑破弹窗 */}
			<span className="max-w-[70%] font-mono break-all text-muted">{id ?? "—"}</span>
		</div>
	);
}
