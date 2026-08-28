package jiamin.chen.orangecloud.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DnsRecordDao {

    @Query("SELECT * FROM dns_records WHERE zoneId = :zoneId ORDER BY name COLLATE NOCASE, type")
    fun observeByZone(zoneId: String): Flow<List<DnsRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: DnsRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<DnsRecordEntity>)

    @Query("DELETE FROM dns_records WHERE id = :recordId")
    suspend fun deleteById(recordId: String)

    @Query("DELETE FROM dns_records WHERE zoneId = :zoneId")
    suspend fun deleteForZone(zoneId: String)

    /** 整域名替换：刷新时先清后插，避免已删除的记录残留。 */
    @Transaction
    suspend fun replaceForZone(zoneId: String, records: List<DnsRecordEntity>) {
        deleteForZone(zoneId)
        insertAll(records)
    }
}
