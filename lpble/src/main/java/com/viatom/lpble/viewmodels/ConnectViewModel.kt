package com.viatom.lpble.viewmodels

import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * author: wujuan
 * created on: 2021/4/6 14:17
 * description:
 */
class ConnectViewModel : ViewModel() {

    /**
     * 是否扫描中
     */
    val _scanning = MutableLiveData<Boolean>().apply {
        value = false
    }
    var scanning : LiveData<Boolean> = _scanning



}
