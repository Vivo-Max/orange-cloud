//
//  BotManagementService.swift
//  Orange Cloud
//
//  Zone 机器人管控（bot-management.read / .write）。
//  GET/PUT /zones/{zone_id}/bot_management —— 全套餐可用（含免费版）。
//

import Foundation

struct BotManagementService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 读当前配置。响应 result 是四种套餐形态之一，本模型只取共有的 base_config 字段。
    func config(zoneId: String) async throws -> BotManagementConfig {
        let response: CFAPIResponse<BotManagementConfig> = try await client.get(
            "zones/\(zoneId)/bot_management"
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    /// 写单个字段（PUT 为合并语义，只发要改的那个），返回生效后的完整配置。
    func update<Value: Codable & Sendable>(
        zoneId: String,
        field: BotManagementField,
        value: Value
    ) async throws -> BotManagementConfig {
        let response: CFAPIResponse<BotManagementConfig> = try await client.put(
            "zones/\(zoneId)/bot_management",
            body: BotManagementUpdate(key: field, value: value)
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }
}
