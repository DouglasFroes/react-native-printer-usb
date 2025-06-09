package com.usbprinter

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class UsbPermissionHelper(private val context: Context) {
    companion object {
        private const val ACTION_USB_PERMISSION = "com.usbprinter.USB_PERMISSION"
    }

    fun checkAndRequestAllUsbPermissions() {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = usbManager.deviceList.values
        for (device in devices) {
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
                            // Permissão concedida ou negada
                            context?.unregisterReceiver(this)
                        }
                    }
                }, filter)
                usbManager.requestPermission(device, permissionIntent)
            }
        }
    }
}
