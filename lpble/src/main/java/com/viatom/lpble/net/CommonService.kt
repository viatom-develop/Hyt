package com.viatom.lpble.net

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.viatom.lpble.data.entity.ReportEntity
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.*

/**
 * author: wujuan
 * created on: 2021/3/30 17:54
 * description:
 */
interface CommonService {

    /**
     * 上传心电文件并分析
     * @param file Part
     * @return Call<RetrofitResponse<Nullable>>
     */
    @Multipart
    @POST("/huawei_ecg_analysis")
    fun ecgAnalysis(@Part file: MultipartBody.Part): Call<RetrofitResponse<ReportEntity?>>



}
