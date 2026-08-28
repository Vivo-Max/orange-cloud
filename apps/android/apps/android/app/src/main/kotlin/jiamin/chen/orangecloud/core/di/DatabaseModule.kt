package jiamin.chen.orangecloud.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jiamin.chen.orangecloud.data.local.DnsRecordDao
import jiamin.chen.orangecloud.data.local.OrangeCloudDatabase
import jiamin.chen.orangecloud.data.local.PushMessageDao
import jiamin.chen.orangecloud.data.local.WorkerDao
import jiamin.chen.orangecloud.data.local.ZoneDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OrangeCloudDatabase =
        Room.databaseBuilder(context, OrangeCloudDatabase::class.java, "orange_cloud.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideZoneDao(database: OrangeCloudDatabase): ZoneDao = database.zoneDao()

    @Provides
    fun provideDnsRecordDao(database: OrangeCloudDatabase): DnsRecordDao = database.dnsRecordDao()

    @Provides
    fun provideWorkerDao(database: OrangeCloudDatabase): WorkerDao = database.workerDao()

    @Provides
    fun providePushMessageDao(database: OrangeCloudDatabase): PushMessageDao = database.pushMessageDao()
}
