package jiamin.chen.orangecloud.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {

    @Query("SELECT * FROM workers WHERE accountId = :accountId ORDER BY id COLLATE NOCASE")
    fun observeByAccount(accountId: String): Flow<List<WorkerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workers: List<WorkerEntity>)

    @Query("DELETE FROM workers WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)

    @Transaction
    suspend fun replaceForAccount(accountId: String, workers: List<WorkerEntity>) {
        deleteForAccount(accountId)
        insertAll(workers)
    }
}
