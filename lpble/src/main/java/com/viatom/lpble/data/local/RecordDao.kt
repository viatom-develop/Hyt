package com.viatom.lpble.data.local

import androidx.paging.DataSource
import androidx.paging.PagingSource
import androidx.room.*
import com.viatom.lpble.data.entity.RecordAndReport
import com.viatom.lpble.data.entity.RecordEntity
import com.viatom.lpble.data.entity.ReportDetail
import kotlinx.coroutines.flow.Flow

/**
 * author: wujuan
 * created on: 2021/4/15 10:32
 * description:
 */
@Dao
interface RecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(recordEntity: RecordEntity) :Long

    @Query("SELECT * FROM record WHERE id= :recordId")
    suspend fun getRecord(recordId: Long) :RecordEntity


    @Transaction
    @Query("SELECT* FROM record WHERE id=:recordId")
    suspend fun getRecordAndReport(recordId: Long): RecordAndReport?

    @Query("UPDATE record SET isAnalysed = :isAnalysed WHERE id= :recordId")
    suspend fun updateWithAnalysed(recordId: Long, isAnalysed: Boolean )


    @Query("SELECT* FROM reportdetail WHERE userId=:userId")
    fun getRecordAndReportList(userId: Long): PagingSource<Int, ReportDetail>




}