package com.viatom.lpble.data.entity.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.viatom.lpble.data.entity.DeviceEntity

@Database(
    entities = [DeviceEntity::class],
    version = 1, exportSchema = false
)
abstract class AppDataBase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao

}
