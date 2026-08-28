//
//  URLScannerModels.swift
//  Orange Cloud
//
//  Cloudflare URL Scanner（v2）。提交 URL 做安全扫描并看报告。
//  POST /accounts/{account_id}/urlscanner/v2/scan
//  GET  /accounts/{account_id}/urlscanner/v2/result/{scan_id}
//  GET  /accounts/{account_id}/urlscanner/v2/screenshots/{scan_id}.png
//
//  ⚠️ 只用 v2，不要用 v1（v1 是旧版，字段与路径都不同）。
//  扫描是异步的：提交后返回 uuid，结果要轮询，未就绪时 result 返回 404/202。
//

import Foundation

nonisolated struct URLScanSubmit: Codable, Sendable {
    let url: String
}

/// 提交成功后的回执
nonisolated struct URLScanAccepted: Codable, Sendable {
    let uuid:    String?
    let api:     String?
    let message: String?
    let result:  String?
}

/// 扫描报告。字段很多，这里只取移动端会展示的那部分。
nonisolated struct URLScanResult: Codable, Sendable {
    let task:     URLScanTask?
    let page:     URLScanPage?
    let verdicts: URLScanVerdicts?
    let stats:    URLScanStats?
}

nonisolated struct URLScanTask: Codable, Sendable {
    let uuid:      String?
    let url:       String?
    let effectiveUrl: String?
    let status:    String?
    let time:      String?

    enum CodingKeys: String, CodingKey {
        case uuid, url, status, time
        case effectiveUrl = "effectiveUrl"
    }
}

nonisolated struct URLScanPage: Codable, Sendable {
    let url:     String?
    let domain:  String?
    let ip:      String?
    let country: String?
    let asn:     String?
    let asnname: String?
    let server:  String?
    let statusCode: Int?

    enum CodingKeys: String, CodingKey {
        case url, domain, ip, country, asn, asnname, server
        case statusCode = "statusCode"
    }
}

nonisolated struct URLScanVerdicts: Codable, Sendable {
    let overall: URLScanVerdict?
}

nonisolated struct URLScanVerdict: Codable, Sendable {
    let malicious:  Bool?
    let categories: [URLScanCategory]?
    let phishing:   [String]?
}

nonisolated struct URLScanCategory: Codable, Sendable {
    let id:   Int?
    let name: String?
}

nonisolated struct URLScanStats: Codable, Sendable {
    let uniqCountries: Int?
    let dataLength:    Int?
    let requests:      Int?
}
