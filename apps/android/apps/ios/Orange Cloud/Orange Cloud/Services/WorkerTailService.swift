//
//  WorkerTailService.swift
//  Orange Cloud
//
//  Workers tail session 的 REST 生命周期 + WebSocket 工厂。
//

import Foundation

struct WorkerTailService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 创建 tail session，返回预签名 wss:// URL
    func createTail(accountId: String, scriptName: String) async throws -> TailSession {
        let response: CFAPIResponse<TailSession> = try await client.post(
            "accounts/\(accountId)/workers/scripts/\(scriptName)/tails",
            body: EmptyResponse()
        )
        guard response.success, let session = response.result else {
            throw response.toAPIError()
        }
        return session
    }

    /// 销毁 tail session（退出日志页时调用，失败不阻塞）
    func deleteTail(accountId: String, scriptName: String, tailId: String) async throws {
        try await client.delete(
            "accounts/\(accountId)/workers/scripts/\(scriptName)/tails/\(tailId)"
        )
    }

    /// 由 session 构造 WebSocket 连接
    func makeSocket(for session: TailSession) throws -> TailSocket {
        guard let url = URL(string: session.url), url.scheme == "wss" else {
            throw APIError.decodingError(URLError(.badURL))
        }
        return TailSocket(url: url)
    }
}
