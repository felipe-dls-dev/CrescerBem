import { expect, test, type Page } from '@playwright/test';
import { createBaby, dayOffset, freshStart, goTab, register } from './helpers';

/** Registra toda requisição de rede e todo erro de política de segurança (CSP) da página. */
function watch(page: Page) {
  const requests: string[] = [];
  const cspErrors: string[] = [];
  page.on('request', (r) => requests.push(r.url()));
  page.on('console', (m) => {
    if (/Content Security Policy|Refused to/i.test(m.text())) cspErrors.push(m.text());
  });
  page.on('pageerror', (e) => cspErrors.push(`Erro na página: ${e.message}`));
  return { requests, cspErrors };
}

test.describe('Privacidade: nada sai do aparelho', () => {
  test('um uso completo do app não faz nenhuma requisição para fora', async ({ page, baseURL }) => {
    const { requests, cspErrors } = watch(page);
    await freshStart(page);
    await createBaby(page, 'Bebê Rede', dayOffset(-40));
    await register(page, /^Momento/);
    await page.getByLabel('Título').fill('Registro de rede');
    await page.getByRole('button', { name: 'Salvar' }).click();
    await goTab(page, 'Saúde');
    await goTab(page, 'Alimentação');
    await goTab(page, 'Calendário');
    await page.goto('/#/relatorio');
    await page.getByRole('button', { name: 'Gerar PDF' }).click();
    await expect(page.getByText('Relatório pronto')).toBeVisible();
    await page.goto('/#/ajustes');
    await page.goto('/#/sobre');

    const external = requests.filter((u) => !u.startsWith(baseURL!) && !u.startsWith('blob:') && !u.startsWith('data:'));
    expect(external, `Requisições externas: ${external.join(', ')}`).toEqual([]);
    expect(cspErrors).toEqual([]);
  });

  test('a política de segurança bloqueia qualquer conexão de rede da página', async ({ page }) => {
    await page.goto('/');
    const csp = await page.locator('meta[http-equiv="Content-Security-Policy"]').getAttribute('content');
    expect(csp).toContain("connect-src 'none'");
    expect(csp).toContain("default-src 'self'");
    // Tentativa real de enviar dados: precisa falhar por causa da política.
    const result = await page.evaluate(async () => {
      try {
        await fetch('https://example.com/coleta', { method: 'POST', body: 'dados' });
        return 'enviou';
      } catch {
        return 'bloqueado';
      }
    });
    expect(result).toBe('bloqueado');
    // O envio em segundo plano (beacon) também é barrado: o navegador registra a violação da política.
    const blocked = await page.evaluate(
      () =>
        new Promise<string>((resolve) => {
          document.addEventListener('securitypolicyviolation', (e) => resolve(`${e.violatedDirective} ${e.blockedURI}`), { once: true });
          navigator.sendBeacon('https://example.com/coleta', 'dados');
          setTimeout(() => resolve('nenhuma violação'), 3000);
        }),
    );
    expect(blocked).toContain('connect-src');
    expect(blocked).toContain('example.com');
  });

  test('nada é guardado fora do navegador: só IndexedDB e preferências locais', async ({ page }) => {
    await freshStart(page);
    await createBaby(page, 'Bebê Local');
    const cookies = await page.context().cookies();
    expect(cookies).toEqual([]);
    const keys = await page.evaluate(() => Object.keys(localStorage));
    // Só preferências (tema, bebê ativo): nenhum dado de saúde.
    for (const key of keys) expect(key).toMatch(/^crescerbem\./);
    const stored = await page.evaluate(() => JSON.stringify(localStorage));
    expect(stored).not.toContain('Bebê Local');
  });
});

test.describe('PWA', () => {
  test('manifesto completo e ícones existentes', async ({ page, request }) => {
    await page.goto('/');
    const href = await page.locator('link[rel="manifest"]').getAttribute('href');
    const manifest = await (await request.get(new URL(href!, page.url()).toString())).json();
    expect(manifest.name).toBe('Crescer Bem');
    expect(manifest.lang).toBe('pt-BR');
    expect(manifest.display).toBe('standalone');
    expect(manifest.start_url).toBeTruthy();
    const sizes = manifest.icons.map((i: { sizes: string }) => i.sizes);
    expect(sizes).toContain('192x192');
    expect(sizes).toContain('512x512');
    for (const icon of manifest.icons) {
      const res = await request.get(new URL(icon.src, page.url()).toString());
      expect(res.ok(), icon.src).toBe(true);
    }
  });

  test('funciona sem internet depois da primeira visita', async ({ page, context }) => {
    await freshStart(page);
    await createBaby(page, 'Bebê Offline');
    await page.evaluate(async () => {
      await navigator.serviceWorker.ready;
    });
    // Espera o service worker assumir o controle da página.
    await page.reload();
    await expect.poll(() => page.evaluate(() => !!navigator.serviceWorker.controller)).toBe(true);
    await context.setOffline(true);
    await page.reload();
    await expect(page.getByText('Bebê Offline tem')).toBeVisible();
    // E continua possível registrar sem rede.
    await register(page, /^Momento/);
    await page.getByLabel('Título').fill('Sem internet');
    await page.getByRole('button', { name: 'Salvar' }).click();
    await goTab(page, 'Calendário');
    await expect(page.getByRole('link', { name: /Sem internet/ })).toBeVisible();
    await context.setOffline(false);
  });
});
