package com.viatom.lpble.net

import com.viatom.lpble.data.entity.ReportEntity
import com.viatom.lpble.retrofit.response.AiRequestRes
import com.viatom.lpble.retrofit.response.AiResult
import com.viatom.lpble.retrofit.response.BaseResponse
import io.reactivex.Observable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
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


    // 2.1.	数据导入
    @POST("/api/v1/ecg/analysis/request")
    @Multipart
    fun requestAi(
        @HeaderMap header: Map<String, String>,
        @Part analyse_file: MultipartBody.Part,
        @Part("ecg_info") ecg_info: RequestBody
    ) : Call<BaseResponse<AiRequestRes?>>


    @POST("/api/v1/ecg/analysis/result/query")
    fun requestAiResult(
        @HeaderMap header: Map<String, String>,
        @Body res: AiRequestRes
    ) : Call<BaseResponse<AiResult?>>

}
