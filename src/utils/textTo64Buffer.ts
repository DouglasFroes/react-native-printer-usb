// textTo64Buffer.ts
// Implementação mínima de Buffer.from para React Native (apenas utf-8)
/* eslint-disable no-bitwise */
function encodeTextToBuffer(text: string): Uint8Array {
  // Sempre usa UTF-8, com suporte total a caracteres especiais/unicode
  if (typeof TextEncoder !== 'undefined') {
    return new TextEncoder().encode(text);
  }
  // Fallback manual (mantém implementação anterior)
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

function removerCaracteresEspeciais(texto: string) {
  return texto
    .normalize('NFD') // Decompõe caracteres acentuados em base + diacrítico
    .replace(/[\u0300-\u036f]/g, ''); // Remove os diacríticos
}

// Função independente para converter texto em base64 buffer para impressoras térmicas
// Remove BufferEncoding do tipo PrinterOptions, pois não existe no ambiente React Native
export interface PrinterOptions {
  beep?: boolean;
  cut?: boolean;
  tailingLine?: boolean;
}

const defaultOptions: Required<PrinterOptions> = {
  beep: false,
  cut: false,
  tailingLine: false,
};

export function textTo64Buffer(
  text1: string,
  opts: PrinterOptions = {}
): string {
  const options = { ...defaultOptions, ...opts };
  const fixAndroid = '\n';
  const bufferHelper = new SimpleBufferHelper();
  // Remove caracteres especiais do texto
  const text = removerCaracteresEspeciais(text1);
  bufferHelper.concat(encodeTextToBuffer(text + fixAndroid));
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
  // Polyfill base64 (não depende de btoa)
  // Usa encodeURIComponent/unescape para garantir suporte a caracteres especiais
  // Corrige para base64 seguro para unicode
  // Usa apenas encodeURIComponent + atob polyfill universal
  function toBase64(uint8: Uint8Array): string {
    let binary = '';
    for (let i = 0; i < uint8.length; i++) {
      binary += String.fromCharCode(uint8[i]!);
    }
    // Converte binário para base64 de forma universal (sem btoa/unescape)
    if (typeof Buffer !== 'undefined') {
      // Node.js ou ambiente com Buffer
      return Buffer.from(uint8).toString('base64');
    }
    // Polyfill universal
    const chars =
      'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
    let output = '';
    let i = 0;
    while (i < binary.length) {
      const c1 = binary.charCodeAt(i++) & 0xff;
      if (i === binary.length) {
        output += chars.charAt(c1 >> 2);
        output += chars.charAt((c1 & 0x3) << 4);
        output += '==';
        break;
      }
      const c2 = binary.charCodeAt(i++) & 0xff;
      if (i === binary.length) {
        output += chars.charAt(c1 >> 2);
        output += chars.charAt(((c1 & 0x3) << 4) | ((c2 & 0xf0) >> 4));
        output += chars.charAt((c2 & 0xf) << 2);
        output += '=';
        break;
      }
      const c3 = binary.charCodeAt(i++) & 0xff;
      output += chars.charAt(c1 >> 2);
      output += chars.charAt(((c1 & 0x3) << 4) | ((c2 & 0xf0) >> 4));
      output += chars.charAt(((c2 & 0xf) << 2) | ((c3 & 0xc0) >> 6));
      output += chars.charAt(c3 & 0x3f);
    }
    return output;
  }
  const base64 = toBase64(buffer);
  return base64;
}
