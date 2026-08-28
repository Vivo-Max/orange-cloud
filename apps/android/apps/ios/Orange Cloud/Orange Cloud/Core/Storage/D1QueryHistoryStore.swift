//
//  D1QueryHistoryStore.swift
//  Orange Cloud
//
//  D1 查询控制台的「最近执行」与「SQL 收藏」本机持久化。
//  按数据库（databaseId）分键，不同库的历史互不串；仅本机保存，不跨设备同步。
//
//  膨胀防线（UserDefaults 不适合放大对象）：
//  单条 SQL 截断到 maxSQLLength、每库历史 maxHistory 条 / 收藏 maxFavorites 条、
//  最多保留 maxDatabases 个库（按最近使用淘汰），编码后再按 maxPayloadBytes 兜底裁剪。
//

import Foundation
import Observation

/// 单个数据库的历史 + 收藏
nonisolated struct D1QueryBook: Codable, Sendable, Equatable {
    var history:   [String] = []
    var favorites: [String] = []
    /// 最近一次写入时间，用于超额时淘汰最久未用的库
    var updatedAt: Date = .distantPast

    enum CodingKeys: String, CodingKey {
        case history
        case favorites
        case updatedAt = "updated_at"
    }

    init(history: [String] = [], favorites: [String] = [], updatedAt: Date = .distantPast) {
        self.history = history
        self.favorites = favorites
        self.updatedAt = updatedAt
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        history   = (try? container.decode([String].self, forKey: .history))   ?? []
        favorites = (try? container.decode([String].self, forKey: .favorites)) ?? []
        updatedAt = (try? container.decode(Date.self,     forKey: .updatedAt)) ?? .distantPast
    }
}

@Observable
@MainActor
final class D1QueryHistoryStore {

    static let shared = D1QueryHistoryStore()

    /// 每库最近执行条数
    static let maxHistory = 12
    /// 每库收藏条数上限
    static let maxFavorites = 20
    /// 单条 SQL 字符上限（超长语句只留前段，避免 UserDefaults 被撑大）
    static let maxSQLLength = 2_000
    /// 保留的数据库数量上限（超出按 updatedAt 淘汰最久未用）
    static let maxDatabases = 12
    /// 编码后总字节兜底
    static let maxPayloadBytes = 256 * 1024

    private static let storeKey = "d1QueryBooksByDatabase"

    private(set) var books: [String: D1QueryBook] = [:]

    private init() {
        if let data = UserDefaults.standard.data(forKey: Self.storeKey),
           let decoded = try? JSONDecoder().decode([String: D1QueryBook].self, from: data) {
            books = decoded
        }
    }

    // MARK: - 读

    func history(for databaseId: String) -> [String] {
        books[databaseId]?.history ?? []
    }

    func favorites(for databaseId: String) -> [String] {
        books[databaseId]?.favorites ?? []
    }

    func isFavorite(_ sql: String, in databaseId: String) -> Bool {
        guard let key = Self.normalize(sql) else { return false }
        return favorites(for: databaseId).contains(key)
    }

    // MARK: - 写

    /// 记一条执行成功的 SQL：去重后顶到最前，超额丢最旧
    func record(_ sql: String, in databaseId: String) {
        guard !databaseId.isEmpty, let entry = Self.normalize(sql) else { return }
        update(databaseId) { book in
            book.history.removeAll { $0 == entry }
            book.history.insert(entry, at: 0)
            if book.history.count > Self.maxHistory {
                book.history.removeLast(book.history.count - Self.maxHistory)
            }
        }
    }

    /// 切换收藏，返回切换后的状态（收藏满时丢最旧一条腾位）
    @discardableResult
    func toggleFavorite(_ sql: String, in databaseId: String) -> Bool {
        guard !databaseId.isEmpty, let entry = Self.normalize(sql) else { return false }
        var isOn = false
        update(databaseId) { book in
            if let index = book.favorites.firstIndex(of: entry) {
                book.favorites.remove(at: index)
                isOn = false
            } else {
                book.favorites.insert(entry, at: 0)
                if book.favorites.count > Self.maxFavorites {
                    book.favorites.removeLast(book.favorites.count - Self.maxFavorites)
                }
                isOn = true
            }
        }
        return isOn
    }

    func removeHistory(_ sql: String, in databaseId: String) {
        guard let entry = Self.normalize(sql) else { return }
        update(databaseId) { $0.history.removeAll { $0 == entry } }
    }

    func clearHistory(in databaseId: String) {
        update(databaseId) { $0.history.removeAll() }
    }

    // MARK: - 内部

    /// 去首尾空白 + 单条长度封顶；空串返回 nil
    private static func normalize(_ sql: String) -> String? {
        let trimmed = sql.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        guard trimmed.count > maxSQLLength else { return trimmed }
        return String(trimmed.prefix(maxSQLLength))
    }

    private func update(_ databaseId: String, _ mutate: (inout D1QueryBook) -> Void) {
        var book = books[databaseId] ?? D1QueryBook()
        mutate(&book)
        book.updatedAt = Date()
        if book.history.isEmpty && book.favorites.isEmpty {
            books.removeValue(forKey: databaseId)
        } else {
            books[databaseId] = book
        }
        trimDatabases()
        persist()
    }

    /// 库数量超额：淘汰最久未使用的
    private func trimDatabases() {
        guard books.count > Self.maxDatabases else { return }
        let stale = books
            .sorted { $0.value.updatedAt < $1.value.updatedAt }
            .prefix(books.count - Self.maxDatabases)
            .map(\.key)
        for key in stale { books.removeValue(forKey: key) }
    }

    private func persist() {
        let defaults = UserDefaults.standard
        guard var data = try? JSONEncoder().encode(books) else { return }
        // 兜底：极端长语句堆叠时按最久未用继续丢库，直到落进预算
        while data.count > Self.maxPayloadBytes, books.count > 1 {
            if let oldest = books.min(by: { $0.value.updatedAt < $1.value.updatedAt })?.key {
                books.removeValue(forKey: oldest)
            } else {
                break
            }
            guard let shrunk = try? JSONEncoder().encode(books) else { return }
            data = shrunk
        }
        defaults.set(data, forKey: Self.storeKey)
    }
}
