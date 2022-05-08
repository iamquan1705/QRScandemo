package com.iamquan.qrcode.model

class WifiQR(
    private var stringQr: String,
    private var ssid: String,
    private var password: String,
    private var type: Int
) : QR(stringQr) {
}
