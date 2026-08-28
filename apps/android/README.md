# Orange Cloud — Android

原生 Kotlin + Jetpack Compose 客户端。设计与价值同源于 iOS 版，功能与交互以 Android 原生为先（不追求与 iOS 一一对应）。
最低 Android 12（API 31），目标 / 编译 API 36。

## 先决条件

- **JDK 17**
- **Android SDK**（platform `android-36` + 对应 build-tools）

用 Android Studio 打开 `apps/android/` 会自动配齐 SDK；或手动安装命令行工具：

```bash
# JDK 17（Homebrew 示例）
brew install --cask temurin@17
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"

# Android SDK（命令行工具，无需 Android Studio）
brew install --cask android-commandlinetools
export ANDROID_HOME="$HOME/Library/Android/sdk"
sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools"
```

`local.properties` 不入库，首次需写入 SDK 路径（Android Studio 会自动生成）：

```properties
sdk.dir=/path/to/Android/sdk
```

## 构建

仓库已包含 Gradle Wrapper（`./gradlew`，Gradle 8.13），直接用即可：

```bash
cd apps/android
./gradlew :app:assemblePlayDebug      # 官方 Play 版（带 Billing）
./gradlew :app:assembleOssDebug       # 开源自编译版（无 Billing，isPro 恒真）
./gradlew :app:testPlayDebugUnitTest  # 单测
```

两个产品风味：

| 风味 | applicationId | 说明 |
|---|---|---|
| `play` | `jiamin.chen.orangecloud` | 官方版，Play Billing，内置官方 OAuth Client |
| `oss` | `jiamin.chen.orangecloud.oss` | 自编译全解锁，无 Billing 依赖，需自填 OAuth Client |

## OAuth Client 注入

`OAUTH_CLIENT_ID` 经 Gradle 注入到 `BuildConfig`：

- `play` 风味内置官方 Client ID——OAuth PKCE 下它是公开标识符而非机密，与 iOS `OAuthConfig.swift` 同值。
- `oss` 风味默认空串。自编译者须在 `apps/android/local.properties` 填入自建的 Client ID：
  ```properties
  OAUTH_CLIENT_ID=你自建的_client_id
  ```
  并部署自己的回调中转——官方 Client 与 `oss.omail.us.kg` 中转不向第三方构建开放，详见根目录 [`CONTRIBUTING.md`](../../CONTRIBUTING.md)。

## 架构

Google 官方 App Architecture，分层无环：

```
ui/        Compose 屏 + ViewModel（每屏一个 UiState: StateFlow）
data/      Repository（单一可信源）
  ├─ remote/  CfApiClient(OkHttp) + DTO(@Serializable)
  └─ local/   Room(@Entity/@Dao) + DataStore
core/      auth / network / design（晨昏天景）/ di（Hilt）
```

- Composable 不直接发网络，只读 ViewModel 暴露的 `UiState`；ViewModel 不持有 OkHttp，只调 Repository。
- Token 只入 Keystore 包裹的加密 DataStore，绝不写明文。
- 数据模型为 `@Serializable` data class，`@SerialName` 映射 snake_case。
- 错误统一 `ApiError`（sealed class），在 ViewModel `catch` 后赋给 `UiState.error`。
- 用户可见文案一律入 `res/values/strings.xml`，禁止硬编码字面量。
