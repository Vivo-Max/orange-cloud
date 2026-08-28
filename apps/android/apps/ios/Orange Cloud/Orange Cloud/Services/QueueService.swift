//
//  QueueService.swift
//  Orange Cloud
//
//  Cloudflare Queues（account 级）。读 queues.read，写 queues.write。
//

import Foundation

struct QueueService {

    private let client: CFAPIClient

    init(client: CFAPIClient) { self.client = client }

    func list(accountId: String) async throws -> [CFQueue] {
        let response: CFAPIResponseArray<CFQueue> = try await client.get("accounts/\(accountId)/queues")
        guard response.success else { throw response.toAPIError() }
        return response.result ?? []
    }

    /// 单个队列详情（含 settings / 生产者 / 消费者，列表项不一定带全）
    func get(accountId: String, queueId: String) async throws -> CFQueue {
        let response: CFAPIResponse<CFQueue> = try await client.get("accounts/\(accountId)/queues/\(queueId)")
        guard response.success, let queue = response.result else { throw response.toAPIError() }
        return queue
    }

    func create(accountId: String, name: String) async throws -> CFQueue {
        let response: CFAPIResponse<CFQueue> = try await client.post(
            "accounts/\(accountId)/queues", body: CFQueueCreate(queueName: name)
        )
        guard response.success, let queue = response.result else { throw response.toAPIError() }
        return queue
    }

    /// 改名 / 改投递设置（保留期、延迟、暂停）。PUT 全字段可选，omit 即不改。
    func update(accountId: String, queueId: String, body: CFQueueUpdate) async throws -> CFQueue {
        let response: CFAPIResponse<CFQueue> = try await client.put(
            "accounts/\(accountId)/queues/\(queueId)", body: body
        )
        guard response.success, let queue = response.result else { throw response.toAPIError() }
        return queue
    }

    /// 清空队列全部消息（不可撤销）
    func purge(accountId: String, queueId: String) async throws {
        let response: CFAPIResponse<EmptyResponse> = try await client.post(
            "accounts/\(accountId)/queues/\(queueId)/purge",
            body: CFQueuePurge(deleteMessagesPermanently: true)
        )
        guard response.success else { throw response.toAPIError() }
    }

    func delete(accountId: String, queueId: String) async throws {
        try await client.delete("accounts/\(accountId)/queues/\(queueId)")
    }
}
