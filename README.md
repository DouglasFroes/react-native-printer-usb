# react-native-printer-usb

React Native module to print on USB thermal printers (ESC/POS) for Android. Supports text, images (base64/URL), barcodes, QR codes, HTML, and raw commands. Handles accented characters, alignment, font, cut, beep, underline, and more.

## Installation

```sh
npm install react-native-printer-usb
```

### Android Setup
- No manual steps required for most React Native projects (autolinking).
- For custom setups, ensure USB permissions in `AndroidManifest.xml`:

```xml
<uses-feature android:name="android.hardware.usb.host" />
<uses-permission android:name="android.permission.USB_PERMISSION" />
```

## Usage

### List Devices
```js
import { getList } from 'react-native-printer-usb';
const devices = getList();
// [{ vendorId, productId, deviceId, manufacturerName, productName, serialNumber, deviceName }]
```

### Print Text
```js
import { printText } from 'react-native-printer-usb';
await printText({
  text: 'Olá, mundo! Çãõé',
  productId, // required
  align: 'center', // 'left' | 'center' | 'right'
  encoding: 'CP850', // or 'utf8', 'ISO-8859-1', etc.
  bold: true,
  underline: true,
  font: 'A', // 'A' | 'B' | 'C'
  size: 2, // 1 (normal), 2 (2x), 4 (4x)
  cut: true, // cut paper after print
  beep: true, // beep after print
  tailingLine: true, // add blank lines at end
});
```

### Print Image (Base64 or URL)
```js
import { printImageBase64, printImageUri } from 'react-native-printer-usb';
await printImageBase64({ base64Image, productId, align: 'center' });
await printImageUri({ imageUri: 'https://...', productId, align: 'center' });
```

### Print Barcode / QR Code
```js
import { barCode, qrCode } from 'react-native-printer-usb';
await barCode({ text: '123456789012', productId, width: 2, height: 80 });
await qrCode({ text: 'https://reactnative.dev', productId, size: 6, align: 'center' });
```

### Print HTML
```js
import { printHtml } from 'react-native-printer-usb';
await printHtml({
  html: '<h1>Impressão HTML</h1>',
  productId,
  align: 'center',
  htmlHeight: 760, // px (optional)
});
```

### Send Raw Data
```js
import { sendRawData } from 'react-native-printer-usb';
await sendRawData({
  productId,
  text: '\x1B\x40Hello\n', // ESC/POS commands
  cut: true,
  tailingLine: true,
  encoding: 'utf8',
});
```

### Cut, Reset, Beep
```js
import { printCut, reset } from 'react-native-printer-usb';
await printCut(true, true, productId); // cut, beep, productId
await reset(productId);
```

## Options Reference

| Option         | Type      | Description                                  |
| -------------- | --------- | -------------------------------------------- |
| text           | string    | Text to print                                |
| productId      | number    | USB Product ID (required)                    |
| align          | string    | 'left', 'center', 'right'                    |
| encoding       | string    | 'utf8', 'CP850', 'ISO-8859-1', etc.          |
| bold           | boolean   | Bold text                                    |
| underline      | boolean   | Underline text                               |
| font           | string    | 'A', 'B', 'C'                                |
| size           | number    | 1 (normal), 2 (2x), 4 (4x)                   |
| cut            | boolean   | Cut paper after print                        |
| beep           | boolean   | Beep after print                             |
| tailingLine    | boolean   | Add blank lines at end                       |
| base64Image    | string    | PNG/JPG base64 image (for printImageBase64)  |
| imageUri       | string    | Image URL (for printImageUri)                |
| html           | string    | HTML string (for printHtml)                  |
| htmlHeight     | number    | Height in px for HTML print (optional)       |

## Example App

See `example/App.tsx` for a full-featured demo with device selection, all print types, and UI/UX best practices.

## Troubleshooting
- Certifique-se de que o dispositivo USB está conectado e com permissão.
- Use encoding compatível com sua impressora (CP850, ISO-8859-1, etc).
- Para imagens, use PNG/JPG pequenos e alinhamento adequado.
- Para impressoras que não cortam/beep, ignore as opções `cut`/`beep`.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
