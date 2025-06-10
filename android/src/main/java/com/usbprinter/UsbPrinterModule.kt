package com.usbprinter

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule

import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableNativeArray
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap

import android.hardware.usb.UsbDevice
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbDeviceConnection

import com.usbprinter.UsbDeviceHelper
import com.usbprinter.UsbDevicePermissionChecker
import com.usbprinter.UsbPrinterTextHelper
import com.usbprinter.UsbPrinterCutHelper
import com.usbprinter.UsbPrinterBarcodeHelper
import com.usbprinter.UsbPrinterQrCodeHelper
import com.usbprinter.UsbPrinterCleanHelper
import com.usbprinter.UsbPrinterOffHelper
import com.usbprinter.UsbPrinterRawHelper
import com.usbprinter.UsbPrinterImageHelper
import com.usbprinter.UsbPrinterHtmlHelper

@ReactModule(name = UsbPrinterModule.NAME)
class UsbPrinterModule(reactContext: ReactApplicationContext) :
  NativeUsbPrinterSpec(reactContext) {

  override fun getName(): String {
    return NAME
  }

  override fun getList(): WritableArray{
    val devices = UsbDeviceHelper.getConnectedUsbDevices(reactApplicationContext)
    val array = WritableNativeArray()
    for (device: UsbDevice in devices) {
      val map = WritableNativeMap()

      map.putString("deviceName", device.getDeviceName())
      map.putInt("deviceId", device.getDeviceId())
      map.putInt("vendorId", device.getVendorId())
      map.putInt("productId", device.getProductId())
      map.putString("manufacturerName", device.getManufacturerName())
      map.putString("productName", device.getProductName())

      array.pushMap(map)
    }

    return array
  }

  private fun getCheckedDevice(productId: Double): UsbDevice? {
    return UsbDevicePermissionChecker.checkDeviceAndPermission(reactApplicationContext, productId.toInt())
  }

  private fun getDeviceErrorMessage(): WritableMap {
    val result = Arguments.createMap()
    result.putBoolean("success", false)
    result.putString("message", "Dispositivo não encontrado ou permissão não concedida. Se solicitado, conceda permissão e tente novamente.")
    return result
  }

  override fun printText(options: ReadableMap, promise: Promise) {
    val productId = options.getDouble("productId")
    val device = getCheckedDevice(productId)
    if (device == null) {
      promise.resolve(getDeviceErrorMessage())
      return
    }
    val result = UsbPrinterTextHelper.printText(reactApplicationContext, options, device)
    promise.resolve(result)
  }

  override fun printCut(tailingLine: Boolean, beep: Boolean, productId: Double, promise: Promise) {
    val device = getCheckedDevice(productId)
    if (device == null) {
      promise.resolve(getDeviceErrorMessage())
      return
    }
    val result = UsbPrinterCutHelper.printCut(reactApplicationContext, tailingLine, beep, device)
    promise.resolve(result)
  }

  override fun barCode(options: ReadableMap, promise: Promise) {
    val productId = options.getDouble("productId")
    val device = getCheckedDevice(productId)
    if (device == null) {
      promise.resolve(getDeviceErrorMessage())
      return
    }
    val result = UsbPrinterBarcodeHelper.printBarcode(reactApplicationContext, options, device)
    promise.resolve(result)
  }

  override fun qrCode(options: ReadableMap, promise: Promise) {
    val productId = options.getDouble("productId")
    val device = getCheckedDevice(productId)
    if (device == null) {
      promise.resolve(getDeviceErrorMessage())
      return
    }
    val result = UsbPrinterQrCodeHelper.printQrCode(reactApplicationContext, options, device)
    promise.resolve(result)
  }

  override fun clean(productId: Double, promise: Promise) {
    val device = getCheckedDevice(productId)
    if (device == null) {
      promise.resolve(getDeviceErrorMessage())
      return
    }
    val result = UsbPrinterCleanHelper.clean(reactApplicationContext, device)
    promise.resolve(result)
  }

  override fun off(productId: Double, promise: Promise) {
    val device = getCheckedDevice(productId)
    if (device == null) {
      promise.resolve(getDeviceErrorMessage())
      return
    }
    val result = UsbPrinterOffHelper.off(reactApplicationContext, device)
    promise.resolve(result)
  }

  override fun sendRawData(base64Data: String, productId: Double, promise: Promise) {
    val device = getCheckedDevice(productId)
    if (device == null) {
      promise.resolve(getDeviceErrorMessage())
      return
    }
    val result = UsbPrinterRawHelper.sendRawData(reactApplicationContext, base64Data, device)
    promise.resolve(result)
  }

  override fun printImageBase64(options: ReadableMap, promise: Promise) {
    val productId = options.getDouble("productId")
    val device = getCheckedDevice(productId)
    if (device == null) {
      promise.resolve(getDeviceErrorMessage())
      return
    }
    val result = UsbPrinterImageHelper.printImageBase64(reactApplicationContext, options, device)
    promise.resolve(result)
  }

  override fun printImageUri(options: ReadableMap, promise: Promise) {
    val productId = options.getDouble("productId")
    val device = getCheckedDevice(productId)
    if (device == null) {
      promise.resolve(getDeviceErrorMessage())
      return
    }
    val result = UsbPrinterImageHelper.printImageUri(reactApplicationContext, options, device)
    promise.resolve(result)
  }

  override fun printHtml(options: ReadableMap, promise: Promise) {
    val productId = options.getDouble("productId")
    val device = getCheckedDevice(productId)
    if (device == null) {
      promise.resolve(getDeviceErrorMessage())
      return
    }
    val result = UsbPrinterHtmlHelper.printHtml(reactApplicationContext, options, device)
    promise.resolve(result)
  }

  companion object {
    const val NAME = "UsbPrinter"
    private const val ACTION_USB_PERMISSION = "com.usbprinter.USB_PERMISSION"
  }
}


