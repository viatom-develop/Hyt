package com.viatom.lpble.net

import com.localebro.okhttpprofiler.OkHttpProfilerInterceptor
import com.viatom.lpble.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory


/**
 * author: wujuan
 * created on: 2021/3/30 17:40
 * description:
 */
object RetrofitManager{


    val retrofit: Retrofit = OkHttpClient.Builder().let { builder ->
        var baseUrl = "https://open.lepudev.com/"
        if (BuildConfig.DEBUG) {
            // Log信息拦截器
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY) //这里可以选择拦截级别

            builder.addInterceptor(OkHttpProfilerInterceptor())

            //设置 Debug Log 模式
            builder.addInterceptor(loggingInterceptor)

        }
        builder.build().let {
            Retrofit.Builder()
                .client(it)
                .baseUrl(baseUrl)
//                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

    }
    val commonService: CommonService =  retrofit.create(CommonService::class.java)

    val APP_ID = "com.guoyao"
    const val language = "zh-CN" //en-US
    const val secret = "e8a5df03c087fe2330de283e908afe25"
    const val accessToken = "7843e203999d8f80e325476f08c16412"
    const val sn = "GY2308281100A"

    fun header() :Map<String, String> {

        return HashMap<String, String>().apply {
            this["secret"] = secret
            this["access-token"] = accessToken
            this["language"] = language
        }
    }




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


