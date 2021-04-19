package com.viatom.lpble.data.entity.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.viatom.lpble.data.entity.*

@Database(
    entities = [DeviceEntity::class, RecordEntity::class, ReportEntity::class, UserEntity::class, PagingKeysEntity::class],
    views = arrayOf(ReportDetail::class),
    version = 2, exportSchema = false
)
@TypeConverters(value = arrayOf(LocalTypeConverter::class))
abstract class AppDataBase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao
    abstract fun recordDao(): RecordDao
    abstract fun reportDao(): ReportDao
    abstract fun userDao(): UserDao

}
