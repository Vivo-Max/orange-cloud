#!/usr/bin/env bash
#
# Orange Cloud — direct（中国大陆 sideload 渠道）一键发布。
#
#   build 签名 APK  →  投放 apps/web/public/  →  据 build.gradle 刷新 latest.json  →  pnpm run deploy
#
# 用法：
#   apps/android/scripts/release-direct.sh ["可选更新说明，写入 latest.json.note 并显示在更新弹窗"]
#
# 前置：apps/android/keystore.properties + 对应 .jks 就位（缺则 release 未签名、装不上）。
# 注意：APK 必须与已装包同签名密钥，否则系统拒绝原地更新。版本号在 app/build.gradle.kts 改（versionCode +1）。
#
set -euo pipefail

ANDROID_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_ROOT="$(cd "$ANDROID_DIR/../.." && pwd)"
WEB_DIR="$REPO_ROOT/apps/web"
GRADLE_FILE="$ANDROID_DIR/app/build.gradle.kts"
APK_OUT="$ANDROID_DIR/app/build/outputs/apk/direct/release/app-direct-release.apk"
PUBLIC_APK="$WEB_DIR/public/orange-cloud.apk"
LATEST_JSON="$WEB_DIR/public/android/latest.json"
NOTE="${1:-}"

export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"

# 版本号自 build.gradle defaultConfig 派生（单一来源）
VERSION_CODE="$(grep -m1 'versionCode *=' "$GRADLE_FILE" | grep -oE '[0-9]+')"
VERSION_NAME="$(grep -m1 'versionName *=' "$GRADLE_FILE" | sed -E 's/.*"(.*)".*/\1/')"
echo "▶ direct 发布：versionName=$VERSION_NAME versionCode=$VERSION_CODE"

# 1) 构建签名 release APK
( cd "$ANDROID_DIR" && ./gradlew :app:assembleDirectRelease )
[ -f "$APK_OUT" ] || { echo "✗ 未找到签名 APK：$APK_OUT（keystore 缺失会产出 *-unsigned.apk）"; exit 1; }

# 2) 验签（apksigner 在 PATH 时）
if command -v apksigner >/dev/null 2>&1; then
  apksigner verify "$APK_OUT" >/dev/null && echo "✔ APK 已签名"
fi

# 3) 投放到 web/public
mkdir -p "$(dirname "$PUBLIC_APK")"
cp "$APK_OUT" "$PUBLIC_APK"
echo "✔ APK → $PUBLIC_APK（$(du -h "$PUBLIC_APK" | cut -f1)）"

# 4) 刷新 latest.json（versionCode/versionName 自动派生，保留 url/minVersionCode；带参则写 note）
node -e '
  const fs = require("fs"), f = process.argv[1];
  const j = JSON.parse(fs.readFileSync(f, "utf8"));
  j.versionCode = Number(process.argv[2]);
  j.versionName = process.argv[3];
  if (process.argv[4] !== "") j.note = process.argv[4];
  fs.writeFileSync(f, JSON.stringify(j, null, 2) + "\n");
' "$LATEST_JSON" "$VERSION_CODE" "$VERSION_NAME" "$NOTE"
echo "✔ 已刷新 $LATEST_JSON"

# 5) 部署 web（新 APK + 清单一并上线）
( cd "$WEB_DIR" && pnpm run deploy )
echo "✅ direct $VERSION_NAME（$VERSION_CODE）已发布。"
