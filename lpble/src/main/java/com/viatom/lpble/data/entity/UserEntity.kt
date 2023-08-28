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
    val name: String,
    val phone: String,
    val gender: String,
    val birthday: String,
    val id_number: String,
//    val height: String = "", // AI 不必须
//    val weight: String = "", // AI 不必须
) : Parcelable
