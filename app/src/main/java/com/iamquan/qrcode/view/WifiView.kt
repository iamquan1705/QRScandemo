package com.iamquan.qrcode.view

import android.content.Context
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.iamquan.qrcode.databinding.ViewWifiBinding

class WifiView(context: Context) : ConstraintLayout(context) {
    private lateinit var binding: ViewWifiBinding

    init {
        binding = ViewWifiBinding.inflate(LayoutInflater.from(context), this, true)
    }
}