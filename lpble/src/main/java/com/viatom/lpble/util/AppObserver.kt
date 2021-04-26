package com.viatom.lpble.util

import android.app.Application
import android.content.Context
import android.util.SparseArray
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.viatom.lpble.BuildConfig
import com.viatom.lpble.ble.BleSO
import com.viatom.lpble.ble.LpBleUtil
import com.viatom.lpble.constants.Constant
import com.viatom.lpble.ext.createDir

class AppObserver : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun appOnCreate(application: Application){
        application.createDir(Constant.Dir.er1EcgDir)

        LpBleUtil.getServiceHelper()
                .initLog(BuildConfig.DEBUG)
                .initRawFolder(SparseArray<String>().apply {
                    this.put(Constant.BluetoothConfig.SUPPORT_MODEL,  Constant.Dir.er1EcgDir)
                }) // 如需下载主机文件必须配置
                .initModelConfig(SparseArray<Int>().apply {
                    this.put(Constant.BluetoothConfig.SUPPORT_MODEL, Constant.BluetoothConfig.SUPPORT_MODEL)
                }) // 配置要支持的设备
                .initService(
                        application,
                        BleSO.getInstance(application)
                ) //必须在initModelConfig initRawFolder之后调用
    }
}