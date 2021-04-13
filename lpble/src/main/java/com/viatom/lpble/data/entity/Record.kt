package com.viatom.lpble.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * author: wujuan
 * created on: 2021/4/12 16:11
 * description: 保存录制的数据
 */
@Entity(tableName = "record")
data class Record(
        @PrimaryKey
        val id: Long,
        val startTime: Long,
        val duration: Int, //s
        val isAnalysed: Boolean,
        val reportId: Long,
        val oriFileName: String, //本地源文件
        val isUploaded: Boolean,
        val collectType: Int = 0, //0 自动  1 手动
) {


}
