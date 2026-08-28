//
//  FileProviderEnumerator.swift
//  Orange Cloud File
//
//  枚举某个容器（桶根 / 文件夹）下的对象与子文件夹。list 用 delimiter=/ 折叠子前缀，
//  每个条目的稳定标识经 [[FileProviderIdentifierStore]] 取/铸；父标识即被枚举的容器本身。
//  R2 无变更流 → enumerateChanges 退化为「无变更」，内容刷新靠系统按需重新枚举。
//

import FileProvider

final class FileProviderEnumerator: NSObject, NSFileProviderEnumerator {

    private let containerIdentifier: NSFileProviderItemIdentifier
    private let client: R2FileProviderClient?
    private let store: FileProviderIdentifierStore?
    private let anchor = NSFileProviderSyncAnchor(Data("v1".utf8))

    init(containerIdentifier: NSFileProviderItemIdentifier,
         client: R2FileProviderClient?,
         store: FileProviderIdentifierStore?) {
        self.containerIdentifier = containerIdentifier
        self.client = client
        self.store = store
        super.init()
    }

    func invalidate() {}

    func enumerateItems(for observer: NSFileProviderEnumerationObserver, startingAt page: NSFileProviderPage) {
        guard let client, let store else {
            observer.finishEnumerating(upTo: nil)
            return
        }
        // 工作集 / 废纸篓 / 非目录容器：报空集
        switch containerIdentifier {
        case .workingSet, .trashContainer:
            observer.finishEnumerating(upTo: nil)
            return
        default:
            break
        }

        let cursor = Self.cursor(from: page)
        Task {
            do {
                let prefix = try await self.containerPrefix(store: store)
                let result = try await client.list(prefix: prefix, cursor: cursor)
                var items: [NSFileProviderItem] = []
                items.reserveCapacity(result.folders.count + result.files.count)
                for folderPrefix in result.folders {
                    let id = await store.folderID(forPrefix: folderPrefix)
                    items.append(FileProviderItem(
                        id: NSFileProviderItemIdentifier(id),
                        parentID: containerIdentifier,
                        filename: FileProviderItem.lastComponent(folderPrefix),
                        content: .folder(prefix: folderPrefix)
                    ))
                }
                for object in result.files {
                    let id = await store.fileID(forKey: object.key)
                    items.append(FileProviderItem(
                        id: NSFileProviderItemIdentifier(id),
                        parentID: containerIdentifier,
                        filename: FileProviderItem.lastComponent(object.key),
                        content: .file(object)
                    ))
                }
                observer.didEnumerate(items)
                observer.finishEnumerating(upTo: Self.page(from: result.nextCursor))
            } catch {
                observer.finishEnumeratingWithError(Self.mapError(error))
            }
        }
    }

    func enumerateChanges(for observer: NSFileProviderChangeObserver, from anchor: NSFileProviderSyncAnchor) {
        observer.finishEnumeratingChanges(upTo: anchor, moreComing: false)
    }

    func currentSyncAnchor(completionHandler: @escaping (NSFileProviderSyncAnchor?) -> Void) {
        completionHandler(anchor)
    }

    // MARK: - 容器 → R2 前缀

    private func containerPrefix(store: FileProviderIdentifierStore) async throws -> String {
        if containerIdentifier == .rootContainer { return "" }
        switch FileProviderIdentifierStore.kind(of: containerIdentifier.rawValue) {
        case .folder:
            guard let prefix = await store.prefix(forFolderID: containerIdentifier.rawValue) else {
                throw NSFileProviderError(.noSuchItem)
            }
            return prefix
        default:
            return ""
        }
    }

    // MARK: - 分页游标 ↔ NSFileProviderPage

    private static func cursor(from page: NSFileProviderPage) -> String? {
        let data = page.rawValue
        if data == NSFileProviderPage.initialPageSortedByName as Data
            || data == NSFileProviderPage.initialPageSortedByDate as Data {
            return nil
        }
        return String(data: data, encoding: .utf8)
    }

    private static func page(from cursor: String?) -> NSFileProviderPage? {
        guard let cursor else { return nil }
        return NSFileProviderPage(Data(cursor.utf8))
    }

    private static func mapError(_ error: Error) -> Error {
        if let clientError = error as? R2FileProviderClient.ClientError {
            switch clientError {
            case .notAuthenticated:
                return NSFileProviderError(.notAuthenticated)
            case .http(let status) where status == 404:
                return NSFileProviderError(.noSuchItem)
            default:
                return NSFileProviderError(.serverUnreachable)
            }
        }
        return error
    }
}
