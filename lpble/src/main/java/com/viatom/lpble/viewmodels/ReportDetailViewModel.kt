package com.viatom.lpble.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viatom.lpble.constants.Constant
import com.viatom.lpble.data.entity.RecordAndReport
import com.viatom.lpble.data.entity.UserEntity
import com.viatom.lpble.data.local.DBHelper
import com.viatom.lpble.ecg.ReportUtil
import com.viatom.lpble.ext.getFile
import com.viatom.lpble.util.doFailure
import com.viatom.lpble.util.doSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

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


    val _pdf = MutableLiveData<File?>().apply {
        value = null
    }
    var pdf: LiveData<File?> = _pdf


    fun queryRecordAndReport(application: Application, recordId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            DBHelper.getInstance(application).queryRecordAndReport(recordId)
                .onStart {

                }
                .catch { }

                .onCompletion { }
                .collect {
                    it.doFailure { }
                    it.doSuccess { result ->
                        _recordAndReport.postValue(result)
                        Log.d("ReportDetail", "queryRecordAndReport success")
                    }
                }

        }

    }


    /**
     * 优先本地加载
     * @param application application
     * @param filename String
     * @param userId Long
     */
    fun loadPdf(application: Application,filename: String, userId: Long, reportId: Long){
        if (filename.isEmpty()){
            Log.d("ReportDetail", "filename is empty， 去创建")
            createPdf(application, userId, reportId)
        }else {
            application.getFile("${Constant.Dir.er1PdfDir}/${filename}").let { local ->
                if (local != null && local.exists()) {
                    Log.d(
                        "ReportDetail",
                        "本地已存在文件：${filename}---- ${local.absolutePath}"
                    )
                   _pdf.value = local
                } else {
                    Log.d("ReportDetail", "本地pdf 不存在， 去创建")
                    createPdf(application, userId, reportId)

                }
            }
        }
    }

    fun createPdf(application: Application, userId: Long, reportId: Long){
        viewModelScope.launch(Dispatchers.IO) {
            DBHelper.getInstance(application).queryUser(userId)
                .collect {
                    it.doFailure { }
                    it.doSuccess { result ->
                        result?.let {
                            inflateReportFile(application, result)?.let { file ->

                                Log.d("ReportDetail", "生成pdf成功: ${file.name}, ${file.absolutePath}")
                                _pdf.postValue(file)
                                //更新db
                                updatePdf(application, reportId,  file.name)

                            }?: Log.d("ReportDetail", "生成的pdf失败")
                        }?: Log.e("ReportDetail", "user is null can not create pdf")
                    }
                }

        }
    }





    fun inflateReportFile(application: Application, userEntity: UserEntity): File? {

        _recordAndReport.value?.let {
            it.recordEntity.run {

                val fileName = "${this.createTime}.pdf"
                Log.d("ReportDetail", "去生成pdf--$fileName")
                return ReportUtil.inflaterReportView(
                    application,
                    it.recordEntity,
                    it.reportEntity,
                    userEntity
                )
                    .let { views ->

                        Log.d("ReportDetail", "views build success")
                        return ReportUtil.makeRecordReport(
                            application,
                            Constant.Dir.er1PdfDir,
                            fileName,
                            views
                        )
                    }

            }
        }
        return null

    }

    fun updatePdf(application: Application, reportId: Long, pdfName: String){
        Log.d("ReportDetail", "updateReportWithPdf")
        viewModelScope.launch(Dispatchers.IO) {
            DBHelper.getInstance(application).updateReportWithPdf(reportId, pdfName)
        }
    }


}