import { Card, CardHead, Badge, EmptyState } from "@/components/dashboard/ui";
import { formatNumber, formatTimestamp } from "@/lib/dashboard/format";
import { getDb } from "@/lib/dashboard/db";
import {
	getCodesStats,
	listPendingRefunds,
	listRecentCodes,
	type AdminCode,
} from "@/lib/codes/admin";
import RefundActions from "@/components/dashboard/RefundActions";
import ResetActivationsButton from "@/components/dashboard/ResetActivationsButton";
import GenerateCodes from "@/components/dashboard/GenerateCodes";
import BindEmailButton from "@/components/dashboard/BindEmailButton";
import { MAX_ACTIVATIONS } from "@/lib/codes/redeem";

// 后台「激活码（安卓渠道）」：自有 KPI + 待处理退款 + 近期激活码。
// 与 Apple IAP 看板并列，独立查询 codes / code_activations 两表（迁移 0005 未应用时容错空显示）。

function fmtMinor(minor: number | null, currency: string | null): string {
	if (minor == null || !currency) return "—";
	try {
		return new Intl.NumberFormat("en-US", { style: "currency", currency }).format(minor / 100);
	} catch {
		return `${(minor / 100).toFixed(2)} ${currency}`;
	}
}

function Stat({ label, value, sub }: { label: string; value: string; sub?: string }) {
	return (
		<Card className="p-4">
			<p className="text-xs font-medium text-muted">{label}</p>
			<p className="mt-1.5 text-2xl font-semibold tracking-tight tabular-nums">{value}</p>
			{sub ? <p className="mt-1 text-xs text-muted">{sub}</p> : null}
		</Card>
	);
}

function StatusBadge({ code }: { code: AdminCode }) {
	if (code.status === "revoked") return <Badge tone="negative">已撤销</Badge>;
	if (code.refundStatus === "requested") return <Badge tone="accent">退款申请中</Badge>;
	return <Badge tone="positive">有效</Badge>;
}

export async function CodesSection() {
	const db = await getDb();
	const [stats, refunds, codes] = await Promise.all([
		getCodesStats(db),
		listPendingRefunds(db),
		listRecentCodes(db, 50),
	]);

	const revenue = stats.revenue.length
		? stats.revenue.map((r) => fmtMinor(r.minor, r.currency)).join(" · ")
		: "—";

	return (
		<div className="flex flex-col gap-4">
			<div className="flex items-center gap-2.5">
				<span className="h-2 w-2 rounded-full bg-accent" />
				<h2 className="text-sm font-semibold tracking-tight">激活码 · 安卓渠道</h2>
				<span className="text-xs text-muted">非 Play 中国大陆买断（Stripe）</span>
			</div>

			<div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
				<Stat label="售出" value={formatNumber(stats.sold)} />
				<Stat label="收入（有效）" value={revenue} />
				<Stat label="有效" value={formatNumber(stats.active)} />
				<Stat label="已撤销" value={formatNumber(stats.revoked)} />
				<Stat
					label="待处理退款"
					value={formatNumber(stats.pendingRefunds)}
					sub={stats.pendingRefunds > 0 ? "需审批" : undefined}
				/>
			</div>

			{/* 待处理退款申请 */}
			<Card>
				<CardHead title="待处理退款申请" hint="官网自助申请，30 天政策；通过即发起 Stripe 退款并撤销码" />
				{refunds.length === 0 ? (
					<EmptyState label="暂无待处理退款" />
				) : (
					<div className="scroll-area overflow-x-auto px-5 pt-3 pb-5">
						<table className="w-full text-sm">
							<thead>
								<tr className="border-b border-border text-left text-xs text-muted">
									<th className="py-2 pr-3 font-medium">激活码</th>
									<th className="py-2 pr-3 font-medium">邮箱</th>
									<th className="py-2 pr-3 font-medium">原因</th>
									<th className="py-2 pr-3 font-medium">金额</th>
									<th className="py-2 pr-3 font-medium whitespace-nowrap">申请时间</th>
									<th className="py-2 font-medium">操作</th>
								</tr>
							</thead>
							<tbody>
								{refunds.map((r) => (
									<tr key={r.code} className="border-b border-border/60 align-top">
										<td className="py-2.5 pr-3 font-mono text-xs whitespace-nowrap">{r.display}</td>
										<td className="py-2.5 pr-3 text-muted">{r.buyerEmail ?? "—"}</td>
										<td className="max-w-[20ch] py-2.5 pr-3 text-muted">{r.reason || "—"}</td>
										<td className="py-2.5 pr-3 tabular-nums whitespace-nowrap">{fmtMinor(r.amountTotal, r.currency)}</td>
										<td className="py-2.5 pr-3 text-xs text-muted whitespace-nowrap">{formatTimestamp(r.requestedAt)}</td>
										<td className="py-2.5">
											<RefundActions code={r.code} />
										</td>
									</tr>
								))}
							</tbody>
						</table>
					</div>
				)}
			</Card>

			{/* 近期激活码 */}
			<Card>
				<CardHead title="激活码" hint="最近 50 枚 · 可手动生成分发给媒体/评测" />
				<div className="px-5 pt-3">
					<GenerateCodes />
				</div>
				{codes.length === 0 ? (
					<EmptyState label="暂无激活码（迁移 0005 应用并产生首笔购买后显示）" />
				) : (
					<div className="scroll-area overflow-x-auto px-5 pt-3 pb-5">
						<table className="w-full text-sm">
							<thead>
								<tr className="border-b border-border text-left text-xs text-muted">
									<th className="py-2 pr-3 font-medium">激活码</th>
									<th className="py-2 pr-3 font-medium">状态</th>
									<th className="py-2 pr-3 font-medium">金额</th>
									<th className="py-2 pr-3 font-medium">邮箱</th>
									<th className="py-2 pr-3 font-medium">设备</th>
									<th className="py-2 font-medium whitespace-nowrap">购买时间</th>
								</tr>
							</thead>
							<tbody>
								{codes.map((c) => (
									<tr key={c.code} className="border-b border-border/60">
										<td className="py-2.5 pr-3 whitespace-nowrap">
											<span className="font-mono text-xs">{c.display}</span>
											{c.source !== "stripe" ? (
												<Badge tone="info" className="ml-2">手动</Badge>
											) : null}
											{c.note ? <div className="text-[11px] text-muted">{c.note}</div> : null}
										</td>
										<td className="py-2.5 pr-3">
											<StatusBadge code={c} />
										</td>
										<td className="py-2.5 pr-3 tabular-nums whitespace-nowrap">{fmtMinor(c.amountTotal, c.currency)}</td>
										<td className="py-2.5 pr-3 text-muted">
											<span className="mr-2">{c.buyerEmail ?? "—"}</span>
											<BindEmailButton code={c.code} current={c.buyerEmail} />
										</td>
										<td className="py-2.5 pr-3 tabular-nums text-muted">
											<span className="mr-2">{c.activations}/{MAX_ACTIVATIONS}</span>
											{c.activations > 0 ? <ResetActivationsButton code={c.code} /> : null}
										</td>
										<td className="py-2.5 text-xs text-muted whitespace-nowrap">{formatTimestamp(c.createdAt)}</td>
									</tr>
								))}
							</tbody>
						</table>
					</div>
				)}
			</Card>
		</div>
	);
}
