package com.viatom.lpble.data.entity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lepu.blepro.ble.data.LepuDevice
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

/**
 * author: wujuan
 * created on: 2021/4/6 12:37
 * description:
 */
@Entity(
        tableName = "devices",
        indices = [Index(value = ["deviceName"], unique = true), Index("productTypeName")]
)
data class DeviceEntity(
        @PrimaryKey
        val deviceName: String,
        val deviceMacAddress: String,
        val productTypeName: String,
        val hwVersion: Char?,
        val fwVersion: String?,
        val blVersion: String?,
        val branchCode: String?,
        val deviceType: Int?,
        val protocolVersion: String?,
        val currentTime: Long?,
        val protocolDataMaxLen: Int?,
        val serialNum: String?,
        val snLength: Int?,
        val data: ByteArray
) {

    companion object {
        @SuppressLint("SimpleDateFormat")
        fun convert2DeviceEntity(b: BluetoothDevice, lepuDevice: LepuDevice): DeviceEntity {
            return lepuDevice.run {
                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

                DeviceEntity(
                    deviceName = b.name,
                    deviceMacAddress = b.address,
                    productTypeName = b.name.split(" ")[0],
                    hwVersion = lepuDevice.hwV,
                    fwVersion = lepuDevice.fwV,
                    blVersion = lepuDevice.btlV,
                    branchCode = lepuDevice.branchCode,
                    deviceType = lepuDevice.deviceType,
                    protocolVersion = lepuDevice.protocolV,
                    currentTime = sdf.parse(lepuDevice.curTime).time,
                            protocolDataMaxLen = lepuDevice.protocolMaxLen,
                            serialNum = lepuDevice.sn,
                            snLength = lepuDevice.snLen,
                            data = lepuDevice.bytes
                    )
            }
        }
    }



}