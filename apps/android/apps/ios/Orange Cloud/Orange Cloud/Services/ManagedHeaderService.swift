//
//  ManagedHeaderService.swift
//  Orange Cloud
//
//  托管请求/响应头（managed-headers.read / .write）。
//

import Foundation

struct ManagedHeaderService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    func transforms(zoneId: String) async throws -> ManagedTransforms {
        let response: CFAPIResponse<ManagedTransforms> = try await client.get(
            "zones/\(zoneId)/managed_headers"
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    /// 开关单条。isRequest 决定改哪一节——两节的 id 空间是独立的。
    func setEnabled(
        zoneId: String,
        id: String,
        enabled: Bool,
        isRequest: Bool
    ) async throws -> ManagedTransforms {
        let toggle = ManagedTransformToggle(id: id, enabled: enabled)
        let body = isRequest
            ? ManagedTransformsPatch(managedRequestHeaders: [toggle], managedResponseHeaders: nil)
            : ManagedTransformsPatch(managedRequestHeaders: nil, managedResponseHeaders: [toggle])
        let response: CFAPIResponse<ManagedTransforms> = try await client.patch(
            "zones/\(zoneId)/managed_headers", body: body
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }
}
