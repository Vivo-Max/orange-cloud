//
//  MockCloudflare.swift
//  Orange Cloud
//
//  诊断专用 mock（仅 DEBUG 构建 + 启动环境变量 ORANGE_MOCK=1 时生效）：
//  拦截 api.cloudflare.com 请求返回 canned 数据，让模拟器无需 OAuth 即可把
//  Dashboard / Zones 驱动到「有账号有数据」状态，用于复现线上 iOS 17.0 崩溃。
//  Release 构建不包含本文件逻辑；平时 DEBUG 运行不带环境变量也完全不生效。
//

#if DEBUG
import Foundation
import os

nonisolated enum MockCloudflare {

    static var isRequested: Bool {
        ProcessInfo.processInfo.environment["ORANGE_MOCK"] == "1"
    }

    /// App 启动最早处调用：注册 URL 拦截 + 给身份索引里的身份塞假 token（绕过 OAuth）
    static func activateIfRequested() {
        guard isRequested else { return }
        URLProtocol.registerClass(MockCFURLProtocol.self)
        if let data = UserDefaults.standard.data(forKey: "authSessions"),
           let list = try? JSONDecoder().decode([AuthSessionMeta].self, from: data) {
            // 默认带假 refresh token（健康态）；ORANGE_MOCK_NO_REFRESH=1 时存无 refresh token 的
            // 残缺 token，复现「登录授权已失效」重授权横幅（真实事故形态，见 AuthManager 注释）。
            // expiresAt 远未来 → 永不触发真实刷新，假 refresh token 不会打到真 token 端点。
            let noRefresh = ProcessInfo.processInfo.environment["ORANGE_MOCK_NO_REFRESH"] == "1"
            for meta in list {
                TokenStore.save(
                    TokenStore.StoredToken(
                        accessToken: "mock-token",
                        refreshToken: noRefresh ? nil : "mock-refresh-token",
                        expiresAt: .distantFuture,
                        scope: meta.scopes.joined(separator: " ")
                    ),
                    sessionId: meta.id
                )
            }
        }
    }
}

