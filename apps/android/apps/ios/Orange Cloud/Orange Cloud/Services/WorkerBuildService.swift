//
//  WorkerBuildService.swift
//  Orange Cloud
//
//  Workers Builds（workers-ci.read / .write）。
//

import Foundation

struct WorkerBuildService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 某个 Worker 的构建记录。未接 CI 的 Worker 会 404，按「没有构建」处理。
    func builds(accountId: String, scriptId: String) async throws -> [WorkerBuild] {
        do {
            let response: CFAPIResponseArray<WorkerBuild> = try await client.get(
                "accounts/\(accountId)/builds/workers/\(scriptId)/builds",
                queryItems: [URLQueryItem(name: "per_page", value: "20")]
            )
            guard response.success else {
                throw response.toAPIError()
            }
            return response.result ?? []
        } catch APIError.notFound {
            return []
        }
    }

    func logs(accountId: String, buildUuid: String) async throws -> [BuildLogLine] {
        let response: CFAPIResponse<BuildLogsPage> = try await client.get(
            "accounts/\(accountId)/builds/builds/\(buildUuid)/logs"
        )
        guard response.success else {
            throw response.toAPIError()
        }
        return response.result?.lines ?? []
    }

    /// 取消进行中的构建（workers-ci.write）
    func cancel(accountId: String, buildUuid: String) async throws {
        let response: CFAPIResponse<WorkerBuild> = try await client.put(
            "accounts/\(accountId)/builds/builds/\(buildUuid)/cancel",
            body: EmptyBody()
        )
        guard response.success else {
            throw response.toAPIError()
        }
    }
}
