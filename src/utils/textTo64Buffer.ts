// textTo64Buffer.ts
// Implementação mínima de Buffer.from para React Native (apenas utf-8)
function encodeTextToBuffer(text: string, encoding: string): Uint8Array {
  if (
    encoding &&
    encoding.toLowerCase() !== 'utf8' &&
    encoding.toLowerCase() !== 'utf-8'
  ) {
    throw new Error('Apenas encoding UTF-8 é suportado nesta implementação.');
  }
  // Converte string para array de bytes utf-8
  const utf8: number[] = [];
  for (let i = 0; i < text.length; i++) {
    let charcode = text.charCodeAt(i);
    if (charcode < 0x80) utf8.push(charcode);
    else if (charcode < 0x800) {
      utf8.push(0xc0 | (charcode >> 6), 0x80 | (charcode & 0x3f));
    } else if (charcode < 0xd800 || charcode >= 0xe000) {
      utf8.push(
        0xe0 | (charcode >> 12),
        0x80 | ((charcode >> 6) & 0x3f),
        0x80 | (charcode & 0x3f)
      );
    } else {
      // surrogate pair
      i++;
      charcode =
        0x10000 + (((charcode & 0x3ff) << 10) | (text.charCodeAt(i) & 0x3ff));
      utf8.push(
        0xf0 | (charcode >> 18),
        0x80 | ((charcode >> 12) & 0x3f),
        0x80 | ((charcode >> 6) & 0x3f),
        0x80 | (charcode & 0x3f)
      );
    }
  }
  return new Uint8Array(utf8);
}

// Pequeno helper para concatenar Uint8Array
class SimpleBufferHelper {
  private buffers: Uint8Array[] = [];
  concat(buf: Uint8Array) {
    this.buffers.push(buf);
  }
  toBuffer(): Uint8Array {
    let total = this.buffers.reduce((acc, b) => acc + b.length, 0);
    let out = new Uint8Array(total);
    let offset = 0;
    for (const b of this.buffers) {
      out.set(b, offset);
      offset += b.length;
    }
    return out;
  }
}

// Função independente para converter texto em base64 buffer para impressoras térmicas
// Remove BufferEncoding do tipo PrinterOptions, pois não existe no ambiente React Native
export interface PrinterOptions {
  beep?: boolean;
  cut?: boolean;
  tailingLine?: boolean;
  encoding?: string; // string simples, ex: 'utf-8', 'ascii', 'utf16le'
}

const defaultOptions: Required<PrinterOptions> = {
  beep: false,
  cut: false,
  tailingLine: false,
  encoding: 'UTF8',
};

export function textTo64Buffer(
  text: string,
  opts: PrinterOptions = {}
): string {
  const options = { ...defaultOptions, ...opts };
  const fixAndroid = '\n';
  const bufferHelper = new SimpleBufferHelper();
  bufferHelper.concat(encodeTextToBuffer(text + fixAndroid, options.encoding));
  if (options.tailingLine) {
    bufferHelper.concat(new Uint8Array([10, 10, 10, 10, 10]));
  }
  if (options.cut) {
    bufferHelper.concat(new Uint8Array([27, 105]));
  }
  if (options.beep) {
    bufferHelper.concat(new Uint8Array([27, 66, 3, 2]));
  }
  // Converte para base64 manualmente
  const buffer = bufferHelper.toBuffer();
  // btoa espera string, então convertemos para string binária
  let binary = '';
  for (let i = 0; i < buffer.length; i++) {
    binary += String.fromCharCode(buffer[i]!);
  }
  // Polyfill base64 (não depende de btoa)
  const chars =
    'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
  let str = binary;
  let output = '';
  for (
    let block = 0, charCode, i = 0, map = chars;
    str.charAt(i | 0) || ((map = '='), i % 1);
    output += map.charAt(63 & (block >> (8 - (i % 1) * 8)))
  ) {
    charCode = str.charCodeAt((i += 3 / 4));
    block = (block << 8) | (charCode & 0xff);
  }
  return output;
}
