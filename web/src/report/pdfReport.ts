// Desenha um ReportDocument em PDF (A4) com jsPDF, no próprio navegador.
// A biblioteca vai empacotada no app (sem CDN) e nada é enviado para fora: o arquivo é gerado
// e baixado direto no aparelho. Mesmo layout do PdfReportWriter do app Android.

import { jsPDF } from 'jspdf';
import type { ReportBlock, ReportDocument, ReportSection } from '../domain/report';

/** Foto já pronta para o PDF (miniatura em JPEG). */
export interface PdfPhoto {
  dataUrl: string;
  width: number;
  height: number;
}

export type PhotoLoader = (photoId: number) => Promise<PdfPhoto | null>;

const PAGE_WIDTH = 595.28;
const PAGE_HEIGHT = 841.89;
const MARGIN = 40;
const FOOTER_HEIGHT = 34;
const SECTION_BAND = 24;
const CELL_PAD = 4;
const THUMB_SIZE = 110;
const PHOTOS_PER_ROW = 4;
const LINE_FACTOR = 1.25;

type Rgb = [number, number, number];
const INK: Rgb = [27, 28, 26];
const MUTED: Rgb = [100, 104, 102];
const BRAND: Rgb = [47, 111, 106];
const BRAND_DARK: Rgb = [30, 80, 76];
const BAND: Rgb = [219, 238, 234];
const HEADER_FILL: Rgb = [234, 240, 238];
const ZEBRA: Rgb = [248, 248, 246];
const RULE: Rgb = [205, 208, 205];

/**
 * As fontes padrão do PDF só têm caracteres do alfabeto latino (cobre todo o português).
 * Qualquer outro símbolo (emoji, aspas curvas...) vira um equivalente simples ou "?".
 */
export function toPdfText(text: string): string {
  return text
    .replace(/[‘’]/g, "'")
    .replace(/[“”]/g, '"')
    .replace(/[–—]/g, '-')
    .replace(/…/g, '...')
    .replace(/•/g, '-')
    .replace(/[\r\t]/g, ' ')
    .replace(/[^\n -~ -ÿ]/gu, '?');
}

interface TextStyle {
  size: number;
  bold?: boolean;
  color?: Rgb;
}

class Renderer {
  readonly pdf = new jsPDF({ unit: 'pt', format: 'a4', compress: true });
  private y = MARGIN;
  private pages = 1;
  private readonly contentWidth = PAGE_WIDTH - 2 * MARGIN;
  private readonly contentBottom = PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT;

  constructor(
    private readonly footer: string,
    private readonly loadPhoto: PhotoLoader | null,
  ) {
    this.pdf.setProperties({ title: 'Relatório de acompanhamento - Crescer Bem', creator: 'Crescer Bem' });
  }

  get pageCount(): number {
    return this.pages;
  }

  private applyStyle({ size, bold = false, color = INK }: TextStyle) {
    this.pdf.setFont('helvetica', bold ? 'bold' : 'normal');
    this.pdf.setFontSize(size);
    this.pdf.setTextColor(...color);
  }

  private wrap(text: string, style: TextStyle, width: number): string[] {
    this.applyStyle(style);
    return this.pdf.splitTextToSize(toPdfText(text), width) as string[];
  }

  private lineHeight(style: TextStyle): number {
    return style.size * LINE_FACTOR;
  }

  private drawLines(lines: string[], style: TextStyle, x: number, y: number) {
    this.applyStyle(style);
    lines.forEach((line, i) => this.pdf.text(line, x, y + i * this.lineHeight(style), { baseline: 'top' }));
  }

  /** Desenha um parágrafo, quebrando de página linha a linha se for preciso. */
  private textBlock(text: string, style: TextStyle, x: number, width: number, after: number) {
    const lines = this.wrap(text, style, width);
    const lh = this.lineHeight(style);
    let index = 0;
    while (index < lines.length) {
      this.ensureSpace(lh);
      const fit = Math.max(1, Math.floor((this.contentBottom - this.y) / lh));
      const chunk = lines.slice(index, index + fit);
      this.drawLines(chunk, style, x, this.y);
      this.y += chunk.length * lh;
      index += chunk.length;
    }
    this.y += after;
  }

  private hr(y = this.y) {
    this.pdf.setDrawColor(...RULE);
    this.pdf.setLineWidth(0.6);
    this.pdf.line(MARGIN, y, PAGE_WIDTH - MARGIN, y);
  }

  private ensureSpace(height: number) {
    if (this.y + height > this.contentBottom) this.newPage();
  }

  private newPage() {
    this.pdf.addPage();
    this.pages += 1;
    this.y = MARGIN;
    this.textBlock('Crescer Bem · Relatório de acompanhamento', { size: 8, color: MUTED }, MARGIN, this.contentWidth, 8);
  }

  header(document: ReportDocument) {
    this.textBlock('Crescer Bem', { size: 11, bold: true, color: BRAND }, MARGIN, this.contentWidth, 2);
    this.textBlock(document.title, { size: 20, bold: true }, MARGIN, this.contentWidth, 8);
    document.headerLines.forEach((line) => this.textBlock(line, { size: 10 }, MARGIN, this.contentWidth, 1));
    this.y += 6;
    this.hr();
    this.y += 12;
  }

