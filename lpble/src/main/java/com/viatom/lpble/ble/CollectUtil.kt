package com.viatom.lpble.ble

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.util.SparseArray
import com.jeremyliao.liveeventbus.LiveEventBus
import com.viatom.lpble.constants.Constant
import com.viatom.lpble.util.SingletonHolder
import com.viatom.lpble.util.doFailure
import com.viatom.lpble.util.doSuccess
import com.viatom.lpble.viewmodels.DashboardViewModel
import kotlinx.coroutines.flow.*

/**
 * author: wujuan
 * created on: 2021/4/12 16:30
 * description:
 */
class CollectUtil private constructor(val application: Application){
    val C_TAG: String = "collectUtil"
    companion object: SingletonHolder<CollectUtil, Application>(::CollectUtil)



    var autoData: FloatArray = FloatArray(0)


    var manualData: FloatArray = FloatArray(0)
    var manualIndex = ArrayList<Int>() // 手动采集到的数据在总数据池中的index 集合




    lateinit var collectService: CollectService

    private val con = object : ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(C_TAG, "onServiceConnected")
            service?.let {
                if (service is CollectService.CollectBinder) {
                    collectService = service.getService()
                    Log.d(C_TAG, "collectService inited")
                    // 应该在service 连接成功后调用 actionCollect
                    LiveEventBus.get(Constant.Event.collectServiceConnected).post(true)

                }
            }

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(C_TAG, "ServiceDisconnected")
        }

    }
    fun initService(): CollectUtil{
        Log.d(C_TAG, "into initService")

        CollectService.startService(application)

        Intent(application, CollectService::class.java).also { intent ->
            application.bindService(intent, con, Context.BIND_AUTO_CREATE)
        }

        return this
        
    }

   suspend fun runAutoCollect(){

       collectService.autoCollect()
           .onStart {
               Log.d("actionCollect", "采集服务已经运行")

           }
           .onCompletion {

           }
           .catch {

           }
           .collect { result ->
               result.doFailure {

               }
               result.doSuccess {
                  when(it){
                      Constant.Collection.AUTO_START -> {
                          //去采集

                      }
                      Constant.Collection.AUTO_STOP -> {
                          //停止采集

                      }
                  }

               }

           }

   }

   suspend fun manualCollect(vm: DashboardViewModel){


        collectService.manualCollect(vm)
            .onStart {
                Log.d(C_TAG, "手动采集开始")
                cleanData()
                vm._manualCollecting.postValue(true)


            }
            .onCompletion {
                Log.d(C_TAG, "手动采集结束")
                vm._manualCollecting.postValue(false)
            }
            .catch {
                Log.d(C_TAG, "手动采集异常终止")
                cleanData()
                vm._manualCollecting.postValue(false)

            }
            .collect { result ->
                result.doFailure {
                    Log.d(C_TAG, "doFailure， $it")
                }
                result.doSuccess {
                    //刷新读秒UI
                    Log.d(C_TAG, "读秒 $it")
                   vm._collectBtnText.postValue("$it S")
                    if (it == Constant.Collection.MANUAL_DURATION ){
                        vm._manualCollecting.postValue(false)
                        vm._collectBtnText.postValue("正在保存")
                        Log.d(C_TAG, "读秒结束后 manualData :${manualData.size}")
                        //todo 将数据持久化

                    }

                }

            }


    }


    /**
     * 不仅要采集数据到缓存中还要讲数据的坐标保存用来显示UI
     * @param type Int
     * @param feed FloatArray
     */
    fun actionCollect(type: Int, feed: FloatArray, index: Int){

        Log.d(C_TAG, "into actionCollect ---type: $type, feed size = ${feed.size}, index = $index")
        if (type == Constant.Collection.TYPE_MANUAL){

            manualData = FloatArray(manualData.size + feed.size).apply { 
                manualData.copyInto(this)
                feed.copyInto(this, manualData.size)
            }

            Log.d(C_TAG, "复制后，manualData Size = ${manualData.size}")
            manualIndex.add(index)
            Log.d(C_TAG, "添加到的index： $index")
            
        }

    }

    fun cleanData(){
        manualData = FloatArray(0)
        manualIndex.clear()
    }

}