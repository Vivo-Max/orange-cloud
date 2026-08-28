// CODEGEN —— 由 android.json 生成 Android 的「新功能」内容。运行：`pnpm changelog:gen`（或 gen:android）。
// 产出 codegen 独占文件：
//   core/whatsnew/WhatsNewReleases.generated.kt   —— releases 列表
//   res/values*/whatsnew.xml                      —— 9 语字符串（Android 自动 merge 进 resources）
// 并幂等地从各 strings.xml 移除旧的 whatsnew_<数字>_* 条目（保留 whatsnew_title / whatsnew_ok 弹窗静态文案）。
// 手维护的 WhatsNew.kt 只把 releases 指向 whatsNewReleases。

import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { emit, finalize } from "./_emit.mjs";

const here = dirname(fileURLToPath(import.meta.url));
const repo = join(here, "..", "..", "..");
const RES = join(repo, "apps/android/app/src/main/res");
const KT = join(repo, "apps/android/app/src/main/kotlin/jiamin/chen/orangecloud/core/whatsnew");

const LOCALES = ["zh-Hans", "en", "zh-Hant", "zh-HK", "ja", "es-MX", "ko", "pt-BR", "pt-PT", "de", "fr", "ar", "tr"];
const DIR = {
  "zh-Hans": "values",
  en: "values-en",
  "zh-Hant": "values-zh-rTW",
  "zh-HK": "values-zh-rHK",
  ja: "values-ja",
  "es-MX": "values-es",
  ko: "values-ko",
  "pt-BR": "values-pt-rBR",
  "pt-PT": "values-pt-rPT",
  de: "values-de",
  fr: "values-fr",
  ar: "values-ar",
  tr: "values-tr",
};

const releases = JSON.parse(readFileSync(join(here, "..", "android.json"), "utf8"));
const isNewer = (a, b) => a.localeCompare(b, undefined, { numeric: true }) > 0;
const inApp = releases
  .filter((r) => r.inApp !== false)
  .sort((a, b) => (isNewer(a.version, b.version) ? -1 : 1));

const verKey = (v) => v.replace(/[^0-9A-Za-z]+/g, "_").replace(/_+$/, "");
const keyOf = (v, i) => `whatsnew_${verKey(v)}_${i}`;

// ---- WhatsNewReleases.generated.kt ----
const ktBody = inApp
  .map((r) => {
    const items = r.items
      .map((_, i) => `            WhatsNewItem(R.string.${keyOf(r.version, i)}_title, R.string.${keyOf(r.version, i)}_detail),`)
      .join("\n");
    return `    WhatsNewRelease(\n        version = "${r.version}",\n        items = listOf(\n${items}\n        ),\n    )`;
  })
  .join(",\n");

const kt = `package jiamin.chen.orangecloud.core.whatsnew

import jiamin.chen.orangecloud.R

// ⚠️ 自动生成 —— 请勿手改。改 packages/changelog/android.json 后运行 \`pnpm changelog:gen\`。
internal val whatsNewReleases: List<WhatsNewRelease> = listOf(
${ktBody},
)
`;
emit(join(KT, "WhatsNewReleases.generated.kt"), kt);

// ---- res/values*/whatsnew.xml + 清理 strings.xml ----
const xmlEsc = (s) =>
  s
    .replace(/\\/g, "\\\\")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/'/g, "\\'")
    .replace(/"/g, '\\"')
    .replace(/\n/g, "\\n");

for (const loc of LOCALES) {
  const rows = [];
  inApp.forEach((r) => {
    r.items.forEach((it, i) => {
      rows.push(`    <string name="${keyOf(r.version, i)}_title">${xmlEsc(it.title[loc] ?? it.title["en"] ?? it.title["zh-Hans"])}</string>`);
      rows.push(`    <string name="${keyOf(r.version, i)}_detail">${xmlEsc(it.detail?.[loc] ?? it.detail?.["en"] ?? "")}</string>`);
    });
  });
  const xml = `<?xml version="1.0" encoding="utf-8"?>\n<!-- ⚠️ 自动生成 —— 请勿手改。改 packages/changelog/android.json 后运行 \`pnpm changelog:gen\`。 -->\n<resources>\n${rows.join("\n")}\n</resources>\n`;
  emit(join(RES, DIR[loc], "whatsnew.xml"), xml);

  // 幂等移除旧的 whatsnew_<数字>_* 行（保留 whatsnew_title / whatsnew_ok）
  const sp = join(RES, DIR[loc], "strings.xml");
  const after = readFileSync(sp, "utf8")
    .split("\n")
    .filter((l) => !/<string name="whatsnew_\d/.test(l))
    .join("\n");
  emit(sp, after);
}

console.log(`✅ gen-android: ${inApp.length} releases → WhatsNewReleases.generated.kt + ${LOCALES.length}× whatsnew.xml`);
finalize("gen-android");
