package io.github.jqssun.maceditor.hookers

import android.annotation.SuppressLint
import android.util.ArraySet
import android.util.Log
import io.github.jqssun.maceditor.BuildConfig
import io.github.jqssun.maceditor.TAG
import io.github.jqssun.maceditor.utils.XposedHelpers
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

class SystemUIHooker {
    companion object {
        var module: XposedModule? = null
            private set
        private const val tileId = "custom(${BuildConfig.APPLICATION_ID}/.QuickTile)"
        private var tileRevealed = false

        @SuppressLint("PrivateApi")
        fun hook(param: PackageLoadedParam, module: XposedModule) {
            this.module = module

            module.hook(
                param.defaultClassLoader.loadClass("com.android.systemui.qs.QSPanelControllerBase")
                    .getDeclaredMethod("setTiles")
            ).intercept(TileSetterHooker())

            module.hook(
                param.defaultClassLoader.loadClass("com.android.systemui.qs.QSTileRevealController\$1")
                    .getDeclaredMethod("run")
            ).intercept(TileRevealAnimHooker())
        }

        class TileSetterHooker : XposedInterface.Hooker {
            override fun intercept(chain: XposedInterface.Chain): Any? {
                if (!tileRevealed) {
                    val tileHost = XposedHelpers.getObjectField(chain.thisObject, "mHost")!!
                    val tileHostClass = tileHost.javaClass
                    try {
                        tileHostClass.getDeclaredMethod("addTile", Int::class.java, String::class.java)
                            .invoke(tileHost, -1, tileId)
                    } catch (_: Throwable) {
                        tileHostClass.getDeclaredMethod("addTile", String::class.java, Int::class.java)
                            .invoke(tileHost, tileId, -1)
                    }
                    module?.log(Log.INFO, TAG, "Tile added to quick settings panel.")
                }
                return chain.proceed()
            }
        }

        class TileRevealAnimHooker : XposedInterface.Hooker {
            override fun intercept(chain: XposedInterface.Chain): Any? {
                if (!tileRevealed) {
                    @Suppress("UNCHECKED_CAST")
                    val tilesToReveal = XposedHelpers.getObjectField(
                        XposedHelpers.getSurroundingThis(chain.thisObject),
                        "mTilesToReveal"
                    ) as ArraySet<String>
                    tilesToReveal.add(tileId)
                    tileRevealed = true
                    module?.log(Log.INFO, TAG, "Tile quick settings panel animation played. MAC Editor will not hook SystemUI on next reboot.")
                }
                return chain.proceed()
            }
        }
    }
}
