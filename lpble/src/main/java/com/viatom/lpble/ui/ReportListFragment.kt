package com.viatom.lpble.ui

import com.viatom.lpble.viewmodels.ReportListViewModel
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import com.viatom.lpble.R
import com.viatom.lpble.adapter.ReportAdapter
import com.viatom.lpble.adapter.ReportViewHolder
import com.viatom.lpble.databinding.FragmentReportListBinding
import com.viatom.lpble.mapper.Entity2ItemModelMapper
import com.viatom.lpble.viewmodels.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * author: wujuan
 * created on: 2021/4/16 17:54
 * description:
 */
class ReportListFragment : Fragment() {
    val re_log: String ="ReportListFragment"

    private lateinit var binding: FragmentReportListBinding
    private val viewModel: ReportListViewModel by activityViewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    private val adapter by lazy { ReportAdapter(this) }
    private val pagingConfig = PagingConfig(
        // 每页显示的数据的大小
        pageSize = 10,

        // 开启占位符
        enablePlaceholders = true,

        // 预刷新的距离，距离最后一个 item 多远时加载数据
        // 默认为 pageSize
        prefetchDistance = 4,

        /**
         * 初始化加载数量，默认为 pageSize * 3
         *
         * internal const val DEFAULT_INITIAL_PAGE_MULTIPLIER = 3
         * val initialLoadSize: Int = pageSize * DEFAULT_INITIAL_PAGE_MULTIPLIER
         */

        /**
         * 初始化加载数量，默认为 pageSize * 3
         *
         * internal const val DEFAULT_INITIAL_PAGE_MULTIPLIER = 3
         * val initialLoadSize: Int = pageSize * DEFAULT_INITIAL_PAGE_MULTIPLIER
         */
        initialLoadSize = 10
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_report_list, container, false)
        binding.lifecycleOwner = this
        binding.ctx = this
        Log.d(re_log, "onCreateView")

        return binding.root
    }
    

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        LinearLayoutManager(context).apply {
            this.orientation = LinearLayoutManager.VERTICAL
            binding.rcv.layoutManager = this

        }

        binding.rcv.adapter = adapter


        mainViewModel._currentUser.value?.userId?.let { userId ->
            viewModel.queryData(requireContext(), userId, Entity2ItemModelMapper(), pagingConfig).observe(viewLifecycleOwner, {

                adapter.submitData(lifecycle, it)
                adapter.notifyDataSetChanged()
            })
        }

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collectLatest { state ->
                binding.swiperRefresh.isRefreshing = state.refresh is LoadState.Loading

                if (state.append is LoadState.NotLoading){
                    binding.size.text = "当前共${adapter.itemCount}例"
                }
            }
        }
        binding.swiperRefresh.setOnRefreshListener {
            adapter.refresh()
        }



    }
    fun back(){
        findNavController().navigate(R.id.report_list_to_dashboard)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(re_log, "onDestroyView")
    }

}