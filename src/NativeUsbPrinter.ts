import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface PrintTextOptions {
  text: string;
  productId: number;
  size?: 1 | 2 | 4;
  align?: 'left' | 'center' | 'right';
  encoding?: string;
  bold?: boolean;
  font?: 'A' | 'B' | 'C';
  cut?: boolean;
  beep?: boolean;
}

export interface UsbDeviceInfo {
  deviceName: string;
  deviceId: number;
  vendorId: number;
  productId: number;
  manufacturerName?: string;
  productName?: string;
}

export interface PrinterResult {
  success: boolean;
  message?: string;
}

export interface Spec extends TurboModule {
  getList(): UsbDeviceInfo[];
  printText(options: PrintTextOptions): Promise<PrinterResult>;
  printCut(
    tailingLine: boolean,
    beep: boolean,
    productId: number
  ): Promise<PrinterResult>;
  barCode(
    text: string,
    width: number,
    height: number,
    productId: number
  ): Promise<PrinterResult>;
  qrCode(text: string, size: number, productId: number): Promise<PrinterResult>;
  clean(productId: number): Promise<PrinterResult>;
  off(productId: number): Promise<PrinterResult>;
  sendRawData(data: string, productId: number): Promise<PrinterResult>;
  printImageBase64(base64: string, productId: number): Promise<PrinterResult>;
  printImageUri(uri: string, productId: number): Promise<PrinterResult>;
  printHtml(html: string, productId: number): Promise<PrinterResult>;
}

const UsbPrinter = TurboModuleRegistry.getEnforcing<Spec>('UsbPrinter');
export default UsbPrinter;
