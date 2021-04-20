package com.viatom.lpble.data.local

import androidx.room.TypeConverter
import com.google.gson.GsonBuilder
import com.viatom.lpble.data.entity.ReportEntity
import com.viatom.lpble.ext.fromJson
import com.viatom.lpble.ext.typeToJson

/**
 * author: wujuan
 * created on: 2021/4/15 13:28
 * description:
 */
open class LocalTypeConverter {
    @TypeConverter
    fun json2FragmentEntity(src: String): List<ReportEntity.Fragment>? =
        GsonBuilder().create().fromJson(src)

    @TypeConverter
    fun FragmentEntity2Json(data: List<ReportEntity.Fragment>): String =
        GsonBuilder().create().typeToJson(data)


    @TypeConverter
    fun json2AiResultEntity(src: String): List<ReportEntity.AiResult>? =
        GsonBuilder().create().fromJson(src)

    @TypeConverter
    fun AiResultEntity2Json(data: List<ReportEntity.AiResult>): String =
        GsonBuilder().create().typeToJson(data)


    @TypeConverter
    fun json2posList(src: String): List<Int>? =
        GsonBuilder().create().fromJson(src)

    @TypeConverter
    fun posList2Json(data: List<Int>):String =
        GsonBuilder().create().typeToJson(data)

    @TypeConverter
    fun json2labelList(src: String): List<String>? =
        GsonBuilder().create().fromJson(src)

    @TypeConverter
    fun labelList2Json(data: List<String>):String =
        GsonBuilder().create().typeToJson(data)
}