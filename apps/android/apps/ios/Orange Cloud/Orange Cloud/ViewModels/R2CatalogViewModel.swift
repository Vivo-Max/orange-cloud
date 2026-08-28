//
//  R2CatalogViewModel.swift
//  Orange Cloud
//
//  R2 桶的 Iceberg 数据目录：启用状态、维护配置、命名空间。
//

import Foundation
import Observation

@Observable
@MainActor
final class R2CatalogViewModel {

    private(set) var catalog: R2Catalog?
    private(set) var namespaces: [R2CatalogNamespace] = []
    var isLoading = false
    var loaded = false
    var isMutating = false
    var error: String?
    var didMutate = false

    private let service: R2CatalogService
    private let accountId: String
    private let bucketName: String

    init(service: R2CatalogService, accountId: String, bucketName: String) {
        self.service = service
        self.accountId = accountId
        self.bucketName = bucketName
    }

    var isEnabled: Bool { catalog?.isActive == true }

    func load() async {
        isLoading = true
        error = nil
        do {
            catalog = try await service.catalog(accountId: accountId, bucketName: bucketName)
            // 命名空间只在已启用时才有意义；失败不连累目录状态显示
            namespaces = isEnabled
                ? ((try? await service.namespaces(accountId: accountId, bucketName: bucketName)) ?? [])
                : []
            loaded = true
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func setEnabled(_ on: Bool) async {
        guard !isMutating else { return }
        isMutating = true
        error = nil
        do {
            if on {
                catalog = try await service.enable(accountId: accountId, bucketName: bucketName)
            } else {
                try await service.disable(accountId: accountId, bucketName: bucketName)
            }
            didMutate.toggle()
            await load()
        } catch {
            self.error = error.localizedDescription
        }
        isMutating = false
    }
}
