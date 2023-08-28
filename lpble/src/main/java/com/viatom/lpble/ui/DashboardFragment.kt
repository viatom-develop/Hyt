package com.viatom.lpble.ui

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.jeremyliao.liveeventbus.LiveEventBus
import com.lepu.blepro.ble.cmd.Er1BleResponse
import com.lepu.blepro.event.EventMsgConst
import com.lepu.blepro.event.InterfaceEvent
import com.viatom.lpble.R
import com.viatom.lpble.ble.BatteryInfo
import com.viatom.lpble.ble.CollectUtil
import com.viatom.lpble.ble.DataController
import com.viatom.lpble.ble.LpBleUtil
import com.viatom.lpble.constants.Constant
import com.viatom.lpble.constants.Constant.RunState
import com.viatom.lpble.constants.Constant.BluetoothConfig
import com.viatom.lpble.databinding.FragmentDashboradBinding
import com.viatom.lpble.viewmodels.DashboardViewModel
import com.viatom.lpble.viewmodels.MainViewModel
import com.viatom.lpble.widget.EcgBkg
import com.viatom.lpble.widget.EcgView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.experimental.and
import kotlin.math.floor

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class DashboardFragment : Fragment() {

    val DASH: String = "dashFragment"


    private lateinit var binding: FragmentDashboradBinding

    private val mainVM: MainViewModel by activityViewModels()

    private val viewModel: DashboardViewModel by activityViewModels()

    lateinit var leadOffDialog: AlertDialog

    lateinit var ecgView: EcgView

    lateinit var collectUtil: CollectUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.setFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        collectUtil = CollectUtil.getInstance(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(DASH, "onDestroyView")
        mainVM.resetDashboard()
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        Log.d(DASH, "onCreateView")
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_dashborad, container, false)
        binding.lifecycleOwner = this
        binding.ctx = this


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        subscribeUi()
        initLiveEvent()
        doWaveTask()
        doWatchTask()

    }


    private fun initView() {
        calScreen()
        EcgBkg(requireContext()).apply {
            binding.waveBg.measure(0, 0)
            binding.waveBg.addView(this)
        }

        ecgView = EcgView(requireContext()).apply {
            binding.wave.measure(0, 0)
            binding.wave.addView(this)

        }

        leadOffDialog = AlertDialog.Builder(context)
                .setTitle(R.string.public_lead_off_tip)
                .setMessage(R.string.public_lead_off_msg)
                .create()


    }

    private fun calScreen() {
        val dm = resources.displayMetrics
        val index = floor(dm.widthPixels / dm.xdpi * 25.4 / 25 * 125).toInt()
        DataController.maxIndex = index * Constant.EcgViewConfig.ECG_CELL_SIZE
        // 假设 x\y dpi 相同
        val mm2px = 25.4.toFloat() / dm.xdpi
        DataController.mm2px = mm2px
    }


    private fun subscribeUi() {
        mainVM.connectState.observe(viewLifecycleOwner, {
            //断开连接清空ecg
            ecgView.clear()
            ecgView.invalidate()
        })

        //实时状态切换 更新UI
        viewModel.runState.observe(viewLifecycleOwner, {
            binding.battery.visibility = if (it in RunState.NONE..RunState.OFFLINE) View.INVISIBLE else View.VISIBLE
            binding.hr.visibility = if (it in RunState.NONE..RunState.PREPARING_TEST || it in RunState.SAVE_FAILED..RunState.LEAD_OFF) View.INVISIBLE else View.VISIBLE
            binding.bpmText.visibility = if (it in RunState.NONE..RunState.PREPARING_TEST || it in RunState.SAVE_FAILED..RunState.LEAD_OFF) View.INVISIBLE else View.VISIBLE
            binding.bpmImg.visibility = if (it in RunState.NONE..RunState.PREPARING_TEST || it in RunState.SAVE_FAILED..RunState.LEAD_OFF) View.INVISIBLE else View.VISIBLE


        })

        //电池UI
        viewModel.battery.observe(viewLifecycleOwner, {

            it?.let {
                binding.battery.run {
                    this.setState(it.state)
                    this.power = it.percent
                }

            }
        })
        // 是否超过最大测量时长
        viewModel.overTime.observe(viewLifecycleOwner, {

            Log.d("dashboard overtime", "$it")
            if (it)
                MaterialDialog.Builder(requireContext())
                        .content(R.string.measure_overtime)
                        .positiveText(R.string.i_know)
                        .show()
        })
        //hr
        viewModel.hr.observe(viewLifecycleOwner, { h ->
            (h < 30 || h > 250).let {
                binding.hr.text = if (it) "--" else h.toString()
            }

        })


        viewModel.collectBtnText.observe(viewLifecycleOwner, {
            //更新采集按钮UI
            binding.collection.run {
                text = it
                background = if (it == getString(R.string.collection)) resources.getDrawable(R.drawable.public_shape_white_corner_28) else resources.getDrawable(R.drawable.public_shape_black_corner_28)
                setTextColor(if (it == getString(R.string.collection)) resources.getColor(R.color.color_363636) else resources.getColor(R.color.white))
            }


        })


    }


    private fun initLiveEvent() {
        LiveEventBus.get(InterfaceEvent.ER1.EventEr1RtData).observe(this, { event ->

            Log.d(DASH, "get(InterfaceEvent.ER1.EventEr1RtData")

            (event as InterfaceEvent).let {
                (it.data as Er1BleResponse.RtData)?.let { data ->

                    data.param.let { param ->

                        //运行状态
                        param.getRunState().run {
                            viewModel._runState.value = this

                            //判断状态是否切换了

                            BluetoothConfig.currentRunState.let { lastState ->

                                Log.d(DASH, "currentRunState = $this,lastState = $lastState")
                                if (this != lastState) {
                                    Log.d(DASH, "currentRunState = $this,lastState = $lastState--------------切换")

                                    ecgView.clear()
                                    ecgView.invalidate()

                                    viewModel._fingerState.value = this in RunState.PREPARING_TEST..RunState.RECORDING
                                }
                            }

                            //更新记录为最新的状态
                            BluetoothConfig.currentRunState = this

//                            if((this !in RunState.PREPARING_TEST..RunState.RECORDING) && (watchTimer != null || waveTimer != null)){
//                                ecgView.clear()
//                                ecgView.invalidate()
//                                stopTimer()
//                            }

                            Log.d(DASH, " runState $this")
                        }


                        //电池
                        BatteryInfo(param.batteryState(), param.battery).run {
                            viewModel._battery.value = this
                            Log.d(DASH, "battery $this")
                        }

                        //最大测量时长
                        viewModel._overTime.value = param.recordTime >= TimeUnit.DAYS.toSeconds(1)

                        //hr
                        viewModel._hr.value = param.hr
                        Log.d(DASH, "hr  ${param.hr}")

                        //心电信号
                        viewModel._isSignalPoor.value = param.isSignalPoor()

                        //wave data
                        viewModel.feedWaveData(data, collectUtil)

                    }

                }

            }

        })

        // 当停止实时任务
        LiveEventBus.get(EventMsgConst.RealTime.EventRealTimeStop).observe(viewLifecycleOwner, {

        })

        //保存及分析流程成功
        LiveEventBus.get(Constant.Event.analysisProcessSuccess).observe(viewLifecycleOwner, {
            it?.let {
                if ((it as String).isNotEmpty())
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }


            viewModel._collectBtnText.value = getString(R.string.collection)
            binding.report.isVisible = true

        })

        //保存及分析流程失败
        LiveEventBus.get(Constant.Event.analysisProcessFailed).observe(viewLifecycleOwner, {
            it?.let {
                if ((it as String).isNotEmpty())
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }

            viewModel._collectBtnText.value = getString(R.string.collection)
            binding.report.isVisible = true


        })


    }


    fun showDialog() {
        if (!BluetoothConfig.bleSdkEnable) {
            Toast.makeText(requireContext(), "初始化中，请稍候再试！", Toast.LENGTH_SHORT).show()
            return
        }
        activity?.supportFragmentManager?.let { ConnectDialog().show(it, "show") }
    }

    var period: Long = 41L

    fun doWaveTask(){
        lifecycleScope.launch {
            while (true) {

                if (BluetoothConfig.currentRunState in RunState.PREPARING_TEST..RunState.RECORDING) {
                    var temp: FloatArray? = DataController.draw(5)

                    Log.d(DASH, "current .temp == ${temp?.size}, ${BluetoothConfig.currentRunState}")

                    if (viewModel._runState.value != RunState.RECORDING) {
                        temp = if (temp == null || temp.isEmpty()) {
                            FloatArray(0)
                        } else {
                            FloatArray(temp.size)
                        }
                    }
                    // 采集数据 自动手动可能同时进行
                    DataController.feed(temp, collectUtil.manualCounting)

                    ecgView.postInvalidate()
                }
                delay(period)
            }
        }
    }

    fun doWatchTask(){
        lifecycleScope.launch {
            while(true) {
                if (BluetoothConfig.currentRunState in RunState.PREPARING_TEST..RunState.RECORDING) {
                    if (period != 0L && DataController.dataRec.size in 101..199) {
                        period = if (DataController.dataRec.size > 150) 39 else period
                    }
                }
                delay(1000)
            }
        }
    }



    fun toReportList() {
        findNavController().navigate(R.id.dashboard_to_report_list)

    }

    /**
     * 手动采集
     */
    fun manualCollect() {
        if (mainVM.connectState.value != LpBleUtil.State.CONNECTED) {
            Toast.makeText(requireContext(), "蓝牙未连接，无法采集", Toast.LENGTH_SHORT).show()
            return
        }
        if (viewModel.fingerState.value == false) {
            Toast.makeText(requireContext(), "导联断开， 无法采集", Toast.LENGTH_SHORT).show()
            return
        }
        if (BluetoothConfig.currentRunState != RunState.RECORDING) {
            Toast.makeText(requireContext(), "不在测量中， 无法采集", Toast.LENGTH_SHORT).show()
            return
        }
        if (activity?.let { collectUtil.manualCounting } == true) {
            Toast.makeText(requireContext(), "正在采集/分析中", Toast.LENGTH_SHORT).show()
            return
        }
        if (!collectUtil.checkService()) {
            Toast.makeText(requireContext(), "正在初始化采集服务", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            activity?.let {
                collectUtil.manualCollect(viewModel, mainVM)

            }
        }
        binding.report.isVisible = false


    }


    fun back() {
        activity?.finish()
    }

}

fun Er1BleResponse.RtParam.batteryState(): Int {
    return sysFlag.toInt() shr 6 and 0x03
}

fun Er1BleResponse.RtParam.isSignalPoor(): Boolean {
    return sysFlag and 0x04 > 0
}

fun Er1BleResponse.RtParam.getRunState(): Int {
    return (runStatus and 0x0F).toInt()
}


