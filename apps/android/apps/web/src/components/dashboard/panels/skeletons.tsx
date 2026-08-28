import { Card, CardHead } from "@/components/dashboard/ui";

/** A single shimmering placeholder block. */
function Bar({ className = "" }: { className?: string }) {
	return <div className={`animate-pulse rounded bg-foreground/[0.08] ${className}`} />;
}

/** KPI cards + revenue card placeholder (mirrors OverviewSection). */
export function KpiSkeleton() {
	return (
		<>
			<div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
				{Array.from({ length: 4 }).map((_, i) => (
					<Card key={i} className="p-5">
						<Bar className="h-3 w-16" />
						<Bar className="mt-3 h-8 w-20" />
						<Bar className="mt-2 h-3 w-24" />
					</Card>
				))}
			</div>
			<Card>
				<CardHead title="营收（按原币种）" />
				<div className="flex flex-wrap gap-2 px-5 pt-3 pb-5">
					{Array.from({ length: 5 }).map((_, i) => (
						<Bar key={i} className="h-14 w-32" />
					))}
				</div>
			</Card>
		</>
	);
}

function ChartCardSkeleton({ title, height = "h-44" }: { title: string; height?: string }) {
	return (
		<Card>
			<CardHead title={title} />
			<div className="p-5">
				<Bar className={`w-full ${height}`} />
			</div>
		</Card>
	);
}

/** Two chart rows placeholder (mirrors ChartsSection). */
export function ChartsSkeleton() {
	return (
		<>
			<div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
				<div className="lg:col-span-2">
					<ChartCardSkeleton title="每日购买趋势" />
				</div>
				<ChartCardSkeleton title="活跃产品构成" height="h-36" />
			</div>
			<div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
				<div className="lg:col-span-2">
					<ChartCardSkeleton title="每日通知量" />
				</div>
				<ChartCardSkeleton title="订阅状态" height="h-28" />
			</div>
		</>
	);
}

/** Single chart card placeholder (mirrors RanksSection). */
export function RanksSkeleton() {
	return <ChartCardSkeleton title="App Store 排名" height="h-52" />;
}

/** Table placeholder with a header, shimmering rows and a pagination bar. */
export function TableCardSkeleton({ title, rows = 8 }: { title: string; rows?: number }) {
	return (
		<Card className="flex flex-col">
			<CardHead title={title} />
			<div className="mt-3 flex flex-col gap-2.5 px-5 pb-4">
				{Array.from({ length: rows }).map((_, i) => (
					<Bar key={i} className="h-7 w-full" />
				))}
			</div>
			<div className="flex items-center justify-between border-t border-border px-5 py-2.5">
				<Bar className="h-4 w-36" />
				<Bar className="h-6 w-28" />
			</div>
		</Card>
	);
}
