package com.viatom.lpble.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lepu.blepro.ble.cmd.Er1BleResponse
import com.viatom.lpble.ble.BatteryInfo
import com.viatom.lpble.ble.DataController
import com.viatom.lpble.ble.WaveFilter
import com.viatom.lpble.constants.Constant.BluetoothConfig.RunState
import com.viatom.lpble.widget.EcgView
import java.util.*

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


    var waveTimer: Timer? =  null
    var waveTask: TimerTask? = null
    var watchTimer: Timer? = null
    var watchTask: TimerTask? = null
    var period: Long = 41L

    fun startWaveTimer(ecgView: EcgView) {
        
        stopWaveTimer()
        waveTimer = Timer()
        waveTask = object : TimerTask() {
            override fun run() {
                var temp: FloatArray? = DataController.draw(5)
                Log.d("dashboard", "DataController.draw(5) == " + Arrays.toString(temp))
                if (_runState.value !== RunState.RECORDING) {  // 非测试状态,画0
                    temp = if (temp == null || temp.isEmpty()) {
                        FloatArray(0)
                    } else {
                        FloatArray(temp.size)
                    }
                }
                DataController.feed(temp)
                ecgView.invalidate()
            }
        }
        waveTimer?.schedule(
            waveTask,
            5,
            period
        )
    }

    fun stopWaveTimer() {
        waveTask?.cancel()
        waveTask = null

        waveTimer?.cancel()
        waveTimer = null
    }

    fun startWatchTimer(ecgView: EcgView) {
        stopWatchTimer()
        watchTimer = Timer()
        watchTask = object : TimerTask() {
            override fun run() {
                if (period == 0L) {
                    return
                }
                if (DataController.dataRec.size in 101..199) {
                    return
                }
                period =
                    if (DataController.dataRec.size > 150) 39 else 41
                startWaveTimer(ecgView)
            }
        }
        watchTimer?.schedule(
            watchTask,
            1000,
            1000L
        )
    }

    fun stopWatchTimer() {
        watchTask?.cancel()
        watchTask = null
    }

    fun stopTimer() {
        stopWatchTimer()
        stopWaveTimer()
    }
    fun startTimer(ecgView: EcgView) {
        startWatchTimer(ecgView)
        startWaveTimer(ecgView)
    }


}