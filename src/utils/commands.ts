// ESC/POS command constants and helpers for USB printers
// https://reference.epson-biz.com/modules/ref_escpos/index.php?content_id=72

/**
 * ESC/POS Command Constants for USB Printers
 * Provides type-safe, well-documented command sets for use with ESC/POS-compatible printers.
 * Includes helpers for text sizing and horizontal rules.
 */

/** Horizontal line patterns for receipts */
export const horizontal_line = {
  hr_58mm: '==================================',
  hr2_58mm: '**********************************',
  hr3_58mm: '----------------------------------',
  hr_80mm: '================================================',
  hr2_80mm: '************************************************',
  hr3_80mm: '------------------------------------------------',
};

/** Feed control sequences */
export const feed_control_sequences = {
  /** Print and line feed */
  ctl_lf: '\x0a',
  /** Form feed */
  ctl_ff: '\x0c',
  /** Carriage return */
  ctl_cr: '\x0d',
  /** Horizontal tab */
  ctl_ht: '\x09',
  /** Vertical tab */
  ctl_vt: '\x0b',
};

/** Line spacing commands */
export const line_spacing = {
  ls_default: '\x1b\x32',
  ls_set: '\x1b\x33',
  ls_set1: '\x1b\x31',
};

/** Hardware control commands */
export const hardware = {
  /** Clear data in buffer and reset modes */
  hw_init: '\x1b\x40',
  /** Printer select */
  hw_select: '\x1b\x3d\x01',
  /** Reset printer hardware */
  hw_reset: '\x1b\x3f\x0a\x00',
};

/** Cash drawer commands */
export const cash_drawer = {
  /** Sends a pulse to pin 2 */
  cd_kick_2: '\x1b\x70\x00',
  /** Sends a pulse to pin 5 */
  cd_kick_5: '\x1b\x70\x01',
};

/** Margin commands */
export const margins = {
  /** Fix bottom size */
  bottom: '\x1b\x4f',
  /** Fix left size */
  left: '\x1b\x6c',
  /** Fix right size */
  right: '\x1b\x51',
};

/** Paper cut commands */
export const paper = {
  /** Full cut paper */
  paper_full_cut: '\x1d\x56\x00',
  /** Partial cut paper */
  paper_part_cut: '\x1d\x56\x01',
  /** Partial cut paper (A) */
  paper_cut_a: '\x1d\x56\x41',
  /** Partial cut paper (B) */
  paper_cut_b: '\x1d\x56\x42',
};

/** Text format commands and helpers */
export const text_format = {
  /** Normal text */
  txt_normal: '\x1b\x21\x06',
  /** Small text */
  txt_small: '\x1b\x21\x01',
  txt_large: '\x1b\x21\x08',
  /** Double height text */
  txt_2height: '\x1b\x21\x10',
  /** Double width text */
  txt_2width: '\x1b\x21\x20',
  /** Double width & height text */
  txt_4square: '\x1b\x21\x30',
  /**
   * Custom text size (width: 1-8, height: 1-8)
   * @param width Width multiplier (1-8)
   * @param height Height multiplier (1-8)
   * @returns ESC/POS command string
   */
  txt_custom_size: (width: number, height: number): string => {
    if (width < 1 || width > 8 || height < 1 || height > 8) {
      throw new Error('Width and height must be between 1 and 8');
    }
    const sizeDec = ((width - 1) << 4) | (height - 1);
    return '\x1d\x21' + String.fromCharCode(sizeDec);
  },
  /** Height multipliers */
  txt_height: {
    1: '\x00',
    2: '\x01',
    3: '\x02',
    4: '\x03',
    5: '\x04',
    6: '\x05',
    7: '\x06',
    8: '\x07',
  } as const,
  /** Width multipliers */
  txt_width: {
    1: '\x00',
    2: '\x10',
    3: '\x20',
    4: '\x30',
    5: '\x40',
    6: '\x50',
    7: '\x60',
    8: '\x70',
  } as const,
  /** Underline font OFF */
  txt_underl_off: '\x1b\x2d\x00',
  /** Underline font 1-dot ON */
  txt_underl_on: '\x1b\x2d\x01',
  /** Underline font 2-dot ON */
  txt_underl2_on: '\x1b\x2d\x02',
  /** Bold font OFF */
  txt_bold_off: '\x1b\x45\x00',
  /** Bold font ON */
  txt_bold_on: '\x1b\x45\x01',
  /** Italic font OFF */
  txt_italic_off: '\x1b\x35',
  /** Italic font ON */
  txt_italic_on: '\x1b\x34',
  /** Font type A */
  txt_font_a: '\x1b\x4d\x00',
  /** Font type B */
  txt_font_b: '\x1b\x4d\x01',
  /** Font type C */
  txt_font_c: '\x1b\x4d\x02',
  /** Left justification */
  txt_align_lt: '\x1b\x61\x00',
  /** Justification */
  txt_align_jt: '\x1b\x61\x03',
  /** Centering */
  txt_align_ct: '\x1b\x61\x01',
  /** Right justification */
  txt_align_rt: '\x1b\x61\x02',
};

/**
 * Main ESC/POS command set, including all helpers and control codes.
 */
export const commands = {
  lf: '\x0a',
  esc: '\x1b',
  fs: '\x1c',
  gs: '\x1d',
  us: '\x1f',
  ff: '\x0c',
  dle: '\x10',
  dc1: '\x11',
  dc4: '\x14',
  eot: '\x04',
  nul: '\x00',
  eol: '\n',
  horizontal_line,
  feed_control_sequences,
  line_spacing,
  hardware,
  cash_drawer,
  margins,
  paper,
  text_format,
};
