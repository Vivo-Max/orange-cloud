# Orange Cloud 自建部署与定制指南

本指南帮助你基于本仓库构建、部署并分发**属于你自己的 Orange Cloud 版本**：自建 Cloudflare OAuth Client、OAuth 回调中转 Worker、设备管理面板，以及 Android / Web 的 CI 构建。

> 阅读对象：有一定动手能力、拥有 Cloudflare 账号和一个自有域名（免费域名如 us.kg 亦可）的用户。

---

## 0. 架构总览

```
┌────────────┐   授权请求    ┌──────────────────┐
│  App/Web    │ ───────────▶ │ Cloudflare OAuth  │
│ (你的构建)  │ ◀─────────── │ (你的 Client)     │
└─────┬──────┘   code+state  └──────────────────┘
      │ 回调 https://你的域名/oauth/callback
      ▼
┌──────────────────────────────────────────────┐
│ Cloudflare Worker（workers/oauth-callback）    │
│  · /oauth/callback  302 中转回 App            │
│  · /api/device/*    设备登记 / 封禁状态         │
│  · /admin           Web 管理面板（密码登录）     │
│  · KV 命名空间 DEVICES：设备记录 + 面板配置      │
└──────────────────────────────────────────────┘
```

- App 是 PKCE 公共客户端（`token_endpoint_auth_method = none`），**没有也不需要 Client Secret**；
- 所有机密（管理密码、API Token）只存在于 Cloudflare Secret / GitHub Secrets，**绝不写进仓库**。

---

## 1. 创建 Cloudflare OAuth Client

1. 登录 Cloudflare 控制台 → 选中你的账号 → **Manage Account → OAuth clients → 创建客户端**。
2. 按此填写：

   | 字段 | 值 |
   |---|---|
   | 客户端名称 | 你的应用名（如 `Orange Cloud`） |
   | 响应类型 | `Code, Token, ID Token` |
   | 授权类型 | `Authorization Code, Refresh Token` |
   | 令牌身份验证方法 | `None (PKCE)` |
   | 重定向（回调）URL | `https://<你的域名>/oauth/callback` |
   | 客户端 URL | `https://<你的域名>` |

3. **范围（Scopes）**：把需要的权限全部勾上（本仓库权限目录支持 142 项，建议全选，App 内登录时可按需裁剪）。
4. 保存后记录 **Client ID**（32 位十六进制）。

## 2. 域名验证 + 转 Public（分发必需）

新客户端默认是 **Private**——只有你自己账号的成员能授权，其他用户会看到
"This is a private application and can only be authorized by members of the developer's account"。

1. 在客户端列表复制 **Verification DNS TXT record** 的完整值
   （形如 `cloudflare_oauth_client_publisher=xxxxxxxx`，每次创建/重置都可能变化，以页面当前显示为准）。
2. 到域名的 DNS 管理页添加记录：
   - 类型 `TXT`，名称 `<回调域名的主机部分>`（如回调是 `oss.example.com` 则填 `oss`），内容粘贴完整值。
3. 编辑客户端 → 高级选项 → 上传 **徽标（Logo）**（转 Public 的必填项），保存。
4. 列表页 **⋯ → Restart Verification**，等状态变绿 **Verified**。
5. **⋯ → Change Visibility → Public**。之后任何 Cloudflare 用户都能授权。

## 3. 部署 OAuth 回调 Worker

代码在 [`workers/oauth-callback/`](../workers/oauth-callback/)。

```bash
cd workers/oauth-callback
npx wrangler login
npx wrangler kv namespace create DEVICES   # 记下返回的 id
```

编辑 `wrangler.toml`：

```toml
name = "你的worker名"
main = "worker.js"
compatibility_date = "2026-08-28"
workers_dev = false
routes = [ { pattern = "<你的域名>", custom_domain = true } ]   # 与 OAuth 回调同域名

[[kv_namespaces]]
binding = "DEVICES"          # 变量名必须叫 DEVICES
id = "上一步创建的KV id"
```

设置管理密码（**不要写进 wrangler.toml**，会进 git）：

```bash
npx wrangler secret put ADMIN_PASSWORD
```

部署：

```bash
npx wrangler deploy
```

也可以全部在 Cloudflare 网页完成：Workers → 粘贴 `worker.js` → Settings 里绑 KV（变量名 `DEVICES`）、加 Secret `ADMIN_PASSWORD`、绑自定义域。

