//
//  StatusPageService.swift
//  Orange Cloud
//
//  Cloudflare 官方状态页（Statuspage v2 API）。公开接口无需鉴权，
//  域名与信封格式都和 Cloudflare API 不同，不走 CFAPIClient。
//

import Foundation

nonisolated struct StatusPageService {

    private let baseURL = URL(string: "https://www.cloudflarestatus.com/api/v2")!
    private let session = URLSession.shared

    /// 总体状态 + 全部组件 + 进行中的事件 + 计划维护
    func summary() async throws -> StatusPageSummary {
        try await fetch("summary.json")
    }

    /// 近期事件（含已解决，Statuspage 返回最近 50 条）
    func recentIncidents() async throws -> [StatusPageIncident] {
        let list: StatusPageIncidentList = try await fetch("incidents.json")
        return list.incidents
    }

    private func fetch<T: Codable & Sendable>(_ path: String) async throws -> T {
        let url = baseURL.appendingPathComponent(path)
        let (data, response): (Data, URLResponse)
        do {
            (data, response) = try await session.data(from: url)
        } catch {
            throw APIError.networkError(error)
        }
        guard let http = response as? HTTPURLResponse else {
            throw APIError.networkError(URLError(.badServerResponse))
        }
        guard (200...299).contains(http.statusCode) else {
            throw APIError.serverError(statusCode: http.statusCode)
        }
        do {
            return try JSONDecoder().decode(T.self, from: data)
        } catch {
            throw APIError.decodingError(error)
        }
    }
}
