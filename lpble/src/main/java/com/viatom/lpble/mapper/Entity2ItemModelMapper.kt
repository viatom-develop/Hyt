package com.viatom.lpble.mapper

import com.viatom.lpble.data.entity.ReportDetail
import com.viatom.lpble.model.ReportItemModel
import java.text.SimpleDateFormat
import java.util.*

class Entity2ItemModelMapper : Mapper<ReportDetail, ReportItemModel> {

    override fun map(input: ReportDetail): ReportItemModel {
                val  simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                return ReportItemModel(createTime = simpleDateFormat.format(input.createTime), type = input.collectType, hr = input.hr, aiDiagnosis = input.aiDiagnosis, recordId = input.recordId)
        }


}