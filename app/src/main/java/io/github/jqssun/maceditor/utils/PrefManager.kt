package io.github.jqssun.maceditor.utils

import android.content.SharedPreferences
import io.github.jqssun.maceditor.BuildConfig
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class PrefManager {
    companion object {
        private var prefs: SharedPreferences? = null

        fun loadPrefs() {
            XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
                override fun onServiceBind(service: XposedService) {
                    XposedChecker.flagAsEnabled()
                    prefs = service.getRemotePreferences(BuildConfig.APPLICATION_ID)
                    _markTileRevealAsDone()
                }

                override fun onServiceDied(service: XposedService) {}
            })
        }

        fun isHookOn(): Boolean {
            if (!XposedChecker.isEnabled()) return false
            return prefs?.getBoolean("hookActive", false) ?: false
        }

        fun toggleHookState() {
            val p = prefs ?: return
            if (!XposedChecker.isEnabled()) return
            p.edit().putBoolean("hookActive", !isHookOn()).apply()
        }

        fun getCustomMac(): String {
            return prefs?.getString("customMac", "") ?: ""
        }

        fun setCustomMac(mac: String) {
            prefs?.edit()?.putString("customMac", mac)?.apply()
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