/// 拦截发往 api.cloudflare.com 的请求，按路径返回 canned JSON
nonisolated final class MockCFURLProtocol: URLProtocol {

    override static func canInit(with request: URLRequest) -> Bool {
        request.url?.host == "api.cloudflare.com"
    }

    override static func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {
        let path = request.url?.path ?? ""
        let body = Self.readBody(of: request)
        let json = Self.responseJSON(path: path, body: body, method: request.httpMethod ?? "GET")
        let data = Data(json.utf8)
        let response = HTTPURLResponse(
            url: request.url!,
            statusCode: 200,
            httpVersion: "HTTP/1.1",
            headerFields: ["Content-Type": "application/json"]
        )!
        // ORANGE_MOCK_ACCOUNTS_DELAY_MS：延迟 /accounts 响应，拉宽「账号加载完成前」的
        // 操作窗口（复现 selectedAccount 翻转时用户已切到其它 tab 的时序）。
        let delayMs = ProcessInfo.processInfo.environment["ORANGE_MOCK_ACCOUNTS_DELAY_MS"].flatMap(Int.init) ?? 0
        let delay: TimeInterval = (path == "/client/v4/accounts" && delayMs > 0) ? Double(delayMs) / 1000 : 0
        let deliver = { [client] in
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: data)
            client?.urlProtocolDidFinishLoading(self)
        }
        if delay > 0 {
            DispatchQueue.global().asyncAfter(deadline: .now() + delay, execute: deliver)
        } else {
            deliver()
        }
    }

    override func stopLoading() {}

    // MARK: - 路由

    private static func responseJSON(path: String, body: String, method: String = "GET") -> String {
        if let rulesets = rulesetsResponse(path: path, body: body, method: method) {
            return rulesets
        }
        if path.hasSuffix("/url_normalization") {
            if method == "PUT", !body.isEmpty { return #"{"success":true,"errors":[],"messages":[],"result":\#(body)}"# }
            return #"{"success":true,"errors":[],"messages":[],"result":{"type":"cloudflare","scope":"incoming"}}"#
        }
        if path == "/client/v4/graphql" {
            if body.contains("zoneTag_in") {
                return trafficJSON()
            }
            // 账户级数据集 → 模拟免费账号 authz（Dashboard 用量区走「无权限」卡片）
            return #"{"data":null,"errors":[{"message":"not authorized for that account","path":[],"extensions":{"code":"authz"}}]}"#
        }
        if path == "/client/v4/accounts" {
            return envelope(#"[{"id":"mockacct","name":"Mock Account","type":"standard"},{"id":"mockacct2","name":"Second Account","type":"standard"}]"#, total: 2)
        }
        if path == "/client/v4/zones" {
            return envelope(#"""
            [
             {"id":"z1","name":"example.com","status":"active","plan":{"name":"Free Website"},"name_servers":["a.ns.cloudflare.com","b.ns.cloudflare.com"]},
             {"id":"z2","name":"orange-cloud.dev","status":"active","plan":{"name":"Pro Website"},"name_servers":["a.ns.cloudflare.com","b.ns.cloudflare.com"]},
             {"id":"z3","name":"pending-site.io","status":"pending","plan":{"name":"Free Website"},"name_servers":["a.ns.cloudflare.com","b.ns.cloudflare.com"]}
            ]
            """#, total: 3)
        }
        if path.hasSuffix("/dns_records") {
            return envelope("[]", total: 12)
        }
        if path.hasSuffix("/workers/scripts") {
            return envelope(#"[{"id":"api-worker","handlers":["fetch"],"logpush":false},{"id":"cron-worker","handlers":["scheduled"],"logpush":false}]"#, total: 2)
        }
        // Pages 项目（验证 DevHub → 列表 → 项目详情的值式导航链路）
        if path.hasSuffix("/pages/projects") {
            return envelope(#"[{"name":"demo-site","subdomain":"demo-site.pages.dev","domains":["demo.example.com"],"production_branch":"main","created_on":"2026-01-01T00:00:00Z"}]"#, total: 1)
        }
        // 其余端点：空数组成功信封（调用方多为 try? 包裹，可优雅降级）
        return envelope("[]", total: 0)
    }

    private static func envelope(_ result: String, total: Int) -> String {
        #"{"success":true,"errors":[],"messages":[],"result":\#(result),"result_info":{"page":1,"per_page":50,"count":\#(total),"total_count":\#(total),"total_pages":1}}"#
    }

    // MARK: - Rulesets phase mock（有状态：模拟器里可真测规则的创建 / 编辑 / 启停 / 删除往返）

    private static let rulesetsStore = OSAllocatedUnfairLock(uncheckedState: seedRulesets())

    private static func seedRulesets() -> [String: [[String: Any]]] {
        [
            "http_request_dynamic_redirect": [[
                "id": "rr1",
                "description": "旧博客跳新站",
                "expression": "(http.request.uri.path contains \"/blog/\")",
                "enabled": true,
                "action": "redirect",
                "action_parameters": ["from_value": ["target_url": ["value": "https://example.com/news/"], "status_code": 301]],
            ]],
            "http_response_compression": [[
                "id": "cr1",
                "description": "静态资源优先 zstd",
                "expression": "(http.request.uri.path contains \"/static/\")",
                "enabled": true,
                "action": "compress_response",
                "action_parameters": ["algorithms": [["name": "zstd"], ["name": "brotli"], ["name": "gzip"]]],
            ]],
        ]
    }

    /// entrypoint 与规则增删改；未播种的 phase 首次 GET 按线上形态回「could not find entrypoint」
    private static func rulesetsResponse(path: String, body: String, method: String) -> String? {
        if let range = path.range(of: "/rulesets/phases/") {
            let phase = String(path[range.upperBound...]).components(separatedBy: "/").first ?? ""
            if method == "PUT", let obj = parseJSON(body), let rules = obj["rules"] as? [[String: Any]] {
                let withIds = rules.enumerated().map { index, rule -> [String: Any] in
                    var rule = rule
                    if rule["id"] == nil { rule["id"] = "r\(index + 1)-new" }
                    return rule
                }
                rulesetsStore.withLockUnchecked { $0[phase] = withIds }
                return rulesetEnvelope(phase: phase)
            }
            let exists = rulesetsStore.withLockUnchecked { $0[phase] != nil }
            guard exists else {
                return #"{"success":false,"errors":[{"code":20217,"message":"could not find entrypoint ruleset"}],"messages":[],"result":null}"#
            }
            return rulesetEnvelope(phase: phase)
        }
        if let range = path.range(of: "/rulesets/rs-") {
            let rest = String(path[range.upperBound...])
            let parts = rest.components(separatedBy: "/rules")
            let phase = parts[0]
            let ruleId = parts.count > 1 ? parts[1].trimmingCharacters(in: CharacterSet(charactersIn: "/")) : ""
            switch method {
            case "POST":
                if var rule = parseJSON(body) {
                    rulesetsStore.withLockUnchecked {
                        rule["id"] = "r\(($0[phase]?.count ?? 0) + 1)-added"
                        $0[phase, default: []].append(rule)
                    }
                }
            case "PATCH":
                if let patch = parseJSON(body) {
                    rulesetsStore.withLockUnchecked {
                        guard var rules = $0[phase],
                              let index = rules.firstIndex(where: { ($0["id"] as? String) == ruleId }) else { return }
                        for (key, value) in patch { rules[index][key] = value }
                        $0[phase] = rules
                    }
                }
            case "DELETE":
                rulesetsStore.withLockUnchecked { $0[phase]?.removeAll { ($0["id"] as? String) == ruleId } }
            default:
                break
            }
            return rulesetEnvelope(phase: phase)
        }
        return nil
    }

    private static func parseJSON(_ body: String) -> [String: Any]? {
        (try? JSONSerialization.jsonObject(with: Data(body.utf8))) as? [String: Any]
    }

    private static func rulesetEnvelope(phase: String) -> String {
        let rules = rulesetsStore.withLockUnchecked { $0[phase] ?? [] }
        let env: [String: Any] = [
            "success": true, "errors": [], "messages": [],
            "result": ["id": "rs-\(phase)", "name": "default", "phase": phase, "rules": rules],
        ]
        let data = (try? JSONSerialization.data(withJSONObject: env)) ?? Data("{}".utf8)
        return String(decoding: data, as: UTF8.self)
    }

    /// 三个 zone 各 24 个小时桶（datetime ISO8601），previous 窗口一条
    private static func trafficJSON() -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        let now = Date()
        func groups(scale: Int) -> String {
            (0..<24).map { i -> String in
                let dt = formatter.string(from: now.addingTimeInterval(TimeInterval(-3600 * (24 - i))))
                let requests = (300 + (i * 37) % 900) * scale
                return #"{"dimensions":{"datetime":"\#(dt)"},"sum":{"requests":\#(requests),"bytes":\#(requests * 2048),"threats":\#(i % 5),"pageViews":\#(requests / 2),"cachedRequests":\#(requests / 3),"cachedBytes":\#(requests * 1024)},"uniq":{"uniques":\#(requests / 10)}}"#
            }.joined(separator: ",")
        }
        func node(_ tag: String, scale: Int) -> String {
            #"{"zoneTag":"\#(tag)","httpRequests1hGroups":[\#(groups(scale: scale))],"previous":[{"sum":{"requests":\#(9000 * scale)}}]}"#
        }
        return #"{"data":{"viewer":{"zones":[\#(node("z1", scale: 1)),\#(node("z2", scale: 3)),\#(node("z3", scale: 0))]}},"errors":null}"#
    }

    private static func readBody(of request: URLRequest) -> String {
        if let data = request.httpBody {
            return String(decoding: data, as: UTF8.self)
        }
        guard let stream = request.httpBodyStream else { return "" }
        stream.open()
        defer { stream.close() }
        var data = Data()
        let bufferSize = 16 * 1024
        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
        defer { buffer.deallocate() }
        while stream.hasBytesAvailable {
            let read = stream.read(buffer, maxLength: bufferSize)
            guard read > 0 else { break }
            data.append(buffer, count: read)
        }
        return String(decoding: data, as: UTF8.self)
    }
}
#endif
