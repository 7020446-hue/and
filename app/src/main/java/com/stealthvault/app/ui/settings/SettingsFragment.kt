package com.stealthvault.app.ui.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.stealthvault.app.R
import com.stealthvault.app.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import android.widget.Toast
import javax.inject.Inject
import com.stealthvault.app.data.local.SecurityPreferenceManager

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    @Inject
    lateinit var securityPrefs: SecurityPreferenceManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSettingsBinding.bind(view)

        // Initialize Dark Mode toggle based on saved preference
        binding.swDarkMode.isChecked =
            securityPrefs.themeMode == AppCompatDelegate.MODE_NIGHT_YES

        binding.swDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                       else AppCompatDelegate.MODE_NIGHT_NO
            securityPrefs.themeMode = mode
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        binding.btnChangePin.setOnClickListener {
            // Placeholder: Reset setup so user can set new PIN via calculator
            securityPrefs.isSetupComplete = false
            Toast.makeText(requireContext(), "Please re-open the app to set a new PIN.", Toast.LENGTH_LONG).show()
        }

        binding.btnManageApps.setOnClickListener {
            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(R.id.appLockerFragment)
        }

        binding.btnViewIntruderLogs.setOnClickListener {

            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(R.id.intruderLogsFragment)
        }
    }
}


