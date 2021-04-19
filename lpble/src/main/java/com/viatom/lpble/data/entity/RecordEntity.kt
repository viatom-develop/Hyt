package com.viatom.lpble.data.entity

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.viatom.lpble.ecg.DataConvert
import java.text.SimpleDateFormat
import java.util.*

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
        val duration: Int = 0, // s
        val data: ByteArray,
        val dataCrc: Int = 0,
        val magic: Int = 0,
        val fileVersion: String = "",

) {
        companion object {
//                fun convert2RecordEntity(createTime: Long, filename: String, type: Int, d: ByteArray, duration: Int): RecordEntity? {
//                        d.size.let { length ->
//                                if (length < 30) {
//                                        Log.d("parseData", "文件大小错误")
//                                        return null
//                                }
//                                val magic = ((d[length - 1].toInt() and 0xFF shl 24) + (d[length - 2].toInt() and 0xFF shl 16)
//                                        + (d[length - 3].toInt() and 0xFF shl 8) + (d[length - 4].toInt() and 0xFF))
//
//                                Log.d("parseData ", "magic $magic")
//                                if (magic != -0x5aa5fbc8) {
//                                        Log.d("parseData", "文件标志错误")
//                                        return null
//                                }
//                                val dataCrc = (d[length - 15].toInt() and 0xFF shl 8) + (d[length - 16].toInt() and 0xFF)
//
//                                val fileVersion = "V" + d[0].toString()
//
//                                return  RecordEntity(
//                                        createTime = createTime,
//                                        fileName = filename,
//                                        collectType = type,
//                                        data = d,
//                                        dataCrc = dataCrc,
//                                        magic = magic,
//                                        fileVersion = fileVersion,
//                                        duration = duration
//                                )
//                        }
//

                fun convert2RecordEntity(
                        createTime: Long,
                        filename: String,
                        type: Int,
                        d: ByteArray,
                        duration: Int
                ): RecordEntity {

                        return RecordEntity(
                                createTime = createTime,
                                fileName = filename,
                                collectType = type,
                                data = d,
                                duration = duration
                        )
                }


                fun getFilterWaveData(recordEntity: RecordEntity): ShortArray {
                        Log.d("setFilterWaveData", "into...");
                        recordEntity.data.let {
                                (it.size - 30).let { length ->
                                        return ShortArray(length).apply {
                                                val convert = DataConvert()
                                                for (i in 0 until length - 30) {
                                                        val tmp: Short =
                                                                convert.unCompressAlgECG(it[10 + i])
                                                        if (tmp.toInt() != -32768) {
                                                                this[i] =
                                                                        tmp /* == 32767 ? 0 : tmp*/ /* (float) (tmp * (1.0035 * 1800) / (4096 * 178.74))*/
                                                        }
                                                }
                                        }
                                }
                        }
                }


        }




}

fun RecordEntity.getProcessTime(position: Int, pattern: String): String{
        val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        return simpleDateFormat.format(position * 60 * 1000 + createTime)
}



fun RecordEntity.getProcessDuration(position: Int): Int{
        val totalMin = duration / 60;
        val mode = duration % 60;

        val start = position * 60;
        var second = (position + 1) * 60;
        if (position == totalMin && mode != 0) {
                second = start + mode;
        }
        return (second - start);
}





