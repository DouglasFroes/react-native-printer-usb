import UsbPrinter, { type PrintTextOptions } from './NativeUsbPrinter';
export type { PrinterResult, UsbDeviceInfo } from './NativeUsbPrinter';

export function getList(): import('./NativeUsbPrinter').UsbDeviceInfo[] {
  return UsbPrinter.getList();
}

export async function printText(options: PrintTextOptions) {
  return UsbPrinter.printText(options);
}

export async function printCut(
  tailingLine: boolean,
  beep: boolean,
  productId: number
) {
  return UsbPrinter.printCut(tailingLine, beep, productId);
}

export async function barCode(
  options: import('./NativeUsbPrinter').BarCodeOptions
) {
  return UsbPrinter.barCode(options);
}

export async function qrCode(
  options: import('./NativeUsbPrinter').QrCodeOptions
) {
  return UsbPrinter.qrCode(options);
}

export async function clean(productId: number) {
  return UsbPrinter.clean(productId);
}

export async function off(productId: number) {
  return UsbPrinter.off(productId);
}

export async function sendRawData(data: string, productId: number) {
  return UsbPrinter.sendRawData(data, productId);
}

export async function printImageBase64(
  options: import('./NativeUsbPrinter').PrintImageBase64Options
) {
  return UsbPrinter.printImageBase64(options);
}

export async function printImageUri(
  options: import('./NativeUsbPrinter').PrintImageUriOptions
) {
  return UsbPrinter.printImageUri(options);
}

export async function printHtml(
  options: import('./NativeUsbPrinter').PrintHtmlOptions
) {
  return UsbPrinter.printHtml(options);
}
