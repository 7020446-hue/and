package com.stealthvault.app.ui.fake

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.stealthvault.app.data.local.SecurityPreferenceManager
import com.stealthvault.app.databinding.ActivityCalculatorBinding
import com.stealthvault.app.ui.vault.VaultActivity
import com.stealthvault.app.utils.CameraHelper
import dagger.hilt.android.AndroidEntryPoint
import net.objecthunter.exp4j.ExpressionBuilder
import java.util.concurrent.Executor
import javax.inject.Inject

@AndroidEntryPoint
class CalculatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalculatorBinding
    private var currentInput = ""
    
    @Inject
    lateinit var securityPrefs: SecurityPreferenceManager
    @Inject
    lateinit var cameraHelper: CameraHelper

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            binding = ActivityCalculatorBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupBiometric()
            setupButtons()
            
            // Secret trigger: Long press history to use Biometrics
            binding.tvHistory.setOnLongClickListener {
                if (securityPrefs.isSetupComplete) {
                    biometricPrompt.authenticate(promptInfo)
                } else {
                    Toast.makeText(this, "Set Master PIN first", Toast.LENGTH_SHORT).show()
                }
                true
            }
        } catch (t: Throwable) {
            val tv = android.widget.TextView(this).apply {
                text = "CRASH: " + android.util.Log.getStackTraceString(t)
                setTextColor(android.graphics.Color.RED)
                textSize = 14f
                setPadding(32, 32, 32, 32)
            }
            // Remove previous content view and show crash
            val scrollView = android.widget.ScrollView(this).apply {
                addView(tv)
                setBackgroundColor(android.graphics.Color.BLACK)
            }
            setContentView(scrollView)
        }
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    launchVault(false)
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Vault Authentication")
            .setSubtitle("Authenticate to access your secure vault")
            .setNegativeButtonText("Use PIN")
            .build()
    }

    private fun setupButtons() {
        val allButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9,
            binding.btnDot, binding.btnAdd, binding.btnSub, binding.btnMul, binding.btnDiv
        )

        allButtons.forEach { button ->
            button.setOnClickListener {
                currentInput += (it as Button).text
                binding.tvDisplay.text = currentInput
            }
        }

        binding.btnClear.setOnClickListener {
            currentInput = ""
            binding.tvDisplay.text = "0"
            binding.tvHistory.text = ""
        }

        binding.btnEquals.setOnClickListener {
            checkUnlock()
        }

        // Scientific / special buttons
        binding.btnSin.setOnClickListener { appendFunction("sin(") }
        binding.btnCos.setOnClickListener { appendFunction("cos(") }
        binding.btnTan.setOnClickListener { appendFunction("tan(") }
        // "log" on a physical calculator means base-10 log
        binding.btnLog.setOnClickListener { appendFunction("log(") }

        binding.btnPlusMinus.setOnClickListener {
            if (currentInput.isNotEmpty()) {
                currentInput = if (currentInput.startsWith("-")) {
                    currentInput.removePrefix("-")
                } else {
                    "-$currentInput"
                }
                binding.tvDisplay.text = currentInput
            }
        }

        binding.btnPercent.setOnClickListener {
            if (currentInput.isNotEmpty()) {
                try {
                    val result = evalExpression(currentInput) / 100.0
                    currentInput = result.toCleanString()
                    binding.tvDisplay.text = currentInput
                } catch (_: Exception) { /* ignore invalid input */ }
            }
        }
    }

    /** Appends a function opener (e.g. "sin(") and refreshes the display. */
    private fun appendFunction(fn: String) {
        currentInput += fn
        binding.tvDisplay.text = currentInput
    }

    private fun checkUnlock() {
        if (!securityPrefs.isSetupComplete) {
            setupPins()
            return
        }

        val master = securityPrefs.masterPin
        val decoy = securityPrefs.decoyPin

        when (currentInput) {
            master -> launchVault(isDecoy = false)
            decoy -> launchVault(isDecoy = true)
            else -> performMath()
        }
        currentInput = ""
    }

    private fun setupPins() {
        val input = currentInput
        if (input.isEmpty()) return

        if (securityPrefs.masterPin == null) {
            securityPrefs.masterPin = input
            Toast.makeText(this, "Master PIN set. Now enter a DECOY PIN and press '='", Toast.LENGTH_LONG).show()
        } else if (securityPrefs.decoyPin == null) {
            if (input == securityPrefs.masterPin) {
                Toast.makeText(this, "Decoy PIN must be different from Master PIN", Toast.LENGTH_SHORT).show()
            } else {
                securityPrefs.decoyPin = input
                securityPrefs.isSetupComplete = true
                Toast.makeText(this, "Setup complete! Enter Master PIN to unlock vault.", Toast.LENGTH_LONG).show()
            }
        }
        currentInput = ""
        binding.tvDisplay.text = "0"
        binding.tvHistory.text = ""
    }

    private fun performMath() {
        try {
            val result = evalExpression(currentInput)
            val resultStr = result.toCleanString()
            binding.tvHistory.text = currentInput
            binding.tvDisplay.text = resultStr
            currentInput = resultStr
        } catch (e: Exception) {
            binding.tvDisplay.text = "Error"
            if (securityPrefs.intruderSelfieEnabled) {
                cameraHelper.takeIntruderPhoto(this)
            }
        }
    }

    /**
     * Evaluates a calculator expression string.
     * Normalises operator symbols and maps the "log" button to base-10 logarithm.
     */
    private fun evalExpression(input: String): Double {
        val expr = input
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            // "log(" from the button → base-10 log; don't touch "log2(" / "log10("
            .replace(Regex("\\blog(?!10|2)\\("), "log10(")
        return ExpressionBuilder(expr).build().evaluate()
    }

    /** Formats a Double result: removes trailing ".0" for whole numbers. */
    private fun Double.toCleanString(): String {
        if (isNaN() || isInfinite()) return "Error"
        return if (this == kotlin.math.floor(this) &&
            this in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()
        ) {
            toLong().toString()
        } else {
            toBigDecimal().stripTrailingZeros().toPlainString()
        }
    }

    private fun launchVault(isDecoy: Boolean) {
        val intent = Intent(this, VaultActivity::class.java).apply {
            putExtra("IS_DECOY", isDecoy)
        }
        startActivity(intent)
        finish()
    }
}
