//
//  R2SQLViewModel.swift
//  Orange Cloud
//
//  R2 SQL 查询控制台：命名空间/表清单（点按插入引用）+ 查询执行。
//  只读引擎（SELECT / EXPLAIN），行数超限截断展示（对齐 D1 控制台的防线）。
//

import Foundation
import Observation

@Observable
@MainActor
final class R2SQLViewModel {

    /// 展示行数上限（查询本身不截断，展示截断——扫描量在服务端已发生）
    static let maxRows = 500

    private(set) var namespaces: [R2CatalogNamespace] = []
    /// 命名空间 sqlName → 表引用清单
    private(set) var tables: [String: [R2CatalogTableIdentifier]] = [:]
    private(set) var result: R2SQLResult?
    private(set) var loadedSchema = false
    var isRunning = false
    var error: String?

    private let sqlService: R2SQLService
    private let catalogService: R2CatalogService
    let accountId: String
    let bucketName: String

    init(sqlService: R2SQLService, catalogService: R2CatalogService, accountId: String, bucketName: String) {
        self.sqlService = sqlService
        self.catalogService = catalogService
        self.accountId = accountId
        self.bucketName = bucketName
    }

    /// 拉命名空间与各自的表（一次拉全，目录规模通常很小；失败静默，不挡查询输入）
    func loadSchema() async {
        guard !loadedSchema else { return }
        namespaces = (try? await catalogService.namespaces(accountId: accountId, bucketName: bucketName)) ?? []
        for namespace in namespaces {
            let list = (try? await catalogService.tables(
                accountId: accountId, bucketName: bucketName, namespace: namespace.sqlName
            )) ?? []
            tables[namespace.sqlName] = list
        }
        loadedSchema = true
    }

    /// 全部表的 SQL 引用（namespace.table），供点按插入
    var tableReferences: [String] {
        namespaces.flatMap { tables[$0.sqlName] ?? [] }.map(\.sqlName)
    }

    func run(sql: String) async {
        let trimmed = sql.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, !isRunning else { return }
        isRunning = true
        error = nil
        do {
            result = try await sqlService.query(accountId: accountId, bucketName: bucketName, sql: trimmed)
        } catch APIError.unauthorized, APIError.forbidden {
            // 独立主机对 OAuth token 的接受度未官方明说：401/403 给出定向指引而非通用文案
            error = String(localized: "R2 SQL 查询被拒绝。请确认授权包含 R2 SQL / 数据目录 / R2 读权限；若仍失败，说明该接口暂不接受 App 的登录方式，请先用 Wrangler 查询。")
        } catch {
            self.error = error.localizedDescription
        }
        isRunning = false
    }
}
