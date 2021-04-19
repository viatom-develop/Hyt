package com.viatom.lpble.data.entity.local

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.paging.*
import androidx.room.Room
import com.viatom.lpble.data.entity.*
import com.viatom.lpble.mapper.Mapper
import com.viatom.lpble.model.ReportItemModel
import com.viatom.lpble.util.LpResult
import com.viatom.lpble.util.SingletonHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

/**
 * author: wujuan
 * created on: 20214/6 10:28
 * description:
 */
class DBHelper private constructor(context: Context) {
    companion object : SingletonHolder<DBHelper, Context>(::DBHelper)

    val db = Room.databaseBuilder(
        context,
        AppDataBase::class.java, "xphealth-db"
    ).build()


    suspend fun insertOrUpdateDevice(deviceEntity: DeviceEntity){

//        deviceDao.getDevice(deviceEntity.deviceName)?.let { d ->
//            Log.d("设备已存在，去更新", deviceEntity.toString())
//            deviceDao.insertDevice(deviceEntity)
//        }?: run {
//
//            Log.d("设备不存在，去新增", deviceEntity.toString())
//            deviceDao.insertDevice(deviceEntity)
//        }

        db.deviceDao().insertDevice(deviceEntity)
    }

    suspend fun getCurrentDeviceDistinctUntilChanged():Flow<LpResult<DeviceEntity>>   =
            getCurrentDevice().distinctUntilChanged()

    suspend fun getCurrentDevice(): Flow<LpResult<DeviceEntity>> {
        return flow{
            try {
               db.deviceDao().getCurrentDevices()?.collect {
                   emit(LpResult.Success(it))
               }
            } catch (e: Exception) {
                emit(LpResult.Failure(e.cause))
            }
        }.flowOn(Dispatchers.IO)

    }


    suspend fun insertRecord( recordEntity: RecordEntity): Flow<LpResult<Long>> {
        return flow{

            try {
                emit(LpResult.Success(db.recordDao().insertRecord(recordEntity)))
            } catch (e: Exception) {
                emit(LpResult.Failure(e.cause))
            }
        }.flowOn(Dispatchers.IO)

    }



    suspend fun insertReport( reportEntity: ReportEntity): Flow<LpResult<Long>> {
        return flow{
            try {
                emit(LpResult.Success(db.reportDao().insertReport(reportEntity)))
            } catch (e: Exception) {
                emit(LpResult.Failure(e.cause))
            }
        }.flowOn(Dispatchers.IO)

    }

    suspend fun updateRecordWithAi(recordId: Long): Flow<LpResult<Int>> {
        return flow {
            try {
            db.recordDao().updateWithAnalysed(recordId, true)
                emit(LpResult.Success(db.recordDao().getRecord(recordId).collectType))
            } catch (e: Exception) {
                emit(LpResult.Failure(e.cause))
            }

        }.flowOn(Dispatchers.IO)

    }

    suspend fun queryRecordAndReport(recordId: Long): Flow<LpResult<RecordAndReport?>> {
        return flow {
            try {

                emit(LpResult.Success(db.recordDao().getRecordAndReport(recordId)))
            } catch (e: Exception) {
                emit(LpResult.Failure(e.cause))
            }

        }.flowOn(Dispatchers.IO)

    }


     fun queryRecordAndReportList(mapper2ItemModel: Mapper<ReportDetail, ReportItemModel>, pageConfig: PagingConfig): Flow<PagingData<ReportItemModel>> {
        return Pager(pageConfig) {
            // 加载数据库的数据
            db.recordDao().getRecordAndReportList()
        }.flow.map { pagingData ->

            pagingData.map { mapper2ItemModel.map(it) }
        }

    }






}