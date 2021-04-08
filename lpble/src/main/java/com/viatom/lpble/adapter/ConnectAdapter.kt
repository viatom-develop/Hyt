package com.viatom.lpble.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.lepu.blepro.objs.Bluetooth
import com.viatom.lpble.R

/**
 * author: wujuan
 * created on: 2021/4/6 15:04
 * description:
 */
class ConnectAdapter(layoutResId: Int, data: MutableList<Bluetooth>?) : BaseQuickAdapter<Bluetooth, BaseViewHolder>(layoutResId, data) {



    override fun convert(holder: BaseViewHolder, item: Bluetooth) {
        holder.setText(R.id.name, item.name)
    }
}