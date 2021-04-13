package com.viatom.lpble.data.entity.local

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.viatom.lpble.data.entity.DeviceEntity
import com.viatom.lpble.util.LpResult
import com.viatom.lpble.util.SingletonHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

/**
 * author: wujuan
 * created on: 20214/6 10:28
 * description:
 */
class DBHelper private constructor(application: Application) {
    companion object : SingletonHolder<DBHelper, Application>(::DBHelper)

    val db = Room.databaseBuilder(
        application,
        AppDataBase::class.java, "xphealth-db"
    ).build()


    suspend fun insertOrUpdateDevice(deviceDao: DeviceDao, deviceEntity: DeviceEntity){

//        deviceDao.getDevice(deviceEntity.deviceName)?.let { d ->
//            Log.d("设备已存在，去更新", deviceEntity.toString())
//            deviceDao.insertDevice(deviceEntity)
//        }?: run {
//
//            Log.d("设备不存在，去新增", deviceEntity.toString())
//            deviceDao.insertDevice(deviceEntity)
//        }

        deviceDao.insertDevice(deviceEntity)
    }

    suspend fun getCurrentDeviceDistinctUntilChanged(deviceDao: DeviceDao):Flow<LpResult<DeviceEntity>>   =
            getCurrentDevice(deviceDao).distinctUntilChanged()

    suspend fun getCurrentDevice(deviceDao: DeviceDao): Flow<LpResult<DeviceEntity>> {
        return flow{
            try {
               deviceDao.getCurrentDevices()?.collect {
                   emit(LpResult.Success(it))
               }
            } catch (e: Exception) {
                emit(LpResult.Failure(e.cause))
            }
        }.flowOn(Dispatchers.IO)

    }



}