package com.viatom.lpble.viewmodels

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viatom.lpble.R
import com.viatom.lpble.constants.Constant
import com.viatom.lpble.data.entity.RecordAndReport
import com.viatom.lpble.data.entity.UserEntity
import com.viatom.lpble.data.local.DBHelper
import com.viatom.lpble.ecg.FilterECGReportWave
import com.viatom.lpble.ecg.ReportUtil
import com.viatom.lpble.util.LpResult
import com.viatom.lpble.util.doFailure
import com.viatom.lpble.util.doSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * author: wujuan
 * created on: 2021/4/16 9:10
 * description:
 */
class ReportDetailViewModel : ViewModel() {


    val _recordAndReport = MutableLiveData<RecordAndReport?>().apply {
        value = null
    }
    var recordAndReport: LiveData<RecordAndReport?> = _recordAndReport


    fun queryRecordAndReport(context: Context, recordId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            DBHelper.getInstance(context).queryRecordAndReport(recordId)
                .onStart {

                }
                .catch { }

                .onCompletion { }
                .collect {
                    it.doFailure { }
                    it.doSuccess { result ->
                        _recordAndReport.postValue(result)
                        Log.d("ReportDetailFragment", "queryRecordAndReport success")

                    }
                }

        }

    }



    fun inflateReportFile(context: Context, userEntity: UserEntity): File? {

        _recordAndReport.value?.let {
            it.recordEntity.run {

                val fileName = "${this.createTime}.pdf"
                Log.d("ReportDetailFragment", "去生成pdf--$fileName")
                return ReportUtil.inflaterReportView(
                    context,
                    it.recordEntity,
                    it.reportEntity,
                    userEntity
                )
                    .let { views ->

                        Log.d("report", "views build success")
                        return ReportUtil.makeRecordReport(
                            context,
                            Constant.Dir.er1PdfDir,
                            fileName,
                            views
                        )
                    }

            }
        }
        return null

    }

    fun updatePdf(context: Context, reportId: Long, pdfName: String){
        viewModelScope.launch(Dispatchers.IO) {
            DBHelper.getInstance(context).updateReportWithPdf(reportId, pdfName)
        }
    }


}