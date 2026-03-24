package com.stealthvault.app.ui.vault.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.stealthvault.app.R
import com.stealthvault.app.databinding.FragmentBrowserBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BrowserFragment : Fragment(R.layout.fragment_browser) {

    private var _binding: FragmentBrowserBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBrowserBinding.bind(view)

        setupWebView()

        binding.etUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                navigateTo(binding.etUrl.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
    }

    private fun navigateTo(input: String) {
        if (input.isBlank()) return
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> "https://www.google.com/search?q=${android.net.Uri.encode(input)}"
        }
        binding.webView.loadUrl(url)
        dismissKeyboard()
    }

    private fun setupWebView() {
        binding.webView.apply {
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    // Reflect the final URL in the address bar (handles redirects)
                    binding.etUrl.setText(url)
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    binding.progressBar.progress = newProgress
                    binding.progressBar.visibility =
                        if (newProgress < 100) View.VISIBLE else View.GONE
                }
            }

            settings.apply {
                javaScriptEnabled = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                domStorageEnabled = true
            }
        }
    }

    /**
     * Called by the hosting activity when the user presses Back.
     * @return `true` if the WebView consumed the event (navigated to the previous page),
     *         `false` if the activity should handle it instead.
     */
    fun onBackPressed(): Boolean {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
            return true
        }
        return false
    }

    private fun dismissKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etUrl.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Purge everything for ultimate stealth
        binding.webView.apply {
            clearHistory()
            clearCache(true)
            clearFormData()
        }
        _binding = null
    }
}
