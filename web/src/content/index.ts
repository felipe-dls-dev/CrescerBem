// Textos de privacidade, cópia de segurança e instalação da versão web do Crescer Bem.
// Linguagem simples, caráter educativo, sem diagnóstico. Escritos para revisão de linguagem (Fable).

export interface HelpSection {
  id: string;
  title: string;
  /** Parágrafos. */
  paragraphs?: string[];
  /** Passos numerados. */
  steps?: string[];
}

export const PRIVACY_SECTIONS: HelpSection[] = [
  {
    id: 'onde-ficam',
    title: 'Onde ficam os seus dados',
    paragraphs: [
      'Tudo o que você registra (bebê, vacinas, consultas, medidas, alimentação, sintomas e fotos) fica guardado só neste navegador, neste aparelho.',
      'O Crescer Bem não tem conta, não usa nuvem e não envia nada para nenhum servidor. Não há anúncios nem programas de medição de uso.',
    ],
  },
  {
    id: 'quem-ve',
    title: 'Quem consegue ver',
    paragraphs: [
      'Só quem usa este navegador, neste aparelho e com este perfil de usuário. Se mais pessoas usam o mesmo aparelho, elas também podem ver os registros.',
      'Dica: em aparelho compartilhado, use um perfil de usuário só seu ou faça uma cópia de segurança e apague os dados ao terminar.',
    ],
  },
  {
    id: 'risco-perda',
    title: 'Quando os dados podem se perder',
    paragraphs: [
      'Como não existe cópia na nuvem, os registros somem se você limpar os dados do site ou do navegador, desinstalar o navegador, trocar de aparelho ou usar uma janela anônima.',
      'Por isso, faça uma cópia de segurança de tempos em tempos. Ela é a única forma de guardar os dados fora deste navegador.',
    ],
  },
  {
    id: 'sem-sincronizacao',
    title: 'Aplicativo do celular e versão web',
    paragraphs: [
      'A versão web e o aplicativo de Android são separados e não trocam dados entre si. O que você registra em um não aparece no outro.',
    ],
  },
];

export const BACKUP_STEPS: HelpSection[] = [
  {
    id: 'fazer-copia',
    title: 'Como fazer uma cópia de segurança',
    steps: [
      'Abra Ajustes e vá até “Cópia de segurança”.',
      'Escolha uma senha com pelo menos 6 caracteres e repita para confirmar.',
      'Toque em “Salvar cópia de segurança”. Um arquivo é baixado no aparelho.',
      'Guarde o arquivo num lugar seguro, por exemplo um pen drive ou o seu e-mail pessoal.',
    ],
    paragraphs: [
      'A senha protege o arquivo. Se você esquecer a senha, não existe como recuperar a cópia. Anote a senha num lugar seguro.',
    ],
  },
  {
    id: 'restaurar-copia',
    title: 'Como restaurar uma cópia',
    steps: [
      'Abra Ajustes e vá até “Cópia de segurança”.',
      'Toque em “Restaurar de um arquivo” e escolha o arquivo salvo.',
      'Digite a senha da cópia.',
      'Confirme. Os dados que estão agora neste navegador serão trocados pelos da cópia.',
    ],
    paragraphs: ['Se a senha estiver errada ou o arquivo estiver danificado, nada é alterado.'],
  },
];

export const INSTALL_SECTIONS: HelpSection[] = [
  {
    id: 'instalar',
    title: 'Como instalar no aparelho',
    paragraphs: [
      'Você pode instalar o Crescer Bem como um aplicativo. Ele abre em tela cheia e funciona mesmo sem internet depois da primeira visita.',
    ],
    steps: [
      'No Chrome ou Edge (celular ou computador): toque em “Instalar no aparelho” em Ajustes, ou no menu do navegador escolha “Instalar app”.',
      'No iPhone ou iPad (Safari): toque no botão de compartilhar e escolha “Adicionar à Tela de Início”.',
    ],
  },
  {
    id: 'protecao',
    title: 'Proteção contra limpeza automática',
    paragraphs: [
      'Em Ajustes você pode pedir ao navegador para proteger os dados deste app contra limpeza automática quando o aparelho estiver com pouco espaço. Mesmo assim, a cópia de segurança continua sendo importante.',
    ],
  },
];

export const NO_REMINDERS_NOTE =
  'Esta versão não envia lembretes nem avisos. Confira as consultas e as vacinas na tela Hoje, ou anote os horários na agenda do seu celular.';

export const NO_ALERTS_NOTE = 'O Crescer Bem não substitui o pediatra. Em caso de emergência, ligue 192 (SAMU).';
