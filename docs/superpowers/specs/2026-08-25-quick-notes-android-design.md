# Quick Notes (FastNote) — Design Spec

Data: 2026-08-25
Status: aprovado para plano de implementação

## Visão geral

Aplicativo Android nativo de anotações rápidas. Diferencial: captura de nota
por texto ou voz a partir de um widget de home screen, sem precisar abrir o
app. Combina a velocidade de captura do Google Keep com organização
(tags, pastas, relacionamentos) inspirada no Obsidian.

Princípio central: **capturar primeiro, organizar depois**. Toda nota criada
rapidamente (widget, voz) cai numa Inbox; organização (tags, pasta, favorito,
arquivo) é decisão posterior, nunca bloqueia a captura.

## Escopo desta spec

Esta spec cobre o **MVP**: captura (app, widget texto, widget voz),
persistência local, Inbox, edição, exclusão, busca, tags, pastas, favoritos,
arquivamento.

**Fora de escopo (fase 2, spec própria depois):** relacionamento entre notas
e backlinks, compartilhamento de conteúdo de outros apps para o Quick Notes
(share intent), histórico de edição, backup, importação/exportação,
sincronização entre dispositivos.

## Plataforma e stack

- Android nativo, Kotlin.
- UI do app: Jetpack Compose.
- Widget de home screen: Jetpack Glance.
- Persistência: Room (offline-first — todas as funcionalidades do MVP
  funcionam sem internet).
- Voz: `SpeechRecognizer` on-device (sem custo de API, sem depender de
  internet na maioria dos aparelhos).
- Só Android por enquanto. iOS não é meta deste projeto — widgets iOS não
  suportam captura interativa (texto/voz) dentro do próprio widget, o que
  quebraria o diferencial central.

## Estrutura do projeto

Reaproveitada a organização já proposta no README, sem inventar camadas
novas:

```text
app/
├── data/
│   ├── local/
│   │   ├── database/
│   │   ├── dao/
│   │   └── entity/
│   └── repository/
├── domain/
│   ├── model/
│   └── usecase/
├── ui/
│   ├── home/        (Inbox)
│   ├── inbox/
│   ├── editor/
│   ├── search/
│   ├── tags/
│   ├── folders/
│   └── settings/
├── widget/
│   ├── QuickNoteWidget
│   └── QuickNoteWidgetReceiver
├── voice/
│   └── VoiceCapture
└── MainActivity
```

## Componentes

### 1. Widget (Jetpack Glance)

Widget de home screen com 4 elementos:

- **Nova nota (texto)** — abre captura rápida por texto.
- **Nova nota (voz)** — abre captura rápida por voz.
- **Abrir Inbox** — atalho que abre o app direto na tela de Inbox.
- **Notas recentes** — lista compacta das últimas N notas capturadas; tocar
  numa nota abre ela no editor.

### 2. Overlay Capture Service

Serviço responsável por exibir um popup flutuante (overlay) por cima de
qualquer app, para captura instantânea sem sair do contexto atual do
usuário. Implementado como foreground service usando a permissão
`SYSTEM_ALERT_WINDOW`.

- **Captura texto:** overlay sobe com campo de texto e teclado com foco
  automático. Ao salvar, grava a nota (Room) com `captureSource =
  WIDGET_TEXT`, `inbox = true`, e fecha o overlay.
- **Captura voz:** overlay sobe com indicador de gravação. `SpeechRecognizer`
  transcreve a fala. Comportamento após a transcrição é configurável (ver
  Settings): salvar automaticamente, revisar o texto antes de salvar, ou
  continuar editando dentro do app. Nota salva com `captureSource =
  WIDGET_VOICE`, `inbox = true`.

Prioridade de design: menor número possível de interações entre "toque no
widget" e "nota salva".

### 3. App principal (Jetpack Compose)

- **Inbox** — tela inicial. Lista cronológica reversa de notas com
  `inbox = true`. É a área central de revisão: o usuário decide aqui se
  organiza, favorita ou arquiva cada nota.
- **Editor** — criar/editar nota: título, conteúdo, tags, pasta, estados
  (favorito, arquivado, inbox).
- **Busca** — busca por título, conteúdo, tags e pasta.
- **Tags** — uma nota pode ter múltiplas tags (relação N:N). Gestão simples
  de criar/associar/remover tag.
- **Pastas** — organização hierárquica (pasta pode ter pasta-pai).
- **Favoritos** — view filtrada por `favorite = true`.
- **Arquivamento** — view filtrada por `archived = true`. Arquivar não
  exclui a nota, só tira da visão principal.
- **Settings** — configura o comportamento pós-transcrição da captura por
  voz (salvar automático / revisar antes / continuar editando).

## Modelo de dados (Room)

```text
Note
├── id
├── title
├── content
├── createdAt
├── updatedAt
├── folderId       (nullable, FK -> Folder)
├── favorite        (bool)
├── archived        (bool)
├── inbox           (bool)
└── captureSource    (APP | WIDGET_TEXT | WIDGET_VOICE)

Tag
├── id
└── name

NoteTag  (junção N:N)
├── noteId
└── tagId

Folder
├── id
├── name
└── parentId        (nullable, FK -> Folder — permite hierarquia)
```

`NoteRelation` (relacionamento entre notas / backlinks) e `Attachment` ficam
fora do MVP — o schema não precisa prever essas tabelas agora, entram junto
com a spec da fase 2.

## Fluxos de captura

```text
Texto:  Widget → Overlay (campo + teclado) → Salvar → Inbox
Voz:    Widget → Overlay (gravando) → SpeechRecognizer → [revisar?] → Salvar → Inbox
```

O usuário nunca escolhe pasta, tag ou categoria no momento da captura rápida
— essas decisões acontecem depois, na revisão da Inbox.

## Riscos e permissões

- **`SYSTEM_ALERT_WINDOW` (overlay):** Android não concede essa permissão
  via prompt padrão — o usuário precisa liberar manualmente em
  Configurações. É fricção real no primeiro uso. Mitigação: tela de
  onboarding explicando o motivo antes de direcionar o usuário para a
  tela de Configurações do sistema.
- **Disponibilidade do `SpeechRecognizer`:** depende do aparelho ter um
  motor de reconhecimento de voz instalado (presente na grande maioria dos
  Android via app do Google, mas não é garantido). Mitigação: se
  indisponível, avisar o usuário sem travar o app — a captura por texto
  continua funcionando normalmente.

## Testes

- **Unit:** DAOs (`Note`, `Tag`, `Folder`), regra de busca (título/conteúdo/
  tag/pasta).
- **Instrumented:** overlay abre/salva/fecha corretamente; widget Glance
  renderiza lista de notas recentes; ações do widget disparam o fluxo certo.
- **Manual:** fluxo de concessão da permissão de overlay (difícil de
  automatizar de forma confiável).

## MVP — checklist

- [ ] Criar notas (app e widget)
- [ ] Editar notas
- [ ] Excluir notas
- [ ] Inbox
- [ ] Busca
- [ ] Favoritos
- [ ] Arquivamento
- [ ] Tags
- [ ] Pastas
- [ ] Widget de captura por texto
- [ ] Widget de captura por voz
- [ ] Widget: atalho Inbox + notas recentes
- [ ] Persistência local com Room
