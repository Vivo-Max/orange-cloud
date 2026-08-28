//
//  ToolboxServices.swift
//  Orange Cloud
//
//  免登录工具箱的网络服务（无需 CF 账号、不走 CFAPIClient）。
//  统一映射到 APIError；遵循 StatusPageService 的 nonisolated struct 约定。
//

import Foundation

// MARK: - DNS（DoH JSON over 1.1.1.1）

nonisolated struct DNSLookupService {

    private let session = URLSession.shared

    func lookup(name: String, type: DNSQueryType) async throws -> [DNSRecordResult] {
        var comps = URLComponents(string: "https://cloudflare-dns.com/dns-query")!
        comps.queryItems = [
            URLQueryItem(name: "name", value: name),
            URLQueryItem(name: "type", value: type.rawValue),
        ]
        guard let url = comps.url else { throw APIError.networkError(URLError(.badURL)) }

        var req = URLRequest(url: url)
        req.setValue("application/dns-json", forHTTPHeaderField: "Accept")

        let data: Data
        let response: URLResponse
        do { (data, response) = try await session.data(for: req) }
        catch { throw APIError.networkError(error) }

        guard let http = response as? HTTPURLResponse else { throw APIError.networkError(URLError(.badServerResponse)) }
        guard (200...299).contains(http.statusCode) else { throw APIError.serverError(statusCode: http.statusCode) }

        let decoded: DoHResponse
        do { decoded = try JSONDecoder().decode(DoHResponse.self, from: data) }
        catch { throw APIError.decodingError(error) }

        return (decoded.answer ?? []).map {
            DNSRecordResult(name: $0.name, typeName: Self.typeName($0.type), ttl: $0.ttl, value: $0.data)
        }
    }

    private static func typeName(_ code: Int) -> String {
        switch code {
        case 1: return "A"
        case 2: return "NS"
        case 5: return "CNAME"
        case 6: return "SOA"
        case 15: return "MX"
        case 16: return "TXT"
        case 28: return "AAAA"
        case 257: return "CAA"
        default: return "TYPE\(code)"
        }
    }
}

// MARK: - CF 数据中心 trace（/cdn-cgi/trace）

nonisolated struct CFTraceService {

    private let session = URLSession.shared

    func trace(host: String) async throws -> CFTraceResult {
        let cleaned = host.trimmingCharacters(in: .whitespacesAndNewlines)
        let target = cleaned.isEmpty ? "1.1.1.1" : cleaned
        guard let url = URL(string: "https://\(target)/cdn-cgi/trace") else {
            throw APIError.networkError(URLError(.badURL))
        }

        let data: Data
        let response: URLResponse
        do { (data, response) = try await session.data(from: url) }
        catch { throw APIError.networkError(error) }

        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
            throw APIError.serverError(statusCode: (response as? HTTPURLResponse)?.statusCode ?? -1)
        }

        let text = String(decoding: data, as: UTF8.self)
        var raw: [String: String] = [:]
        var fields: [TraceField] = []
        for line in text.split(whereSeparator: \.isNewline) {
            guard let eq = line.firstIndex(of: "=") else { continue }
            let key = String(line[..<eq])
            let value = String(line[line.index(after: eq)...])
            raw[key] = value
            fields.append(TraceField(key: key, value: value))
        }
        guard !fields.isEmpty else { throw APIError.decodingError(URLError(.cannotParseResponse)) }
        return CFTraceResult(fields: fields, raw: raw)
    }
}

// MARK: - HTTP 请求器（仅 https）

nonisolated struct HTTPProbeService {

    func probe(method: String, urlString: String) async throws -> HTTPProbeResult {
        let trimmed = urlString.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let url = URL(string: trimmed), url.scheme?.lowercased() == "https" else {
            throw APIError.networkError(URLError(.badURL))   // 仅支持 https
        }

        var req = URLRequest(url: url)
        req.httpMethod = method
        req.timeoutInterval = 20

        let session = URLSession(configuration: .ephemeral)
        let start = Date()
        let data: Data
        let response: URLResponse
        do { (data, response) = try await session.data(for: req) }
        catch { throw APIError.networkError(error) }
        let ms = Int(Date().timeIntervalSince(start) * 1000)

        guard let http = response as? HTTPURLResponse else { throw APIError.networkError(URLError(.badServerResponse)) }

        let headers: [HTTPHeaderItem] = http.allHeaderFields.compactMap { key, value in
            guard let name = key as? String else { return nil }
            return HTTPHeaderItem(name: name, value: "\(value)")
        }.sorted { $0.name.lowercased() < $1.name.lowercased() }

        let previewSlice = data.prefix(4096)
        let preview = String(data: previewSlice, encoding: .utf8) ?? String(decoding: previewSlice, as: UTF8.self)

        return HTTPProbeResult(
            statusCode: http.statusCode,
            statusText: HTTPURLResponse.localizedString(forStatusCode: http.statusCode),
            headers: headers,
            durationMS: ms,
            finalURL: http.url?.absoluteString ?? trimmed,
            bodyByteCount: data.count,
            bodyPreview: preview
        )
    }
}

