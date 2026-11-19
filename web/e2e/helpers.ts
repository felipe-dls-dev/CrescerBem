import { expect, type Page } from '@playwright/test';

/** Data "AAAA-MM-DD" daqui a [days] dias (negativo = no passado), no fuso do navegador de teste. */
export function dayOffset(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() + days);
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

/** Abre o app com os dados limpos. Todos os dados usados nos testes são fictícios. */
export async function freshStart(page: Page): Promise<void> {
  await page.goto('/');
  await page.evaluate(async () => {
    localStorage.clear();
    const dbs = await indexedDB.databases();
    await Promise.all(dbs.map((d) => new Promise<void>((resolve) => {
      const req = indexedDB.deleteDatabase(d.name!);
      req.onsuccess = req.onerror = req.onblocked = () => resolve();
    })));
  });
  await page.goto('/#/hoje');
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Boas-vindas ao Crescer Bem' })).toBeVisible();
}

/** Cadastra um bebê fictício pelo formulário. */
export async function createBaby(page: Page, name = 'Bebê Teste', birth = dayOffset(-40), opts: { vaccines?: boolean } = {}): Promise<void> {
  await page.getByRole('link', { name: 'Cadastrar bebê' }).first().click();
  await page.getByLabel(/^Nome ou apelido/).fill(name);
  await page.getByLabel(/^Data de nascimento/).fill(birth);
  if (opts.vaccines === false) await page.getByLabel('Adicionar calendário de vacinas sugerido').uncheck();
  await page.getByRole('button', { name: 'Salvar' }).click();
  await expect(page.getByText(`${name} tem`)).toBeVisible();
}

/** Abre uma seção recolhida (<details>) pelo título. */
export async function openSection(page: Page, title: string): Promise<void> {
  const summary = page.locator('summary', { hasText: title }).first();
  const details = summary.locator('xpath=..');
  if ((await details.getAttribute('open')) === null) await summary.click();
}

export async function goTab(page: Page, label: string): Promise<void> {
  await page.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: label }).click();
}

export async function register(page: Page, label: string | RegExp): Promise<void> {
  await page.getByRole('button', { name: 'Registrar', exact: true }).click();
  await page.getByRole('dialog').getByRole('button', { name: label }).click();
}
