// Salvar e abrir arquivos no aparelho (sem servidor).

/** Baixa [content] como arquivo no aparelho. */
export function downloadFile(content: Blob | string, fileName: string, mime: string): void {
  const blob = typeof content === 'string' ? new Blob([content], { type: mime }) : content;
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.rel = 'noopener';
  document.body.appendChild(link);
  link.click();
  link.remove();
  // Dá tempo ao navegador de iniciar o download antes de soltar a memória.
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}

/** Lê um arquivo escolhido pelo usuário como texto. */
export const readFileText = (file: File): Promise<string> => file.text();
