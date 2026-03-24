package com.stealthvault.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stealthvault.app.R
import com.stealthvault.app.data.local.SecurityPreferenceManager
import com.stealthvault.app.databinding.DialogChangePinBinding
import com.stealthvault.app.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    @Inject
    lateinit var securityPrefs: SecurityPreferenceManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSettingsBinding.bind(view)

        // ── Appearance ──────────────────────────────────────────────────────────
        binding.swDarkMode.isChecked =
            securityPrefs.themeMode == AppCompatDelegate.MODE_NIGHT_YES

        binding.swDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                       else AppCompatDelegate.MODE_NIGHT_NO
            securityPrefs.themeMode = mode
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        // ── Security ─────────────────────────────────────────────────────────────
        binding.swIntruderSelfie.isChecked = securityPrefs.intruderSelfieEnabled
        binding.swIntruderSelfie.setOnCheckedChangeListener { _, isChecked ->
            securityPrefs.intruderSelfieEnabled = isChecked
        }

        binding.swShakeToHide.isChecked = securityPrefs.shakeToHideEnabled
        binding.swShakeToHide.setOnCheckedChangeListener { _, isChecked ->
            securityPrefs.shakeToHideEnabled = isChecked
        }

        // Auto-lock dropdown
        val lockOptions = resources.getStringArray(R.array.auto_lock_options)
        val lockValues = intArrayOf(0, 1, 5, 10)
        val lockAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, lockOptions)
        binding.actvAutoLock.setAdapter(lockAdapter)

        // Pre-select saved value
        val savedMinutes = securityPrefs.autoLockMinutes
        val savedIndex = lockValues.indexOfFirst { it == savedMinutes }.coerceAtLeast(0)
        binding.actvAutoLock.setText(lockOptions[savedIndex], false)

        binding.actvAutoLock.setOnItemClickListener { _, _, position, _ ->
            securityPrefs.autoLockMinutes = lockValues[position]
        }

        // ── Disguise ─────────────────────────────────────────────────────────────
        binding.btnChangePin.setOnClickListener { showChangePinDialog() }

        binding.btnManageApps.setOnClickListener {
            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(R.id.appLockerFragment)
        }

        binding.btnViewIntruderLogs.setOnClickListener {
            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(R.id.intruderLogsFragment)
        }
    }

    private fun showChangePinDialog() {
        val dialogBinding = DialogChangePinBinding.inflate(LayoutInflater.from(requireContext()))

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.change_pin_title)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.change_pin_save) { _, _ ->
                // Validation is handled before dismiss; dismiss here is a no-op if errors shown
            }
            .create()
            .also { dialog ->
                dialog.show()
                // Override positive button so we can validate without auto-dismissing on error
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener {
                        val current = dialogBinding.etCurrentPin.text?.toString().orEmpty()
                        val newPin = dialogBinding.etNewPin.text?.toString().orEmpty()
                        val confirm = dialogBinding.etConfirmPin.text?.toString().orEmpty()

                        when {
                            current.isEmpty() || newPin.isEmpty() || confirm.isEmpty() -> {
                                Toast.makeText(requireContext(), R.string.change_pin_error_empty, Toast.LENGTH_SHORT).show()
                            }
                            current != securityPrefs.masterPin -> {
                                dialogBinding.tilCurrentPin.error = getString(R.string.change_pin_error_wrong)
                            }
                            newPin != confirm -> {
                                dialogBinding.tilNewPin.error = getString(R.string.change_pin_error_mismatch)
                            }
                            newPin == current -> {
                                dialogBinding.tilNewPin.error = getString(R.string.change_pin_error_same)
                            }
                            else -> {
                                securityPrefs.masterPin = newPin
                                Toast.makeText(requireContext(), R.string.change_pin_success, Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                        }
                    }
            }
    }
}
