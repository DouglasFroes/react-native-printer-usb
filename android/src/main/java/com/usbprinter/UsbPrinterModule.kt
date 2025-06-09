package com.usbprinter

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule

import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableNativeArray
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments

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

@ReactModule(name = UsbPrinterModule.NAME)
class UsbPrinterModule(reactContext: ReactApplicationContext) :
  NativeUsbPrinterSpec(reactContext) {

  override fun getName(): String {
    return NAME
  }

  override fun getList(): WritableArray{
    val helper = UsbDeviceHelper(reactApplicationContext)
    val devices = helper.getConnectedUsbDevices()
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

  override fun printText(text: String, productId: Double, promise: Promise) {
    val checker = UsbDevicePermissionChecker(reactApplicationContext)
    val device = checker.checkDeviceAndPermission(productId.toInt())
    if (device == null) {
      val result = Arguments.createMap()
      result.putBoolean("success", false)
      result.putString("message", "Dispositivo não encontrado ou permissão não concedida. Se solicitado, conceda permissão e tente novamente.")
      promise.resolve(result)
      return
    }
    val printerTextHelper = UsbPrinterTextHelper(reactApplicationContext)
    val result = printerTextHelper.printText(text, device)
    promise.resolve(result)
  }

  override fun printCut(tailingLine: Boolean, beep: Boolean, productId: Double, promise: Promise) {
    val checker = UsbDevicePermissionChecker(reactApplicationContext)
    val device = checker.checkDeviceAndPermission(productId.toInt())
    if (device == null) {
      val result = Arguments.createMap()
      result.putBoolean("success", false)
      result.putString("message", "Dispositivo não encontrado ou permissão não concedida.")
      promise.resolve(result)
      return
    }
    UsbPrinterCutHelper.printCut(reactApplicationContext, tailingLine, beep, productId.toInt(), promise, device)
  }

  override fun barCode(text: String, width: Double, height: Double, productId: Double, promise: Promise) {
    val checker = UsbDevicePermissionChecker(reactApplicationContext)
    val device = checker.checkDeviceAndPermission(productId.toInt())
    if (device == null) {
      val result = Arguments.createMap()
      result.putBoolean("success", false)
      result.putString("message", "Dispositivo não encontrado ou permissão não concedida.")
      promise.resolve(result)
      return
    }
    UsbPrinterBarcodeHelper.printBarcode(reactApplicationContext, text, width, height, productId.toInt(), promise, device)
  }

  override fun qrCode(text: String, size: Double, productId: Double, promise: Promise) {
    val checker = UsbDevicePermissionChecker(reactApplicationContext)
    val device = checker.checkDeviceAndPermission(productId.toInt())
    if (device == null) {
      val result = Arguments.createMap()
      result.putBoolean("success", false)
      result.putString("message", "Dispositivo não encontrado ou permissão não concedida.")
      promise.resolve(result)
      return
    }
    UsbPrinterQrCodeHelper.printQrCode(reactApplicationContext, text, size, productId.toInt(), promise, device)
  }

  override fun clean(productId: Double, promise: Promise) {
    val checker = UsbDevicePermissionChecker(reactApplicationContext)
    val device = checker.checkDeviceAndPermission(productId.toInt())
    if (device == null) {
      val result = Arguments.createMap()
      result.putBoolean("success", false)
      result.putString("message", "Dispositivo não encontrado ou permissão não concedida.")
      promise.resolve(result)
      return
    }
    UsbPrinterCleanHelper.clean(reactApplicationContext, productId.toInt(), promise, device)
  }

  override fun off(productId: Double, promise: Promise) {
    val checker = UsbDevicePermissionChecker(reactApplicationContext)
    val device = checker.checkDeviceAndPermission(productId.toInt())
    if (device == null) {
      val result = Arguments.createMap()
      result.putBoolean("success", false)
      result.putString("message", "Dispositivo não encontrado ou permissão não concedida.")
      promise.resolve(result)
      return
    }
    UsbPrinterOffHelper.off(reactApplicationContext, productId.toInt(), promise, device)
  }

  companion object {
    const val NAME = "UsbPrinter"
    private const val ACTION_USB_PERMISSION = "com.usbprinter.USB_PERMISSION"
  }
}
