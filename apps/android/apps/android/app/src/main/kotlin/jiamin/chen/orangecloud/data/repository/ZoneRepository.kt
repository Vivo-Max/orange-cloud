package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.local.ZoneDao
import jiamin.chen.orangecloud.data.local.toEntity
import jiamin.chen.orangecloud.data.local.toZone
import jiamin.chen.orangecloud.data.model.CreateZoneRequest
import jiamin.chen.orangecloud.data.model.PauseZoneRequest
import jiamin.chen.orangecloud.data.model.Zone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 域名仓库：Room 为单一可信源，网络刷新后写回缓存（官方 App Architecture）。
 * 对应 iOS ZoneService + SwiftData @Query。
 */
@Singleton
class ZoneRepository @Inject constructor(
    private val api: CfApiClient,
    private val zoneDao: ZoneDao,
    private val json: Json,
) {
    /** 观察缓存中的域名（UI 始终读这里）。 */
    fun observeZones(accountId: String): Flow<List<Zone>> =
        zoneDao.observeByAccount(accountId).map { rows -> rows.map { it.toZone(json) } }

    /** 观察单个域名缓存（域名详情用，含状态/套餐/Name Servers）。 */
    fun observeZone(zoneId: String): Flow<Zone?> =
        zoneDao.observeById(zoneId).map { it?.toZone(json) }

    /**
     * 新建 Zone（添加域名，full setup）。成功后把新 Zone 单条 upsert 进缓存
     * （insertAll 的 REPLACE 只换同主键，不动其它域名），列表 Flow 即时可见。
     */
    suspend fun createZone(accountId: String, name: String): Zone {
        val zone = api.post<Zone, CreateZoneRequest>(
            "zones",
            CreateZoneRequest(name = name, account = CreateZoneRequest.AccountRef(accountId)),
        )
        zoneDao.insertAll(listOf(zone.toEntity(accountId, json)))
        return zone
    }

    /**
     * 暂停 / 恢复 Cloudflare 代理（Dashboard 的「Pause Cloudflare on Site」）。
     * 暂停后 DNS 仍由 Cloudflare 解析，但流量不再经过 Cloudflare——WAF、缓存、
     * 源站 IP 隐藏一并失效；生效约需 5 分钟。需要 zone.write。
     * 成功后单条回写缓存，列表/详情的 Flow 即时翻牌。
     */
    suspend fun setPaused(accountId: String, zoneId: String, paused: Boolean): Zone {
        val zone = api.patch<Zone, PauseZoneRequest>("zones/$zoneId", PauseZoneRequest(paused))
        zoneDao.insertAll(listOf(zone.toEntity(accountId, json)))
        return zone
    }

    /** 单个域名拉最新（含暂停态）并回写缓存。 */
    suspend fun refreshZone(accountId: String, zoneId: String): Zone {
        val zone = api.get<Zone>("zones/$zoneId")
        zoneDao.insertAll(listOf(zone.toEntity(accountId, json)))
        return zone
    }

    /** 从网络拉全量（自动翻页）并整账号替换缓存。 */
    suspend fun refreshZones(accountId: String) {
        val zones = fetchAllZones(accountId)
        zoneDao.replaceForAccount(accountId, zones.map { it.toEntity(accountId, json) })
    }

    /** 一次性拉取账号下全部域名（不写缓存），供需要 zone 列表的功能（如 Worker 路由）使用。 */
    suspend fun listZones(accountId: String): List<Zone> = fetchAllZones(accountId)

    private suspend fun fetchAllZones(accountId: String): List<Zone> {
        val all = mutableListOf<Zone>()
        var page = 1
        while (true) {
            val paged = api.getList<Zone>(
                "zones",
                listOf("account.id" to accountId, "page" to page.toString(), "per_page" to "50"),
            )
            all += paged.items
            val totalPages = paged.info?.totalPages ?: 1
            if (page >= totalPages) break
            page++
        }
        return all
    }
}
