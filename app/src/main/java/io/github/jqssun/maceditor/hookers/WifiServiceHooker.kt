package io.github.jqssun.maceditor.hookers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.MacAddress
import android.util.Log
import io.github.jqssun.maceditor.BuildConfig
import io.github.jqssun.maceditor.TAG
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import java.lang.reflect.Method

class WifiServiceHooker {
    companion object {
        var module: XposedModule? = null
            private set

        const val ACTION_APPLY_MAC = "${BuildConfig.APPLICATION_ID}.ACTION_APPLY_MAC"
        const val ACTION_MAC_DETECTED = "${BuildConfig.APPLICATION_ID}.ACTION_MAC_DETECTED"
        private const val RECEIVER_CLASS = "${BuildConfig.APPLICATION_ID}.MacBroadcastReceiver"

        // cached HAL state
        private var halInstance: Any? = null
        private var halMethod: Method? = null
        private var lastIfaceName: String? = null
        private var receiverRegistered = false

        @SuppressLint("PrivateApi")
        fun hook(param: SystemServerStartingParam, module: XposedModule) {
            this.module = module
            module.hook(
                param.classLoader.loadClass("com.android.server.SystemServiceManager")
                    .getDeclaredMethod("loadClassFromLoader", String::class.java, ClassLoader::class.java)
            ).intercept { chain ->
                val result = chain.proceed()
                val className = chain.getArg(0) as String
                if (className == "com.android.server.wifi.WifiService") {
                    val cl = chain.getArg(1) as ClassLoader
                    val halClass = cl.loadClass("com.android.server.wifi.WifiVendorHal")
                    val setStaMethod = halClass.getDeclaredMethod("setStaMacAddress", String::class.java, MacAddress::class.java)
                    val setApMethod = halClass.getDeclaredMethod("setApMacAddress", String::class.java, MacAddress::class.java)
                    halMethod = setStaMethod
                    val hooker = MacAddrHooker()
                    module.hook(setStaMethod).intercept(hooker)
                    module.hook(setApMethod).intercept(hooker)
                }
                result
            }
        }

        @SuppressLint("PrivateApi")
        private fun _getSystemContext(): Context? {
            return try {
                val at = Class.forName("android.app.ActivityThread")
                at.getMethod("currentApplication").invoke(null) as? Context
            } catch (_: Exception) {
                null
            }
        }

        private fun _registerApplyReceiver() {
            if (receiverRegistered) return
            val ctx = _getSystemContext() ?: return
            ctx.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    _applyMacDirectly()
                }
            }, IntentFilter(ACTION_APPLY_MAC), Context.RECEIVER_EXPORTED)
            receiverRegistered = true
            module?.log(Log.INFO, TAG, "Registered apply-MAC receiver in system_server")
        }

        private fun _applyMacDirectly() {
            val hal = halInstance
            val method = halMethod
            val iface = lastIfaceName
            if (hal == null || method == null || iface == null) {
                module?.log(Log.WARN, TAG, "Cannot apply MAC: HAL not cached yet")
                return
            }
            val prefs = module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
            val mac = prefs?.getString("customMac", "") ?: ""
            if (mac.isEmpty()) return

            try {
                method.invoke(hal, iface, MacAddress.fromString(mac))
                module?.log(Log.INFO, TAG, "Directly applied MAC: $mac on $iface")
            } catch (e: Exception) {
                module?.log(Log.ERROR, TAG, "Failed to directly apply MAC: $e")
            }
        }

        private fun _broadcastDeviceMac(mac: MacAddress) {
            try {
                val ctx = _getSystemContext() ?: return
                val intent = Intent(ACTION_MAC_DETECTED).apply {
                    putExtra("mac", mac.toString())
                    setClassName(BuildConfig.APPLICATION_ID, RECEIVER_CLASS)
                }
                ctx.sendBroadcast(intent)
            } catch (e: Exception) {
                module?.log(Log.WARN, TAG, "Could not broadcast MAC: $e")
            }
        }

        class MacAddrHooker : XposedInterface.Hooker {
            override fun intercept(chain: XposedInterface.Chain): Any? {
                val prefs = module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
                val hookActive = prefs?.getBoolean("hookActive", false) ?: false

                if (!hookActive) return chain.proceed()

                // cache HAL instance and iface
                halInstance = chain.thisObject
                lastIfaceName = chain.getArg(0) as? String
                _registerApplyReceiver()

                // broadcast the system-assigned MAC to the app
                (chain.getArg(1) as? MacAddress)?.let { _broadcastDeviceMac(it) }

                val customMac = prefs?.getString("customMac", "") ?: ""
                if (customMac.isNotEmpty()) {
                    val args = chain.args.toTypedArray()
                    args[1] = MacAddress.fromString(customMac)
                    module?.log(Log.INFO, TAG, "Replacing MAC with $customMac on ${chain.getArg(0)}")
                    return chain.proceed(args)
                }

                return chain.proceed()
            }
        }
    }
}
