package com.viatom.lpble.retrofit.response

import android.os.Parcelable
import com.google.gson.Gson
import com.viatom.lpble.data.entity.ReportEntity
import kotlinx.parcelize.Parcelize

@Parcelize
class AiResult: Parcelable {
    var analysis_id = ""
    var analysis_status: Int = 80000
    var analysis_result: AnalysisResult? = null
    var report_url = ""


    class AnalysisResult {
        var baseInfo = BaseInfo()
        var hrInfo = HrInfo()
        var diagnose : Diagnose? = null
        var diagnoseList : List<Diagnose>? = null
        var eventList: List<Event>? = null



        class BaseInfo {
            var analysisTime = ""
        }


        class Diagnose {
            var description = ""
            var diagnoseInfo = ""
            var code = ""
        }

        class Event{
            var eventCode = ""
            var eventName = ""
            var startPos = ""
            var endPos = ""
            var eventTime = ""
            var hr = ""
        }

        class HrInfo {
            var averageHeartRate = 0  // hr
            override fun toString(): String {
                return Gson().toJson(this)
            }
        }




        override fun toString(): String {
            return Gson().toJson(this)
        }
    }




    override fun toString(): String {
        return Gson().toJson(this)
    }

    companion object {
        val NO_STATUS = 80000
        val FINISHED = 80001
        val ANALYSING =80002
        val ERROR =80003
        val UPLOADING =80004
        val OTHER =80006
    }
}

fun AiResult.convertToReportEntity(recordId: Long): ReportEntity{
    var report = ReportEntity()
    report.recordId = recordId
    report.analysisId = this.analysis_id
    this.analysis_result?.let {

        val fragmentList = ArrayList<ReportEntity.Fragment>().apply {
            it.eventList?.forEach { event ->
                this.add(ReportEntity.Fragment(event.startPos, event.eventCode, event.eventName, event.endPos, event.hr))
            }
        }
        report.fragmentList = fragmentList

        report.hr = it.hrInfo.averageHeartRate.toString()
        report.sendTime = it.baseInfo.analysisTime
        report.aiResult = it.diagnose?.diagnoseInfo?:""
        val list = ArrayList<ReportEntity.AiResult>().apply {
            it.diagnoseList?.forEach { d->
                this.add(ReportEntity.AiResult(d.diagnoseInfo, d.code, "", "", "", ""))
            }
        }
        report.aiResultList = list
        report.aiDiagnosis = it.diagnose?.diagnoseInfo?:""
    }
    return report
}