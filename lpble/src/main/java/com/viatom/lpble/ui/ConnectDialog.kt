package com.viatom.lpble.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jeremyliao.liveeventbus.LiveEventBus
import com.lepu.blepro.ble.data.LepuDevice
import com.lepu.blepro.event.EventMsgConst
import com.lepu.blepro.event.InterfaceEvent
import com.lepu.blepro.objs.BluetoothController
import com.viatom.lpble.R
import com.viatom.lpble.adapter.ConnectAdapter
import com.viatom.lpble.ble.LpBleUtil
import com.viatom.lpble.constants.Constant
import com.viatom.lpble.constants.Constant.BluetoothConfig.Companion.SUPPORT_MODEL
import com.viatom.lpble.data.entity.DeviceEntity
import com.viatom.lpble.databinding.ConnectDialogBinding
import com.viatom.lpble.ext.convertDpToPixel
import com.viatom.lpble.ext.createDir
import com.viatom.lpble.ext.screenSize
import com.viatom.lpble.viewmodels.ConnectViewModel
import com.viatom.lpble.viewmodels.MainViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * author: wujuan
 * created on: 2021/4/6 10:31
 * description:
 */
class ConnectDialog : DialogFragment(){

    private lateinit var binding: ConnectDialogBinding

    private val mainVM: MainViewModel by activityViewModels()

    private val viewModel: ConnectViewModel by activityViewModels()

    private lateinit var adapter: ConnectAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.connect_dialog, container, false)
        binding.ctx = this

        activity?.let { fragmentActivity ->
            val width = fragmentActivity.screenSize()[0]

            dialog?.let { d ->
                d.requestWindowFeature(Window.FEATURE_NO_TITLE)

                d.window?.let {
                    it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    it.decorView.setBackgroundColor(Color.TRANSPARENT)
                    it.setDimAmount(0.6f)

                    // 设置宽度
                    val params: WindowManager.LayoutParams = it.attributes

                    params.width = (width - requireContext().convertDpToPixel(32f)).toInt()
                    params.height = WindowManager.LayoutParams.WRAP_CONTENT
                    params.gravity = Gravity.CENTER_HORIZONTAL
                    it.attributes = params
                }
                d.setCanceledOnTouchOutside(true)
            }
        }


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initLiveEvent()
        subscribeUi()

    }
    private fun initView(){
        LinearLayoutManager(context).apply {
            this.orientation = LinearLayoutManager.VERTICAL
            binding.deviceList.layoutManager = this
        }

        adapter = ConnectAdapter(R.layout.connect_device_item, null).apply {
            this.setOnItemClickListener { adapter, view, position ->
                // 去连接
                this.data[position].let {
                    mainVM._toConnectDevice.value = it

                    dialog?.setCanceledOnTouchOutside(false)
                    LpBleUtil.connect(requireContext(),it)

                }
            }
            binding.deviceList.adapter = this
        }
    }





    private fun subscribeUi(){
        mainVM.bleEnable.observe(this, {
            // ble 状态可用即开始扫描
            if (it)viewModel._scanning.value = true

        })

        //更新扫描状态
        viewModel.scanning.observe(viewLifecycleOwner, {
            if (it){
                startScan()
                binding.deviceList.smoothScrollToPosition(0)
            } else LpBleUtil.stopScan()

        })

        mainVM.connectState.observe(this, {

            binding.connectedState.run {
                this.text = LpBleUtil.convertState(it)
            }
        })

        mainVM.curBluetooth.observe(this, {
            binding.connectedName.run {
                this.text = it?.deviceName ?: ""
                this.visibility = it?.let { View.VISIBLE }?: View.INVISIBLE
            }
            binding.connectedState.visibility = it?.let { View.VISIBLE }?: View.INVISIBLE
        })

    }

    private fun initLiveEvent(){

        //扫描通知
        LiveEventBus.get(EventMsgConst.Discovery.EventDeviceFound)
            .observe(this, Observer {
                adapter.setNewData(BluetoothController.getDevices(SUPPORT_MODEL))
                adapter.notifyDataSetChanged()

            })

    }

    fun startScan(){
        //清空
        BluetoothController.clear()
        adapter.setNewInstance(null)
        adapter.notifyDataSetChanged()


        //重新扫描
        LpBleUtil.startScan(SUPPORT_MODEL)
        // 10s后停止扫描
        lifecycleScope.launch {
            delay(10000)
            viewModel._scanning.postValue(false)
        }
    }

    fun reconnect(){
        if (LpBleUtil.isDisconnected(SUPPORT_MODEL))
            mainVM.curBluetooth.value?.deviceName?.let { LpBleUtil.reconnect(SUPPORT_MODEL, it) }

    }

    fun refresh(){
        viewModel._scanning.value = true
    }


    override fun onDestroy() {
        super.onDestroy()
        LpBleUtil.stopScan()
    }

}