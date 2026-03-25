package io.github.jqssun.maceditor.hookers

import android.annotation.SuppressLint
import android.net.MacAddress
import io.github.jqssun.maceditor.BuildConfig
import io.github.jqssun.maceditor.TAG
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam
import io.github.libxposed.api.annotations.XposedHooker

class WifiServiceHooker {
    companion object {
        var module: XposedModule? = null
            private set

        @SuppressLint("PrivateApi")
        fun hook(param: SystemServerLoadedParam, module: XposedModule) {
            this.module = module
            module.hook(
                param.classLoader.loadClass("com.android.server.SystemServiceManager")
                    .getDeclaredMethod("loadClassFromLoader", String::class.java, ClassLoader::class.java),
                SystemServiceManagerHooker::class.java
            )
        }

        @XposedHooker
        class SystemServiceManagerHooker : XposedInterface.Hooker {
            companion object {
                @JvmStatic
                @SuppressLint("PrivateApi")
                fun after(callback: XposedInterface.AfterHookCallback) {
                    val className = callback.args[0] as String
                    if (className == "com.android.server.wifi.WifiService") {
                        val classLoader = callback.args[1] as ClassLoader
                        val wifiVendorHalClass = classLoader.loadClass("com.android.server.wifi.WifiVendorHal")
                        val setStaMacAddressMethod = wifiVendorHalClass
                            .getDeclaredMethod("setStaMacAddress", String::class.java, MacAddress::class.java)
                        val setApMacAddressMethod = wifiVendorHalClass
                            .getDeclaredMethod("setApMacAddress", String::class.java, MacAddress::class.java)

                        module?.hook(setStaMacAddressMethod, MacAddrSetGenericHooker::class.java)
                        module?.hook(setApMacAddressMethod, MacAddrSetGenericHooker::class.java)
                    }
                }
            }
        }

        @XposedHooker
        class MacAddrSetGenericHooker : XposedInterface.Hooker {
            companion object {
                @JvmStatic
                fun before(callback: XposedInterface.BeforeHookCallback) {
                    val prefs = module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
                    val isHookActive = prefs?.getBoolean("hookActive", false) ?: false

                    if (isHookActive) {
                        val customMac = prefs?.getString("customMac", "") ?: ""
                        if (customMac.isNotEmpty()) {
                            @Suppress("DEPRECATION") module?.log("$TAG: Replacing MAC with custom: $customMac on ${callback.args[0]}")
                            callback.args[1] = MacAddress.fromString(customMac)
                        } else {
                            @Suppress("DEPRECATION") module?.log("$TAG: Blocked MAC address change to ${callback.args[1]} on ${callback.args[0]}.")
                            callback.returnAndSkip(true)
                        }
                    }
                }

                @JvmStatic
                fun after(callback: XposedInterface.AfterHookCallback) {
                    if (!callback.isSkipped) {
                        @Suppress("DEPRECATION") module?.log("$TAG: MAC address set to ${callback.args[1]} on ${callback.args[0]}.")
                    }
                }
            }
        }
    }
}
