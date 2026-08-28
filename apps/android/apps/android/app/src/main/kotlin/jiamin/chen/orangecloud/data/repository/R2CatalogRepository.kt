package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.R2Catalog
import jiamin.chen.orangecloud.data.model.R2CatalogNamespace
import jiamin.chen.orangecloud.data.model.R2CatalogNamespaceList
import jiamin.chen.orangecloud.data.model.R2CatalogTableIdentifier
import jiamin.chen.orangecloud.data.model.R2CatalogTableList
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private class CatalogActionBody

/** R2 Data Catalog（r2-catalog.read / .write）。对应 iOS R2CatalogService。 */
@Singleton
class R2CatalogRepository @Inject constructor(
    private val api: CfApiClient,
) {
    /** 单桶目录详情。未启用时接口 404，调用方按「未启用」处理。 */
    suspend fun catalog(accountId: String, bucket: String): R2Catalog =
        api.get("accounts/$accountId/r2-catalog/$bucket")

    /** 启用为 Iceberg 目录。**这是计费动作**，调用方需先确认。 */
    suspend fun enable(accountId: String, bucket: String): R2Catalog =
        api.post("accounts/$accountId/r2-catalog/$bucket/enable", CatalogActionBody())

    /** 停用目录。数据保留，只是不再作为 Iceberg 目录对外提供。 */
    suspend fun disable(accountId: String, bucket: String): R2Catalog =
        api.post("accounts/$accountId/r2-catalog/$bucket/disable", CatalogActionBody())

    suspend fun namespaces(accountId: String, bucket: String): List<R2CatalogNamespace> =
        api.get<R2CatalogNamespaceList>(
            "accounts/$accountId/r2-catalog/$bucket/namespaces",
        ).namespaces.orEmpty()

    /** 某命名空间下的表清单（嵌套命名空间按 Iceberg 惯例以 . 连接进路径）。 */
    suspend fun tables(accountId: String, bucket: String, namespace: String): List<R2CatalogTableIdentifier> =
        api.get<R2CatalogTableList>(
            "accounts/$accountId/r2-catalog/$bucket/namespaces/$namespace/tables",
        ).identifiers.orEmpty()
}
