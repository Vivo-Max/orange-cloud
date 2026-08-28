package jiamin.chen.orangecloud.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import jiamin.chen.orangecloud.data.model.DnsRecord

/** DNS 记录缓存行，按 zoneId 归属（一个域名下的全部记录整组替换）。 */
@Entity(tableName = "dns_records", indices = [Index("zoneId")])
data class DnsRecordEntity(
    @PrimaryKey val id: String,
    val zoneId: String,
    val type: String,
    val name: String,
    val content: String,
    val proxied: Boolean?,
    val ttl: Int,
    val priority: Int?,
    val comment: String?,
    val createdOn: String?,
)

fun DnsRecordEntity.toDnsRecord(): DnsRecord = DnsRecord(
    id = id,
    type = type,
    name = name,
    content = content,
    proxied = proxied,
    ttl = ttl,
    priority = priority,
    comment = comment,
    createdOn = createdOn,
)

fun DnsRecord.toEntity(zoneId: String): DnsRecordEntity = DnsRecordEntity(
    id = id,
    zoneId = zoneId,
    type = type,
    name = name,
    content = content,
    proxied = proxied,
    ttl = ttl,
    priority = priority,
    comment = comment,
    createdOn = createdOn,
)
