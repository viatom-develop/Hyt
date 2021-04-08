package com.viatom.hyt

import android.app.Application
import com.tencent.bugly.crashreport.CrashReport

/**
 * author: wujuan
 * created on: 2021/4/1 10:39
 * description:
 */
class MainApplication : Application(){

    override fun onCreate() {
        super.onCreate()
        CrashReport.initCrashReport(this, "52a74206f2", false)


    }


}