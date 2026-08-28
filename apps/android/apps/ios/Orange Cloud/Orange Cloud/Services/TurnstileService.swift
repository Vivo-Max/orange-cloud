//
//  TurnstileService.swift
//  Orange Cloud
//
//  Turnstile 人机验证组件管理（challenge-widgets.read / .write）。
//

import Foundation

struct TurnstileService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 账号下全部 widget（页码分页）
    func listWidgets(accountId: String) async throws -> [TurnstileWidget] {
        var widgets: [TurnstileWidget] = []
        var page = 1
        while true {
            let response: CFAPIResponseArray<TurnstileWidget> = try await client.get(
                "accounts/\(accountId)/challenges/widgets",
                queryItems: [
                    URLQueryItem(name: "page",     value: String(page)),
                    URLQueryItem(name: "per_page", value: "50"),
                ]
            )
            guard response.success else {
                throw response.toAPIError()
            }
            widgets.append(contentsOf: response.result ?? [])
            let totalPages = response.resultInfo?.totalPages ?? 1
            guard page < totalPages else { break }
            page += 1
        }
        return widgets
    }

    /// 单个 widget 详情（响应含 secret）
    func widget(accountId: String, sitekey: String) async throws -> TurnstileWidget {
        let response: CFAPIResponse<TurnstileWidget> = try await client.get(
            "accounts/\(accountId)/challenges/widgets/\(sitekey)"
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    /// 新建 widget（响应含 sitekey + secret）
    func createWidget(accountId: String, input: TurnstileWidgetInput) async throws -> TurnstileWidget {
        let response: CFAPIResponse<TurnstileWidget> = try await client.post(
            "accounts/\(accountId)/challenges/widgets",
            body: input
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    /// 更新 widget（PUT，name / mode / domains 必填）
    func updateWidget(accountId: String, sitekey: String, input: TurnstileWidgetInput) async throws -> TurnstileWidget {
        let response: CFAPIResponse<TurnstileWidget> = try await client.put(
            "accounts/\(accountId)/challenges/widgets/\(sitekey)",
            body: input
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    /// 删除 widget
    func deleteWidget(accountId: String, sitekey: String) async throws {
        try await client.delete("accounts/\(accountId)/challenges/widgets/\(sitekey)")
    }

    /// 轮换服务端密钥。immediately = false 时旧密钥保留 2 小时宽限期。
    func rotateSecret(accountId: String, sitekey: String, immediately: Bool) async throws -> TurnstileWidget {
        let response: CFAPIResponse<TurnstileWidget> = try await client.post(
            "accounts/\(accountId)/challenges/widgets/\(sitekey)/rotate_secret",
            body: TurnstileRotateRequest(invalidateImmediately: immediately)
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }
}
