package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * R2 Data Catalog —— 把 R2 桶启用为 Apache Iceberg 目录。
 * /accounts/{account_id}/r2-catalog[/{bucket_name}]
 *
 * 2026-08-03 起计费（$9/百万次目录操作，每月含 100 万次免费），
 * UI 上要让用户知道启用是有成本的动作。
 */
@Serializable
data class R2Catalog(
    val id: String? = null,
    val bucket: String? = null,
    /** 完整目录名，形如 account123_analytics-bucket */
    val name: String? = null,
    val status: String? = null,
    /** present 表示已存过访问凭据 */
    @SerialName("credential_status") val credentialStatus: String? = null,
    @SerialName("maintenance_config") val maintenanceConfig: R2CatalogMaintenance? = null,
) {
    val isActive: Boolean get() = status == "active"
}

@Serializable
data class R2CatalogMaintenance(
    val compaction: R2CatalogCompaction? = null,
    @SerialName("snapshot_expiration") val snapshotExpiration: R2CatalogSnapshotExpiration? = null,
)

@Serializable
data class R2CatalogCompaction(
    val state: String? = null,
    /** 接口返回的是字符串数字（如 "128"） */
    @SerialName("target_size_mb") val targetSizeMb: String? = null,
)

@Serializable
data class R2CatalogSnapshotExpiration(
    val state: String? = null,
    @SerialName("max_snapshot_age") val maxSnapshotAge: String? = null,
    @SerialName("min_snapshots_to_keep") val minSnapshotsToKeep: Int? = null,
)

/** 命名空间列表的 result 外壳 */
@Serializable
data class R2CatalogNamespaceList(val namespaces: List<R2CatalogNamespace>? = null)

@Serializable
data class R2CatalogNamespace(
    /** Iceberg 命名空间是分段的，接口以数组返回 */
    val name: List<String>? = null,
) {
    /** 嵌套命名空间用 · 连接展示 */
    val displayName: String get() = name.orEmpty().joinToString(" · ")

    /** SQL 里引用表用的前缀（嵌套命名空间以 . 连接） */
    val sqlName: String get() = name.orEmpty().joinToString(".")
}

/** 表清单的 result 外壳（GET .../namespaces/{ns}/tables） */
@Serializable
data class R2CatalogTableList(val identifiers: List<R2CatalogTableIdentifier>? = null)

@Serializable
data class R2CatalogTableIdentifier(
    val name: String? = null,
    val namespace: List<String>? = null,
) {
    /** SQL 里的完整引用：namespace.table */
    val sqlName: String
        get() = (namespace.orEmpty() + listOfNotNull(name)).filter { it.isNotEmpty() }.joinToString(".")
}
