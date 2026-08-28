//
//  FileProviderItem.swift
//  Orange Cloud File
//
//  R2 对象 / 文件夹（折叠前缀）/ 桶根 → NSFileProviderItem。
//  v1.1：itemIdentifier 不再内容寻址，改用 [[FileProviderIdentifierStore]] 铸的稳定标识，
//  故构造时由调用方（extension / enumerator）显式传入解析好的 id 与 parentID。
//  能力集开放改名/移动（allowsRenaming/allowsReparenting）。
//

import FileProvider
import UniformTypeIdentifiers

final class FileProviderItem: NSObject, NSFileProviderItem {

    enum Content {
        case root(bucketName: String)
        case folder(prefix: String)
        case file(R2FileProviderClient.R2Obj)
    }

    private let id: NSFileProviderItemIdentifier
    private let parentID: NSFileProviderItemIdentifier
    private let displayName: String
    let content: Content

    init(id: NSFileProviderItemIdentifier,
         parentID: NSFileProviderItemIdentifier,
         filename: String,
         content: Content) {
        self.id = id
        self.parentID = parentID
        self.displayName = filename
        self.content = content
    }

    /// 模板默认初始化器（系统兜底用）——当作根处理
    init(identifier: NSFileProviderItemIdentifier) {
        self.id = .rootContainer
        self.parentID = .rootContainer
        self.displayName = identifier.rawValue
        self.content = .root(bucketName: identifier.rawValue)
    }

    var itemIdentifier: NSFileProviderItemIdentifier { id }
    var parentItemIdentifier: NSFileProviderItemIdentifier { parentID }
    var filename: String { displayName }

    var contentType: UTType {
        switch content {
        case .root, .folder:
            return .folder
        case .file(let obj):
            let ext = (obj.key as NSString).pathExtension
            if !ext.isEmpty, let type = UTType(filenameExtension: ext) {
                return type
            }
            if let mime = obj.contentType, let type = UTType(mimeType: mime) {
                return type
            }
            return .data
        }
    }

    var capabilities: NSFileProviderItemCapabilities {
        switch content {
        case .root:
            return [.allowsReading, .allowsContentEnumerating, .allowsAddingSubItems]
        case .folder:
            return [.allowsReading, .allowsContentEnumerating, .allowsAddingSubItems,
                    .allowsDeleting, .allowsRenaming, .allowsReparenting]
        case .file:
            return [.allowsReading, .allowsWriting, .allowsDeleting,
                    .allowsRenaming, .allowsReparenting]
        }
    }

    var documentSize: NSNumber? {
        if case .file(let obj) = content, let size = obj.size {
            return NSNumber(value: size)
        }
        return nil
    }

    var contentModificationDate: Date? {
        if case .file(let obj) = content { return obj.lastModified }
        return nil
    }

    var itemVersion: NSFileProviderItemVersion {
        // 内容版本只反映内容（改名不变 → 不触发重新下载）；元数据版本反映路径/名字（改名即变）
        let contentToken: String
        let metadataToken: String
        switch content {
        case .root(let bucketName):
            contentToken = "root"
            metadataToken = "root:\(bucketName)"
        case .folder(let prefix):
            contentToken = "dir"
            metadataToken = "dir:\(prefix)"
        case .file(let obj):
            contentToken = obj.etag ?? "\(obj.size ?? 0):\(obj.lastModified?.timeIntervalSince1970 ?? 0)"
            metadataToken = obj.key
        }
        return NSFileProviderItemVersion(
            contentVersion: Data(contentToken.utf8),
            metadataVersion: Data(metadataToken.utf8)
        )
    }

    // MARK: - 路径工具（纯字符串，无需查表）

    /// a/b/c.txt → a/b/ ；a/ → ""（根）
    static func parentPrefix(of path: String) -> String {
        let trimmed = path.hasSuffix("/") ? String(path.dropLast()) : path
        guard let slash = trimmed.lastIndex(of: "/") else { return "" }
        return String(trimmed[...slash])
    }

    /// 取相对末段名（去掉父前缀与首尾斜杠）
    static func lastComponent(_ path: String) -> String {
        let trimmed = path.hasSuffix("/") ? String(path.dropLast()) : path
        return trimmed.components(separatedBy: "/").last ?? trimmed
    }
}
