import { getUsdRates } from "@/lib/dashboard/fx";
import {
	fetchCharts,
	fetchExpiring,
	fetchNotifications,
	fetchOverview,
	fetchTransactions,
} from "@/lib/dashboard/queries";
import { fetchRankHistory } from "@/lib/dashboard/ranks";
import { type Filters, PAGE_SIZE } from "@/lib/dashboard/types";
import {
	NotificationTrendCard,
	ProductMixCard,
	PurchaseTrendCard,
	RanksCard,
	StatusBreakdownCard,
} from "./Charts";
import { KpiStats, RevenueCard } from "./Kpis";
import { PlatformSplitCard } from "./PlatformSplit";
import { ExpiringSoonTable, NotificationsTable, TransactionsTable } from "./Tables";

// Async server components — each awaits only its own slice of data and is
// rendered inside a <Suspense> boundary on the page, so it streams independently.

export async function OverviewSection({ filters }: { filters: Filters }) {
	const [overview, fx] = await Promise.all([fetchOverview(filters), getUsdRates()]);
	return (
		<>
			<KpiStats overview={overview} />
			<RevenueCard overview={overview} fx={fx} />
			<PlatformSplitCard platforms={overview.platforms} fx={fx} />
		</>
	);
}

export async function RanksSection() {
	const history = await fetchRankHistory(30);
	return <RanksCard history={history} />;
}

export async function ChartsSection({ filters }: { filters: Filters }) {
	const { purchaseTrend, notificationTrend, productMix, statusBreakdown } =
		await fetchCharts(filters);
	return (
		<>
			<div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
				<div className="lg:col-span-2">
					<PurchaseTrendCard series={purchaseTrend} />
				</div>
				<ProductMixCard data={productMix} />
			</div>
			<div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
				<div className="lg:col-span-2">
					<NotificationTrendCard series={notificationTrend} />
				</div>
				<StatusBreakdownCard data={statusBreakdown} />
			</div>
		</>
	);
}

export async function TransactionsSection({
	filters,
	page,
	query,
}: {
	filters: Filters;
	page: number;
	query: string;
}) {
	const data = await fetchTransactions(filters, page, PAGE_SIZE);
	return <TransactionsTable data={data} query={query} />;
}

export async function NotificationsSection({
	filters,
	page,
	query,
}: {
	filters: Filters;
	page: number;
	query: string;
}) {
	const data = await fetchNotifications(filters, page, PAGE_SIZE);
	return <NotificationsTable data={data} query={query} />;
}

export async function ExpiringSection({ filters }: { filters: Filters }) {
	const rows = await fetchExpiring(filters);
	return <ExpiringSoonTable rows={rows} />;
}
