package com.viatom.lpble.constants

import com.lepu.blepro.objs.Bluetooth

/**
 * author: wujuan
 * created on: 2021/4/1 14:46
 * description:
 */
class Constant{
    interface Dir{
        companion object{
            val er1Dir: String = "/er1"
            val er1EcgDir: String = "/$er1Dir/ecg"
        }

    }

    interface BluetoothConfig{
        companion object{
            val SUPPORT_MODEL: Int = Bluetooth.MODEL_ER1
            val TIME_OUT_MILLIS: Long = 2000
            val CHECK_BLE_REQUEST_CODE = 6001

            var isLpBleEnable: Boolean = false
            var currentRunState: Int = RunState.NONE
        }

        interface RunState{
            /**
             * dashboardState包括状态:<br>
             * 空状态 -2 <br>
             * 离线 -1 <br>
             * 空闲待机充电 0 <br>
             * 空闲待机非充电 0 <br>
             * 测试准备 1 <br>
             * 记录中 2 <br>
             * 分析中 3 <br>
             * 存储成功( >= 30s）4 <br>
             * 存储失败( < 30s） 5 <br>
             * 噪声提示重启测试超过6次,即将进入待机状态 6 <br>
             * 导联断开 7
             **/
            companion object{
                const val NONE = -2
                const val OFFLINE = -1
                const val STANDBY = 0
                const val PREPARING_TEST = 1
                const val RECORDING = 2
                const val ANALYZING = 3
                const val SAVE_SUCCESS = 4
                const val SAVE_FAILED = 5
                const val COMING_STANDBY = 6
                const val LEAD_OFF = 7
            }
        }


    }


    interface EcgViewConfig{
        companion object{
            val ECG_CELL_SIZE: Int = 4
            val PADDING_TOP: Int = 20
        }
    }
    interface EventUI{
       companion object{
           val connectingLoading: String = "connecting_loading"
           val permissionNecessary: String = "permission_necessary"
       }

    }
}