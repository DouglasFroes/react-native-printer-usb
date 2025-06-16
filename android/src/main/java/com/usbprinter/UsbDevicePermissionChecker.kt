package com.usbprinter

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.util.Log

object UsbDevicePermissionChecker {
    private const val ACTION_USB_PERMISSION = "com.usbprinter.USB_PERMISSION"
    private const val TAG = "UsbDevicePermissionChecker"

    fun checkDeviceAndPermission(context: Context, productId: Int): UsbDevice? {
        val devices = UsbDeviceHelper.getConnectedUsbDevices(context)
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val device = devices.firstOrNull { it.productId == productId }
        if (device == null) {
            Log.w(TAG, "Device with productId $productId not found")
            return null
        }

        Log.d(TAG, "Found device: ${device.deviceName}, vendorId: ${device.vendorId}, productId: ${device.productId}")

        if (!usbManager.hasPermission(device)) {
            Log.d(TAG, "Requesting permission for device")
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
                        Log.d(TAG, "Permission response received")
                    }
                }
            }, filter)
            usbManager.requestPermission(device, permissionIntent)
            return null
        }

        // Verifica se o dispositivo é acessível
        if (isDeviceAccessible(context, device)) {
            Log.d(TAG, "Device is accessible and ready")
            return device
        } else {
            Log.w(TAG, "Device has permission but is not accessible")
            return null
        }
    }

    private fun isDeviceAccessible(context: Context, device: UsbDevice): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        var connection: UsbDeviceConnection? = null

        try {
            connection = usbManager.openDevice(device)
            if (connection == null) {
                Log.w(TAG, "Failed to open device connection")
                return false
            }

            // Verifica se há interfaces disponíveis
            if (device.interfaceCount == 0) {
                Log.w(TAG, "Device has no interfaces")
                return false
            }

            // Tenta encontrar uma interface com endpoint OUT para impressão
            for (interfaceIndex in 0 until device.interfaceCount) {
                val usbInterface = device.getInterface(interfaceIndex)
                Log.d(TAG, "Checking interface $interfaceIndex with ${usbInterface.endpointCount} endpoints")

                for (endpointIndex in 0 until usbInterface.endpointCount) {
                    val endpoint = usbInterface.getEndpoint(endpointIndex)
                    if (endpoint.direction == android.hardware.usb.UsbConstants.USB_DIR_OUT) {
                        Log.d(TAG, "Found OUT endpoint - device should work for printing")

                        // Testa se consegue fazer claim da interface
                        if (connection.claimInterface(usbInterface, true)) {
                            connection.releaseInterface(usbInterface)
                            return true
                        }
                    }
                }
            }

            Log.w(TAG, "No suitable interface/endpoint found for printing")
            return false

        } catch (e: Exception) {
            Log.e(TAG, "Error checking device accessibility", e)
            return false
        } finally {
            try {
                connection?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing test connection", e)
            }
        }
    }
}
