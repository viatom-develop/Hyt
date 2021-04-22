package com.viatom.lpble.data.local

import androidx.room.*
import com.viatom.lpble.data.entity.DeviceEntity
import com.viatom.lpble.data.entity.RecordAndReport
import com.viatom.lpble.data.entity.ReportEntity
import kotlinx.coroutines.flow.Flow

/**
 * author: wujuan
 * created on: 2021/4/15 11:38
 * description:
 */
@Dao
interface ReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(reportEntity: ReportEntity): Long

    @Query("UPDATE report SET pdfName = :pdfName WHERE id= :reportId")
    suspend fun updateWithPdf(reportId: Long, pdfName: String)



}