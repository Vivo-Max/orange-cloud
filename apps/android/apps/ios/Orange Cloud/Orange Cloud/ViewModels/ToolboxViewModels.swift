//
//  ToolboxViewModels.swift
//  Orange Cloud
//
//  免登录工具箱各工具的 ViewModel（@Observable / @MainActor）。
//  CIDR 为纯本地同步计算，无 VM，直接在 View 里调 CIDRCalculator。
//

import Foundation
import Observation

// MARK: - DNS 查询

@Observable
@MainActor
final class DNSLookupViewModel {
    var name = ""
    var type: DNSQueryType = .a
    private(set) var results: [DNSRecordResult] = []
    var isLoading = false
    var error: String?
    private(set) var hasRun = false

    private let service = DNSLookupService()

    func run() async {
        let query = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return }
        isLoading = true
        error = nil
        do {
            results = try await service.lookup(name: query, type: type)
        } catch {
            results = []
            self.error = error.localizedDescription
        }
        hasRun = true
        isLoading = false
    }
}

// MARK: - CF 数据中心 trace

@Observable
@MainActor
final class CFTraceViewModel {
    var host = "1.1.1.1"
    private(set) var result: CFTraceResult?
    var isLoading = false
    var error: String?

    private let service = CFTraceService()

    func run() async {
        isLoading = true
        error = nil
        do {
            result = try await service.trace(host: host)
        } catch {
            result = nil
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

// MARK: - HTTP 请求器

@Observable
@MainActor
final class HTTPProbeViewModel {
    var urlString = "https://"
    var method = "GET"
    let methods = ["GET", "HEAD", "POST"]
    private(set) var result: HTTPProbeResult?
    var isLoading = false
    var error: String?

    private let service = HTTPProbeService()

    func run() async {
        let trimmed = urlString.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, trimmed != "https://" else { return }
        isLoading = true
        error = nil
        do {
            result = try await service.probe(method: method, urlString: trimmed)
        } catch {
            result = nil
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

// MARK: - SSL 证书检查

@Observable
@MainActor
final class CertInspectViewModel {
    var host = ""
    private(set) var info: CertInfo?
    var isLoading = false
    var error: String?

    private let service = CertInspectService()

    func run() async {
        let h = host.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !h.isEmpty else { return }
        isLoading = true
        error = nil
        do {
            info = try await service.inspect(host: h)
        } catch {
            info = nil
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

// MARK: - WHOIS

@Observable
@MainActor
final class WhoisViewModel {
    var domain = ""
    private(set) var info: WhoisInfo?
    var isLoading = false
    var error: String?

    private let service = RDAPService()

    func run() async {
        let d = domain.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !d.isEmpty else { return }
        isLoading = true
        error = nil
        do {
            info = try await service.lookup(domain: d)
        } catch {
            info = nil
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

// MARK: - GeoIP

@Observable
@MainActor
final class GeoIPViewModel {
    var ip = ""
    private(set) var result: GeoIPResult?
    private(set) var hasRun = false
    var isLoading = false
    var error: String?

    private let service = GeoIPService()

    func run() async {
        isLoading = true
        error = nil
        do {
            let r = try await service.lookup(ip: ip.trimmingCharacters(in: .whitespacesAndNewlines))
            if r.success {
                result = r
            } else {
                result = nil
                error = r.message ?? String(localized: "查询失败")
            }
        } catch {
            result = nil
            self.error = error.localizedDescription
        }
        hasRun = true
        isLoading = false
    }
}
