package com.viatom.lpble.data.entity.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.viatom.lpble.data.entity.ReportEntity
import com.viatom.lpble.data.entity.DeviceEntity
import com.viatom.lpble.data.entity.RecordEntity

@Database(
    entities = [DeviceEntity::class, RecordEntity::class, ReportEntity::class],
    version = 1, exportSchema = false
)
@TypeConverters(value = arrayOf(LocalTypeConverter::class))
abstract class AppDataBase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao
    abstract fun recordDao(): RecordDao
    abstract fun reportDao(): ReportDao

}
