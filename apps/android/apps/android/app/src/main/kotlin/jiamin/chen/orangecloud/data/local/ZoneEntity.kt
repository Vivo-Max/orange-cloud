package jiamin.chen.orangecloud.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import jiamin.chen.orangecloud.data.model.Zone
import jiamin.chen.orangecloud.data.model.ZonePlan
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** 域名缓存行，按 accountId 归属（切账号显示各自的域名）。 */
@Entity(tableName = "zones", indices = [Index("accountId")])
data class ZoneEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val name: String,
    val status: String,
    val paused: Boolean = false,
    val planName: String?,
    val nameServersJson: String?,
)

fun ZoneEntity.toZone(json: Json): Zone = Zone(
    id = id,
    name = name,
    status = status,
    paused = paused,
    plan = planName?.let { ZonePlan(it) },
    nameServers = nameServersJson?.let {
        runCatching { json.decodeFromString(ListSerializer(String.serializer()), it) }.getOrNull()
    },
)

fun Zone.toEntity(accountId: String, json: Json): ZoneEntity = ZoneEntity(
    id = id,
    accountId = accountId,
    name = name,
    status = status,
    paused = isPaused,
    planName = plan?.name,
    nameServersJson = nameServers?.let { json.encodeToString(ListSerializer(String.serializer()), it) },
)
