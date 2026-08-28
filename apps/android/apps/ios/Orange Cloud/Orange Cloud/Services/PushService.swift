//
//  PushService.swift
//  Orange Cloud
//
//  向 orange-cloud-push 注册设备（不碰 CF token）。带已有 device_key 则更新、key 不变。
//

import Foundation

nonisolated struct PushService {

    /// 注册 / 更新设备，返回 device_key。带 existingKey 时服务端只更新 token、key 不变（端点永久）。
    func register(deviceToken: String, existingKey: String?) async throws -> String {
        guard let url = URL(string: "\(PushConfig.serverURL)/api/register") else {
            throw APIError.networkError(URLError(.badURL))
        }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")

        var bodyDict: [String: Any] = ["device_token": deviceToken, "platform": "ios"]
        if let build = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String { bodyDict["build"] = build }
        if let existingKey { bodyDict["device_key"] = existingKey }
        #if DEBUG
        bodyDict["environment"] = "sandbox"
        #endif
        req.httpBody = try JSONSerialization.data(withJSONObject: bodyDict)

        let data: Data
        let response: URLResponse
        do { (data, response) = try await URLSession.shared.data(for: req) }
        catch { throw APIError.networkError(error) }

        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
            throw APIError.serverError(statusCode: (response as? HTTPURLResponse)?.statusCode ?? -1)
        }

        struct RegisterResponse: Decodable { let device_key: String?; let key: String? }
        let decoded: RegisterResponse
        do { decoded = try JSONDecoder().decode(RegisterResponse.self, from: data) }
        catch { throw APIError.decodingError(error) }

        guard let key = decoded.device_key ?? decoded.key, !key.isEmpty else {
            throw APIError.decodingError(URLError(.cannotParseResponse))
        }
        return key
    }
}
