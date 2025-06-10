import UsbPrinter, { type PrintTextOptions } from './NativeUsbPrinter';
import { textTo64Buffer } from './utils/textTo64Buffer';
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

type PrinterOptionsRawData = {
  text: string;
  productId: number;
  beep?: boolean;
  cut?: boolean;
  tailingLine?: boolean;
  encoding?: string;
};

export async function sendRawData(data: PrinterOptionsRawData) {
  return UsbPrinter.sendRawData(
    textTo64Buffer(data.text, {
      beep: data.beep,
      cut: data.cut,
      tailingLine: data.tailingLine,
    }),
    data.productId
  );
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

export async function reset(productId: number) {
  return UsbPrinter.reset(productId);
}
