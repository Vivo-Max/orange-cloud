//
//  PermissionSelectionViewModel.swift
//  Orange Cloud
//

import Foundation
import Observation

@Observable
@MainActor
final class PermissionSelectionViewModel {

    var permissions: [FeaturePermission] = FeaturePermission.allFeatures

    // 当前选中的 scope ID 列表（用于预览）
    var selectedScopes: [String] {
        FeaturePermission.buildScopeSet(from: permissions).sorted()
    }

    // 当前 scope 字符串（传给 AuthManager）
    var scopeString: String {
        FeaturePermission.buildScopeString(from: permissions)
    }

    func toggleFeature(id: String) {
        guard let index = permissions.firstIndex(where: { $0.id == id }) else { return }
        guard !permissions[index].isRequired else { return }  // 必选项不可关闭
        permissions[index].isEnabled.toggle()
        // 关闭功能时重置为只读
        if !permissions[index].isEnabled {
            permissions[index].canEdit = false
        }
    }

    func toggleEditPermission(id: String) {
        guard let index = permissions.firstIndex(where: { $0.id == id }) else { return }
        guard permissions[index].isEnabled else { return }
        guard permissions[index].hasEditOption else { return }
        permissions[index].canEdit.toggle()
    }

    // MARK: - 快捷预设

    /// 全部开启、全部只读
    func applyAllReadOnly() {
        for i in permissions.indices {
            permissions[i].isEnabled = true
            permissions[i].canEdit = false
        }
    }

    /// 全部开启、可写的都给读写
    func applyAllReadWrite() {
        for i in permissions.indices {
            permissions[i].isEnabled = true
            permissions[i].canEdit = permissions[i].hasEditOption
        }
    }

    /// 仅保留必选项（其余关闭、只读）
    func applyMinimal() {
        for i in permissions.indices {
            permissions[i].isEnabled = permissions[i].isRequired
            permissions[i].canEdit = false
        }
    }
}
