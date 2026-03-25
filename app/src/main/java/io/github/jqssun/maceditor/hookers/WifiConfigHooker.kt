package io.github.jqssun.maceditor.hookers

import android.content.res.Resources
import io.github.jqssun.maceditor.BuildConfig
import io.github.jqssun.maceditor.TAG
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam
import io.github.libxposed.api.annotations.XposedHooker

class WifiConfigHooker {
    companion object {
        private var module: XposedModule? = null

        private val TARGET_KEYS = setOf(
            "config_wifi_connected_mac_randomization_supported",
            "config_wifi_p2p_mac_randomization_supported",
            "config_wifi_ap_mac_randomization_supported"
        )

        fun hook(param: SystemServerLoadedParam, module: XposedModule) {
            this.module = module
            module.hook(
                Resources::class.java.getDeclaredMethod("getBoolean", Int::class.javaPrimitiveType),
                ResourceBoolHooker::class.java
            )
        }

        @XposedHooker
        class ResourceBoolHooker : XposedInterface.Hooker {
            companion object {
                @JvmStatic
                fun after(callback: XposedInterface.AfterHookCallback) {
                    val prefs = module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
                    if (prefs?.getBoolean("forceShowMacRandomization", false) != true) return

                    val res = callback.thisObject as? Resources ?: return
                    val id = callback.args[0] as Int
                    try {
                        val name = res.getResourceEntryName(id)
                        if (name in TARGET_KEYS) {
                            callback.result = true
                            @Suppress("DEPRECATION") module?.log("$TAG: Forced $name to true")
                        }
                    } catch (_: Resources.NotFoundException) {
                        // not our resource, ignore
                    }
                }
            }
        }
    }
}