部署后验证：

- 浏览器打开 `https://<你的域名>/admin` → 出现密码登录页；
- 输入管理密码进入设备管理面板。

### 面板功能

- 设备名单：机器码、备注、客户端、最近活跃、状态，支持 30 秒自动刷新；
- 封禁 / 解禁 / 移除 / 编辑备注，全部实时写入 KV；
- **设备数量上限**（面板设置页，默认 20）：超限后新设备无法登记与登录，已有设备不受影响；
- 主题切换、自定义背景图、前景透明度。

## 4. GitHub Actions 构建（Android APK + Web）

Fork 本仓库后，在 **Settings → Secrets and variables → Actions** 配置：

**Secrets（机密）**

| 名称 | 用途 | 必填 |
|---|---|---|
| `OAUTH_CLIENT_ID` | 你的 OAuth Client ID | 否（不填用仓库默认值） |
| `CLOUDFLARE_API_TOKEN` | Web 构建用（Cloudflare API Token） | 构建 Web 时必填 |
| `ADMIN_PASSWORD` | Web 构建环境变量 | 构建 Web 时必填 |
| `KEYSTORE_BASE64` | 固定签名密钥（base64 编码的 .keystore） | 分发时强烈建议 |
| `KEYSTORE_ALIAS` | 密钥别名（如 `release`） | 配了密钥则必填 |
| `KEYSTORE_PASSWORD` | 密钥库与密钥密码 | 配了密钥则必填 |

> **签名一致性 = 可覆盖安装**：不配 `KEYSTORE_*` 时 CI 每次生成临时密钥，每个版本签名都不同，Android 会拒绝覆盖安装（用户只能卸载重装、丢失本地账号数据）。配置固定密钥后所有版本签名一致，升级直接覆盖。生成方式：
> ```bash
> keytool -genkey -v -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10950
> base64 -w 0 release.keystore   # 输出粘贴到 Secret KEYSTORE_BASE64
> ```
> 注意：如果已经用临时密钥发布过版本，老用户仍需卸载重装一次（机器码不受影响），之后的版本就能正常覆盖了。

**Variables（变量）**

| 名称 | 示例 | 必填 |
|---|---|---|
| `OAUTH_REDIRECT_URI` | `https://<你的域名>/oauth/callback` | 使用自有域名时必填 |

触发构建：**Actions → Build & Release (Web + Android) → Run workflow**，填写版本号（如 `2.1.01`），完成后自动生成 GitHub Release 并附带 OSS APK。

> 机器码采用 Widevine 设备 ID，与签名无关，更换签名密钥不影响设备识别与封禁名单。

## 5. 本地构建 Android

```bash
cd apps/android
cat > local.properties <<EOF
sdk.dir=<你的SDK路径>
OAUTH_CLIENT_ID=<你的Client ID>
OAUTH_REDIRECT_URI=https://<你的域名>/oauth/callback
EOF
./gradlew assembleOssRelease
```

## 6. iOS 构建

1. 安装 Xcode，打开 `apps/ios/` 工程；
2. 修改 `Orange Cloud/Core/Auth/OAuthConfig.swift` 中的 `clientId` 与 `redirectUri` 为你的值；
3. 用自己的 Apple 开发者证书签名（CI 不含 iOS 构建）。

## 7. 定制建议

- **应用名 / 图标 / 包名**：`apps/android/app/src/main/res/values/strings.xml` 的 `app_name`、`build.gradle.kts` 的 `applicationIdSuffix`、各 `mipmap` 目录图标；
- **关于页链接**：`ui/settings/SettingsScreen.kt`（GitHub 仓库地址、隐私政策、使用条款）；
- **多语言**：13 个 `values-*/strings.xml`，新增文案记得全部补齐；
- **版本更新日志**：编辑 `packages/changelog/android.json` / `ios.json` 后运行 `pnpm changelog:gen`。

## 8. 安全须知

- `ADMIN_PASSWORD`、API Token、keystore 密码等**只放 Secret**，永远不要提交进仓库；
- 如果密码曾经明文泄露（包括发在聊天记录里），请立即更换：`npx wrangler secret put ADMIN_PASSWORD`；
- 封禁与上限对终端用户不可感知（只表现为"登录失败"），请勿在公开渠道说明该机制细节。
