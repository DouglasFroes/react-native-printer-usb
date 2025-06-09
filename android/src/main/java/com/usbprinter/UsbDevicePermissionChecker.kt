package com.usbprinter

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class UsbDevicePermissionChecker(private val context: Context) {
    companion object {
        private const val ACTION_USB_PERMISSION = "com.usbprinter.USB_PERMISSION"
    }

    fun checkDeviceAndPermission(productId: Int): UsbDevice? {
        val helper = UsbDeviceHelper(context)
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val device = helper.getConnectedUsbDevices().firstOrNull { it.getProductId() == productId }
        if (device == null) return null
        if (!usbManager.hasPermission(device)) {
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
            )
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == ACTION_USB_PERMISSION) {
                        context?.unregisterReceiver(this)
                    }
                }
            }, filter)
            usbManager.requestPermission(device, permissionIntent)
            return null
        }
        return device
    }
}
