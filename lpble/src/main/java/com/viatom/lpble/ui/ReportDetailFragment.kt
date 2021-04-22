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
    private val LogTag: String ="ReportDetailFragment"

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

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_report_detail, container, false)
        binding.lifecycleOwner = this
        binding.ctx = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeUi()
        initData()
    }
    fun subscribeUi(){
        viewModel.recordAndReport.observe(viewLifecycleOwner, {

            Log.d(LogTag, "recordAndReport: $it")

            it?.let {
                //先从本地加载
                requireContext().getFile("${Constant.Dir.er1PdfDir}/${it.reportEntity.pdfName}").let { local ->
                    if (local != null && local.exists()){

                        Log.d(LogTag, "本地已存在文件：${it.reportEntity.pdfName}---- ${local.absolutePath}")
                        binding.pdfView.fromFile(local).load()
                    }else {
                        //生成pdf
                        mainViewModel.currentUser.value?.let { user ->
                            viewModel.inflateReportFile(requireContext(), user )?.let { file ->
                                Log.d(LogTag, "生成pdf成功: ${file.name}, ${file.absolutePath}")
                                binding.pdfView.fromFile(file).load()
                                //更新db
                                viewModel.updatePdf(requireContext(), it.reportEntity.id,  file.name)

                            }?: run{
                                Log.d(LogTag, "生成的pdf失败")
                            }
                        }
                    }
                }


            }
        })
    }

    fun initData(){
        viewModel.queryRecordAndReport(requireContext(), recordId)
    }

    fun back(){
        findNavController().popBackStack()
    }



}