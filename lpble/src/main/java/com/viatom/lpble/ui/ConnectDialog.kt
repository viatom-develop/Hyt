import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.jeremyliao.liveeventbus.LiveEventBus
import com.lepu.blepro.ble.data.LepuDevice
import com.lepu.blepro.event.EventMsgConst
import com.lepu.blepro.event.InterfaceEvent
import com.lepu.blepro.objs.BluetoothController
import com.viatom.lpble.R
import com.viatom.lpble.adapter.ConnectAdapter
import com.viatom.lpble.ble.LpBleUtil
import com.viatom.lpble.constants.Constant.BluetoothConfig.Companion.SUPPORT_MODEL
import com.viatom.lpble.data.entity.DeviceEntity
import com.viatom.lpble.databinding.ConnectDialogBinding
import com.viatom.lpble.ext.convertDpToPixel
import com.viatom.lpble.ext.createDir
import com.viatom.lpble.ext.screenSize
import com.viatom.lpble.viewmodels.ConnectViewModel
import com.viatom.lpble.viewmodels.MainViewModel


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
        LinearLayoutManager(context).apply {
            this.orientation = LinearLayoutManager.VERTICAL
            binding.deviceList.layoutManager = this
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLiveEvent()
        subscribeUi()
        initData()

    }

    fun initData(){
        adapter = ConnectAdapter(R.layout.connect_device_item, null).apply {
            this.setOnItemClickListener { adapter, view, position ->
                // 去连接
                this.data[position]?.let {
                    mainVM._connectLoading.value = true
                    viewModel._toConnectDevice.value = it

                    dialog?.setCanceledOnTouchOutside(false)
                    LpBleUtil.connect(requireContext(),it)

                }
            }
            binding.deviceList.adapter = this
        }


    }

    private fun startScan(){
        //清空
        BluetoothController.clear()
        adapter.setNewInstance(null)
        adapter.notifyDataSetChanged()
        //重新扫描
        LpBleUtil.startScan(SUPPORT_MODEL)

    }


    private fun subscribeUi(){
        mainVM.bleEnable.observe(this, {
            if (it)startScan()

        })

        //扫描点击响应
        viewModel.isRefreshing.observe(viewLifecycleOwner, {
            if (it)
                startScan()

        })

        mainVM.connectState.observe(this, {

            binding.connectedState.run {
                this.text = LpBleUtil.convertState(it)
            }
        })

        mainVM.curBluetooth.observe(this, {
            binding.connectedName.text = it?.deviceName?: ""
        })

    }

    fun initLiveEvent(){

        //扫描通知
        LiveEventBus.get(EventMsgConst.Discovery.EventDeviceFound)
            .observe(this, Observer {
                adapter.setNewData(BluetoothController.getDevices(SUPPORT_MODEL))
                adapter.notifyDataSetChanged()

            })
        // 设备信息通知
        LiveEventBus.get(InterfaceEvent.ER1.EventEr1Info)
                .observe(this, { event ->

                    event as InterfaceEvent
                    Log.d("EventEr1Info","currentDevice init")

                    //根目录下创建设备名文件夹
                    viewModel.toConnectDevice.value?.device?.let { b ->
                        b.name?.let {
                            requireContext().createDir(it)
                        }

                        //保存设备
                        viewModel.saveDevice(requireActivity().application, DeviceEntity.convert2DeviceEntity(b, event.data as LepuDevice))

                        //ui
                        mainVM._connectLoading.value = false
                        dialog?.setCanceledOnTouchOutside(true)


                    }


                })

    }

    override fun onDestroy() {
        super.onDestroy()
        LpBleUtil.stopScan()
    }

}