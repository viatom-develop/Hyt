package com.viatom.lpble.ble

import android.app.Application
import android.util.Log
import com.lepu.blepro.observer.BleServiceObserver
import com.viatom.lpble.util.SingletonHolder

/**
 * author: wujuan
 * description: 订阅BleService的生命周期
 */
class BleSO private constructor(val application: Application) : BleServiceObserver{
    val TAG : String = "LpBleUtil"



    companion object : SingletonHolder<BleSO, Application>(::BleSO)

    override fun onServiceCreate() {
        Log.d(TAG, "Ble service onCreate")
    }


    /**
     * 蓝牙服务销毁时释放
     */
    override fun onServiceDestroy() {
        Log.d(TAG,"Realm close yes")

    }


}