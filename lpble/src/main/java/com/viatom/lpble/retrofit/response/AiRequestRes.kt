package com.viatom.lpble.retrofit.response

import com.google.gson.Gson

class AiRequestRes {
    val analysis_id : String = ""

    override fun toString(): String {
        return Gson().toJson(this)
    }
}