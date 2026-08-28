import Link from "next/link";
import type { ReactNode } from "react";
import { TimeText } from "@/components/dashboard/prefs";
import { Badge, Card, CardHead, EmptyState, PlatformBadge } from "@/components/dashboard/ui";
import { formatMoney, formatRelativeFuture } from "@/lib/dashboard/format";
import type { ExpiringRow, NotificationRow, Page, TransactionRow } from "@/lib/dashboard/queries";
import { offerTypeLabel, productLabel, txTypeLabel } from "@/lib/dashboard/types";
import { NotificationRows } from "./NotificationRows";

function MonoId({ id }: { id: string | null }) {
	if (!id) return <span className="text-muted">—</span>;
	return (
		<span className="font-mono text-[11px] text-muted" title={id}>
			{id}
		</span>
	);
}

// ---------------------------------------------------------------------------
// Pagination (server-rendered links that preserve all other search params)
// ---------------------------------------------------------------------------

function Pagination({
	page,
	totalPages,
	total,
	paramKey,
	query,
}: {
	page: number;
	totalPages: number;
	total: number;
	paramKey: string;
	query: string;
}) {
	const href = (p: number) => {
		const sp = new URLSearchParams(query);
		if (p <= 1) sp.delete(paramKey);
		else sp.set(paramKey, String(p));
		const qs = sp.toString();
		return qs ? `/admin?${qs}` : "/admin";
	};

	return (
		<div className="flex items-center justify-between border-t border-border px-5 py-2.5 text-xs text-muted">
			<span className="tabular-nums">
				共 {total} 条 · 第 {page}/{totalPages} 页
			</span>
			<div className="flex gap-1.5">
				<PageLink href={href(page - 1)} disabled={page <= 1}>
					上一页
				</PageLink>
				<PageLink href={href(page + 1)} disabled={page >= totalPages}>
					下一页
				</PageLink>
			</div>
		</div>
	);
}

function PageLink({
	href,
	disabled,
	children,
}: {
	href: string;
	disabled: boolean;
	children: ReactNode;
}) {
	const base = "rounded-md border border-border px-2 py-1 font-medium";
	if (disabled) {
		return <span className={`${base} opacity-40`}>{children}</span>;
	}
	return (
		<Link href={href} scroll={false} className={`${base} transition-colors hover:bg-foreground/[0.06]`}>
			{children}
		</Link>
	);
}

// ---------------------------------------------------------------------------
// Shared table chrome
// ---------------------------------------------------------------------------

function Th({ children, className = "" }: { children: ReactNode; className?: string }) {
	return <th className={`px-5 py-2 font-medium whitespace-nowrap ${className}`}>{children}</th>;
}

function Td({ children, className = "" }: { children: ReactNode; className?: string }) {
	return <td className={`px-5 py-2.5 whitespace-nowrap ${className}`}>{children}</td>;
}

// ---------------------------------------------------------------------------
// Notifications (paginated, rows are clickable -> detail modal)
// ---------------------------------------------------------------------------

export function NotificationsTable({ data, query }: { data: Page<NotificationRow>; query: string }) {
	return (
		<Card className="flex flex-col">
			<CardHead title="通知" hint="点击任意一行查看关联交易详情，支持翻页" />
			{data.total === 0 ? (
				<EmptyState label="该筛选下暂无通知" />
			) : (
				<>
					<div className="scroll-area mt-2 overflow-x-auto">
						<table className="w-full text-sm">
							<thead>
								<tr className="border-b border-border text-left text-xs text-muted">
									<Th>时间</Th>
									<Th>平台</Th>
									<Th>类型</Th>
									<Th>子类型</Th>
									<Th>原始交易 ID</Th>
									<Th className="text-right">金额</Th>
									<Th> </Th>
								</tr>
							</thead>
							<tbody>
								<NotificationRows rows={data.rows} />
							</tbody>
						</table>
					</div>
					<Pagination
						page={data.page}
						totalPages={data.totalPages}
						total={data.total}
						paramKey="notif_page"
						query={query}
					/>
				</>
			)}
		</Card>
	);
}

// ---------------------------------------------------------------------------
// Transactions (paginated)
// ---------------------------------------------------------------------------

