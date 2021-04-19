package com.viatom.lpble.ble

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.lepu.blepro.utils.LepuBleLog
import com.viatom.lpble.constants.Constant
import com.viatom.lpble.constants.Constant.Collection.Companion.AUTO_DURATION_MILLS
import com.viatom.lpble.constants.Constant.Collection.Companion.AUTO_INTERVAL
import com.viatom.lpble.constants.Constant.Collection.Companion.AUTO_START
import com.viatom.lpble.constants.Constant.Collection.Companion.AUTO_STOP
import com.viatom.lpble.constants.Constant.Collection.Companion.MANUAL_DURATION_S
import com.viatom.lpble.util.LpResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * author: wujuan
 * created on: 2021/4/12 16:23
 * description:
 */
class CollectService : Service(){

    private val binder = CollectBinder()

   inner class CollectBinder: Binder(){
       fun getService(): CollectService = this@CollectService
   }

    companion object {

        @JvmStatic
        fun startService(context: Context) {
            LepuBleLog.d("startService")
            Intent(context, CollectService::class.java).also { intent -> context.startService(intent)}
        }

        @JvmStatic
        fun stopService(context: Context) {
            val intent = Intent(context, CollectService::class.java)
            context.stopService(intent)
        }

    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    /**
     *
     * 自动采集
     * @return Flow<LpResult<Int>>
     */
    suspend fun autoCollect(): Flow<LpResult<Int>> {
        return flow{
            try {
                while (true){
                    delay(AUTO_INTERVAL) //
                    emit(LpResult.Success(AUTO_START))
                    delay(AUTO_DURATION_MILLS)
                    emit(LpResult.Success(AUTO_STOP))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emit(LpResult.Failure(e.cause))
            }
        }.flowOn(Dispatchers.IO)

    }


    /**
     * 手动采集
     * @return Flow<LpResult<Int>>
     */
    suspend fun manualCount(): Flow<LpResult<Int>> {
        return flow{
            try {
                Log.d("manualCollect", " start.....")
                for (i in 0..MANUAL_DURATION_S){

                    if (LpBleUtil.isDisconnected(Constant.BluetoothConfig.SUPPORT_MODEL)) {
                        emit(LpResult.Failure(Exception("蓝牙已断开, 采集失败")))
                        return@flow
                    }

                    if (Constant.BluetoothConfig.currentRunState !in  Constant.RunState.PREPARING_TEST..Constant.RunState.RECORDING) {
                        emit(LpResult.Failure(Exception("导联断开, 停止采集")))
                        return@flow
                    }

                    emit(LpResult.Success(i))
                    delay(1000)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("manualCollect", "$e")
                emit(LpResult.Failure(e.cause))
            }
        }.flowOn(Dispatchers.IO)

    }

}