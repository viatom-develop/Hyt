package com.viatom.lpble.ble

/**
 * author: wujuan
 * created on: 2021/4/8 13:37
 * description:
 */
object WaveFilter {
    init {
        System.loadLibrary("native-lib")
        System.loadLibrary("offline-lib")
    }

    external fun filter(f: Double, reset: Boolean): DoubleArray

    external fun shortfilter(shorts: ShortArray?): ShortArray

    external fun offlineFilter(doubles: DoubleArray): DoubleArray

    // reset filter
    fun resetFilter() {
        filter(0.0, true)
    }
}