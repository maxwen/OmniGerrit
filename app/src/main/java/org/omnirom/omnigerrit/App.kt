package org.omnirom.omnigerrit

import android.app.Application
import org.omnirom.omnigerrit.model.ChangeFilter
import org.omnirom.omnigerrit.model.Device
import org.omnirom.omnigerrit.utils.BuildImageUtils
import org.omnirom.omnigerrit.utils.DeviceUtils

class App : Application() {
    private val TAG = "App"

    override fun onCreate() {
        super.onCreate( )

        BuildImageUtils.buildType = Device.getBuildType(this)
        BuildImageUtils.device = Device.getDevice(this)
        BuildImageUtils.version = Device.getVersion(this)
        ChangeFilter.defaultBranch = Device.getBranch(this)
    }
}