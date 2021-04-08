package com.viatom.lpble.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lepu.blepro.objs.Bluetooth
import com.viatom.lpble.data.entity.DeviceEntity
import com.viatom.lpble.data.entity.local.DBHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * author: wujuan
 * created on: 2021/4/6 14:17
 * description:
 */
class ConnectViewModel : ViewModel() {

    /**
     * 是否扫描中
     */
    val _isRefreshing = MutableLiveData<Boolean>().apply {
        value = false
    }
    var isRefreshing : LiveData<Boolean> = _isRefreshing

    /**
     * 当前蓝牙
     */
    val _toConnectDevice = MutableLiveData<Bluetooth?>().apply {
        value = null
    }
    var toConnectDevice: LiveData<Bluetooth?> = _toConnectDevice

    fun saveDevice(application: Application, deviceEntity: DeviceEntity){
        DBHelper.getInstance(application).let {
            viewModelScope.launch(Dispatchers.IO) {
                it.insertOrUpdateDevice(it.db.deviceDao(), deviceEntity)
            }

        }
    }


}