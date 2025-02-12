package com.example.videocallingapp.models

import android.webkit.JavascriptInterface
import com.example.videocallingapp.activites.CallActivity

class InterfaceJava(private val callActivity: CallActivity) {
    @JavascriptInterface
    fun onPeerConnected() {
        callActivity.onPeerConnected()
    }
}