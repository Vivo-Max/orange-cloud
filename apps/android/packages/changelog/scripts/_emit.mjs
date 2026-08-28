// 共享写出助手：幂等（内容相同不写，避免 mtime 抖动）+ 支持 check 模式（只比对不写）。
// CHANGELOG_CHECK=1 时，凡内容与磁盘不一致即记为 drift 并最终非零退出。
import { readFileSync, writeFileSync, existsSync } from "node:fs";

const CHECK = process.env.CHANGELOG_CHECK === "1";
const drift = [];

/** 内容与现有文件一致则跳过；check 模式下记 drift；否则写盘。 */
export function emit(path, content) {
  const cur = existsSync(path) ? readFileSync(path, "utf8") : null;
  if (cur === content) return false;
  if (CHECK) {
    drift.push(path);
    return false;
  }
  writeFileSync(path, content);
  return true;
}

/** 收尾：check 模式下若有 drift 则报告并置非零退出码。 */
export function finalize(label) {
  if (CHECK && drift.length) {
    console.error(`❌ ${label}: ${drift.length} 个生成文件与源不同步 —— 请运行 \`pnpm changelog:gen\` 后提交：`);
    for (const p of drift) console.error("   - " + p);
    process.exitCode = 1;
  }
}

export const isCheck = CHECK;
