//
//  OAuthConfig.swift
//  Orange Cloud
//
//  在 Cloudflare Dashboard 创建 OAuth Client 后，填入真实 clientID。
//  redirect_uri 必须与 Dashboard 中注册的完全一致。
//

import Foundation

nonisolated enum OAuthConfig {
    /// 官方 OAuth Client（Cloudflare Dashboard → OAuth clients）。
    /// 仅供官方构建使用；自编译请自建 Client 与回调中转，见 CONTRIBUTING.md。
    static let clientID = "3f07f9b06b058b655d5e7c031f965347"

    /// 自定义 scheme，供 Web 后端 302 跳回 App
    static let callbackScheme = "orangecloud"

    // Cloudflare OAuth 只接受 https redirect_uri，指向 Web 后端回调中转（见 apps/web/README.md）
//    #if DEBUG
//    static let redirectURI = "http://localhost:3000/oauth/callback"
//    #else
    static let redirectURI = "https://oss.omail.us.kg/oauth/callback"
//    #endif

    // Cloudflare OAuth 端点
    static let authorizationURL = URL(string: "https://dash.cloudflare.com/oauth2/auth")!
    static let tokenURL         = URL(string: "https://dash.cloudflare.com/oauth2/token")!
    static let revokeURL        = URL(string: "https://dash.cloudflare.com/oauth2/revoke")!
    static let userInfoURL      = URL(string: "https://dash.cloudflare.com/oauth2/userinfo")!
}
