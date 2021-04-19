package com.viatom.lpble.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.hi.dhl.jdatabinding.DataBindingViewHolder
import com.hi.dhl.jdatabinding.dowithTry
import com.viatom.lpble.R
import com.viatom.lpble.data.entity.RecordAndReport
import com.viatom.lpble.databinding.FragmentReportLitemBinding
import com.viatom.lpble.model.ReportItemModel

/**
 * author: wujuan
 * created on: 2021/4/16 19:52
 * description:
 */
class ReportAdapter :
    PagingDataAdapter<ReportItemModel, ReportViewHolder>(ReportItemModel.diffCallback) {

    lateinit var viewHolder: ReportViewHolder

    var total: Int = 0

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        dowithTry {
            val data = getItem(position)
            data?.let {
                holder.bindData(data, position)
            }
            total =position
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = inflateView(parent, R.layout.fragment_report_litem)
        return ReportViewHolder(view)
    }

    private fun inflateView(viewGroup: ViewGroup, @LayoutRes viewType: Int): View {
        val layoutInflater = LayoutInflater.from(viewGroup.context)
        return layoutInflater.inflate(viewType, viewGroup, false)
    }






}

class ReportViewHolder(view: View) : DataBindingViewHolder<ReportItemModel>(view) {
    val mBinding: FragmentReportLitemBinding by viewHolderBinding(view)


    override fun bindData(data: ReportItemModel, position: Int) {
        mBinding.apply {
            data.id = "#${position + 1}"
            report = data
            executePendingBindings()
        }
    }

}