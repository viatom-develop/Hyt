package com.viatom.lpble.viewmodels

import android.content.Context
import androidx.lifecycle.*
import androidx.paging.*
import com.viatom.lpble.data.entity.RecordAndReport
import com.viatom.lpble.data.entity.ReportDetail
import com.viatom.lpble.data.entity.local.DBHelper
import com.viatom.lpble.data.entity.local.RecordDao
import com.viatom.lpble.mapper.Entity2ItemModelMapper
import com.viatom.lpble.model.ReportItemModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * author: wujuan
 * created on: 2021/4/16 18:44
 * description:
 */
class ReportListViewModel: ViewModel() {


//    fun queryData(context: Context, entity2ItemModelMapper: Entity2ItemModelMapper, pagingConfig: PagingConfig){
//    }

    fun queryData(context: Context, entity2ItemModelMapper: Entity2ItemModelMapper, pagingConfig: PagingConfig): LiveData<PagingData<ReportItemModel>> =
      DBHelper.getInstance(context).queryRecordAndReportList(entity2ItemModelMapper, pagingConfig ).cachedIn(viewModelScope).asLiveData()




}