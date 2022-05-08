package com.iamquan.qrcode.model

data class UrlQR(
    var stringQR: String,
    var title: String,
    var url: String
) : QR(stringQR)