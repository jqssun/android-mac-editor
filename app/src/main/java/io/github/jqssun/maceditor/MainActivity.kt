package io.github.jqssun.maceditor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.github.jqssun.maceditor.databinding.ActivityMainBinding
import io.github.jqssun.maceditor.hookers.WifiServiceHooker
import io.github.jqssun.maceditor.utils.MacTextWatcher
import io.github.jqssun.maceditor.utils.MacUtils
import io.github.jqssun.maceditor.utils.PrefManager
import io.github.jqssun.maceditor.utils.XposedChecker

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var updatingUI = false

    private val macReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            _refreshDeviceMac()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        _setupToggles()
        _setupMacCard()
        binding.footerNote.text = getString(R.string.footer_note, getString(R.string.force_mac_randomization_label))

        PrefManager.loadPrefs { runOnUiThread { _refreshAll() } }
        _refreshAll()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(macReceiver, IntentFilter(MacBroadcastReceiver.ACTION_MAC_DETECTED), RECEIVER_NOT_EXPORTED)
        _refreshAll()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(macReceiver)
    }

    private fun _refreshAll() {
        updatingUI = true
        _updateStatusCard()
        _refreshDeviceMac()
        _refreshActiveMac()
        binding.hookSwitch.isChecked = PrefManager.isHookOn()
        binding.forceRandomizationSwitch.isChecked = PrefManager.isForceShowMacRandomization()
        val saved = PrefManager.getCustomMac()
        if (saved.isNotEmpty() && binding.edittextNewMac.text.isNullOrEmpty()) {
            binding.edittextNewMac.setText(saved)
        }
        updatingUI = false
    }

    private fun _updateStatusCard() {
        val enabled = XposedChecker.isEnabled()
        val hookOn = enabled && PrefManager.isHookOn()

        when {
            !enabled -> {
                binding.moduleStatusIcon.setImageResource(R.drawable.ic_disabled_24)
                binding.moduleStatus.text = getString(R.string.status_not_activated)
                binding.serviceStatus.text = getString(R.string.status_detail_not_activated)
            }
            !hookOn -> {
                binding.moduleStatusIcon.setImageResource(R.drawable.ic_warning_24)
                binding.moduleStatus.text = getString(R.string.status_activated)
                binding.serviceStatus.text = getString(R.string.status_detail_hook_off)
            }
            else -> {
                binding.moduleStatusIcon.setImageResource(R.drawable.ic_baseline_router_24)
                binding.moduleStatus.text = getString(R.string.status_activated)
                binding.serviceStatus.text = getString(R.string.status_detail_hook_on)
            }
        }
    }

    private fun _setupToggles() {
        binding.hookSwitch.setOnCheckedChangeListener { _, checked ->
            if (updatingUI) return@setOnCheckedChangeListener
            PrefManager.setHookState(checked)
            _updateStatusCard()
        }
        binding.forceRandomizationSwitch.setOnCheckedChangeListener { _, checked ->
            if (updatingUI) return@setOnCheckedChangeListener
            PrefManager.setForceShowMacRandomization(checked)
        }
    }

    private fun _refreshDeviceMac() {
        val localPrefs = getSharedPreferences(MacBroadcastReceiver.PREFS_NAME, MODE_PRIVATE)
        val mac = localPrefs.getString("deviceMac", null)
        binding.textviewDeviceMac.text = mac ?: getString(R.string.mac_not_set)
    }

    private fun _refreshActiveMac() {
        val saved = PrefManager.getCustomMac()
        binding.textviewCurrentMac.text = saved.ifEmpty { getString(R.string.mac_not_set) }
    }

    private fun _setupMacCard() {
        val editText = binding.edittextNewMac
        editText.filters = arrayOf(InputFilter.AllCaps(), InputFilter.LengthFilter(17))
        editText.addTextChangedListener(MacTextWatcher())

        binding.btnGenerateMac.setOnClickListener {
            editText.setText(MacUtils.generateRandom())
        }

        binding.btnSetMac.setOnClickListener {
            val mac = editText.text.toString().uppercase()
            when (MacUtils.validate(mac)) {
                MacUtils.ValidationResult.BAD_LENGTH ->
                    _showError(getString(R.string.error_bad_length))
                MacUtils.ValidationResult.ALL_ZEROS ->
                    _showError(getString(R.string.error_all_zeros))
                MacUtils.ValidationResult.ODD_FIRST_OCTET ->
                    _showError(getString(R.string.error_odd_first_octet))
                MacUtils.ValidationResult.VALID -> {
                    PrefManager.setCustomMac(mac)
                    binding.textviewCurrentMac.text = mac
                    _applyMac()
                }
            }
        }
    }

    private fun _applyMac() {
        sendBroadcast(Intent(WifiServiceHooker.ACTION_APPLY_MAC))
        Snackbar.make(binding.root, R.string.mac_set_success, Snackbar.LENGTH_LONG)
            .setAction(R.string.open_wifi_settings) {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
            .show()
    }

    private fun _showError(msg: String) {
        MaterialAlertDialogBuilder(this)
            .setMessage(msg)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
