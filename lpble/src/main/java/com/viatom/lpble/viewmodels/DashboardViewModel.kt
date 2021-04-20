package com.viatom.lpble.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lepu.blepro.ble.cmd.Er1BleResponse
import com.viatom.lpble.ble.*
import com.viatom.lpble.constants.Constant.RunState

/**
 * author: wujuan
 * created on: 2021/4/7 11:17
 * description:
 */
class DashboardViewModel : ViewModel() {

    val _runState = MutableLiveData<Int>().apply {
        value = RunState.NONE
    }
    var runState : LiveData<Int> = _runState


    val _battery = MutableLiveData<BatteryInfo>().apply {
        value = null
    }
    var battery : LiveData<BatteryInfo> = _battery

    val _overTime = MutableLiveData<Boolean>().apply {
        value = false
    }
    var overTime : LiveData<Boolean> = _overTime

    val _hr = MutableLiveData<Int>().apply {
        value = 0
    }
    var hr : LiveData<Int> = _hr

    val _isSignalPoor = MutableLiveData<Boolean>().apply {
        value = false
    }
    var isSignalPoor : LiveData<Boolean> = _isSignalPoor




//
//    /**
//     *  是否自动采集中
//     */
//    val _autoCollecting = MutableLiveData<Boolean>().apply {
//        value = false
//    }
//    var autoCollecting : LiveData<Boolean> = _autoCollecting



    val _collectBtnText = MutableLiveData<String>().apply {
        value = "采集"
    }
    var collectBtnText : LiveData<String> = _collectBtnText



    /**
     *  实时任务时导联是否正常
     */
    val _fingerState = MutableLiveData<Boolean>().apply {
        value = false
    }
    var fingerState : LiveData<Boolean> = _fingerState

    /**
     * 实时数据池添加数据
     * @param data RtData
     */
    fun feedWaveData(data: Er1BleResponse.RtData){
        data.wave.wFs?.let {
            Log.d("dashboard", "去添加实时数据")

            for (i in it.indices) {
                val d: DoubleArray = WaveFilter.filter(it[i].toDouble(), false)
                if (d.isNotEmpty()) {
                    val floatArray = FloatArray(d.size).apply {
                        for (j in d.indices) {
                            this[j] = d[j].toFloat()
                        }
                    }

                    DataController.receive(floatArray)
                }
            }
        }
    }




    }

