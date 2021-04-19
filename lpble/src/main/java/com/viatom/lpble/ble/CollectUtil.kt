package com.viatom.lpble.ble

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.jeremyliao.liveeventbus.LiveEventBus
import com.viatom.lpble.constants.Constant.*
import com.viatom.lpble.constants.Constant.Collection.Companion.AUTO_DURATION_MILLS
import com.viatom.lpble.constants.Constant.Collection.Companion.AUTO_START
import com.viatom.lpble.constants.Constant.Collection.Companion.AUTO_STOP
import com.viatom.lpble.constants.Constant.Collection.Companion.MANUAL_DURATION_S
import com.viatom.lpble.constants.Constant.Collection.Companion.TYPE_AUTO
import com.viatom.lpble.constants.Constant.Collection.Companion.TYPE_MANUAL
import com.viatom.lpble.data.entity.RecordEntity
import com.viatom.lpble.data.entity.ReportEntity
import com.viatom.lpble.data.entity.local.DBHelper
import com.viatom.lpble.ext.getFile
import com.viatom.lpble.net.RetrofitManager
import com.viatom.lpble.net.RetrofitResponse
import com.viatom.lpble.net.isSuccess
import com.viatom.lpble.util.*
import com.viatom.lpble.viewmodels.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.util.*


/**
 * author: wujuan
 * created on: 2021/4/12 16:30
 * description:
 */
class CollectUtil private constructor(val context: Context) {
    val C_TAG: String = "collectUtil"

    companion object : SingletonHolder<CollectUtil, Context>(::CollectUtil)


    var autoData: FloatArray = FloatArray(0)
    var autoCreateTime: Long = 0L
    var autoCounting: Boolean = false


    var manualData: FloatArray = FloatArray(0)
    var manualIndex: Int = 0 // 手动采集到的数据在总数据池中的index
    var manualCreateTime: Long = 0L
    var manualCounting: Boolean = false


    lateinit var collectService: CollectService

