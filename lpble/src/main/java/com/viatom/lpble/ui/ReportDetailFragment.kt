package com.viatom.lpble.ui

import com.viatom.lpble.viewmodels.ReportDetailViewModel
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.viatom.lpble.R
import com.viatom.lpble.databinding.FragmentReportDetailBinding
import kotlin.properties.Delegates

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
private const val RECORD_ID = "record_id"

class ReportDetailFragment : Fragment() {
    private lateinit var binding: FragmentReportDetailBinding

    private val viewModel: ReportDetailViewModel by activityViewModels()

    private var recordId by Delegates.notNull<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            recordId = it.getLong(RECORD_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_report_detail, container, false)

        binding.lifecycleOwner = this


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        view.findViewById<Button>(R.id.button_second).setOnClickListener {
//            findNavController().navigate(R.id.action_ReportDetailFragment_to_DashboardFragment)
//        }

//        subscribeUi()
//        initData()


    }
    fun subscribeUi(){
        viewModel.recordAndReport.observe(viewLifecycleOwner, {
            it?.let {

            }
        })
    }

    fun initData(){
        viewModel.queryRecordAndReport(requireContext(), recordId)


    }

    companion object {
        @JvmStatic
        fun newInstance(recordId: Long) =
            ReportDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(RECORD_ID, recordId)

                }
            }
    }


}