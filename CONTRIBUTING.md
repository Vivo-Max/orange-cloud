# Contributing to Orange Cloud

[English](#english) | [中文](#中文)

## English

### Licensing model (read this first)

Orange Cloud is **dual-licensed**:

- This repository: [AGPL-3.0 + Commons Clause](LICENSE) — free for
  personal use and self-compiled builds (all features unlocked via the
  `OPENSOURCE_UNLOCKED` compilation condition); no commercial
  use/resale; derivative distributions must stay open source, keep
  origin notices, and use their own name/icon (see
  [TRADEMARK.md](TRADEMARK.md)).
- Official App Store build: proprietary license by the copyright
  holder, with a free tier and a paid Pro tier.

### Contributor License Agreement

Every contribution requires signing the [CLA](CLA.md) (comment
"I have read the CLA Document and I hereby sign the CLA" on your first
PR). This grants the maintainer the right to ship your code in both the
open-source and the App Store builds. PRs without a signed CLA cannot
be merged — no exceptions, however small the change.

### Building the iOS app

1. Xcode 26+ (iOS 26 SDK). Open
   `apps/ios/Orange Cloud/Orange Cloud.xcodeproj`.
2. **OAuth client**: `Core/Auth/OAuthConfig.swift` ships with the
   official client ID. Under OAuth PKCE this is a public identifier,
   not a secret — but the official client and the
   `90dd.adsl8.workers.dev` callback relay are **not for third-party
   builds**: create your own Cloudflare OAuth client and deploy your
   own callback relay (see [`apps/web/`](apps/web/README.md)), then
   replace the client ID and redirect URI in `OAuthConfig.swift`.
3. **Full unlock**: add `OPENSOURCE_UNLOCKED` to
   `SWIFT_ACTIVE_COMPILATION_CONDITIONS` of the *Orange Cloud* target.
   Self-compiled builds then have every Pro feature enabled — this is
   intentional, not a hack.
4. Change your Bundle ID / App Group / signing team to your own.

### Building the Android app

Native Kotlin + Jetpack Compose in [`apps/android/`](apps/android/README.md)
(min API 31, target/compile API 36).

1. JDK 17 + Android SDK (`android-36`). Open `apps/android/` in Android
   Studio, or use the bundled Gradle wrapper directly: `./gradlew
   :app:assembleOssDebug`.
2. Two product flavors: `play` (Google Play, with Billing) and `oss`
   (self-compiled, no Billing, `isPro` always true — the equivalent of
   the iOS `OPENSOURCE_UNLOCKED` build).
3. **OAuth client**: the `play` flavor embeds the official client ID (a
   public PKCE identifier, same value as iOS). The `oss` flavor ships
   empty — set your own in `apps/android/local.properties`
   (`OAUTH_CLIENT_ID=…`) and deploy your own callback relay; the
   official client is not for third-party builds.

See [`apps/android/README.md`](apps/android/README.md) for the full
build matrix and architecture.

### Code guidelines

- **MVVM boundaries**: Views never call the API directly — they bind to
  `@Observable` ViewModels; ViewModels never own `URLSession` — they
  call Services, which go through `CFAPIClient`.
- **Tokens live in the Keychain only** — never UserDefaults, never
  hardcoded.
- **async/await everywhere**; no completion handlers. Errors surface as
  `APIError` and are caught in the ViewModel.
- **Data models are `Codable` structs** with `CodingKeys` mapping
  snake_case fields.
- **Localization**: source language is Simplified Chinese; new
  user-facing strings must be added to the string catalogs (en,
  zh-Hant, zh-HK, ja) in `Localizable.xcstrings` — both the app's and
  the widget extension's if the string is used in `Shared/`. In `String`
  contexts (not `LocalizedStringKey`), wrap with `String(localized:)`.

## 中文

### 授权模式（先读这个）

本项目**双许可**：公开仓库为 [AGPL-3.0 + Commons Clause](LICENSE)——
自编译自用完全自由（`OPENSOURCE_UNLOCKED` 编译条件解锁全部功能）、禁止
商业使用与转售、二开分发必须保持开源并保留来源声明、且须更换名称与图标
（见 [TRADEMARK.md](TRADEMARK.md)）；App Store 官方版为专有授权，提供
免费层 + Pro 付费层。

### 贡献者协议

所有贡献必须先签署 [CLA](CLA.md)：在你的第一个 PR 里留言
"I have read the CLA Document and I hereby sign the CLA"。该协议授予
维护者把你的代码同时用于开源版与 App Store 专有版的权利。未签署的 PR
一律不合并。

### 编译 iOS 版

1. Xcode 26+，打开 `apps/ios/Orange Cloud/Orange Cloud.xcodeproj`。
2. **OAuth Client**：`OAuthConfig.swift` 内置官方 Client ID——OAuth
   PKCE 下它是公开标识符而非机密，但官方 Client 与
   `90dd.adsl8.workers.dev` 回调中转**不向第三方构建开放**：请自建
   Cloudflare OAuth Client 并部署自己的回调中转（见
   [`apps/web/`](apps/web/README.md)），然后在 `OAuthConfig.swift`
   中替换为你自己的 Client ID 与 redirect URI。
3. **全功能解锁**：向主 App target 的
   `SWIFT_ACTIVE_COMPILATION_CONDITIONS` 添加 `OPENSOURCE_UNLOCKED`，
   即可解锁全部 Pro 功能——这是设计意图，不是漏洞。
4. Bundle ID / App Group / 签名团队请改为你自己的。

### 编译 Android 版

原生 Kotlin + Jetpack Compose，位于 [`apps/android/`](apps/android/README.md)
（最低 API 31，目标 / 编译 API 36）。

1. JDK 17 + Android SDK（`android-36`）。用 Android Studio 打开
   `apps/android/`，或直接用内置的 Gradle Wrapper：`./gradlew
   :app:assembleOssDebug`。
2. 两个产品风味：`play`（Google Play，带 Billing）与 `oss`（自编译、
   无 Billing、`isPro` 恒真——等价于 iOS 的 `OPENSOURCE_UNLOCKED` 构建）。
3. **OAuth Client**：`play` 风味内置官方 Client ID（OAuth PKCE 下为
   公开标识符，与 iOS 同值）；`oss` 风味默认空串，请在
   `apps/android/local.properties` 填入自建 Client（`OAUTH_CLIENT_ID=…`）
   并部署自己的回调中转——官方 Client 不向第三方构建开放。

完整构建矩阵与架构见 [`apps/android/README.md`](apps/android/README.md)。

### 代码规范

- **MVVM 边界**：View 不直接调 API，只绑定 `@Observable` ViewModel；
  ViewModel 不持有 `URLSession`，只调 Service，Service 统一走
  `CFAPIClient`。
- **Token 只进 Keychain**——不写 UserDefaults，不硬编码。
- **全部 async/await**，不用 completion handler；错误统一为
  `APIError`，在 ViewModel 中捕获。
- **数据模型为 `Codable` struct**，snake_case 字段用 `CodingKeys`
  映射。
- **本地化**：源语言为简体中文；新增用户可见文案须同步写入
  `Localizable.xcstrings` 的四个目标语言（en / zh-Hant / zh-HK / ja），
  `Shared/` 中用到的 key 主 App 与 Widget 两份 catalog 都要有；
  `String` 上下文（非 `LocalizedStringKey`）必须包 `String(localized:)`。
