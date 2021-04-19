package com.viatom.lpble.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class PagingKeysEntity(
    @PrimaryKey
    val name: String,
    val nextKey: Int?
)