export function TransactionsTable({ data, query }: { data: Page<TransactionRow>; query: string }) {
	return (
		<Card className="flex flex-col">
			<CardHead title="交易" hint="按时间倒序，支持翻页" />
			{data.total === 0 ? (
				<EmptyState label="该筛选下暂无交易" />
			) : (
				<>
					<div className="scroll-area mt-2 overflow-x-auto">
						<table className="w-full text-sm">
							<thead>
								<tr className="border-b border-border text-left text-xs text-muted">
									<Th>时间</Th>
									<Th>平台</Th>
									<Th>交易 ID</Th>
									<Th>类型</Th>
									<Th>产品</Th>
									<Th>商店</Th>
									<Th className="text-right">金额</Th>
									<Th>优惠</Th>
									<Th>优惠标识</Th>
									<Th>状态</Th>
								</tr>
							</thead>
							<tbody>
								{data.rows.map((t) => {
									const offer = offerTypeLabel(t.offer_type);
									return (
										<tr key={t.transaction_id} className="border-b border-border/50 last:border-0">
											<Td className="text-muted">
												<TimeText ms={t.purchase_date} />
											</Td>
											<Td>
												<PlatformBadge platform={t.platform} />
											</Td>
											<Td>
												<MonoId id={t.transaction_id} />
											</Td>
											<Td>{txTypeLabel(t.type)}</Td>
											<Td>{productLabel(t.product_id)}</Td>
											<Td>
												{t.storefront ? (
													<span className="font-mono text-[11px]">{t.storefront}</span>
												) : (
													<span className="text-muted">—</span>
												)}
											</Td>
											<Td className="text-right tabular-nums">
												{formatMoney(t.price_millis, t.currency ?? "USD")}
												<span className="ml-1 text-[11px] text-muted">{t.currency}</span>
											</Td>
											<Td>
												{offer ? (
													<Badge tone="accent">{offer}</Badge>
												) : (
													<span className="text-muted">—</span>
												)}
											</Td>
											<Td>
												<MonoId id={t.offer_identifier} />
											</Td>
											<Td>
												{t.revocation_date ? (
													<Badge tone="negative">已退款</Badge>
												) : (
													<Badge tone="positive">正常</Badge>
												)}
											</Td>
										</tr>
									);
								})}
							</tbody>
						</table>
					</div>
					<Pagination
						page={data.page}
						totalPages={data.totalPages}
						total={data.total}
						paramKey="tx_page"
						query={query}
					/>
				</>
			)}
		</Card>
	);
}

// ---------------------------------------------------------------------------
// Expiring soon (forward-looking list, not paginated)
// ---------------------------------------------------------------------------

export function ExpiringSoonTable({ rows }: { rows: ExpiringRow[] }) {
	return (
		<Card>
			<CardHead title="即将到期订阅" hint="有效的年/月订阅，按到期时间升序" />
			{rows.length === 0 ? (
				<EmptyState label="暂无即将到期的订阅" />
			) : (
				<div className="scroll-area mt-2 overflow-x-auto">
					<table className="w-full text-sm">
						<thead>
							<tr className="border-b border-border text-left text-xs text-muted">
								<Th>到期</Th>
								<Th>剩余</Th>
								<Th>平台</Th>
								<Th>产品</Th>
								<Th className="text-right">金额</Th>
								<Th>自动续订</Th>
							</tr>
						</thead>
						<tbody>
							{rows.map((s) => (
								<tr
									key={s.original_transaction_id}
									className="border-b border-border/50 last:border-0"
								>
									<Td className="text-muted">
										<TimeText ms={s.expires_date} />
									</Td>
									<Td>{formatRelativeFuture(s.expires_date)}</Td>
									<Td>
										<PlatformBadge platform={s.platform} />
									</Td>
									<Td>{productLabel(s.product_id)}</Td>
									<Td className="text-right tabular-nums">
										{formatMoney(s.price_millis, s.currency ?? "USD")}
										<span className="ml-1 text-[11px] text-muted">{s.currency}</span>
									</Td>
									<Td>
										{s.auto_renew_status === 1 ? (
											<Badge tone="positive">开启</Badge>
										) : (
											<Badge tone="muted">关闭</Badge>
										)}
									</Td>
								</tr>
							))}
						</tbody>
					</table>
				</div>
			)}
		</Card>
	);
}
