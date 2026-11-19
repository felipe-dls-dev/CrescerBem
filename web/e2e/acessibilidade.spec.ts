import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { createBaby, dayOffset, freshStart, goTab, openSection, register } from './helpers';

// Capturas de tela opcionais para revisão visual: SHOTS=pasta npx playwright test acessibilidade
const SHOTS = process.env.SHOTS;

async function seed(page: Page) {
  await createBaby(page, 'Bebê Acessível', dayOffset(-40));
  await register(page, /^Momento/);
  await page.getByLabel('Título').fill('Primeiro sorriso');
  await page.getByLabel('Descrição (opcional)').fill('Sorriu para a avó.');
  await page.getByRole('button', { name: 'Salvar' }).click();
  await register(page, /^Medida/);
  await page.getByLabel('Valor (kg)').fill('4,250');
  await page.getByRole('button', { name: 'Salvar' }).click();
  await register(page, /^Sintoma/);
  await page.getByRole('button', { name: 'Cólica', exact: true }).click();
  await page.getByRole('button', { name: 'Salvar' }).click();
  await register(page, /^Alimentação/);
  await page.getByRole('button', { name: 'Salvar' }).click();
  await register(page, /^Consulta/);
  await page.getByLabel('Consulta ou especialidade').fill('Pediatra');
  await page.getByLabel(/^Data( \*)?$/).fill(dayOffset(3));
  await page.getByRole('button', { name: 'Salvar' }).click();
}

const SCREENS: { name: string; go: (page: Page) => Promise<void> }[] = [
  { name: 'hoje', go: async (p) => { await goTab(p, 'Hoje'); await openSection(p, 'Próximos compromissos'); await openSection(p, 'Registros de hoje'); await openSection(p, 'Alimentação de hoje'); } },
  { name: 'calendario', go: (p) => goTab(p, 'Calendário') },
  { name: 'bebe', go: async (p) => { await goTab(p, 'Bebê'); await openSection(p, 'Peso'); await openSection(p, 'Momentos'); } },
  { name: 'saude-vacinas', go: (p) => goTab(p, 'Saúde') },
  { name: 'saude-consultas', go: async (p) => { await goTab(p, 'Saúde'); await p.getByRole('tab', { name: 'Consultas' }).click(); } },
  { name: 'saude-medicacoes', go: async (p) => { await goTab(p, 'Saúde'); await p.getByRole('tab', { name: 'Medicações' }).click(); } },
  { name: 'saude-sintomas', go: async (p) => { await goTab(p, 'Saúde'); await p.getByRole('tab', { name: 'Sintomas' }).click(); } },
  { name: 'alimentacao', go: (p) => goTab(p, 'Alimentação') },
  { name: 'ajustes', go: async (p) => { await p.goto('/#/ajustes'); } },
  { name: 'relatorio', go: async (p) => { await p.goto('/#/relatorio'); } },
  { name: 'sobre', go: async (p) => { await p.goto('/#/sobre'); } },
  { name: 'form-bebe', go: async (p) => { await p.goto('/#/form/BABY'); } },
  { name: 'form-sintoma', go: async (p) => { await p.goto('/#/form/HEALTH_EVENT'); } },
  { name: 'form-alimentacao', go: async (p) => { await p.goto('/#/form/FEEDING?preset=FRUIT'); } },
  { name: 'form-medicamento', go: async (p) => { await p.goto('/#/form/MEDICATION'); } },
  { name: 'form-vacina', go: async (p) => { await p.goto('/#/form/VACCINE'); } },
  { name: 'form-consulta', go: async (p) => { await p.goto('/#/form/APPOINTMENT'); } },
  { name: 'form-medida', go: async (p) => { await p.goto('/#/form/GROWTH'); } },
];

for (const theme of ['claro', 'escuro'] as const) {
  test(`acessibilidade (WCAG 2.2 AA) em todas as telas, tema ${theme}`, async ({ page }, testInfo) => {
    test.setTimeout(180_000);
    await freshStart(page);
    // Sem bebê ainda: tela de boas-vindas.
    await page.evaluate((t) => localStorage.setItem('crescerbem.tema', t === 'escuro' ? 'DARK' : 'LIGHT'), theme);
    await page.reload();
    const problems: string[] = [];
    const check = async (name: string) => {
      const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']).analyze();
      for (const v of results.violations) {
        problems.push(`[${name}] ${v.id} (${v.impact}): ${v.help}\n    ${v.nodes.slice(0, 3).map((n) => n.target.join(' ')).join('\n    ')}`);
      }
      if (SHOTS) await page.screenshot({ path: `${SHOTS}/${testInfo.project.name}-${theme}-${name}.png`, fullPage: true });
    };

    await check('boas-vindas');
    await seed(page);
    for (const screen of SCREENS) {
      await screen.go(page);
      await page.waitForTimeout(150);
      await check(screen.name);
    }
    // Diálogo "Registrar".
    await page.goto('/#/hoje');
    await page.getByRole('button', { name: 'Registrar', exact: true }).click();
    await expect(page.getByRole('dialog')).toBeVisible();
    await check('dialogo-registrar');

    expect(problems, problems.join('\n')).toEqual([]);
  });
}

test('teclado: link para pular ao conteúdo, foco visível e diálogos que prendem o foco', async ({ page }) => {
  await freshStart(page);
  await createBaby(page);
  await page.reload(); // o link de pular existe para quem acabou de abrir a página
  await page.keyboard.press('Tab');
  await expect(page.getByRole('link', { name: 'Pular para o conteúdo' })).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page.locator('#conteudo')).toBeFocused();

  // Diálogo aberto: o foco fica dentro dele e Esc fecha.
  await page.getByRole('button', { name: 'Registrar', exact: true }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  for (let i = 0; i < 12; i++) {
    await page.keyboard.press('Tab');
    // O foco fica no diálogo (ou sai para a barra do navegador, que o JavaScript enxerga como o <body>);
    // nunca cai em algo da página que está atrás.
    expect(await page.evaluate(() => document.activeElement === document.body || !!document.activeElement?.closest('dialog'))).toBe(true);
  }
  await page.keyboard.press('Escape');
  await expect(dialog).toBeHidden();
});

test('alvos de toque têm pelo menos 44 px nos controles principais (celular)', async ({ page }, info) => {
  test.skip(info.project.name !== 'celular', 'Só faz sentido no celular');
  await freshStart(page);
  await createBaby(page);
  const targets = [
    page.getByRole('button', { name: 'Registrar', exact: true }),
    page.getByRole('button', { name: /Usar tema/ }),
    page.getByRole('link', { name: 'Ajustes' }),
    page.getByRole('navigation', { name: 'Principal' }).getByRole('link').first(),
  ];
  for (const t of targets) {
    const box = await t.boundingBox();
    expect(box!.width).toBeGreaterThanOrEqual(44);
    expect(box!.height).toBeGreaterThanOrEqual(44);
  }
});
