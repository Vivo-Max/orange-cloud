// 漂移守卫：以 check 模式跑两个 codegen（只比对不写盘）。
// 任一生成文件与 ios.json/android.json 不同步即非零退出 —— 适合 CI / pre-commit。
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const here = dirname(fileURLToPath(import.meta.url));
const env = { ...process.env, CHANGELOG_CHECK: "1" };

let code = 0;
for (const script of ["gen-ios.mjs", "gen-android.mjs"]) {
  const r = spawnSync(process.execPath, [join(here, script)], { stdio: "inherit", env });
  if (r.status !== 0) code = 1;
}
if (code === 0) console.log("✅ 生成文件与 changelog 源同步");
else console.error("→ 运行 `pnpm changelog:gen` 重新生成后提交。");
process.exit(code);
