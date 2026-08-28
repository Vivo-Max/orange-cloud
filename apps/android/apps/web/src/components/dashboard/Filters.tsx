"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useTransition } from "react";
import {
	PLATFORM_LABEL,
	PLATFORM_ORDER,
	PRODUCT_LABEL,
	PRODUCT_ORDER,
	RANGE_OPTIONS,
} from "@/lib/dashboard/types";

interface Option {
	value: string;
	label: string;
}

const PRODUCT_OPTIONS: Option[] = [
	{ value: "all", label: "全部" },
	...PRODUCT_ORDER.map((id) => ({ value: id, label: PRODUCT_LABEL[id] })),
];

const PLATFORM_OPTIONS: Option[] = [
	{ value: "all", label: "全部" },
	...PLATFORM_ORDER.map((id) => ({ value: id, label: PLATFORM_LABEL[id] })),
];

// Changing any filter resets the per-list page cursors.
const PAGE_PARAMS = ["tx_page", "notif_page"];

export function Filters() {
	const router = useRouter();
	const pathname = usePathname();
	const params = useSearchParams();
	const [pending, startTransition] = useTransition();

	const current = {
		days: params.get("days") ?? "all",
		product: params.get("product") ?? "all",
		platform: params.get("platform") ?? "all",
	};

	function setParam(key: string, value: string, defaultValue: string) {
		const next = new URLSearchParams(params.toString());
		if (value === defaultValue) next.delete(key);
		else next.set(key, value);
		for (const p of PAGE_PARAMS) next.delete(p);
		const qs = next.toString();
		startTransition(() => {
			router.push(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
		});
	}

	return (
		<div
			className={`flex flex-wrap items-end gap-x-6 gap-y-4 transition-opacity ${pending ? "opacity-60" : ""}`}
		>
			<Segmented
				label="时间范围"
				options={RANGE_OPTIONS.map((o) => ({ value: o.value, label: o.label }))}
				value={current.days}
				onSelect={(v) => setParam("days", v, "all")}
			/>
			<Segmented
				label="产品"
				options={PRODUCT_OPTIONS}
				value={current.product}
				onSelect={(v) => setParam("product", v, "all")}
			/>
			<Segmented
				label="平台"
				options={PLATFORM_OPTIONS}
				value={current.platform}
				onSelect={(v) => setParam("platform", v, "all")}
			/>
		</div>
	);
}

function Segmented({
	label,
	options,
	value,
	onSelect,
}: {
	label: string;
	options: Option[];
	value: string;
	onSelect: (value: string) => void;
}) {
	return (
		<div className="flex flex-col gap-1.5">
			<span className="text-[11px] font-medium tracking-wide text-muted uppercase">{label}</span>
			<div className="inline-flex flex-wrap gap-1 rounded-lg border border-border bg-surface-2 p-1">
				{options.map((o) => {
					const active = o.value === value;
					return (
						<button
							key={o.value}
							type="button"
							onClick={() => onSelect(o.value)}
							aria-pressed={active}
							className={`rounded-md px-2.5 py-1 text-xs font-medium transition-colors ${
								active ? "bg-accent text-white shadow-sm" : "text-muted hover:text-foreground"
							}`}
						>
							{o.label}
						</button>
					);
				})}
			</div>
		</div>
	);
}
