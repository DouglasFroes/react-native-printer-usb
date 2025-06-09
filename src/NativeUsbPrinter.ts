import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

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
  printText(text: string, productId: number): Promise<PrinterResult>;
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
}

const UsbPrinter = TurboModuleRegistry.getEnforcing<Spec>('UsbPrinter');
export default UsbPrinter;
