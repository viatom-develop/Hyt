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
import com.viatom.lpble.BuildConfig
import com.viatom.lpble.R
import com.viatom.lpble.ble.BleSO
import com.viatom.lpble.ble.LpBleUtil
import com.viatom.lpble.ble.WaveFilter
import com.viatom.lpble.constants.Constant
import com.viatom.lpble.constants.Constant.BluetoothConfig.Companion.SUPPORT_MODEL
import com.viatom.lpble.data.entity.DeviceEntity
import com.viatom.lpble.data.entity.local.DBHelper
import com.viatom.lpble.ext.createDir
import com.viatom.lpble.util.doFailure
import com.viatom.lpble.util.doSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
     * 连接过程中的蓝牙对象
     */
    val _toConnectDevice = MutableLiveData<Bluetooth?>().apply {
        value = null
    }
    var toConnectDevice: LiveData<Bluetooth?> = _toConnectDevice


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
                it.getCurrentDevice(it.db.deviceDao())
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
                                Log.d("main", "查询当前设备成功${it}")
                                _curBluetooth.postValue(it)
                            }

                        }
            }

        }
    }

    fun saveDevice(application: Application, deviceEntity: DeviceEntity){
        DBHelper.getInstance(application).let {
            viewModelScope.launch(Dispatchers.IO) {
                Log.e("main", "saveDevice")
                it.insertOrUpdateDevice(it.db.deviceDao(), deviceEntity)
            }

        }
    }

    //重置dashboard
    fun resetDashboard(){

        Constant.BluetoothConfig.currentRunState = Constant.BluetoothConfig.RunState.NONE
        WaveFilter.resetFilter()


    }
}