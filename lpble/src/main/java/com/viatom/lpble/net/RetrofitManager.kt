package com.viatom.lpble.net

import com.viatom.lpble.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*


/**
 * author: wujuan
 * created on: 2021/3/30 17:40
 * description:
 */
object RetrofitManager{


    val retrofit: Retrofit = OkHttpClient.Builder().let { builder ->
        var baseUrl: String
        if (BuildConfig.DEBUG) {
            baseUrl = "https://ai.viatomtech.com.cn/"
            // Log信息拦截器
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY) //这里可以选择拦截级别

            //设置 Debug Log 模式
            builder.addInterceptor(loggingInterceptor)

        }else{
            baseUrl = "https://lepucare.viatomtech.com.cn/"
        }
        builder.build().let {
            Retrofit.Builder()
                .client(it)
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

    }
    val commonService: CommonService =  retrofit.create(CommonService::class.java)







}

fun String.isSuccess(): Boolean{
    JSONObject(toString()).let {
        return it.getInt("code") == 200
    }
}
fun String.msg(): String{
    JSONObject(toString()).let {
        return it.getString("msg")
    }
}
fun ResponseBody.data(): String{
    JSONObject(toString()).let {
        return it.getString("data")
    }
}


