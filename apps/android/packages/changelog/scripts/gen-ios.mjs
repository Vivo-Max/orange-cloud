// CODEGEN —— 由 ios.json 生成 iOS 的「新功能」内容。运行：`pnpm changelog:gen`（或 gen:ios）。
// 产出两个 codegen 独占文件（folder-sync 自动纳入 target，无需改 pbxproj）：
//   Core/WhatsNew/WhatsNewReleases.generated.swift  —— releases 数组（仅 inApp 版本）
//   Core/WhatsNew/WhatsNew.xcstrings                —— 13 语字符串目录（table = "WhatsNew"）
// 手维护的 WhatsNew.swift 只引用 WhatsNewGenerated.releases；Localizable.xcstrings 完全不动。

import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { emit, finalize } from "./_emit.mjs";

const here = dirname(fileURLToPath(import.meta.url));
const repo = join(here, "..", "..", "..");
const IOS = join(repo, "apps/ios/Orange Cloud/Orange Cloud/Core/WhatsNew");

const LOCALES = ["zh-Hans", "en", "zh-Hant", "zh-HK", "ja", "es-MX", "ko", "pt-BR", "pt-PT", "de", "fr", "ar", "tr"];
const releases = JSON.parse(readFileSync(join(here, "..", "ios.json"), "utf8"));
const isNewer = (a, b) => a.localeCompare(b, undefined, { numeric: true }) > 0;

const inApp = releases
  .filter((r) => r.inApp !== false)
  .sort((a, b) => (isNewer(a.version, b.version) ? -1 : 1));

const swiftStr = (s) => '"' + s.replace(/\\/g, "\\\\").replace(/"/g, '\\"') + '"';

// ---- WhatsNewReleases.generated.swift ----
const body = inApp
  .map((r) => {
    const items = r.items
      .map((it) => {
        const lines = [
          `                icon:   ${swiftStr(it.icon ?? "sparkles")},`,
          `                title:  String(localized: ${swiftStr(it.title["zh-Hans"])}, table: "WhatsNew")` + (it.detail ? "," : ""),
        ];
        if (it.detail) lines.push(`                detail: String(localized: ${swiftStr(it.detail["zh-Hans"])}, table: "WhatsNew")`);
        return `            WhatsNewItem(\n${lines.join("\n")}\n            )`;
      })
      .join(",\n");
    return `        WhatsNewRelease(version: ${swiftStr(r.version)}, items: [\n${items}\n        ])`;
  })
  .join(",\n");

const swift = `//
//  WhatsNewReleases.generated.swift
//  Orange Cloud
//
//  ⚠️ 自动生成 —— 请勿手改。改 packages/changelog/ios.json 后运行 \`pnpm changelog:gen\`。
//  字符串走 WhatsNew.xcstrings（table: "WhatsNew"），与 Localizable.xcstrings 解耦。
//

import Foundation

nonisolated enum WhatsNewGenerated {
    static let releases: [WhatsNewRelease] = [
${body}
    ]
}
`;
emit(join(IOS, "WhatsNewReleases.generated.swift"), swift);

// ---- WhatsNew.xcstrings ----
const strings = {};
for (const r of inApp) {
  for (const it of r.items) {
    for (const map of [it.title, it.detail].filter(Boolean)) {
      const key = map["zh-Hans"];
      const localizations = {};
      for (const loc of LOCALES) {
        if (loc === "zh-Hans") continue;
        if (map[loc] != null) localizations[loc] = { stringUnit: { state: "translated", value: map[loc] } };
      }
      strings[key] = { localizations };
    }
  }
}
const sorted = {};
for (const k of Object.keys(strings).sort()) sorted[k] = strings[k];
const catalog = { sourceLanguage: "zh-Hans", strings: sorted, version: "1.0" };
emit(join(IOS, "WhatsNew.xcstrings"), JSON.stringify(catalog, null, 2) + "\n");

console.log(`✅ gen-ios: ${inApp.length} releases, ${Object.keys(sorted).length} 条字符串 → WhatsNewReleases.generated.swift + WhatsNew.xcstrings`);
finalize("gen-ios");
