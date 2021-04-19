package com.viatom.lpble.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * author: wujuan
 * created on: 2021/4/16 11:09
 * description:
 */
@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val height: String,
    val weight: String,
    val birthday: String,
    val gender: String
)
