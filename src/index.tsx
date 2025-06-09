import UsbPrinter from './NativeUsbPrinter';
export type { PrinterTestResult, UsbDeviceInfo } from './NativeUsbPrinter';

export function getList(): import('./NativeUsbPrinter').UsbDeviceInfo[] {
  return UsbPrinter.getList();
}

export async function printText(
  text: string,
  productId: number
): Promise<import('./NativeUsbPrinter').PrinterTestResult> {
  return UsbPrinter.printText(text, productId);
}
