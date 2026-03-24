package io.github.jqssun.maceditor

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.jqssun.maceditor.utils.PrefManager

class QuickTile: TileService() {
    override fun onCreate() {
        super.onCreate()
        PrefManager.loadPrefs()
    }

    override fun onStartListening() {
        super.onStartListening()
        setButtonState()
    }

    override fun onClick() {
        super.onClick()
        PrefManager.toggleHookState()
        setButtonState()
    }

    private fun setButtonState() {
        if (PrefManager.isHookOn())
            qsTile.state = Tile.STATE_ACTIVE
        else
            qsTile.state = Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
}
