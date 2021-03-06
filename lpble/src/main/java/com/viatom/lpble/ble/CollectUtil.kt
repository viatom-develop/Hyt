package com.viatom.lpble.ble

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.jeremyliao.liveeventbus.LiveEventBus
import com.viatom.lpble.constants.Constant.*
import com.viatom.lpble.constants.Constant.Collection.Companion.AUTO_DURATION_MILLS
import com.viatom.lpble.constants.Constant.Collection.Companion.AUTO_START
import com.viatom.lpble.constants.Constant.Collection.Companion.AUTO_STOP
import com.viatom.lpble.constants.Constant.Collection.Companion.AUTO_EXIT
import com.viatom.lpble.constants.Constant.Collection.Companion.MANUAL_DURATION_S
import com.viatom.lpble.constants.Constant.Collection.Companion.TYPE_AUTO
import com.viatom.lpble.constants.Constant.Collection.Companion.TYPE_MANUAL
import com.viatom.lpble.data.entity.RecordEntity
import com.viatom.lpble.data.entity.ReportEntity
import com.viatom.lpble.data.local.DBHelper
import com.viatom.lpble.ext.createFile
import com.viatom.lpble.net.RetrofitManager
import com.viatom.lpble.net.RetrofitResponse
import com.viatom.lpble.net.isSuccess
import com.viatom.lpble.util.*
import com.viatom.lpble.viewmodels.DashboardViewModel
import com.viatom.lpble.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*
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
                    // ?????????service ????????????????????? actionCollect
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
                Log.d("actionCollect", "??????????????????????????????")

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
                        "???????????? doSuccess $it"
                    )
                    when (it) {
                        AUTO_START -> {
                            //????????????????????????????????? ?????????????????????????????????????????? ?????????????????? ?????????30?????????????????????
                            Log.d(
                                C_TAG,
                                "???????????? autoCreateTime$autoCreateTime  autoCounting $autoCounting"
                            )

                            cleanAutoData()
                            autoCreateTime = System.currentTimeMillis()
                            autoCounting = true



                        }
                        AUTO_STOP -> {
                            //???????????? ?????????
                            Log.d(C_TAG, "???????????????????????????????????????  autoCreateTime$autoCreateTime  autoCounting $autoCounting")
                            if (!autoCounting || autoCreateTime == 0L){
                                Log.d(C_TAG, "30?????? ???????????????????????????")
                                return@doSuccess
                            }

                            if (!autoCounting){
                                Log.d(C_TAG, "??????????????????????????????")
                                return@collect
                            }
                            autoCounting = false

                                mainViewModel._curBluetooth.value?.deviceName?.let { deviceName ->

                                    saveCollectEcg(TYPE_AUTO)?.let {
                                        //??????deviceName  userId  ?????????????????? ?????????
                                        mainViewModel._currentUser.value?.userId?.let { userId ->
                                            insertRecord(it, TYPE_AUTO, userId, deviceName)
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
                            Log.e(C_TAG, "??????????????????  ?????????????????????")
                            finishCollecting(false, TYPE_AUTO)
                        }
                    }

                }

            }

    }

    // ???????????????????????????flow??? ???????????????
    fun allFlow(): Flow<LpResult<Boolean>> = flow {

    }


    suspend fun manualCollect(vm: DashboardViewModel, mainViewModel: MainViewModel) {
        if (!this::collectService.isInitialized){
            Log.d(C_TAG, "!this::collectService.isInitialized")
            return
        }
        collectService.manualCount()
            .onStart {
                Log.d(C_TAG, "??????????????????")
                cleanData()
                manualCreateTime = Calendar.getInstance().timeInMillis
                manualCounting = true


            }
            .onCompletion {
                Log.d(C_TAG, "??????????????????")

            }
            .catch {
                Log.e(C_TAG, "??????????????????")

                finishCollecting(false, TYPE_MANUAL,"??????????????????")

            }.cancellable()
            .collect { result ->
                result.doFailure {
                    Log.e(C_TAG, "?????? doFailure??? $it")
                    finishCollecting(false, TYPE_MANUAL, "${it?.message}")
                }
                result.doSuccess { res ->
                    //????????????UI
                    Log.d(C_TAG, "?????? $res")
                    if (!manualCounting){
                        Log.d(C_TAG, "??????????????????????????????")
                        return@collect
                    }

                    vm._collectBtnText.postValue("$res S")
                    if (res == MANUAL_DURATION_S) {
                        manualCounting = false
                        vm._collectBtnText.postValue("??????")
                        Log.d(C_TAG, "??????30 manualData :${manualData.size}")

                        //??????txt -> record?????????DB -> ??????txt,???????????????????????? -> report?????????DB -> record??????DB ????????????

                            mainViewModel._curBluetooth.value?.deviceName?.let { deviceName ->

                                saveCollectEcg(TYPE_MANUAL)?.let {
                                    //??????deviceName  userId  ?????????????????? ?????????
                                    mainViewModel._currentUser.value?.userId?.let { userId ->
                                        insertRecord(it, TYPE_MANUAL, userId, deviceName)
                                    } ?: run {
                                        Log.e(C_TAG, "userId is null ")
                                    }


                                } ?: {
                                    Log.e(C_TAG, "TYPE_MANUAL: saveCollectEcg is null  ")
                                    finishCollecting(false, TYPE_MANUAL, "??????????????????")
                                }
                            } ?: run {
                                Log.e(C_TAG, "deviceName is null ")
                            }
                    }

                }

            }



    }


    fun insertRecord(file: File, type: Int, userId: Long, deviceName: String) {
        Log.d(C_TAG, "into insertRecord... ")
//        val bytes: ByteArray = FileIOUtils.readFile2BytesByStream(file.absoluteFile)
        if (type == TYPE_AUTO && autoData.isEmpty()  || type == TYPE_MANUAL && manualData.isEmpty()){
            Log.d(C_TAG, "into insertRecord type =$type, data isEmpty ")
            return
        }
        RecordEntity.convert2RecordEntity(
            if (type == TYPE_AUTO) autoCreateTime else manualCreateTime,
            file.name,
            type,
            if (type == TYPE_AUTO) autoData else manualData,
            if (type == TYPE_AUTO) AUTO_DURATION_MILLS else MANUAL_DURATION_S,
            deviceName,
            userId

        ).let { record ->
            DBHelper.getInstance(context).let {
                GlobalScope.launch {
                    it.insertRecord(
                        record
                    ).collect { result ->
                        result.doFailure {
                            Log.e(C_TAG, "?????????????????????")
                            finishCollecting(false, type, "?????????????????????")
                        }
                        result.doSuccess { id ->
                            Log.d(C_TAG, "????????????????????????????????????, id: $id")
                            uploadFile(file, id, type)


                        }
                    }
                }
            }

        }

    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????
     * @param file File
     */
    fun uploadFile(file: File, recordId: Long, type: Int) {

        RetrofitManager.commonService.let { api ->
            Log.d(C_TAG, "???????????????????????????...... ${file.name}")
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
                                                    Log.d(C_TAG, "???????????? $it")
                                                    when (it.isSuccess()) {
                                                        true -> {
                                                            it.data?.run {
                                                                Log.d(C_TAG, "???????????? ????????????$type")
                                                                this.recordId = recordId
                                                                this.pdfName =""
                                                                insertReport(this, type)

                                                            }
                                                        }
                                                        else -> {
                                                            // ???????????????
                                                            Log.e(C_TAG, "???????????? ????????????$type")
                                                            updateRecordWithAi(recordId, type) //  ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                                                            finishCollecting(false, type, "???????????????????????????")
                                                        }
                                                    }


                                                } else finishCollecting(false, type)
                                        }

                                        override fun onFailure(
                                            call: Call<RetrofitResponse<ReportEntity?>>,
                                            t: Throwable
                                        ) {
                                            Log.e(C_TAG, "???????????????????????? $t")
                                            finishCollecting(false, type, "??????????????????")
                                        }


                                    })
                            }

                    }
            }
        }
    }


    /**
     * ??????????????????????????????
     * @param reportEntity ReportEntity
     */
    fun insertReport(reportEntity: ReportEntity, type: Int) {


        DBHelper.getInstance(context).let {
            GlobalScope.launch {
                it.insertReport(
                    reportEntity
                ).collect { result ->
                    result.doFailure {
                        Log.e(C_TAG, "??????????????????????????????????????????, recordId: $it")
                        finishCollecting(false, type, "????????????????????????")
                    }
                    result.doSuccess {
                        Log.d(C_TAG, "????????????????????????????????????, id: $it")
                        //?????????????????????????????????
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
                        Log.e(C_TAG, "???????????????????????????, ??????????????? $it")
                        finishCollecting(false, type, "???????????????????????????")
                    }
                    result.doSuccess {
                        Log.d(C_TAG, "????????????????????????????????????, ??????????????? $it")
                        //?????????????????????????????????
                        finishCollecting(true, type, "????????????")

                    }
                }
            }
        }


    }


    /**
     * ???????????????????????????????????????????????????????????????????????????UI
     * @param type Int
     * @param feed FloatArray
     */
    fun actionCollectAuto(feed: FloatArray) {


        if (!autoCounting) {
            Log.e(C_TAG, "????????????????????? ?????????????????????")
            return
        }
        autoData = FloatArray(autoData.size + feed.size).apply {
            autoData.copyInto(this)
            feed.copyInto(this, autoData.size)
        }

    }


    fun actionCollectManual(feed: FloatArray) {

        if (!manualCounting) {
            Log.e(C_TAG, "????????????????????? ?????????????????????")
            return
        }

        manualData = FloatArray(manualData.size + feed.size).apply {
            manualData.copyInto(this)
            feed.copyInto(this, manualData.size)
        }

        Log.d(C_TAG, "????????? ?????? ???manualData Size = ${manualData.size}")



    }



    fun saveCollectEcg(type: Int): File? {


        Log.d(
            C_TAG,
            "saveCollectEcg = type$type, manualData:${manualData.size}, autoData: ${autoData.size}"
        )

        if (type == TYPE_MANUAL && manualData.isEmpty() || type == TYPE_AUTO && autoData.isEmpty()) {
            Log.d(C_TAG, "?????????????????? ????????????")
            return null
        }
        if (type == TYPE_MANUAL && manualCreateTime == 0L || type == TYPE_AUTO && autoCreateTime == 0L) {
            Log.d(C_TAG, "createTime = 0 ????????????")
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
                            bufferedWriter.write("125,II,1500,")
                            for (i in 0..it) {
                                bufferedWriter.write(((data[i] * 1500).roundToInt()).toString())
                                bufferedWriter.write(",")
                            }
                        }

                        bufferedWriter.write((data[data.size - 1] * 1500).roundToInt().toString())

                        bufferedWriter.close()

                        Log.d(C_TAG, "???????????????????????????${file.name} ")
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


