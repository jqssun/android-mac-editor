package io.github.jqssun.maceditor

import android.os.Bundle
import android.text.InputFilter
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.github.jqssun.maceditor.databinding.ActivityMainBinding
import io.github.jqssun.maceditor.utils.MacTextWatcher
import io.github.jqssun.maceditor.utils.MacUtils
import io.github.jqssun.maceditor.utils.PrefManager
import io.github.jqssun.maceditor.utils.XposedChecker

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        PrefManager.loadPrefs()
        _setupStatus()
        _setupMacCard()

        binding.footerNote.text = getString(R.string.footer_note, getString(R.string.force_mac_randomization_label))
    }

    private fun _setupStatus() {
        if (!XposedChecker.isEnabled()) {
            binding.moduleStatus.text = getString(R.string.status_not_activated)
            binding.serviceStatus.text = getString(R.string.status_detail_not_activated)
            return
        }

        binding.hookToggleCard.visibility = View.VISIBLE
        binding.forceRandomizationCard.visibility = View.VISIBLE
        binding.macAddrCard.visibility = View.VISIBLE
        binding.moduleStatus.text = getString(R.string.status_activated)

        binding.hookSwitch.isChecked = PrefManager.isHookOn()
        _updateHookStatusText(binding.hookSwitch.isChecked)

        binding.hookSwitch.setOnCheckedChangeListener { _, checked ->
            PrefManager.toggleHookState()
            _updateHookStatusText(checked)
        }

        binding.forceRandomizationSwitch.isChecked = PrefManager.isForceShowMacRandomization()
        binding.forceRandomizationSwitch.setOnCheckedChangeListener { _, checked ->
            PrefManager.setForceShowMacRandomization(checked)
        }
    }

    private fun _updateHookStatusText(on: Boolean) {
        binding.serviceStatus.text = if (on)
            getString(R.string.status_detail_hook_on)
        else
            getString(R.string.status_detail_hook_off)
    }

    private fun _setupMacCard() {
        val editText = binding.edittextNewMac
        editText.filters = arrayOf(InputFilter.AllCaps(), InputFilter.LengthFilter(17))
        editText.addTextChangedListener(MacTextWatcher())

        val saved = PrefManager.getCustomMac()
        if (saved.isNotEmpty()) {
            binding.textviewCurrentMac.text = saved
            editText.setText(saved)
        } else {
            binding.textviewCurrentMac.text = getString(R.string.mac_not_set)
        }

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
                    Snackbar.make(binding.root, R.string.mac_set_success, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun _showError(msg: String) {
        MaterialAlertDialogBuilder(this)
            .setMessage(msg)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
