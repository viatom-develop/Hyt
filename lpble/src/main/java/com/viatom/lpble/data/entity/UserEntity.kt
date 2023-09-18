package com.viatom.lpble.data.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

/**
 * author: wujuan
 * created on: 2021/4/16 11:09
 * description:
 */
@Entity(tableName = "user")
@Parcelize
data class UserEntity(
    @PrimaryKey
    val userId: Long = 0,
    val name: String, // 姓名，必填
    val phone: String,  // 手机号，必填
    val gender: String, // 性别（1：男；2：女）
    val birthday: String,  // 生日（yyyy-MM-dd）
    val id_number: String,  // 身份证号码，对于签字报告必填(国药项目可不填)
    // gender、birthday会影响分析结果，请填写真实数据
//    val height: String = "", //  身高cm，非必填
//    val weight: String = "", // 体重kg，非必填
) : Parcelable
