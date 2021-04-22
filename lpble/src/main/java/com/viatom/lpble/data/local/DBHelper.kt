package com.viatom.lpble.data.local

import android.content.Context
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
    suspend fun insertOrUpdateUser(userEntity: UserEntity):Flow<LpResult<Boolean>> =
        flow{
            try {
                db.userDao().insertUser(userEntity)
                emit(LpResult.Success(true))
            } catch (e: Exception) {
                emit(LpResult.Failure(e.cause))
            }
        }.flowOn(Dispatchers.IO)

    suspend fun insertOrUpdateDevice(deviceEntity: DeviceEntity){

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

                db.reportDao().insertReport(reportEntity)
                emit(LpResult.Success(reportEntity.recordId))
            } catch (e: Exception) {
                emit(LpResult.Failure(e.cause))
            }
        }.flowOn(Dispatchers.IO)

    }

    /**
     * 对上传分析过的record标记
     * @param recordId Long
     * @return Flow<LpResult<Int>>
     */
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

    /**
     * 联合查询record and report  用于展示分析报告详情
     * @param recordId Long
     * @return Flow<LpResult<RecordAndReport>>
     */
    suspend fun queryRecordAndReport(recordId: Long): Flow<LpResult<RecordAndReport>> {
        return flow {
            try {
                emit(LpResult.Success(db.recordDao().getRecordAndReportDetail(recordId)))

            } catch (e: Exception) {
                emit(LpResult.Failure(e.cause))
            }

        }.flowOn(Dispatchers.IO)

    }


    /**
     * 内联查询record report 生成视图用于展示分析列表
     * @param userId Long
     * @param mapper2ItemModel Mapper<ReportDetail, ReportItemModel>
     * @param pageConfig PagingConfig
     * @return Flow<PagingData<ReportItemModel>>
     */
     fun queryRecordAndReportList(userId: Long, mapper2ItemModel: Mapper<ReportDetail, ReportItemModel>, pageConfig: PagingConfig): Flow<PagingData<ReportItemModel>> {
        return Pager(pageConfig) {
            // 加载数据库的数据
            db.recordDao().getRecordAndReportList(userId)
        }.flow.map { pagingData ->

            pagingData.map { mapper2ItemModel.map(it) }
        }

    }


    suspend fun updateReportWithPdf(reportId: Long, pdfName: String) {
         db.reportDao().updateWithPdf(reportId, pdfName)

    }






}