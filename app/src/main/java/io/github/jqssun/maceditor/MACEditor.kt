package io.github.jqssun.maceditor

import android.util.Log
import io.github.jqssun.maceditor.hookers.WifiConfigHooker
import io.github.jqssun.maceditor.hookers.WifiServiceHooker
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

const val TAG = "MACEditor"

private lateinit var module: MACEditor

class MACEditor : XposedModule() {

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        super.onModuleLoaded(param)
        module = this
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        super.onSystemServerStarting(param)
        try {
            WifiServiceHooker.hook(param, this)
        } catch (e: Exception) {
            log(Log.ERROR, TAG, "ERROR: $e")
        }
        try {
            WifiConfigHooker.hook(param, this)
        } catch (e: Exception) {
            log(Log.ERROR, TAG, "Failed to hook WiFi config resources: $e")
        }
    }
}
