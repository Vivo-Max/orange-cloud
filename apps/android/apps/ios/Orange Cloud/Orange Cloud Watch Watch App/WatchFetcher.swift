//
//  WatchFetcher.swift
//  Orange Cloud Watch Watch App
//
//  Watch 自取数：用共享钥匙串里的有效 token 直查 GraphQL（单 Zone 24h）。
//  token 过期/缺失或失败返回 nil，由调用方回退桥接快照。仿 Widget 的 WidgetFetcher。
//

import Foundation

nonisolated enum WatchFetcher {

    private static let endpoint = URL(string: "https://api.cloudflare.com/client/v4/graphql")!

    private static let query = """
    query ($zoneTag: string!, $since: Time!, $until: Time!, $prevSince: Time!) {
      viewer {
        zones(filter: { zoneTag: $zoneTag }) {
          current: httpRequests1hGroups(
            limit: 25, orderBy: [datetime_ASC],
            filter: { datetime_geq: $since, datetime_lt: $until }
          ) {
            sum  { requests bytes threats cachedRequests }
            uniq { uniques }
          }
          previous: httpRequests1hGroups(
            limit: 25,
            filter: { datetime_geq: $prevSince, datetime_lt: $since }
          ) {
            sum { requests }
          }
        }
      }
    }
    """

    private struct Response: Codable {
        let data: Payload?
        struct Payload: Codable { let viewer: Viewer }
        struct Viewer: Codable { let zones: [Zone] }
        struct Zone: Codable {
            let current:  [Group]?
            let previous: [Group]?
        }
        struct Group: Codable {
            let sum:  Sum?
            let uniq: Uniq?
        }
        struct Sum: Codable {
            let requests:       Int?
            let bytes:          Int?
            let threats:        Int?
            let cachedRequests: Int?
        }
        struct Uniq: Codable { let uniques: Int? }
    }

    private struct Variables: Codable {
        let zoneTag: String
        let since: String
        let until: String
        let prevSince: String
    }

    private struct Request: Codable {
        let query: String
        let variables: Variables
    }

    /// 拉取单 Zone 最新 24h 指标；任何失败返回 nil
    static func freshZone(zoneId: String, name: String) async -> WidgetZoneMetrics? {
        guard let token = SharedAuth.currentValidAccessToken() else { return nil }

        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        let now = Date()
        let body = Request(query: query, variables: Variables(
            zoneTag: zoneId,
            since: formatter.string(from: now.addingTimeInterval(-24 * 3600)),
            until: formatter.string(from: now),
            prevSince: formatter.string(from: now.addingTimeInterval(-48 * 3600))
        ))

        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 12
        guard let encoded = try? JSONEncoder().encode(body) else { return nil }
        request.httpBody = encoded

        guard let (data, response) = try? await URLSession.shared.data(for: request),
              (response as? HTTPURLResponse)?.statusCode == 200,
              let decoded = try? JSONDecoder().decode(Response.self, from: data),
              let zone = decoded.data?.viewer.zones.first else {
            return nil
        }

        let groups = zone.current ?? []
        guard !groups.isEmpty else { return nil }

        let requests = groups.reduce(0) { $0 + ($1.sum?.requests ?? 0) }
        let cached = groups.reduce(0) { $0 + ($1.sum?.cachedRequests ?? 0) }
        let previous = (zone.previous ?? []).reduce(0) { $0 + ($1.sum?.requests ?? 0) }

        return WidgetZoneMetrics(
            id: zoneId,
            name: name,
            requests: requests,
            bytes: groups.reduce(0) { $0 + ($1.sum?.bytes ?? 0) },
            threats: groups.reduce(0) { $0 + ($1.sum?.threats ?? 0) },
            uniques: groups.reduce(0) { $0 + ($1.uniq?.uniques ?? 0) },
            cacheHitRate: requests > 0 ? Double(cached) / Double(requests) * 100 : nil,
            requestsTrend: previous > 0 ? (Double(requests) - Double(previous)) / Double(previous) * 100 : nil,
            requestsSeries: groups.map { $0.sum?.requests ?? 0 },
            bytesSeries: groups.map { $0.sum?.bytes ?? 0 },
            updatedAt: now
        )
    }
}
