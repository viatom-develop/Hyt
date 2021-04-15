package com.viatom.lpble.net

import android.content.Context
import android.widget.Toast
import com.google.gson.annotations.SerializedName


/**
 * author: wujuan
 * created on: 2021/4/14 16:05
 * description:
 */
data class RetrofitResponse<D>(
    @SerializedName("data") val data: D?,
    @SerializedName("code") val code: Int,
    @SerializedName("msg") val msg: String,
) {
}

//fun RetrofitResponse<*>.isSuccess(context: Context): Boolean{
//    return when (code) {
//        200 -> {
//            true
//        }
//        else -> {
//            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
//            false
//        }
//    }
//
//}

fun RetrofitResponse<*>.isSuccess(): Boolean{
    return when (code) {
        200 -> {
            true
        }
        else -> {

            false
        }
    }

}

