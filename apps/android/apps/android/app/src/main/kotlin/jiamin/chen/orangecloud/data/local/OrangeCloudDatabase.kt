package jiamin.chen.orangecloud.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ZoneEntity::class, DnsRecordEntity::class, WorkerEntity::class, PushMessageEntity::class],
    version = 5,
    exportSchema = true,
)
abstract class OrangeCloudDatabase : RoomDatabase() {
    abstract fun zoneDao(): ZoneDao
    abstract fun dnsRecordDao(): DnsRecordDao
    abstract fun workerDao(): WorkerDao
    abstract fun pushMessageDao(): PushMessageDao
}
