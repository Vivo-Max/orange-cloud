//
//  URLScannerService.swift
//  Orange Cloud
//
//  URL Scanner v2（url-scanner.read / .write）。
//

import Foundation

struct URLScannerService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 提交扫描。异步任务，返回 uuid 后需轮询结果。
    func submit(accountId: String, url: String) async throws -> String? {
        let response: CFAPIResponse<URLScanAccepted> = try await client.post(
            "accounts/\(accountId)/urlscanner/v2/scan",
            body: URLScanSubmit(url: url)
        )
        guard response.success else {
            throw response.toAPIError()
        }
        // 不同版本回执里 uuid 可能落在 uuid 或 result 字段
        return response.result?.uuid ?? response.result?.result
    }

    /// 取扫描报告。未就绪时接口 404，此处返回 nil 交由调用方继续轮询。
    func result(accountId: String, scanId: String) async throws -> URLScanResult? {
        do {
            let response: CFAPIResponse<URLScanResult> = try await client.get(
                "accounts/\(accountId)/urlscanner/v2/result/\(scanId)"
            )
            guard response.success else {
                throw response.toAPIError()
            }
            return response.result
        } catch APIError.notFound {
            return nil
        }
    }

    /// 截图 URL（PNG）。直接交给 AsyncImage 加载，鉴权头由 client 统一带。
    func screenshotPath(accountId: String, scanId: String) -> String {
        "accounts/\(accountId)/urlscanner/v2/screenshots/\(scanId).png"
    }
}
