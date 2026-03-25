package io.github.jqssun.maceditor.hookers

import android.content.res.Resources
import android.util.Log
import io.github.jqssun.maceditor.BuildConfig
import io.github.jqssun.maceditor.TAG
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

class WifiConfigHooker {
    companion object {
        private var module: XposedModule? = null

        private val TARGET_KEYS = setOf(
            "config_wifi_connected_mac_randomization_supported",
            "config_wifi_p2p_mac_randomization_supported",
            "config_wifi_ap_mac_randomization_supported"
        )

        fun hook(param: SystemServerStartingParam, module: XposedModule) {
            this.module = module
            module.hook(
                Resources::class.java.getDeclaredMethod("getBoolean", Int::class.javaPrimitiveType)
            ).intercept(ResourceBoolHooker())
        }

        class ResourceBoolHooker : XposedInterface.Hooker {
            override fun intercept(chain: XposedInterface.Chain): Any? {
                val result = chain.proceed()
                val prefs = module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
                if (prefs?.getBoolean("forceShowMacRandomization", false) != true) return result

                val res = chain.thisObject as? Resources ?: return result
                val id = chain.getArg(0) as Int
                try {
                    val name = res.getResourceEntryName(id)
                    if (name in TARGET_KEYS) {
                        module?.log(Log.INFO, TAG, "Forced $name to true")
                        return true
                    }
                } catch (_: Resources.NotFoundException) {
                }
                return result
            }
        }
    }
}
