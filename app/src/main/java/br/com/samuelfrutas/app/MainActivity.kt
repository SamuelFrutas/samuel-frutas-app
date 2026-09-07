package br.com.samuelfrutas.app

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.FrameLayout
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(244, 246, 245)) }
        webView = WebView(this)
        progress = ProgressBar(this).apply { visibility = View.VISIBLE }
        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        val progressParams = FrameLayout.LayoutParams(72, 72)
        progressParams.gravity = android.view.Gravity.CENTER
        root.addView(progress, progressParams)
        setContentView(root)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadsImagesAutomatically = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.allowFileAccess = false
        settings.allowContentAccess = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(false)
        settings.userAgentString = settings.userAgentString + " SamuelFrutasAndroid/1.0"

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView, url: String) {
                progress.visibility = View.GONE
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                if (request.isForMainFrame) {
                    progress.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Não foi possível carregar o Painel de Pedidos. Verifique sua internet.", Toast.LENGTH_LONG).show()
                }
            }
        }
        webView.webChromeClient = object : WebChromeClient() {}

        if (savedInstanceState == null) {
            webView.loadUrl("https://samuelfrutas.github.io/samuel-frutas/pedidos/admin")
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        }
        super.onDestroy()
    }
}
