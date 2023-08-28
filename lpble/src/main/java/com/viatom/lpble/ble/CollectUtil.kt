package com.viatom.lpble.ble

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.jeremyliao.liveeventbus.LiveEventBus
import com.viatom.lpble.BuildConfig
import com.viatom.lpble.constants.Constant.*
import com.viatom.lpble.constants.Constant.Collection.Companion.AUTO_DURATION_MILLS
import com.viatom.lpble.constants.Constant.Collection.Companion.AUTO_START
import com.viatom.lpble.constants.Constant.Collection.Companion.AUTO_STOP
import com.viatom.lpble.constants.Constant.Collection.Companion.AUTO_EXIT
import com.viatom.lpble.constants.Constant.Collection.Companion.MANUAL_DURATION_S
import com.viatom.lpble.constants.Constant.Collection.Companion.TYPE_AUTO
import com.viatom.lpble.constants.Constant.Collection.Companion.TYPE_MANUAL
import com.viatom.lpble.data.entity.DeviceEntity
import com.viatom.lpble.data.entity.RecordEntity
import com.viatom.lpble.data.entity.ReportEntity
import com.viatom.lpble.data.entity.UserEntity
import com.viatom.lpble.data.local.DBHelper
import com.viatom.lpble.ext.createFile
import com.viatom.lpble.net.RetrofitManager
import com.viatom.lpble.net.RetrofitResponse
import com.viatom.lpble.net.isSuccess
import com.viatom.lpble.retrofit.request.Device
import com.viatom.lpble.retrofit.request.Ecg
import com.viatom.lpble.retrofit.request.EcgInfo
import com.viatom.lpble.retrofit.response.*
import com.viatom.lpble.util.*
import com.viatom.lpble.viewmodels.DashboardViewModel
import com.viatom.lpble.viewmodels.MainViewModel
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.roundToInt


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
    var manualCreateTime: Long = 0L
    var manualCounting: Boolean = false
    var tempValueManual: Boolean = false
    var tempValueAuto: Boolean = false


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

    fun checkService(): Boolean{
        return this::collectService.isInitialized
    }

    fun unbindService(){
        if (checkService())
            context.unbindService(con)
    }

    fun initService(): CollectUtil {
        Log.d(C_TAG, "into initService")

        CollectService.startService(context)

        Intent(context, CollectService::class.java).also { intent ->
            context.bindService(intent, con, Context.BIND_AUTO_CREATE)
        }

        return this

    }

    suspend fun runAutoCollect(mainViewModel: MainViewModel) {

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

                    Log.d(
                        C_TAG,
                        "自动采集 doSuccess $it"
                    )
                    when (it) {
                        AUTO_START -> {
                            //检查当前设备实时状态， 只要这一刻是测量准备和测量中 就开始采集， 否则这30分钟不进行采集
                            Log.d(
                                C_TAG,
                                "自动采集 autoCreateTime$autoCreateTime  autoCounting $autoCounting"
                            )

                            cleanAutoData()
                            autoCreateTime = System.currentTimeMillis()
                            autoCounting = true



                        }
                        AUTO_STOP -> {
                            //停止采集 去分析
                            Log.d(C_TAG, "自动采集倒计时结束，去分析  autoCreateTime$autoCreateTime  autoCounting $autoCounting")
                            if (!autoCounting || autoCreateTime == 0L){
                                Log.d(C_TAG, "30秒前 不满足执行采集条件")
                                return@doSuccess
                            }

                            if (!autoCounting){
                                Log.d(C_TAG, "其他原因已经停止采集")
                                return@collect
                            }
                            autoCounting = false

                                mainViewModel._curBluetooth.value?.let { device ->

                                    saveCollectEcg(TYPE_AUTO)?.let {
                                        //检查deviceName  userId  保存到数据库 并分析
                                        mainViewModel._currentUser.value?.let { user ->
                                            insertRecord(it, TYPE_AUTO, user, device)
                                        } ?: run {
                                            Log.e(C_TAG, "userId is null ")
                                        }

                                    } ?: run{
                                        Log.e(C_TAG, "saveCollectEcg return null ")
                                        finishCollecting(false, TYPE_AUTO)
                                    }
                                } ?: run {
                                    Log.e(C_TAG, "deviceName is null ")
                                }
                            }

                        AUTO_EXIT ->{
                            Log.e(C_TAG, "自动采集中断  等待下一次执行")
                            finishCollecting(false, TYPE_AUTO)
                        }
                    }

                }

            }

    }

    // 将每个步骤写到一个flow中 应该会更好
    fun allFlow(): Flow<LpResult<Boolean>> = flow {

    }


    suspend fun manualCollect(vm: DashboardViewModel, mainViewModel: MainViewModel) {
        if (!this::collectService.isInitialized){
            Log.d(C_TAG, "!this::collectService.isInitialized")
            return
        }
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
                Log.e(C_TAG, "读秒异常终止")

                finishCollecting(false, TYPE_MANUAL,"采集异常终止")

            }.cancellable()
            .collect { result ->
                result.doFailure {
                    Log.e(C_TAG, "读秒 doFailure， $it")
                    finishCollecting(false, TYPE_MANUAL, "${it?.message}")
                }
                result.doSuccess { res ->
                    //刷新读秒UI
                    Log.d(C_TAG, "读秒 $res")
                    if (!manualCounting){
                        Log.d(C_TAG, "其他原因已经停止采集")
                        return@collect
                    }

                    vm._collectBtnText.postValue("$res S")
                    if (res == MANUAL_DURATION_S) {
                        manualCounting = false
                        vm._collectBtnText.postValue("采集")
                        Log.d(C_TAG, "读秒30 manualData :${manualData.size}")

                        //保存txt -> record保存到DB -> 上传txt,等待返回分析结果 -> report保存到DB -> record更新DB 分析状态

                            mainViewModel._curBluetooth.value?.let { device ->

                                saveCollectEcg(TYPE_MANUAL)?.let {
                                    //检查deviceName  userId  保存到数据库 并分析
                                    mainViewModel._currentUser.value?.let { user ->
                                        insertRecord(it, TYPE_MANUAL, user, device)
                                    } ?: run {
                                        Log.e(C_TAG, "userId is null ")
                                    }


                                } ?: {
                                    Log.e(C_TAG, "TYPE_MANUAL: saveCollectEcg is null  ")
                                    finishCollecting(false, TYPE_MANUAL, "文件保存失败")
                                }
                            } ?: run {
                                Log.e(C_TAG, "deviceName is null ")
                            }
                    }

                }

            }



    }


    fun insertRecord(file: File, type: Int, user: UserEntity, device: DeviceEntity) {
        Log.d(C_TAG, "into insertRecord... ")
//        val bytes: ByteArray = FileIOUtils.readFile2BytesByStream(file.absoluteFile)
        if (type == TYPE_AUTO && autoData.isEmpty()  || type == TYPE_MANUAL && manualData.isEmpty()){
            Log.d(C_TAG, "into insertRecord type =$type, data isEmpty ")
            return
        }

        val measureTime = if (type == TYPE_AUTO) autoCreateTime else manualCreateTime
        val duration = if (type == TYPE_AUTO) AUTO_DURATION_MILLS else MANUAL_DURATION_S
        RecordEntity.convert2RecordEntity(
            measureTime,
            file.name,
            type,
            if (type == TYPE_AUTO) autoData else manualData,
            duration,
            device.deviceName,
            user.userId

        ).let { record ->
            DBHelper.getInstance(context).let {
                GlobalScope.launch {
                    it.insertRecord(
                        record
                    ).collect { result ->
                        result.doFailure {
                            Log.e(C_TAG, "数据库存储失败")
                            finishCollecting(false, type, "数据库存储失败")
                        }
                        result.doSuccess { id ->
                            Log.d(C_TAG, "保存心电记录到数据库成功, id: $id")
//                            uploadFile(file, id, type)
                            uploadEcgInfo(id, type, file, user,
                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date(measureTime)), duration.toString())

                        }
                    }
                }
            }

        }

    }

    /**
     * 分析接口可能存在超时，可以自主设计表记录在下次启动时重新尝试上传并分析
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

                                Log.d(C_TAG, "upload param...... ${body.body.toString()}")
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
                                                                this.pdfName =""
                                                                insertReport(this, type)

                                                            }
                                                        }
                                                        else -> {
                                                            // 文件异常等
                                                            Log.e(C_TAG, "分析失败 采集类型$type")
                                                            updateRecordWithAi(recordId, type) //  根据自己的业务决定此处是否更新状态为已分析，如不标记则可以在合适的时候再次上传分析
                                                            finishCollecting(false, type, "文件异常，分析失败")
                                                        }
                                                    }


                                                } else finishCollecting(false, type)
                                        }

                                        override fun onFailure(
                                            call: Call<RetrofitResponse<ReportEntity?>>,
                                            t: Throwable
                                        ) {
                                            Log.e(C_TAG, "分析接口请求失败 $t")
                                            finishCollecting(false, type, "接口响应异常")
                                        }


                                    })
                            }

                    }
            }
        }
    }


    /**
     * 测量数据导入
     */
    private fun uploadEcgInfo(recordId: Long, type: Int,file: File,user: UserEntity, time:String, duration: String) {

        val ecgInfo = EcgInfo(user, Device(RetrofitManager.sn), Ecg(time, duration))
        Log.d(C_TAG, "ecgInfo $ecgInfo")

        val analyse_file_body : RequestBody = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file)
        val analyse_file = MultipartBody.Part.createFormData("analyse_file", file.name, analyse_file_body)
        val ecg_info: RequestBody = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), ecgInfo.toString())

        RetrofitManager.commonService.requestAi(
            RetrofitManager.header(),
            analyse_file,
            ecg_info
        ).enqueue(object: Callback<BaseResponse<AiRequestRes?>>{
            override fun onResponse(call: Call<BaseResponse<AiRequestRes?>>, response: Response<BaseResponse<AiRequestRes?>>) {
                if (response.isSuccessful)
                    response.body()?.let { res ->
                        Log.d(C_TAG, "测量数据导入结果 ${res.toString()}")
                        when (res.isSuccess()) {
                            true -> {
                                res.data?.let {
                                    // 分析
                                    getAiResult(recordId, type, it)
                                }?: kotlin.run {
                                    Log.d(C_TAG, "测量数据导入失败， 采集类型$type")
                                    finishCollecting(false, type, "测量数据导入失败，$response")
                                }
                            }
                            else -> {
                                Log.d(C_TAG, "测量数据导入失败， 采集类型$type")
                                finishCollecting(false, type, "测量数据导入失败，$response")
                            }
                        }


                    } else finishCollecting(false, type)
            }

            override fun onFailure(call: Call<BaseResponse<AiRequestRes?>>, t: Throwable) {
                Log.d(C_TAG, "测量数据导入响应异常， 采集类型$type")

                finishCollecting(false, type, "测量数据导入响应异常，${t}")
            }

        })
    }

    /**
     * 获取AI分析结果
     */
    private fun getAiResult(recordId: Long, type: Int,aiRequestRes: AiRequestRes) {

        RetrofitManager.commonService.requestAiResult(
            RetrofitManager.header(),
            aiRequestRes
        ).enqueue(object :Callback<BaseResponse<AiResult?>>{
            override fun onResponse(call: Call<BaseResponse<AiResult?>>, response: Response<BaseResponse<AiResult?>>) {
                if (response.isSuccessful)
                    response.body()?.let { res ->
                        Log.d(C_TAG, "Ai分析结果 $res")
                        when (res.isSuccess()) {
                            true -> {
                                res.data?.let {
                                    when(it.analysis_status){
                                        AiResult.ANALYSING ->{
                                            GlobalScope.launch {
                                                delay(2000)
                                                getAiResult(recordId, type, aiRequestRes)
                                            }
                                        }
                                        AiResult.FINISHED ->{
                                            if (it.report_url != "") {
                                                Log.d(C_TAG, "分析成功 采集类型$type")

                                                insertReport(it.convertToReportEntity(recordId), type)
                                                return
                                            }else{
                                                GlobalScope.launch {
                                                    delay(2000)
                                                    getAiResult(recordId, type, aiRequestRes)
                                                }
                                            }
                                        }
                                        else ->{
                                            Log.d(C_TAG, "分析失败， ${res.data?.analysis_status}")
                                            finishCollecting(false, type, "分析失败，${res.data?.analysis_status}")

                                        }
                                    }
                                }?: kotlin.run {
                                    Log.e(C_TAG, "分析失败， 采集类型$type")
                                    finishCollecting(false, type, "分析失败，$response")
                                }
                            }
                            else -> {
                                Log.e(C_TAG, "分析失败， 采集类型$type")
                                finishCollecting(false, type, "分析失败，$response")
                            }
                        }

                    } else finishCollecting(false, type)
            }

            override fun onFailure(call: Call<BaseResponse<AiResult?>>, t: Throwable) {
                Log.e(C_TAG, "分析失败， 采集类型$type")
                finishCollecting(false, type, "分析失败，${t.message}")
            }

        })

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
                        Log.e(C_TAG, "保存分析结果保存到数据库失败, recordId: $it")
                        finishCollecting(false, type, "分析结果存储失败")
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
                        Log.e(C_TAG, "分析结果状更新失败, 采集类型： $it")
                        finishCollecting(false, type, "分析结果状更新失败")
                    }
                    result.doSuccess {
                        Log.d(C_TAG, "心电记录分析状态更新成功, 采集类型： $it")
                        //结束了整个上传分析流程
                        finishCollecting(true, type, "分析成功")

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
    fun actionCollectAuto(feed: FloatArray) {


        if (!autoCounting) {
            Log.e(C_TAG, "自动读秒已结束 不能再添加数据")
            return
        }
        autoData = FloatArray(autoData.size + feed.size).apply {
            autoData.copyInto(this)
            feed.copyInto(this, autoData.size)
        }

    }


    fun actionCollectManual(feed: FloatArray) {

        if (!manualCounting) {
            Log.e(C_TAG, "手动读秒已结束 不能再添加数据")
            return
        }

        manualData = FloatArray(manualData.size + feed.size).apply {
            manualData.copyInto(this)
            feed.copyInto(this, manualData.size)
        }

        Log.d(C_TAG, "复制后 手动 ，manualData Size = ${manualData.size}")



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


        val fileName = if (type == TYPE_MANUAL) "$manualCreateTime.txt" else "$autoCreateTime.txt"
       fileName.run {
            context.createFile(Dir.er1EcgDir, this)?.let { file ->
//        context.getFile("${Dir.er1EcgDir}/20210412162855.txt")?.let { file ->

                if (!file.exists()) {
                    Log.d(C_TAG, "saveCollectEcg  !file.exists")
                    return null
                }
                try {
                    BufferedWriter(FileWriter(file)).use { bufferedWriter ->

                        val data = if (type == TYPE_MANUAL)manualData else autoData

                        (data.size - 1).also {
//                            bufferedWriter.write("125,II,1500,")
//                            bufferedWriter.write("F-0-01,125,II,1500,")
                            bufferedWriter.write("F-0-01,125,II,405.35,")
                            for (i in 0..it) {
//                                bufferedWriter.write(((data[i] * 1500).roundToInt()).toString())
                                bufferedWriter.write(((data[i] * 405.35).roundToInt()).toString())
                                bufferedWriter.write(",")
                            }
                        }

//                        bufferedWriter.write((data[data.size - 1] * 1500).roundToInt().toString())
                        bufferedWriter.write((data[data.size - 1] * 405.35).roundToInt().toString())

                        bufferedWriter.close()

                        Log.d(C_TAG, "数据文件保存完成，${file.name} ")
                        return file

                    }
                } catch (e: IOException) {
                    Log.e(C_TAG, "write txt ai file error")
                    return null
                }

            }

        }
        return null
    }



    fun finishCollecting(isSuccess: Boolean, type: Int, msg: String = "") {
        Log.d(C_TAG, "finishCollecting $isSuccess, $type")
        if (type == TYPE_MANUAL) {
            if (isSuccess) {
                LiveEventBus.get(Event.analysisProcessSuccess).post(msg)
            } else {
                LiveEventBus.get(Event.analysisProcessFailed).post(msg)
            }
            cleanData()

        } else {

            cleanAutoData()
        }


    }

    fun cleanData() {
        manualCounting = false
        manualData = FloatArray(0)
        manualCreateTime = 0L
        DataController.dataSrcCollect = null
        tempValueManual = false
        Log.d(C_TAG, "cleanData")
    }

    fun cleanAutoData() {
        autoCounting = false
        autoData = FloatArray(0)
        autoCreateTime = 0L
        tempValueAuto = false
    }

    fun releaseAll(){
        cleanData()
        cleanAutoData()
        unbindService()
    }

}