  async section(section: ReportSection) {
    // Evita título de seção sozinho no pé da página.
    this.ensureSpace(SECTION_BAND + 40);
    this.pdf.setFillColor(...BAND);
    this.pdf.roundedRect(MARGIN, this.y, this.contentWidth, SECTION_BAND, 4, 4, 'F');
    const style: TextStyle = { size: 13, bold: true, color: BRAND_DARK };
    const lines = this.wrap(section.title, style, this.contentWidth - 16);
    this.drawLines(lines, style, MARGIN + 8, this.y + (SECTION_BAND - this.lineHeight(style)) / 2);
    this.y += SECTION_BAND + 8;
    for (const block of section.blocks) {
      if (block.kind === 'paragraph') this.textBlock(block.text, { size: 10 }, MARGIN, this.contentWidth, 6);
      else if (block.kind === 'table') this.table(block);
      else await this.entry(block);
    }
    this.y += 10;
  }

  private async entry(entry: Extract<ReportBlock, { kind: 'entry' }>) {
    this.ensureSpace(30);
    this.textBlock(entry.heading, { size: 10.5, bold: true }, MARGIN, this.contentWidth, 2);
    entry.lines.forEach((l) => this.textBlock(l, { size: 10 }, MARGIN + 10, this.contentWidth - 10, 1));
    if (entry.photos.length > 0) await this.photos(entry.photos);
    this.y += 4;
    this.hr();
    this.y += 8;
  }

  private async photos(ids: number[]) {
    if (!this.loadPhoto) return;
    this.y += 4;
    for (let i = 0; i < ids.length; i += PHOTOS_PER_ROW) {
      this.ensureSpace(THUMB_SIZE + 6);
      let x = MARGIN + 10;
      for (const id of ids.slice(i, i + PHOTOS_PER_ROW)) {
        const photo = await this.loadPhoto(id);
        if (photo) {
          const scale = Math.min(THUMB_SIZE / photo.width, THUMB_SIZE / photo.height);
          this.pdf.addImage(photo.dataUrl, 'JPEG', x, this.y, photo.width * scale, photo.height * scale);
        }
        x += THUMB_SIZE + 8;
      }
      this.y += THUMB_SIZE + 6;
    }
  }

  private table(block: Extract<ReportBlock, { kind: 'table' }>) {
    const totalWeight = block.weights.reduce((a, b) => a + b, 0);
    const widths = block.weights.map((w) => (w / totalWeight) * this.contentWidth);
    const cellStyle: TextStyle = { size: 9 };
    const headStyle: TextStyle = { size: 9, bold: true };
    const lh = this.lineHeight(cellStyle);

    const drawRow = (cells: string[], style: TextStyle, fill: Rgb | null) => {
      const wrapped = cells.map((c, i) => this.wrap(c, style, widths[i] - 2 * CELL_PAD));
      const height = Math.max(...wrapped.map((l) => l.length)) * lh + 2 * CELL_PAD;
      return { wrapped, height, style, fill };
    };
    const paint = (row: ReturnType<typeof drawRow>) => {
      if (row.fill) {
        this.pdf.setFillColor(...row.fill);
        this.pdf.rect(MARGIN, this.y, this.contentWidth, row.height, 'F');
      }
      let x = MARGIN;
      row.wrapped.forEach((lines, i) => {
        this.drawLines(lines, row.style, x + CELL_PAD, this.y + CELL_PAD);
        x += widths[i];
      });
      this.y += row.height;
      this.hr();
    };

    const header = drawRow(block.headers, headStyle, HEADER_FILL);
    this.ensureSpace(header.height + 30);
    paint(header);
    block.rows.forEach((cells, index) => {
      const row = drawRow(cells, cellStyle, index % 2 === 1 ? ZEBRA : null);
      if (this.y + row.height > this.contentBottom) {
        this.newPage();
        paint(drawRow(block.headers, headStyle, HEADER_FILL));
      }
      paint(row);
    });
    this.y += 8;
  }

  /** Rodapé com o aviso e o número da página, em todas as páginas. */
  finish() {
    const total = this.pages;
    const style: TextStyle = { size: 8, color: MUTED };
    for (let page = 1; page <= total; page++) {
      this.pdf.setPage(page);
      const footerTop = PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT + 10;
      this.hr(footerTop);
      const label = `Página ${page} de ${total}`;
      this.applyStyle(style);
      const labelWidth = this.pdf.getTextWidth(label);
      const lines = this.wrap(this.footer, style, this.contentWidth - labelWidth - 16);
      this.drawLines(lines, style, MARGIN, footerTop + 6);
      this.pdf.text(label, PAGE_WIDTH - MARGIN - labelWidth, footerTop + 6, { baseline: 'top' });
    }
  }
}

export interface RenderedPdf {
  pdf: jsPDF;
  pageCount: number;
}

/** Desenha o documento. [loadPhoto] só é chamado para as fotos presentes no relatório. */
export async function renderReportPdf(document: ReportDocument, loadPhoto: PhotoLoader | null = null): Promise<RenderedPdf> {
  const renderer = new Renderer(document.footer, loadPhoto);
  renderer.header(document);
  for (const section of document.sections) await renderer.section(section);
  renderer.finish();
  return { pdf: renderer.pdf, pageCount: renderer.pageCount };
}
