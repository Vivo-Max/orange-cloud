package jiamin.chen.orangecloud.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import jiamin.chen.orangecloud.data.model.WorkerScript

/** Workers 脚本缓存行，按 accountId 归属（切账号显示各自的脚本）。 */
@Entity(tableName = "workers", indices = [Index("accountId")])
data class WorkerEntity(
    @PrimaryKey val rowKey: String,   // accountId + ":" + id，避免跨账号同名脚本主键冲突
    val accountId: String,
    val id: String,
    val etag: String?,
    val createdOn: String?,
    val modifiedOn: String?,
    val usageModel: String?,
    val handlersJson: String?,
    val logpush: Boolean?,
)

fun WorkerEntity.toWorker(): WorkerScript = WorkerScript(
    id = id,
    etag = etag,
    createdOn = createdOn,
    modifiedOn = modifiedOn,
    usageModel = usageModel,
    handlers = handlersJson?.split(',')?.filter { it.isNotEmpty() },
    logpush = logpush,
)

fun WorkerScript.toEntity(accountId: String): WorkerEntity = WorkerEntity(
    rowKey = "$accountId:$id",
    accountId = accountId,
    id = id,
    etag = etag,
    createdOn = createdOn,
    modifiedOn = modifiedOn,
    usageModel = usageModel,
    handlersJson = handlers?.joinToString(","),
    logpush = logpush,
)
