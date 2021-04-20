package com.viatom.lpble.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.viatom.lpble.data.entity.DeviceEntity
import com.viatom.lpble.data.entity.UserEntity

/**
 * author: wujuan
 * created on: 2021/4/16 11:13
 * description:
 */
@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}