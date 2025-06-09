import UsbPrinter from './NativeUsbPrinter';
export type { PrinterResult, UsbDeviceInfo } from './NativeUsbPrinter';

export function getList(): import('./NativeUsbPrinter').UsbDeviceInfo[] {
  return UsbPrinter.getList();
}

export async function printText(text: string, productId: number) {
  return UsbPrinter.printText(text, productId);
}

export async function printCut(
  tailingLine: boolean,
  beep: boolean,
  productId: number
) {
  return UsbPrinter.printCut(tailingLine, beep, productId);
}

export async function barCode(
  text: string,
  width: number,
  height: number,
  productId: number
) {
  return UsbPrinter.barCode(text, width, height, productId);
}

export async function qrCode(text: string, size: number, productId: number) {
  return UsbPrinter.qrCode(text, size, productId);
}

export async function clean(productId: number) {
  return UsbPrinter.clean(productId);
}

export async function off(productId: number) {
  return UsbPrinter.off(productId);
}
