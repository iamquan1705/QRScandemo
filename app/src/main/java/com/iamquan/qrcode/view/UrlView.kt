package com.iamquan.qrcode.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.iamquan.qrcode.databinding.ViewUrlBinding
import com.iamquan.qrcode.databinding.ViewWifiBinding

class UrlView(context: Context) : ConstraintLayout(context) {
    private lateinit var binding: ViewUrlBinding
    init {
        binding = ViewUrlBinding.inflate(LayoutInflater.from(context), this, true)
    }
}