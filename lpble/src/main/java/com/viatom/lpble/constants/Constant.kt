package com.viatom.lpble.constants

import com.lepu.blepro.objs.Bluetooth
import com.viatom.lpble.data.entity.UserEntity

/**
 * author: wujuan
 * created on: 2021/4/1 14:46
 * description:
 */
class Constant{
    interface Dir{
        companion object{
            val er1Dir: String = "/er1"
            val er1EcgDir: String = "/$er1Dir/ecg" //保存采集到的数据.txt
        }

    }

    interface BluetoothConfig{
        companion object{
            val SUPPORT_MODEL: Int = Bluetooth.MODEL_ER1
            val TIME_OUT_MILLIS: Long = 2000 //s
            val CHECK_BLE_REQUEST_CODE = 6001
            var currentRunState: Int = RunState.NONE // 设备实时状态

        }
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


    interface Collection{
        companion object{
            val TYPE_MANUAL: Int = 1
            val TYPE_AUTO: Int = 0

            val AUTO_INTERVAL: Long = 5 * 60 * 1000
            val AUTO_DURATION_MILLS: Long = 5 * 1000 //ms
            val AUTO_START: Int = 1001
            val AUTO_STOP: Int = 1000

            val MANUAL_DURATION_S: Int = 30 //s
            val MANUAL_START: Int = 2001
            val MANUAL_STOP: Int = 2000



        }
    }


    interface EcgViewConfig{
        companion object{
            val ECG_CELL_SIZE: Int = 4
            val PADDING_TOP: Int = 20
        }
    }
    interface Event{
       companion object{
           val connectingLoading: String = "connecting_loading"
           val permissionNecessary: String = "permission_necessary"


           val collectServiceConnected: String = "collect_service_connected"

           val analysisProcessFailed: String = "analysis_process_failed"
           val analysisProcessSuccess: String = "analysis_process_success"
       }

    }

    interface Report{
        companion object{
            const val A4_WIDTH = 2520 / 2 // 210 * 6

            const val  A4_HEIGHT = 3564 / 2 // 297 * 6

        }
    }

    interface User{
        companion object{
           var currentUser: UserEntity? = null
        }
    }
}