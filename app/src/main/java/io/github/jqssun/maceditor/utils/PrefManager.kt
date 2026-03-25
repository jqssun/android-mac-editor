package io.github.jqssun.maceditor.utils

import android.content.SharedPreferences
import io.github.jqssun.maceditor.BuildConfig
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class PrefManager {
    companion object {
        private var prefs: SharedPreferences? = null
        private var loaded = false

        fun loadPrefs(onReady: (() -> Unit)? = null) {
            if (loaded) {
                if (XposedChecker.isEnabled()) onReady?.invoke()
                return
            }
            loaded = true
            XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
                override fun onServiceBind(service: XposedService) {
                    XposedChecker.flagAsEnabled()
                    prefs = service.getRemotePreferences(BuildConfig.APPLICATION_ID)
                    _markTileRevealAsDone()
                    onReady?.invoke()
                }

                override fun onServiceDied(service: XposedService) {}
            })
        }

        fun isHookOn(): Boolean {
            return prefs?.getBoolean("hookActive", false) ?: false
        }

        fun setHookState(on: Boolean) {
            prefs?.edit()?.putBoolean("hookActive", on)?.apply()
        }

        fun getCustomMac(): String {
            return prefs?.getString("customMac", "") ?: ""
        }

        fun setCustomMac(mac: String) {
            val version = (prefs?.getLong("macVersion", 0L) ?: 0L) + 1
            prefs?.edit()
                ?.putString("customMac", mac)
                ?.putLong("macVersion", version)
                ?.apply()
        }

        fun isForceShowMacRandomization(): Boolean {
            return prefs?.getBoolean("forceShowMacRandomization", false) ?: false
        }

        fun setForceShowMacRandomization(on: Boolean) {
            prefs?.edit()?.putBoolean("forceShowMacRandomization", on)?.apply()
        }

        private fun _markTileRevealAsDone() {
            prefs?.edit()?.putBoolean("tileRevealDone", true)?.apply()
        }
    }
}
