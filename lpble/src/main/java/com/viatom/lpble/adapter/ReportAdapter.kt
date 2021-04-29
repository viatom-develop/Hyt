package com.viatom.lpble.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.paging.PagingDataAdapter
import com.hi.dhl.jdatabinding.DataBindingViewHolder
import com.hi.dhl.jdatabinding.dowithTry
import com.viatom.lpble.R
import com.viatom.lpble.databinding.FragmentReportItemBinding
import com.viatom.lpble.model.ReportItemModel
import com.viatom.lpble.ui.ReportListFragment

/**
 * author: wujuan
 * created on: 2021/4/16 19:52
 * description:
 */
class ReportAdapter(val fragment: Fragment) :
    PagingDataAdapter<ReportItemModel, ReportViewHolder>(ReportItemModel.diffCallback) {



    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        dowithTry {
            val data = getItem(position)
            data?.let {
                holder.bindData(data, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = inflateView(parent, R.layout.fragment_report_item)
        return ReportViewHolder(view, fragment)
    }

    private fun inflateView(viewGroup: ViewGroup, @LayoutRes viewType: Int): View {
        val layoutInflater = LayoutInflater.from(viewGroup.context)
        return layoutInflater.inflate(viewType, viewGroup, false)
    }






}

class ReportViewHolder(view: View, val fragment: Fragment) : DataBindingViewHolder<ReportItemModel>(view) {
    val mBinding: FragmentReportItemBinding by viewHolderBinding(view)


    override fun bindData(data: ReportItemModel, position: Int) {
        mBinding.apply {
            data.id = "#${position + 1}"
            report = data
            ctx = fragment as ReportListFragment
            executePendingBindings()
        }
    }

}