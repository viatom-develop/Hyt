package com.viatom.lpble.ui

import com.viatom.lpble.viewmodels.ReportDetailViewModel
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.viatom.lpble.R
import com.viatom.lpble.constants.Constant
import com.viatom.lpble.databinding.FragmentReportDetailBinding
import com.viatom.lpble.ext.getFile
import com.viatom.lpble.viewmodels.MainViewModel
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ReportDetailFragment : Fragment() {
    private val LogTag: String ="ReportDetail"

    private lateinit var binding: FragmentReportDetailBinding

    private val viewModel: ReportDetailViewModel by activityViewModels()

    private val mainViewModel: MainViewModel by activityViewModels()

    private var recordId by Delegates.notNull<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            recordId = it.getLong("recordId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(LogTag, "onCreateView")
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_report_detail, container, false)
        binding.lifecycleOwner = this
        binding.ctx = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.queryRecordAndReport(requireContext(), recordId)
        subscribeUi()
    }
    private fun subscribeUi(){
        viewModel.recordAndReport.observe(viewLifecycleOwner, {

            it?.let {
                mainViewModel.currentUser.value?.userId?.let { it1 ->
                    viewModel.loadPdf(requireContext(), it.reportEntity.pdfName,
                        it1, it.reportEntity.id
                    )
                }

            }
        })

        viewModel.pdf.observe(viewLifecycleOwner, {
            it?.let {
                binding.pdfView.fromFile(it).load()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel._recordAndReport.value = null
        viewModel._pdf.value = null
    }



    fun back(){
        findNavController().popBackStack()
    }



}