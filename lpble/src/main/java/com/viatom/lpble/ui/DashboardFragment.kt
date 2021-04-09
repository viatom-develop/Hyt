package com.viatom.lpble.ui

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.afollestad.materialdialogs.MaterialDialog
import com.jeremyliao.liveeventbus.LiveEventBus
import com.lepu.blepro.ble.cmd.Er1BleResponse
import com.lepu.blepro.event.EventMsgConst
import com.lepu.blepro.event.InterfaceEvent
import com.viatom.lpble.R
import com.viatom.lpble.ble.BatteryInfo
import com.viatom.lpble.ble.DataController
import com.viatom.lpble.constants.Constant
import com.viatom.lpble.constants.Constant.BluetoothConfig.RunState
import com.viatom.lpble.constants.Constant.BluetoothConfig
import com.viatom.lpble.databinding.FragmentDashboradBinding
import com.viatom.lpble.viewmodels.DashboardViewModel
import com.viatom.lpble.viewmodels.MainViewModel
import com.viatom.lpble.widget.EcgBkg
import com.viatom.lpble.widget.EcgView
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.experimental.and
import kotlin.math.floor

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class DashboardFragment : Fragment() {


    private lateinit var binding: FragmentDashboradBinding

    private val mainVM: MainViewModel by activityViewModels()

    private val viewModel: DashboardViewModel by activityViewModels()

    lateinit var leadOffDialog: AlertDialog

    lateinit var ecgView: EcgView




    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_dashborad, container, false)
        binding.ctx = this

        activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)




        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        subscribeUi()
        initLiveEvent()


//        view.findViewById<Button>(R.id.button_first).setOnClickListener {
//            findNavController().navigate(R.id.action_DashboardFragment_to_SecondFragment)
//        }
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
        //状态切换
        viewModel.runState.observe(viewLifecycleOwner, {
            binding.battery.visibility =  if(it in RunState.NONE..RunState.OFFLINE) View.INVISIBLE else View.VISIBLE
            binding.hr.visibility = if(it in RunState.NONE..RunState.PREPARING_TEST || it in RunState.SAVE_FAILED..RunState.LEAD_OFF) View.INVISIBLE else View.VISIBLE
            binding.bpmText.visibility = if(it in RunState.NONE..RunState.PREPARING_TEST || it in RunState.SAVE_FAILED..RunState.LEAD_OFF) View.INVISIBLE else View.VISIBLE
            binding.bpmImg.visibility = if(it in RunState.NONE..RunState.PREPARING_TEST || it in RunState.SAVE_FAILED..RunState.LEAD_OFF) View.INVISIBLE else View.VISIBLE

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
                binding.hr.text =   if (it) "--" else h.toString()
            }

        })



    }


    private fun initLiveEvent() {
        LiveEventBus.get(InterfaceEvent.ER1.EventEr1RtData).observe(this, { event ->

            (event as InterfaceEvent).let {
                (it.data as Er1BleResponse.RtData)?.let { data ->

                    data.param.let { param ->

                        //运行状态
                        param.getRunState().run {
                            viewModel._runState.value = this

                            //判断状态是否切换了

                            synchronized(BluetoothConfig.currentRunState){
                                BluetoothConfig.currentRunState.let { lastState->

                                    Log.d("dashboard", "currentRunState = $this,lastState = $lastState")
                                    if (this != lastState){
                                        ecgView.clear()
                                        ecgView.invalidate()
                                        if (this in RunState.PREPARING_TEST..RunState.RECORDING) startTimer(ecgView) else stopTimer()
                                    }
                                }

                                //更新记录为最新的状态
                                BluetoothConfig.currentRunState = this
                            }

                            ecgView.setRunState(this)

                            Log.d("dashboard runState", "$this")
                        }


                        //电池
                        BatteryInfo(param.batteryState(), param.battery).run {
                            viewModel._battery.value = this
                            Log.d("dashboard battery", "$this")
                        }

                        //最大测量时长
                        viewModel._overTime.value = param.recordTime >= TimeUnit.DAYS.toSeconds(1)

                        //hr
                        viewModel._hr.value = param.hr
                        Log.d("dashboard hr", "${param.hr}")

                        //心电信号
                        viewModel._isSignalPoor.value = param.isSignalPoor()

                        //wave data
                        viewModel.feedWaveData(data)

                    }

                }

            }

        })

        // 当停止实时任务
        LiveEventBus.get(EventMsgConst.RealTime.EventRealTimeStop).observe(viewLifecycleOwner, {

        })
    }


    fun showDialog(){
        if (!BluetoothConfig.isLpBleEnable){
            Toast.makeText(requireContext(), "初始化中，请稍候再试！", Toast.LENGTH_SHORT).show()
            return
        }
        activity?.supportFragmentManager?.let { ConnectDialog().show(it, "show") }
    }

    var waveTimer: Timer? =  null
    var waveTask: TimerTask? = null
    var watchTimer: Timer? = null
    var watchTask: TimerTask? = null
    var period: Long = 41L

    fun startWaveTimer(ecgView: EcgView) {

        stopWaveTimer()
        waveTimer = Timer()
        waveTask = object : TimerTask() {
            override fun run() {
                var temp: FloatArray? = DataController.draw(5)
                Log.d("dashboard", "DataController.draw(5) == " + Arrays.toString(temp))
                if (viewModel._runState.value !== RunState.RECORDING) {  // 非测试状态,画0
                    temp = if (temp == null || temp.isEmpty()) {
                        FloatArray(0)
                    } else {
                        FloatArray(temp.size)
                    }
                }
                DataController.feed(temp)
                ecgView.invalidate()
            }
        }
        waveTimer?.schedule(
                waveTask,
                5,
                period
        )
    }

    fun stopWaveTimer() {
        waveTask?.cancel()
        waveTask = null

        waveTimer?.cancel()
        waveTimer = null
    }

    fun startWatchTimer(ecgView: EcgView) {
        stopWatchTimer()
        watchTimer = Timer()
        watchTask = object : TimerTask() {
            override fun run() {
                if (period == 0L) {
                    return
                }
                if (DataController.dataRec.size in 101..199) {
                    return
                }
                period =
                        if (DataController.dataRec.size > 150) 39 else period
                startWaveTimer(ecgView)
            }
        }
        watchTimer?.schedule(
                watchTask,
                1000,
                1000L
        )
    }

    fun stopWatchTimer() {
        watchTask?.cancel()
        watchTask = null
    }

    fun stopTimer() {
        stopWatchTimer()
        stopWaveTimer()
    }
    fun startTimer(ecgView: EcgView) {
        startWatchTimer(ecgView)
        startWaveTimer(ecgView)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }

}

fun Er1BleResponse.RtParam.batteryState(): Int{
    return sysFlag.toInt() shr 6 and 0x03
}

fun Er1BleResponse.RtParam.isSignalPoor(): Boolean{
    return sysFlag and 0x04 > 0
}

fun Er1BleResponse.RtParam.getRunState(): Int{
    return (runStatus and 0x0F).toInt()
}


