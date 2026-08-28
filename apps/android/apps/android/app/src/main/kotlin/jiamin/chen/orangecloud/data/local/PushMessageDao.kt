package jiamin.chen.orangecloud.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PushMessageDao {
    @Insert
    suspend fun insert(message: PushMessageEntity): Long

    @Query("SELECT * FROM push_messages ORDER BY receivedAt DESC LIMIT 300")
    fun observe(): Flow<List<PushMessageEntity>>

    @Query("DELETE FROM push_messages WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM push_messages")
    suspend fun clear()
}
