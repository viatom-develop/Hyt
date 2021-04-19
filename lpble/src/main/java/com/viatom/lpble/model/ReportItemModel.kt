package com.viatom.lpble.model

import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import kotlinx.android.parcel.Parcelize

/**
 * author: wujuan
 * created on: 2021/4/16 18:50
 * description:
 */
@Parcelize
data class ReportItemModel (
    var id: String = "",
    val createTime: String,
    val type: Int,
    val hr: String,
    val aiDiagnosis: String,
    val recordId: Long,

    ): Parcelable {
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<ReportItemModel>() {
            override fun areItemsTheSame(
                oldItem: ReportItemModel,
                newItem: ReportItemModel
            ): Boolean =
                oldItem.createTime == newItem.createTime

            override fun areContentsTheSame(
                oldItem: ReportItemModel,
                newItem: ReportItemModel
            ): Boolean =
                oldItem == newItem
        }


    }
}
