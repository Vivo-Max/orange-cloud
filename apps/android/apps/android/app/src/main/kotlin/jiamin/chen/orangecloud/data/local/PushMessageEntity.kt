package jiamin.chen.orangecloud.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 推送收件箱一行（NSE/FCM 服务落库，App 读同一表）。明文存储；E2E 解密后再落库。 */
@Entity(tableName = "push_messages")
data class PushMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String?,
    val body: String?,
    val group: String?,
    val url: String?,
    val receivedAt: Long,
)
