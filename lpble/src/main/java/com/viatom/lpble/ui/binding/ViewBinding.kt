package com.viatom.lpble.ui.binding

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.viatom.lpble.R
import com.viatom.lpble.model.ReportItemModel


@BindingAdapter("fragment", "bindClick")
fun reportItemClick(view: View, fragment: Fragment, model: ReportItemModel) {
    view.setOnClickListener {
        if (model.aiResult == "2"){
            return@setOnClickListener
        }

        Bundle().apply {
            this.putLong("recordId", model.recordId)
            Log.d("bindclick", "$this")
            fragment.findNavController().navigate(R.id.report_list_to_report_detail, this)
        }
    }


}

