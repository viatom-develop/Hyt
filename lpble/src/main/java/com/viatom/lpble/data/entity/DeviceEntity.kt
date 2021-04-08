package com.viatom.lpble.data.entity

import android.bluetooth.BluetoothDevice
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lepu.blepro.ble.data.LepuDevice

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
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceName: String,
    val deviceMacAddress: String,
    val productTypeName: String,
    val hwVersion: Char?,
    val fwVersion: String?,
    val blVersion: String?,
    val branchCode: String?,
    val deviceType: Int?,
    val protocolVersion: String?,
    val currentTime: String?,
    val protocolDataMaxLen: Int?,
    val serialNum: String?,
    val snLength: Int?,
    val data: ByteArray
) {

    companion object {
        fun convert2DeviceEntity(b: BluetoothDevice, lepuDevice: LepuDevice): DeviceEntity {
            return lepuDevice.run {

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
                    currentTime = lepuDevice.curTime,
                    protocolDataMaxLen = lepuDevice.protocolMaxLen,
                    serialNum = lepuDevice.sn,
                    snLength = lepuDevice.snLen,
                    data = lepuDevice.bytes
                )
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeviceEntity

        if (id != other.id) return false
        if (deviceName != other.deviceName) return false
        if (deviceMacAddress != other.deviceMacAddress) return false
        if (productTypeName != other.productTypeName) return false
        if (hwVersion != other.hwVersion) return false
        if (fwVersion != other.fwVersion) return false
        if (blVersion != other.blVersion) return false
        if (branchCode != other.branchCode) return false
        if (deviceType != other.deviceType) return false
        if (protocolVersion != other.protocolVersion) return false
        if (currentTime != other.currentTime) return false
        if (protocolDataMaxLen != other.protocolDataMaxLen) return false
        if (serialNum != other.serialNum) return false
        if (snLength != other.snLength) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + deviceName.hashCode()
        result = 31 * result + deviceMacAddress.hashCode()
        result = 31 * result + productTypeName.hashCode()
        result = 31 * result + (hwVersion?.hashCode() ?: 0)
        result = 31 * result + (fwVersion?.hashCode() ?: 0)
        result = 31 * result + (blVersion?.hashCode() ?: 0)
        result = 31 * result + (branchCode?.hashCode() ?: 0)
        result = 31 * result + (deviceType ?: 0)
        result = 31 * result + (protocolVersion?.hashCode() ?: 0)
        result = 31 * result + (currentTime?.hashCode() ?: 0)
        result = 31 * result + (protocolDataMaxLen ?: 0)
        result = 31 * result + (serialNum?.hashCode() ?: 0)
        result = 31 * result + (snLength ?: 0)
        result = 31 * result + data.contentHashCode()
        return result
    }


}