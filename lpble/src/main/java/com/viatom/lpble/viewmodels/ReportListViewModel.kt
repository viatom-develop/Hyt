package com.viatom.lpble.viewmodels

import android.content.Context
import androidx.lifecycle.*
import androidx.paging.*
import com.viatom.lpble.data.local.DBHelper
import com.viatom.lpble.mapper.Entity2ItemModelMapper
import com.viatom.lpble.model.ReportItemModel

/**
 * author: wujuan
 * created on: 2021/4/16 18:44
 * description:
 */
class ReportListViewModel: ViewModel() {


    fun queryData(context: Context, userId: Long,  entity2ItemModelMapper: Entity2ItemModelMapper, pagingConfig: PagingConfig): LiveData<PagingData<ReportItemModel>> =
      DBHelper.getInstance(context).queryRecordAndReportList(userId, entity2ItemModelMapper, pagingConfig ).cachedIn(viewModelScope).asLiveData()




}