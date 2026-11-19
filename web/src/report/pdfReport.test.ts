// @vitest-environment node
import { describe, expect, it } from 'vitest';
import { buildReport, emptyReportData, type ReportData } from '../domain/report';
import { renderReportPdf, toPdfText } from './pdfReport';
import { reportFileName } from './reportName';
import { scaleFactor } from './photoLoader';

const baby = { id: 1, name: 'Bebê Teste', birthDate: '2026-09-01', colorIndex: 0 };

function bigData(): ReportData {
  const data = emptyReportData();
  for (let i = 0; i < 120; i++) {
    const day = String((i % 28) + 1).padStart(2, '0');
    data.measurements.push({ id: i + 1, babyId: 1, date: `2026-11-${day}`, type: 'WEIGHT', value: 4 + i / 100, notes: 'Pesagem na consulta de rotina com observações longas para quebrar a linha da tabela' });
    data.moments.push({ id: i + 1, babyId: 1, dateTime: `2026-11-${day}T10:00`, title: `Momento ${i}`, description: 'Descrição '.repeat(30) });
  }
  return data;
}

const request = {
  baby,
  start: '2026-11-01',
  end: '2026-11-30',
  types: ['GROWTH', 'MOMENT', 'VACCINE'] as ('GROWTH' | 'MOMENT' | 'VACCINE')[],
  includePhotos: false,
};

describe('PDF do relatório', () => {
  it('gera um PDF válido com várias páginas, sem rede', async () => {
    const doc = buildReport(request, bigData(), '2026-12-01T10:00');
    const { pdf, pageCount } = await renderReportPdf(doc);
    const bytes = new Uint8Array(pdf.output('arraybuffer'));
    expect(new TextDecoder().decode(bytes.slice(0, 5))).toBe('%PDF-');
    expect(pageCount).toBeGreaterThan(3);
    expect(pdf.getNumberOfPages()).toBe(pageCount);
  });

  it('relatório vazio cabe em uma página', async () => {
    const doc = buildReport(request, emptyReportData(), '2026-12-01T10:00');
    const { pageCount } = await renderReportPdf(doc);
    expect(pageCount).toBe(1);
  });

  it('só chama o carregador de fotos para as fotos do relatório', async () => {
    const data = emptyReportData();
    data.moments.push({ id: 7, babyId: 1, dateTime: '2026-11-02T10:00', title: 'Com foto' });
    data.photos['MOMENT:7'] = [11, 12];
    const asked: number[] = [];
    const doc = buildReport({ ...request, includePhotos: true }, data, '2026-12-01T10:00');
    await renderReportPdf(doc, async (id) => {
      asked.push(id);
      return null;
    });
    expect(asked).toEqual([11, 12]);
    const none: number[] = [];
    await renderReportPdf(buildReport({ ...request, includePhotos: false }, data, '2026-12-01T10:00'), async (id) => {
      none.push(id);
      return null;
    });
    expect(none).toEqual([]);
  });

  it('caracteres fora do alfabeto latino viram texto seguro', () => {
    expect(toPdfText('Olá, açaí ª º ° · ok')).toBe('Olá, açaí ª º ° · ok');
    expect(toPdfText('Bebê 😀 “aspas” – traço…')).toBe('Bebê ? "aspas" - traço...');
  });

  it('nome do arquivo sem acentos nem espaços', () => {
    expect(reportFileName('Maria José D’Ávila', '2026-11-01', '2026-11-30')).toBe(
      'crescer-bem-maria-jose-d-avila-2026-11-01-a-2026-11-30.pdf',
    );
    expect(reportFileName('???', '2026-11-01', '2026-11-30')).toBe('crescer-bem-bebe-2026-11-01-a-2026-11-30.pdf');
  });

  it('fator de escala das fotos', () => {
    expect(scaleFactor(800, 600, 1600)).toBe(1);
    expect(scaleFactor(2000, 1500, 1600)).toBeCloseTo(0.8, 4);
    expect(scaleFactor(0, 0, 1600)).toBe(1);
  });
});
