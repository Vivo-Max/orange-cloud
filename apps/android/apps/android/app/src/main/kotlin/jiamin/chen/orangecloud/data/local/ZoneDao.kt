package jiamin.chen.orangecloud.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoneDao {

    @Query("SELECT * FROM zones WHERE accountId = :accountId ORDER BY name COLLATE NOCASE")
    fun observeByAccount(accountId: String): Flow<List<ZoneEntity>>

    @Query("SELECT * FROM zones WHERE id = :zoneId LIMIT 1")
    fun observeById(zoneId: String): Flow<ZoneEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(zones: List<ZoneEntity>)

    @Query("DELETE FROM zones WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)

    /** 整账号替换：刷新时先清后插，避免已删除的域名残留 */
    @Transaction
    suspend fun replaceForAccount(accountId: String, zones: List<ZoneEntity>) {
        deleteForAccount(accountId)
        insertAll(zones)
    }
}
