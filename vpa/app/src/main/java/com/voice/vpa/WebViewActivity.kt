package com.voice.vpa

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val FILECHOOSER_RESULTCODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        webView = findViewById(R.id.webView1)
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            // For Android 5.0+
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                this@WebViewActivity.filePathCallback?.onReceiveValue(null)
                this@WebViewActivity.filePathCallback = filePathCallback
                val intent = fileChooserParams.createIntent()
                try {
                    startActivityForResult(intent, FILECHOOSER_RESULTCODE)
                } catch (e: Exception) {
                    this@WebViewActivity.filePathCallback = null
                    return false
                }
                return true
            }
        }

        val url = intent.getStringExtra("url")
        webView.loadUrl(url ?: "http://192.168.0.16:8282")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (filePathCallback == null) return
            val result = if (resultCode == Activity.RESULT_OK) {
                data?.data?.let { arrayOf(it) } ?: arrayOf()
            } else {
                null
            }
            filePathCallback?.onReceiveValue(result)
            filePathCallback = null
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}