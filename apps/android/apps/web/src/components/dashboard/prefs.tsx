"use client";

import { createContext, type ReactNode, useContext, useEffect, useState } from "react";
import { formatTs, type TimeZonePref } from "@/lib/dashboard/format";

export type DisplayCurrency = "USD" | "CNY";

interface PrefsValue {
	tz: TimeZonePref;
	setTz: (tz: TimeZonePref) => void;
	currency: DisplayCurrency;
	setCurrency: (c: DisplayCurrency) => void;
}

const PrefsContext = createContext<PrefsValue | null>(null);

const TZ_KEY = "ocd_tz";
const CUR_KEY = "ocd_cur";

export function PreferencesProvider({ children }: { children: ReactNode }) {
	// Default to UTC/USD so the server render and first client render match
	// (no hydration mismatch); real preferences load from localStorage on mount.
	const [tz, setTz] = useState<TimeZonePref>("UTC");
	const [currency, setCurrency] = useState<DisplayCurrency>("USD");
	const [loaded, setLoaded] = useState(false);

	useEffect(() => {
		const t = localStorage.getItem(TZ_KEY);
		if (t === "UTC" || t === "Asia/Shanghai") setTz(t);
		const c = localStorage.getItem(CUR_KEY);
		if (c === "USD" || c === "CNY") setCurrency(c);
		setLoaded(true);
	}, []);

	useEffect(() => {
		if (loaded) localStorage.setItem(TZ_KEY, tz);
	}, [tz, loaded]);
	useEffect(() => {
		if (loaded) localStorage.setItem(CUR_KEY, currency);
	}, [currency, loaded]);

	return (
		<PrefsContext.Provider value={{ tz, setTz, currency, setCurrency }}>
			{children}
		</PrefsContext.Provider>
	);
}

export function usePrefs(): PrefsValue {
	const ctx = useContext(PrefsContext);
	if (!ctx) throw new Error("usePrefs must be used within PreferencesProvider");
	return ctx;
}

/** Renders an epoch-ms timestamp in the user's chosen timezone. */
export function TimeText({
	ms,
	mode = "datetime",
}: {
	ms: number | null | undefined;
	mode?: "datetime" | "date";
}) {
	const { tz } = usePrefs();
	if (ms == null) return <>—</>;
	return <>{formatTs(ms, tz, mode)}</>;
}

const SEG_BTN = "rounded-md px-2 py-0.5 text-[11px] font-medium transition-colors";

/** Header control to switch all displayed times between UTC and Beijing time. */
export function TimezoneToggle() {
	const { tz, setTz } = usePrefs();
	const options: { value: TimeZonePref; label: string }[] = [
		{ value: "UTC", label: "UTC" },
		{ value: "Asia/Shanghai", label: "北京" },
	];
	return (
		<div className="inline-flex items-center gap-1 rounded-lg border border-border bg-surface-2 p-0.5">
			{options.map((o) => (
				<button
					key={o.value}
					type="button"
					onClick={() => setTz(o.value)}
					aria-pressed={tz === o.value}
					className={`${SEG_BTN} ${tz === o.value ? "bg-accent text-white" : "text-muted hover:text-foreground"}`}
				>
					{o.label}
				</button>
			))}
		</div>
	);
}

/** "更新于 <time> <tz label>", reflecting the timezone toggle. */
export function UpdatedAt({ ms }: { ms: number }) {
	const { tz } = usePrefs();
	return (
		<span className="text-xs whitespace-nowrap text-muted">
			更新于 {formatTs(ms, tz)} {tz === "UTC" ? "UTC" : "北京时间"}
		</span>
	);
}
