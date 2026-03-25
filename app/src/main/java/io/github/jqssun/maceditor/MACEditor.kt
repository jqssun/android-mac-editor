package io.github.jqssun.maceditor

import io.github.jqssun.maceditor.hookers.SystemUIHooker
import io.github.jqssun.maceditor.hookers.WifiConfigHooker
import io.github.jqssun.maceditor.hookers.WifiServiceHooker
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam

const val TAG = "MACEditor"

private lateinit var module: MACEditor

@Suppress("DEPRECATION")
class MACEditor(base: XposedInterface, param: ModuleLoadedParam) : XposedModule(base, param) {
    init {
        module = this
    }

    override fun onSystemServerLoaded(param: SystemServerLoadedParam) {
        try {
            WifiServiceHooker.hook(param, this)
        } catch (e: Exception) {
            log("$TAG: ERROR: $e")
        }
        try {
            WifiConfigHooker.hook(param, this)
        } catch (e: Exception) {
            log("$TAG: Failed to hook WiFi config resources: $e")
        }
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        when (param.packageName) {
            "com.android.systemui" -> {
                val prefs = getRemotePreferences(BuildConfig.APPLICATION_ID)
                if (!prefs.getBoolean("tileRevealDone", false)) {
                    try {
                        log("$TAG: Hooking System UI to show quick settings tile.")
                        SystemUIHooker.hook(param, this)
                    } catch (e: Exception) {
                        log("$TAG: ERROR: $e")
                    }
                }
            }
        }
    }
}
