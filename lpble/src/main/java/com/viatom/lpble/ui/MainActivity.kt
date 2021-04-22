package com.viatom.lpble.ui

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import com.lepu.blepro.ble.data.LepuDevice
import com.lepu.blepro.event.EventMsgConst
import com.lepu.blepro.event.InterfaceEvent
import com.lepu.blepro.objs.Bluetooth
import com.lepu.blepro.observer.BIOL
import com.lepu.blepro.observer.BleChangeObserver
import com.viatom.lpble.R
import com.viatom.lpble.ble.CollectUtil
import com.viatom.lpble.ble.LpBleUtil
import com.viatom.lpble.ble.LpBleUtil.State
import com.viatom.lpble.constants.Constant
import com.viatom.lpble.constants.Constant.BluetoothConfig.Companion.CHECK_BLE_REQUEST_CODE
import com.viatom.lpble.constants.Constant.BluetoothConfig.Companion.SUPPORT_MODEL
import com.viatom.lpble.data.entity.DeviceEntity
import com.viatom.lpble.data.entity.UserEntity
import com.viatom.lpble.ext.checkBluetooth
import com.viatom.lpble.ext.createDir
import com.viatom.lpble.ext.permissionNecessary
import com.viatom.lpble.viewmodels.MainViewModel

/**
 * MainActivity初始化流程
 * 1.检查权限
 * 2.检查蓝牙可用状态
 * 3.初始化蓝牙服务
 * 4.订阅蓝牙interface， 监测连接状态
 * 5.初始化自动采集服务，完成之后开始运行自动采集
 * 6.读取本地当前设备， 如果存在则去重连
 *
 *
 * @property TAG String
 * @property mainVM MainViewModel
 * @property dialog ProgressDialog
 */
class MainActivity : AppCompatActivity(), BleChangeObserver {

    private val TAG: String = "MainActivity"
    private val mainVM: MainViewModel by viewModels()

    private lateinit var dialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 跳转传入UserEntity， userId 应为医汇通平台的id
        intent.getParcelableExtra<UserEntity>("userEntity")?.let {
            // 将用户更新到db 及viewmodel
            mainVM.saveUser(application, it)
        }?: run{
            //测试
            mainVM.saveUser(
                application, UserEntity(
                    userId = 1002,
                    name = "李四",
                    height = "176",
                    weight = "70",
                    birthday = "1994-05-6",
                    gender = "男"
                )
            )

        }


        subscribeUi()
        initLiveEvent()
        permissionNecessary()


    }

    /**
     * 先检查蓝牙权限及状态 再初始化蓝牙服务， 之后再初始化自动采集服务
     */
    private fun initLiveEvent() {

        LiveEventBus.get(Constant.Event.permissionNecessary).observe(this, { p ->
            //权限OK, 检查蓝牙状态
            if (p == true)
                checkBluetooth(CHECK_BLE_REQUEST_CODE).let {
                    Log.e("main", "蓝牙状态 $it")
                    mainVM._bleEnable.value = true
                    mainVM.initBle(application)


                }
        })

        // 当BleService onServiceConnected执行后发出通知 蓝牙sdk 初始化完成
        LiveEventBus.get(EventMsgConst.Ble.EventServiceConnectedAndInterfaceInit).observe(
            this, {
                mainVM._lpBleEnable.value = true
                lifecycle.addObserver(
                    BIOL(
                        this,
                        intArrayOf(SUPPORT_MODEL)
                    )
                ) // ble service 初始完成后添加订阅才有效

                //初始化自动采集服务
                CollectUtil.getInstance(application).initService()

                // 读取本地最近保存的设备
                mainVM.getCurrentDevice(application)


            }
        )

        //同步时间
        LiveEventBus.get(InterfaceEvent.ER1.EventEr1SetTime)
            .observe(this, Observer {
                LpBleUtil.getInfo(SUPPORT_MODEL)

            })

        // 设备信息通知
        LiveEventBus.get(InterfaceEvent.ER1.EventEr1Info)
            .observe(this, { event ->

                event as InterfaceEvent
                Log.d("main", "currentDevice init")
                mainVM.toConnectDevice.let {
                    // 来自connectDialog 触发的通知

                    //根目录下创建设备名文件夹
                    mainVM.toConnectDevice.value?.device?.let { b ->
                        b.name?.let {
                            createDir(it)
                        }

                        //保存设备
                        mainVM.saveDevice(
                            application,
                            DeviceEntity.convert2DeviceEntity(b, event.data as LepuDevice)
                        )

                        //ui
                        mainVM._toConnectDevice.value = null

                    }
                }

            })

        LiveEventBus.get(Constant.Event.collectServiceConnected).observe(this, {
            // 采集服务已经初始化成功, 去运行自动采集
            mainVM.runAutoCollect(application)
        })


    }

    private fun subscribeUi() {

        //手机ble状态
        mainVM.bleEnable.observe(this, {
            if (it) {
                if (mainVM.lpBleEnable.value == true) LpBleUtil.reInitBle() else mainVM.initBle(
                    application
                )
            }
        })



        // 查询到当前设备后去重连
        mainVM.curBluetooth.observe(this, { device ->
            Log.e("curBluetooth", "to reconnect...")
            device?.deviceName?.let {

                LpBleUtil.reconnect(SUPPORT_MODEL, it)
            }

        })

        mainVM.toConnectDevice.observe(this, {
            it?.let {
                //表示正在连接
                showConnecting(it)
            } ?: hideConnecting()

        })
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

        when (state) {
            State.DISCONNECTED -> {
                LpBleUtil.stopRtTask(SUPPORT_MODEL)
                mainVM.resetDashboard()

                //app 运行时如果断开 去重连。
                if (LpBleUtil.isAutoConnect(SUPPORT_MODEL)) { //默认自动重连开启
                    mainVM.curBluetooth.value?.deviceName?.let {
                        LpBleUtil.reconnect(
                            SUPPORT_MODEL,
                            it
                        )
                    }
                }

            }
            State.CONNECTED -> {
                //去开启实时任务
                if (LpBleUtil.isRtStop(SUPPORT_MODEL)) LpBleUtil.startRtTask(SUPPORT_MODEL)
            }
        }
    }

    private fun showConnecting(b: Bluetooth) {
        if (!this::dialog.isInitialized)
            dialog = ProgressDialog(this)
        dialog.setMessage("正在连接 ${b.name}...")
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun hideConnecting() {
        if (this::dialog.isInitialized) dialog.dismiss()
    }


}