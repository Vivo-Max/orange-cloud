// 发布说明单一数据源（每平台一份）。App 端由 scripts/gen-*.mjs 生成 What's New，
// 官网由本模块直接 import。新增一版只改 ios.json / android.json，再跑 `pnpm changelog:gen`。

import iosData from "./ios.json";
import androidData from "./android.json";

/** 9 语 canonical 码（BCP-47）。Android 资源目录码映射在 scripts/gen-android.mjs。 */
export const LOCALES = [
  "zh-Hans",
  "en",
  "zh-Hant",
  "zh-HK",
  "ja",
  "es-MX",
  "ko",
  "pt-BR",
  "pt-PT",
] as const;
export type Locale = (typeof LOCALES)[number];

/** locale -> 文案。缺某 locale 时由 localize() 回退 en -> zh-Hans。 */
export type Localized = Partial<Record<Locale, string>>;

export interface ChangelogItem {
  /** iOS = SF Symbol；Android/历史条目可缺省（官网渲染为无图标项目符号）。 */
  icon?: string;
  title: Localized;
  detail?: Localized;
}

export interface Release {
  /** 平台版本号（须 == 该平台的 MARKETING_VERSION / versionName，历史 TestFlight 条目可含 build，如 "1.0 (5)"）。 */
  version: string;
  date: string;
  /** 渠道徽章文案，如 "App Store" / "TestFlight β" / "Google Play"。 */
  channel: string;
  /** 是否进入 App 内「新功能」弹窗（codegen 只取 true）。历史/纯官网条目设 false。默认 true。 */
  inApp?: boolean;
  /** 官网是否展示。默认 true；Phase 4 由 D1 live_versions 接管未发布版本的门控。 */
  live?: boolean;
  items: ChangelogItem[];
}

export type Track = "ios" | "android";

export const ios: Release[] = iosData as Release[];
export const android: Release[] = androidData as Release[];
export const tracks: Record<Track, Release[]> = { ios, android };

/** 数值分段比较：a 是否比 b 新（"1.3.0" > "1.2.1"，"1.0 (5)" > "1.0 (4)"）。 */
export function isNewer(a: string, b: string): boolean {
  return a.localeCompare(b, undefined, { numeric: true }) > 0;
}

/** 进入 App 内 What's New 的 release（inApp !== false），新版在前。 */
export function inAppReleases(track: Track): Release[] {
  return tracks[track]
    .filter((r) => r.inApp !== false)
    .slice()
    .sort((a, b) => (isNewer(a.version, b.version) ? -1 : 1));
}

/** 官网展示状态：已上架 / 审核中 / 已过审待发布。 */
export type ReleaseStatus = "live" | "in_review" | "pending_release";
export type PendingState = "in_review" | "pending_release";

/** 某 track 的上架状态（来自 D1 release_state）。 */
export interface TrackState {
  /** 最新已上架版本（单调前进）。 */
  liveVersion?: string;
  /** 在审 / 待发布版本（可空）。 */
  pendingVersion?: string;
  pendingState?: PendingState;
}

/**
 * 官网某 track 应展示的 release 及其状态（新版在前）：
 *  - 命中 pendingVersion → in_review / pending_release（官网标「审核中」）
 *  - live === true 或 version <= liveVersion → live（正常展示）
 *  - 其余（已提交代码但未送审 / 更新版本）→ 不展示
 */
export function decoratedReleases(
  track: Track,
  state: TrackState | null | undefined,
): Array<{ release: Release; status: ReleaseStatus }> {
  const st = state ?? {};
  const out: Array<{ release: Release; status: ReleaseStatus }> = [];
  for (const r of tracks[track]) {
    let status: ReleaseStatus | null = null;
    if (st.pendingVersion && r.version === st.pendingVersion) status = st.pendingState ?? "in_review";
    else if (r.live === true) status = "live";
    else if (st.liveVersion != null && !isNewer(r.version, st.liveVersion)) status = "live";
    if (status) out.push({ release: r, status });
  }
  return out.sort((a, b) => (isNewer(a.release.version, b.release.version) ? -1 : 1));
}

/** 某 track 的最新版本号（数值最大），无则 null。go-live 信号用它解析「哪个版本上架了」。 */
export function latestVersion(track: Track): string | null {
	const versions = tracks[track].map((r) => r.version);
	if (versions.length === 0) return null;
	return versions.reduce((a, b) => (isNewer(b, a) ? b : a));
}

/** 取某 locale 文案，缺则回退 en -> zh-Hans。 */
export function localize(map: Localized | undefined, locale: string): string | undefined {
  if (!map) return undefined;
  return map[locale as Locale] ?? map.en ?? map["zh-Hans"];
}
