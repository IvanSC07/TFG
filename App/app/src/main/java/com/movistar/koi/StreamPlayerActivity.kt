package com.movistar.koi

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.movistar.koi.databinding.ActivityStreamPlayerBinding

class StreamPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStreamPlayerBinding

    companion object {
        const val EXTRA_STREAM_URL = "stream_url"
        const val EXTRA_PLATFORM = "platform"
        const val EXTRA_STREAM_TITLE = "stream_title"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStreamPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL) ?: ""
        val platform = intent.getStringExtra(EXTRA_PLATFORM) ?: ""
        val streamTitle = intent.getStringExtra(EXTRA_STREAM_TITLE) ?: "Stream"

        // Configurar toolbar
        binding.toolbar.title = streamTitle
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        setupWebView(streamUrl, platform)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(streamUrl: String, platform: String) {
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowContentAccess = true
            settings.allowFileAccess = true

            // Mejorar rendimiento para video
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    if (newProgress == 100) {
                        binding.progressBar.visibility = android.view.View.GONE
                    } else {
                        binding.progressBar.visibility = android.view.View.VISIBLE
                    }
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    showError("Error cargando el stream: $description")
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    url: String?
                ): Boolean {
                    // Mantener todas las navegaciones dentro del WebView
                    return false
                }
            }

            // Cargar stream optimizado según plataforma
            val optimizedUrl = getOptimizedStreamUrl(streamUrl, platform)
            loadUrl(optimizedUrl)
        }
    }

    private fun getOptimizedStreamUrl(originalUrl: String, platform: String): String {
        return when (platform.lowercase()) {
            "twitch" -> {
                val channelName = extractTwitchChannel(originalUrl)
                if (channelName != null) {
                    // Usar el embed de Twitch
                    "https://player.twitch.tv/?channel=$channelName&parent=localhost&autoplay=true"
                } else {
                    originalUrl
                }
            }
            "youtube" -> {
                val videoId = extractYouTubeVideoId(originalUrl)
                if (videoId != null) {
                    // Usar el embed de YouTube
                    "https://www.youtube.com/embed/$videoId?autoplay=1&rel=0"
                } else {
                    originalUrl
                }
            }
            else -> originalUrl
        }
    }

    private fun extractTwitchChannel(url: String): String? {
        val patterns = arrayOf(
            "twitch\\.tv/([a-zA-Z0-9_]+)",
            "https://(?:www\\.)?twitch\\.tv/([a-zA-Z0-9_]+)"
        )

        patterns.forEach { pattern ->
            val regex = pattern.toRegex()
            val match = regex.find(url)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun extractYouTubeVideoId(url: String): String? {
        val patterns = arrayOf(
            "youtu\\.be/([a-zA-Z0-9_-]+)",
            "youtube\\.com/watch\\?v=([a-zA-Z0-9_-]+)",
            "youtube\\.com/embed/([a-zA-Z0-9_-]+)"
        )

        patterns.forEach { pattern ->
            val regex = pattern.toRegex()
            val match = regex.find(url)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = android.view.View.GONE
        binding.errorText.text = message
        binding.errorText.visibility = android.view.View.VISIBLE
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
    }
}