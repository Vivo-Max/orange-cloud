//
//  WorkerUploadViewModel.swift
//  Orange Cloud
//
//  新建 Worker / 更新现有 Worker 代码。两者都是 PUT /workers/scripts/{name}：
//  新建无旧绑定；更新则先读 /settings（OAuth 下可读）把现有绑定 inherit 回去，
//  避免覆盖代码时把变量 / 密钥 / KV·D1·R2 绑定一并清空。
//

import Foundation
import Observation

@Observable
@MainActor
final class WorkerUploadViewModel {

    /// 单文件单上限 25 MiB（同 Workers 资源单文件上限）
    static let maxAssetBytes = 25 * 1024 * 1024

    var isUploading = false
    var error: String?
    var didUpload = false       // sensoryFeedback 触发器

    // 原地编辑：读现有源码预填（/content/v2，OAuth 下可读）
    var isLoadingSource = false
    var sourceUneditable = false   // 多模块打包产物：无法安全单文件替换（会丢其它模块）

    // 静态资源上传进度
    var uploadedAssets = 0
    var totalAssets = 0

    private let service: WorkerService
    let accountId: String

    init(service: WorkerService, accountId: String) {
        self.service = service
        self.accountId = accountId
    }

    /// 默认兼容性日期：取今天（module worker 必填），用户可在表单改
    static var defaultCompatibilityDate: String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(identifier: "UTC")
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: Date())
    }

    /// Worker 名称规则：小写字母 / 数字开头，其后可含小写字母 / 数字 / 连字符 / 下划线
    static func isValidName(_ name: String) -> Bool {
        name.range(of: "^[a-z0-9][a-z0-9_-]*$", options: .regularExpression) != nil
    }

    /// 新建脚本（无旧绑定）
    func create(name: String, code: String, isModule: Bool, compatibilityDate: String) async -> Bool {
        guard !isUploading else { return false }
        isUploading = true
        error = nil
        defer { isUploading = false }
        do {
            try await service.deployScript(
                accountId: accountId, scriptName: name, code: code,
                isModule: isModule, compatibilityDate: compatibilityDate
            )
            didUpload.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    /// 新建多模块 Worker（entryName 为入口模块，其余为依赖模块 / 数据模块）
    func createMultiModule(name: String, modules: [WorkerUploadModule], entryName: String, compatibilityDate: String) async -> Bool {
        guard !isUploading, !modules.isEmpty else { return false }
        isUploading = true
        error = nil
        defer { isUploading = false }
        do {
            try await service.deployModules(
                accountId: accountId, scriptName: name, modules: modules,
                entryName: entryName, compatibilityDate: compatibilityDate
            )
            didUpload.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    /// 新建静态资源站（assets-only）：算 manifest → 建上传 session → 按桶传 → PUT 挂资源
    func createWithAssets(name: String, assets: [PagesDeployFile], compatibilityDate: String, spa: Bool) async -> Bool {
        guard !isUploading, !assets.isEmpty else { return false }
        isUploading = true
        error = nil
        uploadedAssets = 0
        totalAssets = 0
        defer { isUploading = false }

        if let big = assets.first(where: { $0.data.count > Self.maxAssetBytes }) {
            error = String(localized: "文件 \(big.path) 超过 25 MB，超出单文件上限")
            return false
        }

        var manifest: [String: WorkerAssetManifestEntry] = [:]
        var fileByHash: [String: PagesDeployFile] = [:]
        for file in assets {
            let ext = (file.path as NSString).pathExtension
            let hash = AssetHash.workerAsset(data: file.data, ext: ext)
            manifest[file.path] = WorkerAssetManifestEntry(hash: hash, size: file.data.count)
            fileByHash[hash] = file
        }

        do {
            let session = try await service.createAssetsUploadSession(
                accountId: accountId, scriptName: name, manifest: manifest
            )
            var completionJWT = session.jwt
            let buckets = (session.buckets ?? []).filter { !$0.isEmpty }
            totalAssets = buckets.reduce(0) { $0 + $1.count }

            if totalAssets > 0 {
                guard let sessionJWT = session.jwt else {
                    error = String(localized: "上传会话未返回令牌")
                    return false
                }
                for bucket in buckets {
                    let files = bucket.compactMap { hash -> (hash: String, base64: String, contentType: String)? in
                        guard let file = fileByHash[hash] else { return nil }
                        return (hash, file.data.base64EncodedString(), file.contentType)
                    }
                    if let token = try await service.uploadAssetsBucket(
                        accountId: accountId, sessionJWT: sessionJWT, files: files
                    ) {
                        completionJWT = token
                    }
                    uploadedAssets += files.count
                }
            }

            guard let completionJWT else {
                error = String(localized: "未获得部署完成令牌")
                return false
            }
            try await service.deployWithAssets(
                accountId: accountId, scriptName: name, completionJWT: completionJWT,
                compatibilityDate: compatibilityDate,
                htmlHandling: "auto-trailing-slash",
                notFoundHandling: spa ? "single-page-application" : "none",
                mainModule: nil
            )
            didUpload.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    /// 原地编辑前读取当前线上源码用于预填。多模块打包产物置 sourceUneditable，
    /// 单文件替换会丢失其它模块，故不允许原地编辑（引导用户走多模块整体替换）。
    func fetchSource(scriptName: String) async -> WorkerContent? {
        isLoadingSource = true
        sourceUneditable = false
        error = nil
        defer { isLoadingSource = false }
        do {
            let content = try await service.content(accountId: accountId, scriptName: scriptName)
            sourceUneditable = !content.isEditable
            return content
        } catch {
            self.error = error.localizedDescription
            return nil
        }
    }

    /// 更新现有脚本代码：保留绑定与兼容性日期（从 /settings 读取后 inherit 回写）
    func replace(scriptName: String, code: String, isModule: Bool) async -> Bool {
        guard !isUploading else { return false }
        isUploading = true
        error = nil
        defer { isUploading = false }
        do {
            let settings = try await service.settings(accountId: accountId, scriptName: scriptName)
            try await service.deployScript(
                accountId: accountId, scriptName: scriptName, code: code,
                isModule: isModule,
                compatibilityDate: settings.compatibilityDate ?? Self.defaultCompatibilityDate,
                compatibilityFlags: settings.compatibilityFlags,
                inheritBindings: settings.bindings.map { $0.asInherit() }
            )
            didUpload.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }
}
