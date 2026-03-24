package com.stealthvault.app.ui.vault.fragments

import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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

    /** Common ad/tracker domains to block. */
    private val adHosts = setOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "ads.google.com",
        "adnxs.com",
        "ads.yahoo.com",
        "advertising.com",
        "moatads.com",
        "outbrain.com",
        "taboola.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "adsrvr.org",
        "amazon-adsystem.com",
        "criteo.com",
        "casalemedia.com",
        "scorecardresearch.com",
        "quantserve.com",
        "adsafeprotected.com"
    )

    private inner class AdBlockingWebViewClient : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            val host = request.url.host?.lowercase() ?: return super.shouldInterceptRequest(view, request)
            if (adHosts.any { host == it || host.endsWith(".$it") }) {
                return WebResourceResponse("text/plain", "utf-8", null)
            }
            return super.shouldInterceptRequest(view, request)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBrowserBinding.bind(view)

        setupWebView()

        binding.etUrl.setOnEditorActionListener { _, _, _ ->
            val url = binding.etUrl.text.toString()
            if (url.isNotEmpty()) {
                val fullUrl = if (url.startsWith("http")) url else "https://www.google.com/search?q=$url"
                binding.webView.loadUrl(fullUrl)
            }
            true
        }
    }

    private fun setupWebView() {
        binding.webView.apply {
            webViewClient = AdBlockingWebViewClient()
            settings.apply {
                javaScriptEnabled = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                domStorageEnabled = true
            }
        }
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
