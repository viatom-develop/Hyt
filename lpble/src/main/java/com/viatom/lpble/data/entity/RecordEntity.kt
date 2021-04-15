package com.viatom.lpble.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * author: wujuan
 * created on: 2021/4/12 16:11
 * description: 保存录制的数据
 */
@Entity(tableName = "record")
data class RecordEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0,
        val createTime: Long,
        var isAnalysed: Boolean = false,
        val fileName: String, //本地源文件
        val collectType: Int = 0, //0 自动  1 手动
) {
        companion object{
                fun convert2RecordEntity(createTime: Long, filename: String, type: Int ): RecordEntity{
                      return  RecordEntity(createTime = createTime, fileName = filename, collectType = type)
                }
        }


}
