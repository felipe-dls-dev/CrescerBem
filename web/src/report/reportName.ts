/** Nome do arquivo do relatório: sem acentos nem espaços, ex.: "crescer-bem-ana-2026-11-01-a-2026-11-30.pdf". */
export const reportFileName = (babyName: string, start: string, end: string): string => {
  const slug =
    babyName
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '') || 'bebe';
  return `crescer-bem-${slug}-${start}-a-${end}.pdf`;
};
