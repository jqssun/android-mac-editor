package io.github.jqssun.maceditor.hookers

import android.annotation.SuppressLint
import android.net.MacAddress
import android.util.Log
import io.github.jqssun.maceditor.BuildConfig
import io.github.jqssun.maceditor.TAG
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam

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

        class MacAddrSetGenericHooker : XposedInterface.Hooker {
            companion object {
                @JvmStatic
                fun before(callback: XposedInterface.BeforeHookCallback) {
                    val prefs = module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
                    val isHookActive = prefs?.getBoolean("hookActive", false) ?: false

                    if (isHookActive) {
                        module?.log(Log.INFO, TAG, "Blocked MAC address change to ${callback.args[1]} on ${callback.args[0]}.", null)
                        callback.returnAndSkip(true)
                    }
                }

                @JvmStatic
                fun after(callback: XposedInterface.AfterHookCallback) {
                    if (!callback.isSkipped) {
                        module?.log(Log.INFO, TAG, "Allowed MAC address change to ${callback.args[1]} on ${callback.args[0]}.", null)
                    }
                }
            }
        }
    }
}
