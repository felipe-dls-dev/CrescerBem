/// <reference types="vitest" />
import { defineConfig, type Plugin } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

// Política de segurança: nenhuma conexão de rede é permitida a partir da página.
// Só vale no build (o servidor de desenvolvimento precisa de WebSocket para o HMR).
const CSP = [
  "default-src 'self'",
  "script-src 'self'",
  "style-src 'self'",
  "img-src 'self' blob: data:",
  "font-src 'self'",
  "connect-src 'none'",
  "object-src 'none'",
  "base-uri 'self'",
  "form-action 'none'",
  "worker-src 'self'",
  "manifest-src 'self'",
].join('; ');

const cspPlugin = (): Plugin => ({
  name: 'crescer-bem-csp',
  apply: 'build',
  transformIndexHtml: () => [
    { tag: 'meta', attrs: { 'http-equiv': 'Content-Security-Policy', content: CSP }, injectTo: 'head-prepend' },
  ],
});

export default defineConfig({
  // Caminhos relativos: funciona em qualquer endereço (ex.: usuario.github.io/crescer-bem/).
  base: './',
  plugins: [
    react(),
    cspPlugin(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['icon.svg', 'icon-192.png', 'icon-512.png', 'icon-maskable-512.png'],
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,png,woff2}'],
        // Partes do jsPDF que o app nunca usa (conversão de HTML): não precisam ficar guardadas.
        globIgnores: ['**/html2canvas*', '**/purify*'],
        navigateFallback: 'index.html',
      },
      manifest: {
        name: 'Crescer Bem',
        short_name: 'Crescer Bem',
        description: 'Acompanhe a saúde do seu bebê. Tudo fica só neste aparelho.',
        lang: 'pt-BR',
        start_url: './',
        scope: './',
        display: 'standalone',
        background_color: '#FAF8F4',
        theme_color: '#2F6F6A',
        icons: [
          { src: 'icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'icon-512.png', sizes: '512x512', type: 'image/png' },
          { src: 'icon-maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
    }),
  ],
  build: { target: 'es2022', sourcemap: false },
  test: {
    environment: 'jsdom',
    include: ['src/**/*.test.ts', 'src/**/*.test.tsx'],
    setupFiles: ['src/test-setup.ts'],
  },
});
