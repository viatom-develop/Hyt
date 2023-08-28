package com.viatom.lpble.retrofit.response

import com.google.gson.Gson

class BaseResponse<T> {
    /**
     * {
     * "code": 0,
     * "data": "string",
     * "message": "string",
     * "reason": "string"
     * }
     */

    var code: Int = 0
    var message: String = ""
    var reason: String = ""
    var data: T? = null

    override fun toString(): String {
        return Gson().toJson(this)
    }
}

data class Message(
    val loc: List<String>,
    val msg: String,
    val type: String
)

fun BaseResponse<*>.isSuccess(): Boolean {
    return code == 0
}