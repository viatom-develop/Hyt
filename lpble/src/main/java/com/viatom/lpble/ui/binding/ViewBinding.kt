package com.viatom.lpble.ui.binding

import android.app.Activity
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.viatom.lpble.model.ReportItemModel


@BindingAdapter("bindClick")
fun bindingClick(view: View, model: ReportItemModel) {
//    view.setOnClickListener {
//        DetailActivity.jumpAcrtivity(
//            view.context,
//            model
//        )
//    }
}