// MARK: - WHOIS（RDAP over HTTPS）

nonisolated struct RDAPService {

    private let session = URLSession.shared

    func lookup(domain: String) async throws -> WhoisInfo {
        let name = domain.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard name.contains("."), let tld = name.split(separator: ".").last.map(String.init) else {
            throw APIError.networkError(URLError(.badURL))
        }
        let base = try await rdapBase(forTLD: tld)
        let joined = base.hasSuffix("/") ? "\(base)domain/\(name)" : "\(base)/domain/\(name)"
        guard let url = URL(string: joined) else { throw APIError.networkError(URLError(.badURL)) }

        let data: Data
        let response: URLResponse
        do { (data, response) = try await session.data(from: url) }
        catch { throw APIError.networkError(error) }
        guard let http = response as? HTTPURLResponse else { throw APIError.networkError(URLError(.badServerResponse)) }
        if http.statusCode == 404 { throw APIError.notFound }
        guard (200...299).contains(http.statusCode) else { throw APIError.serverError(statusCode: http.statusCode) }

        guard let obj = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else {
            throw APIError.decodingError(URLError(.cannotParseResponse))
        }
        return Self.parse(obj, domain: name)
    }

    /// IANA RDAP bootstrap：按 TLD 找到对应 RDAP 服务器基址
    private func rdapBase(forTLD tld: String) async throws -> String {
        guard let url = URL(string: "https://data.iana.org/rdap/dns.json") else {
            throw APIError.networkError(URLError(.badURL))
        }
        let data: Data
        do { (data, _) = try await session.data(from: url) }
        catch { throw APIError.networkError(error) }
        guard let obj = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
              let services = obj["services"] as? [[Any]] else {
            throw APIError.decodingError(URLError(.cannotParseResponse))
        }
        for service in services where service.count >= 2 {
            guard let tlds = service[0] as? [String], let urls = service[1] as? [String] else { continue }
            if tlds.contains(tld), let base = urls.first(where: { $0.hasPrefix("https://") }) ?? urls.first {
                return base
            }
        }
        throw APIError.notFound   // 该 TLD 无 RDAP
    }

    private static func parse(_ obj: [String: Any], domain: String) -> WhoisInfo {
        let statuses = obj["status"] as? [String] ?? []
        let events = obj["events"] as? [[String: Any]] ?? []
        func eventDate(_ action: String) -> Date? {
            guard let raw = events.first(where: { ($0["eventAction"] as? String) == action })?["eventDate"] as? String else { return nil }
            return isoDate(raw)
        }
        let nameservers = (obj["nameservers"] as? [[String: Any]])?.compactMap { $0["ldhName"] as? String } ?? []
        return WhoisInfo(
            domain: (obj["ldhName"] as? String) ?? domain,
            statuses: statuses,
            registrar: registrarName(obj["entities"]),
            created: eventDate("registration"),
            updated: eventDate("last changed"),
            expires: eventDate("expiration"),
            nameservers: nameservers
        )
    }

    private static func registrarName(_ entities: Any?) -> String? {
        guard let list = entities as? [[String: Any]] else { return nil }
        for entity in list {
            let roles = entity["roles"] as? [String] ?? []
            guard roles.contains("registrar") else { continue }
            if let vcard = entity["vcardArray"] as? [Any], vcard.count >= 2, let props = vcard[1] as? [[Any]] {
                for prop in props where prop.count >= 4 {
                    if (prop[0] as? String) == "fn", let value = prop[3] as? String { return value }
                }
            }
        }
        return nil
    }

    private static func isoDate(_ s: String) -> Date? {
        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime]
        if let d = iso.date(from: s) { return d }
        iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return iso.date(from: s)
    }
}

// MARK: - GeoIP（ipwho.is，无 key）

nonisolated struct GeoIPService {

    private let session = URLSession.shared

    func lookup(ip: String) async throws -> GeoIPResult {
        let trimmed = ip.trimmingCharacters(in: .whitespacesAndNewlines)
        let path = trimmed.isEmpty ? "https://ipwho.is/" : "https://ipwho.is/\(trimmed)"
        guard let url = URL(string: path) else { throw APIError.networkError(URLError(.badURL)) }

        let data: Data
        let response: URLResponse
        do { (data, response) = try await session.data(from: url) }
        catch { throw APIError.networkError(error) }
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
            throw APIError.serverError(statusCode: (response as? HTTPURLResponse)?.statusCode ?? -1)
        }
        do { return try JSONDecoder().decode(GeoIPResult.self, from: data) }
        catch { throw APIError.decodingError(error) }
    }
}
