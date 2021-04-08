package com.viatom.lpble.ui

import com.viatom.lpble.viewmodels.MainViewModel
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import com.lepu.blepro.ble.data.LepuDevice
import com.lepu.blepro.event.EventMsgConst
import com.lepu.blepro.event.InterfaceEvent
import com.lepu.blepro.observer.BIOL
import com.lepu.blepro.observer.BleChangeObserver
import com.viatom.lpble.BuildConfig
import com.viatom.lpble.R
import com.viatom.lpble.ble.BleSO
import com.viatom.lpble.ble.LpBleUtil
import com.viatom.lpble.ble.LpBleUtil.State
import com.viatom.lpble.constants.Constant
import com.viatom.lpble.constants.Constant.BluetoothConfig.Companion.SUPPORT_MODEL
import com.viatom.lpble.constants.Constant.BluetoothConfig.Companion.CHECK_BLE_REQUEST_CODE
import com.viatom.lpble.constants.Constant.BluetoothConfig.Companion.isLpBleEnable
import com.viatom.lpble.constants.Constant.Dir
import com.viatom.lpble.ext.checkBluetooth
import com.viatom.lpble.ext.createDir
import com.viatom.lpble.ext.permissionNecessary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity(), BleChangeObserver {

    val TAG: String = "MainActivity"
    private val mainVM: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        subscribeUi()
        initLiveEvent()

        mainVM.permission(this)



    }

    private fun initLiveEvent() {
        //同步时间
        LiveEventBus.get(InterfaceEvent.ER1.EventEr1SetTime)
                .observe(this, Observer {
                    LpBleUtil.getInfo(SUPPORT_MODEL)

                })


    }

    fun subscribeUi(){

        mainVM.bleEnable.observe(this, {
            if (it){
               if (isLpBleEnable) LpBleUtil.reInitBle() else  mainVM.initBle(application)
            }
        })

        // 当BleService onServiceConnected执行后发出通知
        LiveEventBus.get(EventMsgConst.Ble.EventServiceConnectedAndInterfaceInit).observe(
                this, {
            isLpBleEnable = true
            lifecycle.addObserver(BIOL(this, intArrayOf(SUPPORT_MODEL)))

            mainVM.getCurrentDevice(application)
        }
        )

        mainVM.connectLoading.observe(this, {

            if (it == true) mainVM.showConnecting(this) else mainVM.hideConnecting()
        })

        // 查询最新保存的设备， 去重连
        mainVM.curBluetooth.observe(this, { device ->
            device?.deviceName?.let {
                Log.d("main", "to reconnect...")
                LpBleUtil.reconnect(SUPPORT_MODEL, it)
            }

        })
    }

//    fun permission() = runBlocking<Unit>{
//        // 启动并发的协程以验证主线程并未阻塞
//        launch {
//            for (k in 1..3) {
//                println("I'm not blocked $k")
//                delay(100)
//            }
//        }
//
//        this@MainActivity.permissionNecessary().collect { per ->
//            Log.e("collect", per.toString())
//            //权限OK, 检查蓝牙状态
//            if (per)
//                this@MainActivity.checkBluetooth(CHECK_BLE_REQUEST_CODE).let {
//                    Log.e(TAG, "蓝牙状态 $it")
//                    mainVM._bleEnable.value = true
//
//                    if (it) initBle(this@MainActivity)
//                }
//
//        }
//
//    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHECK_BLE_REQUEST_CODE) {
            //重启蓝牙权限后
            LpBleUtil.reInitBle()
            mainVM._bleEnable.value = true
        }
    }

    override fun onBleStateChanged(model: Int, state: Int) {

        mainVM._connectState.value = state

        when(state){
            State.DISCONNECTED -> {

                //如果断开 并且是需要重连时。（手动断开时会自动连接标志置为false）
                if (LpBleUtil.isAutoConnect(SUPPORT_MODEL)){
                    Log.d("main", "去重连....")
                    mainVM.curBluetooth.value?.deviceName?.let { LpBleUtil.reconnect(SUPPORT_MODEL, it) }
                }
            }
            State.CONNECTED ->{
                //去开启实时任务
                if (LpBleUtil.isRtStop(SUPPORT_MODEL)) LpBleUtil.startRtTask(SUPPORT_MODEL, 200)
            }
        }
    }
}