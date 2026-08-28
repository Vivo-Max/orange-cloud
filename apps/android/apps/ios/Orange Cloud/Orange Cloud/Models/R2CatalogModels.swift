//
//  R2CatalogModels.swift
//  Orange Cloud
//
//  R2 Data Catalog —— 把 R2 桶启用为 Apache Iceberg 目录。
//  /accounts/{account_id}/r2-catalog[/{bucket_name}]
//
//  2026-08-03 起计费（$9/百万次目录操作，每月含 100 万次免费；压实另计），
//  UI 上要让用户知道启用是有成本的动作。
//

import Foundation

nonisolated struct R2Catalog: Codable, Sendable {
    let id:                String?
    /// 所属桶名
    let bucket:            String?
    /// 完整目录名，形如 account123_analytics-bucket
    let name:              String?
    /// active / …
    let status:            String?
    /// present 表示已存过访问凭据
    let credentialStatus:  String?
    let maintenanceConfig: R2CatalogMaintenance?

    enum CodingKeys: String, CodingKey {
        case id, bucket, name, status
        case credentialStatus  = "credential_status"
        case maintenanceConfig = "maintenance_config"
    }

    var isActive: Bool { status == "active" }
}

nonisolated struct R2CatalogMaintenance: Codable, Sendable {
    let compaction:         R2CatalogCompaction?
    let snapshotExpiration: R2CatalogSnapshotExpiration?

    enum CodingKeys: String, CodingKey {
        case compaction
        case snapshotExpiration = "snapshot_expiration"
    }
}

nonisolated struct R2CatalogCompaction: Codable, Sendable {
    let state:        String?
    /// 接口返回的是字符串数字（如 "128"）
    let targetSizeMb: String?

    enum CodingKeys: String, CodingKey {
        case state
        case targetSizeMb = "target_size_mb"
    }
}

nonisolated struct R2CatalogSnapshotExpiration: Codable, Sendable {
    let state:              String?
    let maxSnapshotAge:     String?
    let minSnapshotsToKeep: Int?

    enum CodingKeys: String, CodingKey {
        case state
        case maxSnapshotAge     = "max_snapshot_age"
        case minSnapshotsToKeep = "min_snapshots_to_keep"
    }
}

/// 命名空间列表的 result 外壳
nonisolated struct R2CatalogNamespaceList: Codable, Sendable {
    let namespaces: [R2CatalogNamespace]?
}

nonisolated struct R2CatalogNamespace: Codable, Identifiable, Hashable, Sendable {
    /// Iceberg 命名空间是分段的，接口以数组返回
    let name: [String]?

    var id: String { displayName }

    /// 嵌套命名空间用 · 连接展示
    var displayName: String {
        (name ?? []).joined(separator: " · ")
    }

    /// SQL 里引用表用的前缀（嵌套命名空间以 . 连接）
    var sqlName: String {
        (name ?? []).joined(separator: ".")
    }
}

/// 表清单的 result 外壳（GET .../namespaces/{ns}/tables）
nonisolated struct R2CatalogTableList: Codable, Sendable {
    let identifiers: [R2CatalogTableIdentifier]?
}

nonisolated struct R2CatalogTableIdentifier: Codable, Identifiable, Hashable, Sendable {
    let name:      String?
    let namespace: [String]?

    var id: String { sqlName }

    /// SQL 里的完整引用：namespace.table
    var sqlName: String {
        ((namespace ?? []) + [name ?? ""]).filter { !$0.isEmpty }.joined(separator: ".")
    }
}
