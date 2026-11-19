# Crescer Bem — versão web (PWA)

Versão web do app Crescer Bem. **Todos os dados ficam só no navegador** (IndexedDB), sem conta e sem nuvem.
Não sincroniza com o app Android. Sem lembretes/alarmes nesta versão.

## Comandos

```bash
npm install
npm run dev        # desenvolvimento
npm test           # testes unitários (domínio, dados, backup, PDF)
npm run build      # compila para dist/ (com CSP connect-src 'none')
npm run e2e        # testes de fluxo, privacidade, offline e acessibilidade (usa o build)
```

## Privacidade (garantias verificadas por teste)

- CSP com `connect-src 'none'`: a página não consegue enviar dados para fora.
- Sem CDN, fontes externas ou analytics; jsPDF e tudo mais vão empacotados.
- Cópia de segurança: JSON criptografado por senha (PBKDF2-SHA256 + AES-GCM), gerado no aparelho.
- Funciona offline (service worker) depois da primeira visita.

## Publicar no GitHub Pages

1. Crie o repositório no GitHub e envie o código (`git remote add origin ...; git push -u origin main`).
2. Em *Settings > Pages*, escolha **Source: GitHub Actions**.
3. O fluxo `.github/workflows/web-pages.yml` testa, compila e publica. O link fica em `https://<usuario>.github.io/<repositorio>/`.

Obs.: os dados de cada pessoa ficam no navegador dela, separados por endereço. O repositório pode ser público sem expor dados.

## Estrutura

- `src/domain/` regras portadas do Kotlin (idade, vacinas, relatório...), com testes equivalentes.
- `src/data/` IndexedDB (Dexie), repositórios e preferências.
- `src/backup/` cópia de segurança criptografada. `src/report/` PDF.
- `src/ui/` telas e componentes. `src/content/` textos de privacidade e ajuda.
- `e2e/` testes Playwright.
