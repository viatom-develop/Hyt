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
import com.viatom.lpble.data.entity.local.DBHelper
import com.viatom.lpble.ecg.FilterECGReportWave
import com.viatom.lpble.ecg.ReportUtil
import com.viatom.lpble.util.doFailure
import com.viatom.lpble.util.doSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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


    val _user = MutableLiveData<UserEntity?>().apply {
        value = null
    }
    var user: LiveData<UserEntity?> = _user

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
                    }
                }

        }

    }

    fun inflaterReportView(context: Context): List<View>? {

        val record = _recordAndReport.value?.recordEntity ?: return null
        val report = _recordAndReport.value?.reportEntity ?: return null

        return ArrayList<View>().also { viewList ->
            val inflater = LayoutInflater.from(context)
            val params = RelativeLayout.LayoutParams(
                Constant.Report.A4_WIDTH,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            inflater.inflate(R.layout.widget_report, null).also { reportView ->
                reportView.layoutParams = params

                ReportUtil.setupRecordInfo(
                    reportView,
                    record,
                    report,
                    _user.value, 0
                )
                viewList.add(reportView)
            }

            //添加结果和建议
            report.aiResultList?.let { aiResList ->
                for (item in aiResList) {

                    inflater.inflate(R.layout.report_diagnose_content_item, null)
                        .let { diagnoseView ->
                            diagnoseView.layoutParams = params
                            diagnoseView.findViewById<TextView>(R.id.diagnose).apply {
                                this.text = item.aiDiagnosis
                            }

                            diagnoseView.findViewById<TextView>(R.id.advice_content).apply {
                                this.text = item.phoneContent.replace("\\n", "\n")
                            }
                            viewList.add(diagnoseView)
                        }
                }
            }




                report.fragmentList?.let { fragmentList ->
                    //添加波形片段
                    val scale = 6.0f
                    val imgWidth = scale * 25 * 7.8f //25mm/s，每行7秒，每mm对应11.8f //39格子
                    val imgHeight = scale * 10 * 6f //每mm对应11.8f*10mm/mV*每行2.5mV*9行//12格子

                    Collections.sort(
                        fragmentList,
                        Collections.reverseOrder { request1, request2 ->
                            request1.startPose.toInt().compareTo(request2.startPose.toInt())
                        })

                    for (i in fragmentList.indices) {
                            fragmentList[i].let { frag ->

                               inflater.inflate(R.layout.report_list_item, null, false).also { itemView ->
                                   itemView.findViewById<TextView>(R.id.name_val).apply {
                                        this.text = frag.name
                                    }

                                    //填写片段起始时间
                                   itemView.findViewById<TextView>(R.id.time_val).apply {
                                        val startPoint: Int = frag.startPose.toInt() / 2
                                        val time = startPoint / 125 + record.createTime
                                        val date = Date(TimeUnit.SECONDS.toMillis(time))

                                        Log.d(
                                            ReportDetailViewModel::class.simpleName,
                                            "record createTime $time"
                                        )
                                        val timeStr = SimpleDateFormat(
                                            "yyyy-MM-dd HH:mm:ss",
                                            Locale.getDefault()
                                        ).format(date)
                                        this.text = timeStr


                                        //获取当前片段对应的标记
                                        findViewById<RelativeLayout>(R.id.rl_wave)
                                            .let { waveLayout ->

                                                layoutParams = RelativeLayout.LayoutParams(
                                                    Constant.Report.A4_WIDTH, 390
                                                ).apply {
                                                    addRule(
                                                        RelativeLayout.CENTER_HORIZONTAL,
                                                        RelativeLayout.TRUE
                                                    )
                                                }

                                                val wave = FilterECGReportWave(
                                                    context,
                                                    record,
                                                    report,
                                                    imgWidth,
                                                    imgHeight,
                                                    0,
                                                    i,
                                                    startPoint
                                                )
                                                waveLayout.invalidate()
                                                waveLayout.addView(wave)

                                            }
                                    }

                                   viewList.add(itemView)

                                }
                            }
                    }


                }

                //添加报告说明
                inflater.inflate(R.layout.report_tip_item, null).apply {
                    this.layoutParams = params
                    viewList.add(this)
                }

        }
    }


}