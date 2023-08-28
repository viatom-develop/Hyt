package com.viatom.lpble.retrofit.request

import com.google.gson.Gson
import com.viatom.lpble.data.entity.UserEntity
import com.viatom.lpble.net.RetrofitManager.APP_ID
import com.viatom.lpble.net.RetrofitManager.accessToken

data class EcgInfo (
    var user: UserEntity,
    var device: Device,
    var ecg: Ecg,
    val analysis_type: String = "1",
    val service_ability: String = "1",
    val access_token: String = accessToken,
    val application_id: String = APP_ID
){
    override fun toString(): String {
        return Gson().toJson(this)
    }
}

data class Device(
    val sn: String,
    val model: String = "er1",
    val band: String = "Lepu")

data class Ecg(
    val measure_time: String,
    val duration: String,
    val sample_rate: String = "125",
    val lead: String = "II")