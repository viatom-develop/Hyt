package com.viatom.lpble.viewmodels

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import android.util.SparseArray
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lepu.blepro.objs.Bluetooth
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog
import com.viatom.lpble.BuildConfig
import com.viatom.lpble.R
import com.viatom.lpble.ble.BleSO
import com.viatom.lpble.ble.LpBleUtil
import com.viatom.lpble.constants.Constant
import com.viatom.lpble.constants.Constant.BluetoothConfig.Companion.SUPPORT_MODEL
import com.viatom.lpble.data.entity.DeviceEntity
import com.viatom.lpble.data.entity.local.DBHelper
import com.viatom.lpble.ext.checkBluetooth
import com.viatom.lpble.ext.createDir
import com.viatom.lpble.ext.createTip
import com.viatom.lpble.ext.permissionNecessary
import com.viatom.lpble.util.doFailure
import com.viatom.lpble.util.doSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * author: wujuan
 * created on: 2021/4/6 14:22
 * description:
 */
class MainViewModel: ViewModel() {


    /**
     * 蓝牙状态
     */
    val _bleEnable = MutableLiveData<Boolean>().apply {
        value = false
    }
    var bleEnable : LiveData<Boolean> = _bleEnable

    /**
     * 当前蓝牙
     */
    val _curBluetooth = MutableLiveData<DeviceEntity?>().apply {
        value = null
    }
    var curBluetooth: LiveData<DeviceEntity?> = _curBluetooth

    /**
     * 连接状态
     */
    val _connectState = MutableLiveData<Int>().apply {
        value = LpBleUtil.State.DISCONNECTED
    }
    var connectState: LiveData<Int> = _connectState




    /**
     * 显示loading
     */
    val _connectLoading = MutableLiveData<Boolean>().apply {
        value = false
    }
    var connectLoading: LiveData<Boolean?> = _connectLoading
    private var connectTip: QMUITipDialog? = null

    fun showConnecting(context: Context ){
//       connectTip?.let {
//           it.show()
//       }?: run {
//           connectTip = context.createTip(R.string.connecting)
//           connectTip!!.show()
//
//       }
    }

    fun hideConnecting(){
//        connectTip?.let { it.hide() }
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
                    _bleEnable.value = true
                    initBle(activity.application)


                }

        }

    }

    fun initBle(application: Application){

        application.createDir(Constant.Dir.er1EcgDir)

        LpBleUtil.getServiceHelper()
            .initLog(BuildConfig.DEBUG)
            .initRawFolder(SparseArray<String>().apply {
                this.put(SUPPORT_MODEL,  Constant.Dir.er1EcgDir)
            })
            .initModelConfig(SparseArray<Int>().apply {
                this.put(SUPPORT_MODEL, SUPPORT_MODEL)
            })
            .initService(
                application,
                BleSO.getInstance(application)
            ) //必须在initModelConfig initRawFolder之后调用
    }


    fun getCurrentDevice(application: Application){
        DBHelper.getInstance(application).let {
            viewModelScope.launch {
                it.getCurrentDeviceDistinctUntilChanged(it.db.deviceDao())
                        .onStart {
                            Log.d("main", "开始查询当前设备")
                        }
                        .catch {
                            Log.d("main", "查询当前设备出错")
                        }
                        .onCompletion {
                            Log.d("main", "查询当前设备结束")

                        }
                        .collectLatest { result ->
                            result.doFailure {
                                Log.d("main", "查询当前设备失败")
                            }
                            result.doSuccess {
                                Log.d("main", "查询当前设备成功${it.toString()}")
                                _curBluetooth.postValue(it)

                            }

                        }
            }

        }
    }
}