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

export interface PrinterTestResult {
  success: boolean;
  message?: string;
}

export interface Spec extends TurboModule {
  getList(): UsbDeviceInfo[];
  printText(text: string, productId: number): Promise<any>;
}

const UsbPrinter = TurboModuleRegistry.getEnforcing<Spec>('UsbPrinter');
export default UsbPrinter;