    private val con = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(C_TAG, "onServiceConnected")
            service?.let {
                if (service is CollectService.CollectBinder) {
                    collectService = service.getService()
                    Log.d(C_TAG, "collectService inited")
                    // 应该在service 连接成功后调用 actionCollect
                    LiveEventBus.get(Event.collectServiceConnected).post(true)

                }
            }

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(C_TAG, "ServiceDisconnected")
        }

    }

    fun initService(): CollectUtil {
        Log.d(C_TAG, "into initService")

        CollectService.startService(context)

        Intent(context, CollectService::class.java).also { intent ->
            context.bindService(intent, con, Context.BIND_AUTO_CREATE)
        }

        return this

    }

    suspend fun runAutoCollect() {
        collectService.autoCollect()
            .onStart {
                Log.d("actionCollect", "自动采集服务已经运行")


            }
            .onCompletion {
                finishCollecting(false, TYPE_AUTO)
            }
            .catch {
                finishCollecting(false, TYPE_AUTO)
            }
            .collect { result ->
                result.doFailure {
                    finishCollecting(false, TYPE_AUTO)
                }
                result.doSuccess {
                    when (it) {
                        AUTO_START -> {
                            //检查当前设备实时状态， 只要这一刻是测量准备和测量中 就开始采集， 否则这30分钟不进行采集
                            Log.d(
                                C_TAG,
                                "自动 此时的实时状态::$BluetoothConfig.currentRunState  autoCounting $autoCounting"
                            )
                            Log.d(C_TAG, "launch...  currentThread：${Thread.currentThread().name}")

                            if (BluetoothConfig.currentRunState in RunState.PREPARING_TEST..RunState.RECORDING) {

                                cleanAutoData()

                                autoCreateTime = System.currentTimeMillis()
                                autoCounting = true
                                Log.d(
                                    C_TAG,
                                    "自动 autoCreateTime$autoCreateTime  autoCounting $autoCounting"
                                )

                            }

                        }
                        AUTO_STOP -> {
                            //停止采集 去分析
                            Log.d(C_TAG, "自动采集倒计时结束，去分析")
                            autoCounting = false

                            Log.d(
                                C_TAG,
                                "自动 autoCreateTime$autoCreateTime  autoCounting $autoCounting"
                            )
                            saveCollectEcg(TYPE_AUTO)?.let { file ->
                                insertRecord(file, TYPE_AUTO)
                            }?: finishCollecting(false, TYPE_AUTO)

                        }
                    }

                }

            }

    }


    fun allFlow(): Flow<LpResult<Boolean>> = flow {

    }


    suspend fun manualCollect(vm: DashboardViewModel) {


        collectService.manualCount()
            .onStart {
                Log.d(C_TAG, "手动采集开始")
                cleanData()
                manualCreateTime = Calendar.getInstance().timeInMillis
                manualCounting = true


            }
            .onCompletion {
                Log.d(C_TAG, "手动读秒结束")

            }
            .catch {
                Log.d(C_TAG, "读秒异常终止")

                finishCollecting(false, TYPE_MANUAL)

            }
            .collect { result ->
                result.doFailure {
                    Log.d(C_TAG, "读秒 doFailure， $it")
                    finishCollecting(false, TYPE_MANUAL)
                }
                result.doSuccess { res ->
                    //刷新读秒UI
                    Log.d(C_TAG, "读秒 $res")

                    vm._collectBtnText.postValue("$res S")
                    if (res == MANUAL_DURATION_S) {
                        manualCounting = false
                        vm._collectBtnText.postValue("采集")
                        Log.d(C_TAG, "读秒30 manualData :${manualData.size}")

                        //保存txt -> record保存到DB -> 上传txt,等待返回分析结果 -> report保存到DB -> record更新DB 分析状态
                        saveCollectEcg(TYPE_MANUAL)?.let {
                            //保存到数据库 并分析
                            insertRecord(it, TYPE_MANUAL)
                        } ?: finishCollecting(false, TYPE_MANUAL)


                    }

                }

            }


    }


    fun insertRecord(file: File, type: Int) {
        val bytes: ByteArray = FileIOUtils.readFile2BytesByStream(file.absoluteFile)
        RecordEntity.convert2RecordEntity(
            if (type == TYPE_AUTO) autoCreateTime else manualCreateTime,
            file.name,
            type,
            bytes,
            if (type == TYPE_AUTO) (AUTO_DURATION_MILLS / 1000).toInt() else MANUAL_DURATION_S,

            )?.let { record ->
            // parse
            DBHelper.getInstance(context).let {
                GlobalScope.launch {
                    it.insertRecord(
                        record
                    ).collect { result ->
                        result.doFailure {
                            finishCollecting(false, type)
                        }
                        result.doSuccess { id ->
                            Log.d(C_TAG, "保存心电记录到数据库成功, id: $id")
                            uploadFile(file, id, type)


                        }
                    }
                }
            }

        }?: run{
            finishCollecting(false, type)
            Toast.makeText(context, "心电文件异常, 无法保存", Toast.LENGTH_SHORT).show()
        }

    }

    /**
     *
     * @param file File
     */
    fun uploadFile(file: File, recordId: Long, type: Int) {

        RetrofitManager.commonService.let { api ->
            Log.d(C_TAG, "开始上传并分析文件...... ${file.name}")
            GlobalScope.launch(Dispatchers.IO) {
                RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file)
                    .let { requestBody ->
                        MultipartBody.Part.createFormData("ecgFile", file.name, requestBody)
                            .let { body ->

                                Log.d(C_TAG, "upload param...... ${body.body}")
                                api.ecgAnalysis(body)
                                    .enqueue(object :
                                        retrofit2.Callback<RetrofitResponse<ReportEntity?>> {
                                        override fun onResponse(
                                            call: Call<RetrofitResponse<ReportEntity?>>,
                                            response: Response<RetrofitResponse<ReportEntity?>>
                                        ) {

                                            if (response.isSuccessful)
                                                response.body()?.let {
                                                    Log.d(C_TAG, "分析结果 $it")
                                                    when (it.isSuccess()) {
                                                        true -> {
                                                            it.data?.run {
                                                                Log.d(C_TAG, "分析成功 采集类型$type")
                                                                this.recordId = recordId
                                                                insertReport(this, type)
                                                            }
                                                        }
                                                        else -> {
                                                            // 文件异常等
                                                            updateRecordWithAi(recordId, type)
                                                            finishCollecting(false, type)
                                                        }
                                                    }


                                                } else finishCollecting(false, type)
                                        }

                                        override fun onFailure(
                                            call: Call<RetrofitResponse<ReportEntity?>>,
                                            t: Throwable
                                        ) {
                                            Log.d(C_TAG, "分析接口请求失败 $t")
                                            finishCollecting(false, type)
                                        }


                                    })
                            }

                    }
            }
        }
    }


    /**
     * 保存分析结果到数据库
     * @param reportEntity ReportEntity
     */
    fun insertReport(reportEntity: ReportEntity, type: Int) {


        DBHelper.getInstance(context).let {
            GlobalScope.launch {
                it.insertReport(
                    reportEntity
                ).collect { result ->
                    result.doFailure {
                        finishCollecting(false, type)
                    }
                    result.doSuccess {
                        Log.d(C_TAG, "保存分析结果到数据库成功, id: $it")
                        //更新心电记录的分析状态
                        updateRecordWithAi(it, type)
                    }
                }
            }
        }

    }

    fun updateRecordWithAi(recordId: Long, type: Int) {
        DBHelper.getInstance(context).let {
            GlobalScope.launch {
                it.updateRecordWithAi(
                    recordId
                ).collect { result ->
                    result.doFailure {
                        finishCollecting(false, type)
                    }
                    result.doSuccess {
                        Log.d(C_TAG, "心电记录分析状态更新成功, 采集类型： $it")
                        //结束了整个上传分析流程
                        finishCollecting(true, type)

                    }
                }
            }
        }


    }


    /**
     * 不仅要采集数据到缓存中还要讲数据的坐标保存用来显示UI
     * @param type Int
     * @param feed FloatArray
     */
    fun actionCollectAuto(feed: FloatArray, index: Int) {

        Log.d(C_TAG, " feed size = ${feed.size}, index = $index")
            if (!autoCounting) {
                Log.d(C_TAG, "自动读秒已结束 不能再添加数据")
                return
            }
            autoData = FloatArray(autoData.size + feed.size).apply {
                autoData.copyInto(this)
                feed.copyInto(this, autoData.size)
            }

            Log.d(C_TAG, "复制后，自动 Size = ${autoData.size} $autoCreateTime  $autoCounting")
        }


    fun actionCollectManual(feed: FloatArray, index: Int) {

        Log.d(C_TAG, " feed size = ${feed.size}, index = $index")
            if (!manualCounting) {
                Log.d(C_TAG, "手动读秒已结束 不能再添加数据")
                return
            }

            manualData = FloatArray(manualData.size + feed.size).apply {
                manualData.copyInto(this)
                feed.copyInto(this, manualData.size)
            }

            Log.d(C_TAG, "复制后 手动 ，manualData Size = ${manualData.size}")
            manualIndex = index
            Log.d(C_TAG, "添加到的手动  index： $index")


    }

    fun saveCollectEcg(type: Int): File? {

        Log.d(
            C_TAG,
            "saveCollectEcg = type$type, manualData:${manualData.size}, autoData: ${autoData.size}"
        )

        if (type == TYPE_MANUAL && manualData.isEmpty() || type == TYPE_AUTO && autoData.isEmpty()) {
            Log.d(C_TAG, "采集数据有误 无法保存")
            return null
        }
        if (type == TYPE_MANUAL && manualCreateTime == 0L || type == TYPE_AUTO && autoCreateTime == 0L) {
            Log.d(C_TAG, "createTime = 0 无法保存")
            return null
        }

//        "$manualCreateTime.txt".run {
//            if (context.createFile(Dir.er1EcgDir, this)) {
        context.getFile("${Dir.er1EcgDir}/20210412162855.txt").let { file ->

            if (!file.exists()) {
                Log.d(C_TAG, "saveCollectEcg  !file.exists")
                return null
            }
            try {
//                        BufferedWriter(FileWriter(file)).use { bufferedWriter ->
//
//                            (manualData.size - 1).also {
//                                bufferedWriter.write("125,II,405,")
//                                for (i in 0 until it) {
//                                    bufferedWriter.write(manualData[i].toString())
//                                    bufferedWriter.write(",")
//                                }
//                                bufferedWriter.write(manualData[it - 1].toString())
//                            }
//                            bufferedWriter.close()
                Log.d(C_TAG, "数据文件保存完成，${file.name} ")
                return file

//                        }
            } catch (e: IOException) {
                Log.d(C_TAG, "write txt ai file error")
                return null
            }


//                }
//            }

        }
        return null
    }


    fun cleanData() {
        manualData = FloatArray(0)
        manualIndex = 0
        manualCreateTime = 0L
        Log.d(C_TAG, "autoCreateTime")
    }

    fun cleanAutoData() {
        autoData = FloatArray(0)
        autoCreateTime = 0L
    }

    fun finishCollecting(isSuccess: Boolean, type: Int) {
        Log.d(C_TAG, "finishCollecting $isSuccess, $type")
        if (type == TYPE_MANUAL) {
            if (isSuccess) {
                LiveEventBus.get(Event.analysisProcessSuccess).post(type)
            } else {
                LiveEventBus.get(Event.analysisProcessFailed).post(type)
            }
            cleanData()
            manualCounting = false
        } else {
            autoCounting = false
            cleanAutoData()
        }


    }

}


