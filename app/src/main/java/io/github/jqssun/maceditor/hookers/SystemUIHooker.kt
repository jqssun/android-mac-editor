package io.github.jqssun.maceditor.hookers

import android.annotation.SuppressLint
import android.util.ArraySet
import io.github.jqssun.maceditor.BuildConfig
import io.github.jqssun.maceditor.TAG
import io.github.jqssun.maceditor.utils.XposedHelpers
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.annotations.XposedHooker

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
                    .getDeclaredMethod("setTiles"),
                TileSetterHooker::class.java
            )

            module.hook(
                param.defaultClassLoader.loadClass("com.android.systemui.qs.QSTileRevealController\$1")
                    .getDeclaredMethod("run"),
                TileRevealAnimHooker::class.java
            )
        }

        @XposedHooker
        class TileSetterHooker : XposedInterface.Hooker {
            companion object {
                @JvmStatic
                fun before(callback: XposedInterface.BeforeHookCallback) {
                    if (!tileRevealed) {
                        val tileHost = XposedHelpers.getObjectField(callback.thisObject, "mHost")!!
                        val tileHostClass = tileHost.javaClass

                        // handle non-aosp implementation
                        try {
                            tileHostClass.getDeclaredMethod("addTile", Int::class.java, String::class.java)
                                .invoke(tileHost, -1, tileId)
                        } catch (t: Throwable) {
                            tileHostClass.getDeclaredMethod("addTile", String::class.java, Int::class.java)
                                .invoke(tileHost, tileId, -1)
                        }
                        @Suppress("DEPRECATION") module?.log("$TAG: Tile added to quick settings panel.")
                    }
                }
            }
        }

        @XposedHooker
        class TileRevealAnimHooker : XposedInterface.Hooker {
            companion object {
                @JvmStatic
                fun before(callback: XposedInterface.BeforeHookCallback) {
                    if (!tileRevealed) {
                        @Suppress("UNCHECKED_CAST")
                        val tilesToReveal = XposedHelpers.getObjectField(
                            XposedHelpers.getSurroundingThis(callback.thisObject),
                            "mTilesToReveal"
                        ) as ArraySet<String>
                        tilesToReveal.add(tileId)
                        tileRevealed = true
                        @Suppress("DEPRECATION") module?.log("$TAG: Tile quick settings panel animation played. MAC Editor will not hook SystemUI on next reboot.")
                    }
                }
            }
        }
    }
}
