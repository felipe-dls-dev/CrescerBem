import { expect, test } from '@playwright/test';
import { createBaby, dayOffset, freshStart, goTab, openSection, register } from './helpers';

test.describe('Fluxos principais', () => {
  test.beforeEach(async ({ page }) => {
    await freshStart(page);
  });

  test('primeiro acesso explica a privacidade e permite cadastrar o bebê', async ({ page }) => {
    await expect(page.getByText('ficam só neste navegador')).toBeVisible();
    await expect(page.getByText('não substitui a avaliação de um profissional de saúde')).toBeVisible();
    await createBaby(page, 'Ana Teste', dayOffset(-40));
    await expect(page.getByText('Ana Teste tem 5 semanas e 5 dias')).toBeVisible();
  });

  test('formulário do bebê mostra erros em linguagem simples', async ({ page }) => {
    await page.getByRole('link', { name: 'Cadastrar bebê' }).first().click();
    await page.getByRole('button', { name: 'Salvar' }).click();
    const alert = page.getByRole('alert').first();
    await expect(alert).toContainText('Informe o nome ou um apelido.');
    await expect(alert).toContainText('Informe a data de nascimento.');
  });

  test('momento aparece em Hoje e em Bebê', async ({ page }) => {
    await createBaby(page);
    await register(page, /^Momento/);
    await page.getByLabel('Título').fill('Primeiro sorriso');
    await page.getByRole('button', { name: 'Salvar' }).click();
    await openSection(page, 'Registros de hoje');
    await expect(page.getByRole('link', { name: /Primeiro sorriso/ })).toBeVisible();
    await goTab(page, 'Bebê');
    await openSection(page, 'Momentos');
    await expect(page.getByRole('link', { name: /Primeiro sorriso/ })).toBeVisible();
  });

  test('medida de peso atualiza o resumo e valores improváveis pedem confirmação', async ({ page }) => {
    await createBaby(page);
    await register(page, /^Medida/);
    await page.getByLabel('Valor (kg)').fill('3450');
    await page.getByRole('button', { name: 'Salvar' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Confira se o valor está em quilos');
    await dialog.getByRole('button', { name: 'Corrigir' }).click();
    await page.getByLabel('Valor (kg)').fill('4,250');
    await page.getByRole('button', { name: 'Salvar' }).click();
    await expect(page.locator('.stat .value', { hasText: '4,250 kg' })).toBeVisible();
  });

  test('vacinas: calendário sugerido, marcar como aplicada e não duplicar', async ({ page }) => {
    await createBaby(page);
    await goTab(page, 'Saúde');
    await expect(page.getByText('Ao nascer', { exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Adicionar calendário sugerido' }).click();
    await expect(page.getByText('O calendário sugerido já está completo.')).toBeVisible();

    await openSection(page, 'Ao nascer');
    await page.getByRole('link', { name: /BCG/ }).click();
    await page.getByRole('button', { name: 'Aplicada', exact: true }).click();
    await page.getByRole('button', { name: 'Salvar' }).click();
    await openSection(page, 'Ao nascer');
    await expect(page.getByRole('link', { name: /BCG/ })).toContainText('Aplicada');
  });

  test('consulta agendada aparece em Próximas', async ({ page }) => {
    await createBaby(page, 'Bebê Teste', dayOffset(-40), { vaccines: false });
    await register(page, /^Consulta/);
    await page.getByLabel('Consulta ou especialidade').fill('Pediatra de rotina');
    await page.getByLabel(/^Data( \*)?$/).fill(dayOffset(7));
    await page.getByRole('button', { name: 'Salvar' }).click();
    await goTab(page, 'Saúde');
    await page.getByRole('tab', { name: 'Consultas' }).click();
    await expect(page.getByRole('link', { name: /Pediatra de rotina/ })).toBeVisible();
    await expect(page.getByText('Próximas (1)')).toBeVisible();
  });

  test('sintoma: febre em bebê pequeno mostra sinais de atenção com aviso educativo', async ({ page }) => {
    await createBaby(page, 'Bebê Teste', dayOffset(-30));
    await register(page, /^Sintoma/);
    await page.getByRole('button', { name: 'Febre', exact: true }).click();
    await page.getByLabel('Temperatura (opcional)').fill('38,2');
    await page.getByRole('button', { name: 'Salvar' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Sinais que merecem atenção');
    await expect(dialog).toContainText('Febre em bebê com menos de 3 meses.');
    await expect(dialog).toContainText('ligue 192 (SAMU)');
    await dialog.getByRole('button', { name: 'Entendi' }).click();
    await expect(page.getByText('Bebê Teste tem')).toBeVisible();
  });

  test('sintoma exige ao menos um sintoma', async ({ page }) => {
    await createBaby(page);
    await register(page, /^Sintoma/);
    await page.getByRole('button', { name: 'Salvar' }).click();
    await expect(page.getByRole('alert').first()).toContainText('Escolha pelo menos um sintoma.');
  });

  test('alimentação: registro rápido atualiza o resumo do dia', async ({ page }) => {
    await createBaby(page);
    await goTab(page, 'Alimentação');
    await page.getByRole('link', { name: 'Fórmula', exact: true }).click();
    await page.getByLabel('Quantidade em ml (opcional)').fill('90');
    await page.getByRole('button', { name: 'Salvar' }).click();
    await expect(page.getByText('90 ml').first()).toBeVisible();
    await expect(page.getByText('1 registro(s)').first()).toBeVisible();
  });

  test('alimentação: alimento novo entra na lista de já experimentados', async ({ page }) => {
    await createBaby(page);
    await goTab(page, 'Alimentação');
    await page.getByRole('link', { name: 'Fruta', exact: true }).click();
    await page.getByLabel('Qual fruta?').fill('Banana');
    await page.getByLabel('Primeira vez com este alimento').check();
    await page.getByRole('button', { name: 'Salvar' }).click();
    await openSection(page, 'Alimentos já experimentados');
    await expect(page.getByText('Banana', { exact: true })).toBeVisible();
    await expect(page.getByText('Alimentos novos: Banana')).toBeVisible();
  });

  test('medicamento repetido agenda todas as doses e permite marcar como realizada', async ({ page }) => {
    await createBaby(page);
    await register(page, /^Agendar medicamento/);
    await page.getByLabel('Nome do medicamento').fill('Remédio Teste');
    await page.getByLabel('Repetir esse horário').check();
    await page.getByLabel('A cada quantas horas').fill('8');
    await page.getByLabel('Por quantos dias').fill('1');
    await expect(page.getByText('Serão agendadas 3 doses')).toBeVisible();
    await page.getByRole('button', { name: 'Salvar' }).click();
    await goTab(page, 'Saúde');
    await page.getByRole('tab', { name: 'Medicações' }).click();
    await expect(page.getByText('Próximas (3)')).toBeVisible();
    await page.getByRole('button', { name: 'Realizar agora' }).first().click();
    await expect(page.getByText('Próximas (2)')).toBeVisible();
    await expect(page.getByText('Histórico (1)')).toBeVisible();
  });

  test('excluir um registro pede confirmação', async ({ page }) => {
    await createBaby(page);
    await register(page, /^Momento/);
    await page.getByLabel('Título').fill('Para excluir');
    await page.getByRole('button', { name: 'Salvar' }).click();
    await goTab(page, 'Bebê');
    await openSection(page, 'Momentos');
    await page.getByRole('link', { name: /Para excluir/ }).click();
    await page.getByRole('button', { name: 'Excluir' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Excluir' }).click();
    await openSection(page, 'Momentos');
    await expect(page.getByText('Para excluir')).toHaveCount(0);
  });

  test('calendário mostra marcadores acessíveis e permite registrar no dia', async ({ page }) => {
    await createBaby(page);
    await register(page, /^Momento/);
    await page.getByLabel('Título').fill('Marco do dia');
    await page.getByRole('button', { name: 'Salvar' }).click();
    await goTab(page, 'Calendário');
    await expect(page.getByRole('button', { name: /hoje\. 1 registro\(s\): Momento/ })).toBeVisible();
    await expect(page.getByRole('link', { name: /Marco do dia/ })).toBeVisible();
    await page.getByRole('button', { name: 'Registrar neste dia' }).click();
    await expect(page.getByRole('dialog')).toContainText('O que você quer registrar?');
  });

  test('cor do tema: muda na hora ao tocar e volta ao sair sem salvar', async ({ page }) => {
    await createBaby(page, 'Cor Teste', dayOffset(-40));
    const primary = () => page.evaluate(() => document.documentElement.style.getPropertyValue('--primary'));
    const antes = await primary();
    await page.goto('/#/form/BABY?id=1');
    await page.getByRole('radio', { name: 'Rosa' }).click();
    await expect.poll(primary).toBe('#9C4A6B');
    await page.goto('/#/hoje');
    await expect.poll(primary).toBe(antes);
  });

  test('vários bebês: troca de bebê muda os dados mostrados', async ({ page }) => {
    await createBaby(page, 'Primeiro Teste', dayOffset(-100));
    await page.goto('/#/form/BABY');
    await page.getByLabel(/^Nome ou apelido/).fill('Segundo Teste');
    await page.getByLabel(/^Data de nascimento/).fill(dayOffset(-10));
    await page.getByRole('button', { name: 'Salvar' }).click();
    await expect(page.getByText('Segundo Teste tem 1 semana', { exact: false }).or(page.getByText('Segundo Teste tem 10 dias'))).toBeVisible();
    await page.getByLabel('Bebê ativo').selectOption({ label: 'Primeiro Teste' });
    await expect(page.getByText('Primeiro Teste tem')).toBeVisible();
  });

  test('tema escuro persiste ao recarregar', async ({ page }) => {
    await createBaby(page);
    await page.getByRole('button', { name: 'Usar tema escuro' }).click();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
    await page.reload();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
  });
});

test.describe('Cópia de segurança, relatório e apagar tudo', () => {
  test.beforeEach(async ({ page }) => {
    await freshStart(page);
    await createBaby(page, 'Bebê Cópia', dayOffset(-40));
    await register(page, /^Momento/);
    await page.getByLabel('Título').fill('Momento da cópia');
    await page.getByRole('button', { name: 'Salvar' }).click();
  });

  test('exportar, apagar tudo e restaurar devolve os dados', async ({ page }) => {
    await page.goto('/#/ajustes');
    await page.getByLabel('Senha da cópia').fill('senha-de-teste');
    await page.getByLabel('Repita a senha').fill('senha-de-teste');
    const [download] = await Promise.all([page.waitForEvent('download'), page.getByRole('button', { name: 'Salvar cópia de segurança' }).click()]);
    expect(download.suggestedFilename()).toMatch(/^crescer-bem-copia-\d{4}-\d{2}-\d{2}\.json$/);
    const file = await download.path();

    // Apagar tudo (duas confirmações).
    await page.getByRole('button', { name: 'Apagar todos os dados' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Continuar' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Apagar tudo' }).click();
    await expect(page.getByRole('heading', { name: 'Boas-vindas ao Crescer Bem' })).toBeVisible();

    // Restaurar com senha errada não altera nada; com a certa, traz tudo de volta.
    await page.goto('/#/ajustes');
    await page.locator('input[type=file][accept*=json]').setInputFiles(file);
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel('Senha da cópia').fill('senha-errada');
    await dialog.getByRole('button', { name: 'Substituir meus dados' }).click();
    await expect(dialog).toContainText('Senha incorreta ou arquivo alterado.');
    await dialog.getByLabel('Senha da cópia').fill('senha-de-teste');
    await dialog.getByRole('button', { name: 'Substituir meus dados' }).click();
    await expect(page.getByText('Bebê Cópia tem')).toBeVisible();
    await goTab(page, 'Bebê');
    await openSection(page, 'Momentos');
    await expect(page.getByText('Momento da cópia')).toBeVisible();
  });

  test('senhas diferentes ou curtas não deixam exportar', async ({ page }) => {
    await page.goto('/#/ajustes');
    const button = page.getByRole('button', { name: 'Salvar cópia de segurança' });
    await page.getByLabel('Senha da cópia').fill('123');
    await expect(page.getByText('Use pelo menos 6 caracteres.')).toBeVisible();
    await expect(button).toBeDisabled();
    await page.getByLabel('Senha da cópia').fill('senha-de-teste');
    await page.getByLabel('Repita a senha').fill('outra-coisa');
    await expect(page.getByText('As senhas não são iguais.')).toBeVisible();
    await expect(button).toBeDisabled();
  });

  test('arquivo que não é cópia do Crescer Bem é recusado sem alterar nada', async ({ page }) => {
    await page.goto('/#/ajustes');
    await page.locator('input[type=file][accept*=json]').setInputFiles({ name: 'x.json', mimeType: 'application/json', buffer: Buffer.from('{"oi":1}') });
    await expect(page.getByText('não parece ser uma cópia de segurança do Crescer Bem')).toBeVisible();
    await page.goto('/#/hoje');
    await expect(page.getByText('Bebê Cópia tem')).toBeVisible();
  });

  test('relatório em PDF é gerado e baixado no aparelho', async ({ page }) => {
    await page.goto('/#/relatorio');
    await page.getByRole('button', { name: 'Gerar PDF' }).click();
    await expect(page.getByText('Relatório pronto')).toBeVisible();
    const [download] = await Promise.all([page.waitForEvent('download'), page.getByRole('button', { name: 'Baixar' }).click()]);
    expect(download.suggestedFilename()).toMatch(/^crescer-bem-bebe-copia-.*\.pdf$/);
    const fs = await import('node:fs/promises');
    const bytes = await fs.readFile((await download.path())!);
    expect(bytes.subarray(0, 5).toString()).toBe('%PDF-');
  });

  test('apagar um bebê remove os registros dele', async ({ page }) => {
    await page.goto('/#/ajustes');
    await page.getByRole('button', { name: 'Excluir Bebê Cópia' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Continuar' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Excluir definitivamente' }).click();
    await expect(page.getByText('Nenhum bebê cadastrado.')).toBeVisible();
  });
});
