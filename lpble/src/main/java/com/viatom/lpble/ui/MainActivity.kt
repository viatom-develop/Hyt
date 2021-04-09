package com.viatom.lpble.ui

import com.viatom.lpble.viewmodels.MainViewModel
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import com.lepu.blepro.ble.data.LepuDevice
import com.lepu.blepro.event.EventMsgConst
import com.lepu.blepro.event.InterfaceEvent
import com.lepu.blepro.objs.Bluetooth
import com.lepu.blepro.observer.BIOL
import com.lepu.blepro.observer.BleChangeObserver
import com.viatom.lpble.BuildConfig
import com.viatom.lpble.R
import com.viatom.lpble.ble.BleSO
import com.viatom.lpble.ble.LpBleUtil
import com.viatom.lpble.ble.LpBleUtil.State
import com.viatom.lpble.ble.WaveFilter
import com.viatom.lpble.constants.Constant
import com.viatom.lpble.constants.Constant.BluetoothConfig.Companion.SUPPORT_MODEL
import com.viatom.lpble.constants.Constant.BluetoothConfig.Companion.CHECK_BLE_REQUEST_CODE
import com.viatom.lpble.constants.Constant.BluetoothConfig.Companion.isLpBleEnable
import com.viatom.lpble.constants.Constant.Dir
import com.viatom.lpble.data.entity.DeviceEntity
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

    private lateinit var dialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        subscribeUi()
        initLiveEvent()
        permission(this)





    }

    /**
     * 处理由SDK发送的通知
     */
    private fun initLiveEvent() {
        //同步时间
        LiveEventBus.get(InterfaceEvent.ER1.EventEr1SetTime)
                .observe(this, Observer {
                    LpBleUtil.getInfo(SUPPORT_MODEL)

                })

        // 设备信息通知
        LiveEventBus.get(InterfaceEvent.ER1.EventEr1Info)
                .observe(this, { event ->

                    event as InterfaceEvent
                    Log.d("EventEr1Info","currentDevice init")
                   mainVM.toConnectDevice?.let {
                        // 来自connectDialog 触发的通知

                       //根目录下创建设备名文件夹
                       mainVM.toConnectDevice.value?.device?.let { b ->
                           b.name?.let {
                               createDir(it)
                           }

                           //保存设备
                           mainVM.saveDevice(application, DeviceEntity.convert2DeviceEntity(b, event.data as LepuDevice))

                           //ui
                           mainVM._toConnectDevice.value = null

                       }
                   }

                })

    }

    private fun subscribeUi(){

         //手机ble状态
        mainVM.bleEnable.observe(this, {
            if (it){
               if (isLpBleEnable) LpBleUtil.reInitBle() else  mainVM.initBle(application)
            }
        })

        // 当BleService onServiceConnected执行后发出通知 蓝牙sdk 初始化完成
        LiveEventBus.get(EventMsgConst.Ble.EventServiceConnectedAndInterfaceInit).observe(
                this, {
            isLpBleEnable = true
            lifecycle.addObserver(BIOL(this, intArrayOf(SUPPORT_MODEL))) // ble service 初始完成后添加订阅才有效

            // 读取本地最近保存的设备
            mainVM.getCurrentDevice(application)
            }
        )



        // 查询到当前设备后去重连
        mainVM.curBluetooth.observe(this, { device ->
            device?.deviceName?.let {
                Log.d("main", "to reconnect...")
                LpBleUtil.reconnect(SUPPORT_MODEL, it)
            }

        })

        mainVM.toConnectDevice.observe(this, {
            it?.let {
                //表示正在连接
                showConnecting(it)
            }?: hideConnecting()

        })
    }
    fun permission(activity: FragmentActivity) = runBlocking<Unit>{
        // 启动并发的协程以验证主线程并未阻塞
        launch {
            for (k in 1..3) {
                println("I'm not blocked $k")
                delay(100)
            }
        }

        activity.permissionNecessary().collect { per ->
            Log.e("collect", per.toString())
            //权限OK, 检查蓝牙状态
            if (per)
                activity.checkBluetooth(Constant.BluetoothConfig.CHECK_BLE_REQUEST_CODE).let {
                    Log.e("main", "蓝牙状态 $it")
                    mainVM._bleEnable.value = true
                    mainVM.initBle(activity.application)


                }

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHECK_BLE_REQUEST_CODE) {
            //重启蓝牙权限后
            LpBleUtil.reInitBle()
            mainVM._bleEnable.value = true
        }
    }

    /**
     * lpble sdk 蓝牙状态更新
     * @param model Int
     * @param state Int
     */
    override fun onBleStateChanged(model: Int, state: Int) {

        mainVM._connectState.value = state

        when(state){
            State.DISCONNECTED -> {
                LpBleUtil.stopRtTask(SUPPORT_MODEL)
                mainVM.resetDashboard()

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

    private fun showConnecting(b : Bluetooth){
        if (this::dialog.isInitialized){
            dialog.setMessage("正在连接 ${b.name}...")
            dialog.setCancelable(false)
            dialog.show()
        }else{
            dialog = ProgressDialog(this)
        }
    }

    private fun hideConnecting(){
        if (this::dialog.isInitialized)dialog.dismiss()
    }
}