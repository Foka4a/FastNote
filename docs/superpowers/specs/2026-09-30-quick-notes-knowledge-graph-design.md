# Quick Notes — Knowledge Graph (Fase 2)

Data: 2026-09-30
Status: aprovado para plano de implementação

## Visão geral

Fase 2 do Quick Notes estende o MVP com um diferencial central: **grafo de conhecimento**. Notas deixam de ser silos isolados e passam a se conectar por relacionamentos explícitos, criando um sistema de organização mais potente — combinando a velocidade do Google Keep com a inteligência de grafo do Obsidian.

**Princípio:** texto é a fonte única de verdade. Links são escritos no conteúdo usando sintaxe `[[Título]]` e também podem ser criados via botão "ligar nota" no editor. A UI exibe relacionamentos bidireccionais (backlinks) e oferece visualização em tela dedicada de grafo.

## Escopo desta spec

Esta spec cobre:
- Ligação de notas por `[[Título]]` (inline no texto) e por botão "ligar nota".
- Criação de "fantasmas" — notas referenciadas mas não criadas.
- Persistência de links em tabela `NoteLink`.
- Migração Room de versão 1 → 2.
- Parser de sintaxe `[[...]]`.
- Seção "Ligações" no editor (backlinks + links de saída).
- Tela de grafo com nodes (notas + tags) e arestas (links entre notas + nota–tag).
- Gestos e interação no grafo (pan, zoom, arrastar, tocar).

**Fora de escopo (fase 3+):**
- Mini-grafo local no editor.
- Pastas como nós do grafo.
- Aliases e links para seção/bloco.
- Share intent com preserve de links.
- Sincronização entre dispositivos.

## Dependência do MVP

Esta spec **depende completamente** do MVP (spec 2026-08-25). Todos os componentes da fase 2 pressupõem que captura, Inbox, edição básica, tags, pastas e busca já funcionam perfeitamente.

## Plataforma e stack

Sem mudança em relação ao MVP:
- Android nativo, Kotlin.
- Jetpack Compose para UI.
- Room para persistência.
- Adiciona: parser de sintaxe (implementado em Kotlin puro, sem dependências).
- Canvas Compose + simulação force-directed própria para o grafo (sem bibliotecas externas como D3.js ou Force Graph).

## Modelo de dados (Room — versão 2)

### Entidades

Mantém todas as entidades da versão 1:

```text
NoteEntity v1
├── id
├── title
├── content
├── createdAt
├── updatedAt
├── folderId       (nullable, FK -> Folder)
├── favorite       (bool)
├── archived       (bool)
├── inbox          (bool)
└── captureSource  (APP | WIDGET_TEXT | WIDGET_VOICE)

TagEntity
├── id
└── name

NoteTagEntity (junção N:N)
├── noteId
└── tagId

FolderEntity
├── id
├── name
└── parentId
```

**Nova tabela:**

```text
NoteLink
├── sourceId       (PK, FK -> NoteEntity CASCADE on delete)
├── targetId       (nullable, FK -> NoteEntity SET NULL on delete)
├── targetTitle    (String — preserva o título digitado mesmo se o link é fantasma)
└── index          (PK — ordem dos links dentro da nota)
```

**Semântica de `NoteLink`:**
- `sourceId`: a nota que contém o link.
- `targetId`: a nota alvo (null = fantasma).
- `targetTitle`: título que apareceu entre `[[...]]` no conteúdo. Trim de espaços em branco, case-insensitive para match, mas preserva o original para UI.
- Primary key composto: `(sourceId, index)`. Garante ordem linear dos links dentro de cada nota e evita duplicatas na mesma posição.

## Migração Room (v1 → v2)

**Estratégia:** não-destrutiva, com backfill.

1. Cria tabela `NoteLink` vazia (versão 2).
2. Lê TODAS as notas existentes na versão 1.
3. Para cada nota, executa `LinkParser.extractLinks(content)` → lista de `[[Título]]`.
4. Para cada link extraído:
   - Query case-insensitive em `NoteEntity.title` para resolver o alvo.
   - Se encontra: `INSERT NoteLink(sourceId, targetId, targetTitle, index)`.
   - Se não encontra: `INSERT NoteLink(sourceId, NULL, targetTitle, index)` (fantasma).
5. Migration implementada com `Migration` class do Room (sem callback destructivo).

**Teste:** `MigrationTestHelper` valida que dados pré-existentes são preservados e links corretamente recuperados.

## Lógica de links

### LinkParser

Classe utilitária que extrai `[[...]]` do texto.

```kotlin
object LinkParser {
    fun extractLinks(content: String): List<LinkInfo>
    // LinkInfo(title: String, index: Int)
    
    data class LinkInfo(val title: String, val index: Int)
}
```

- Regex simples: `\[\[(.*?)\]\]`.
- Trim espaços em branco do título extraído.
- Preserva ordem de aparição (importante para `NoteLink.index`).

### Salvar nota

Sempre que a nota é salva (incluindo criação):

1. Parse `content` com `LinkParser.extractLinks()`.
2. DELETE todos os `NoteLink` com `sourceId = this.id`.
3. Para cada link extraído:
   - Query case-insensitive por título.
   - INSERT novo `NoteLink(sourceId, targetId?, title, index)`.
4. Persiste `NoteEntity` e `NoteLink` em transação.

### Renomear nota

Quando o título de uma nota muda:

1. Query TODAS as notas que contêm links para o título antigo (`NoteLink.targetTitle LIKE ?`).
2. Para cada uma:
   - Lê `content`.
   - Substitui `[[Título Antigo]]` → `[[Título Novo]]` (case-insensitive match, preserva capitalização original do novo).
   - Re-parse e recalcula `NoteLink`.
   - Persiste em transação.

**Estilo Obsidian:** renomear propaga automaticamente; é a operação mais cara do sistema, mas correção e consistência são prioridade.

### Criar nota com fantasmas pendentes

Quando uma nova nota é criada com título `T`:

1. Query `NoteLink` onde `targetTitle LIKE T` (case-insensitive) e `targetId IS NULL`.
2. Para cada match: UPDATE `NoteLink.targetId = <novo id da nota>`.

### Excluir nota

Quando uma nota é deletada:

- Constraint `CASCADE` em `NoteLink.sourceId` → remove links de saída.
- Constraint `SET NULL` em `NoteLink.targetId` → links que apontavam para ela viram fantasmas (targetId = NULL, targetTitle preservado).

## Editor — UI Ligações

### Botão "ligar nota"

- Posicionado no toolbar ou action bar do editor.
- Toque abre modal/bottom sheet com busca incremental de notas existentes.
- Seleção de uma nota: INSERE `[[Título]]` na posição do cursor ou no final do conteúdo.
- Se não há cursor definido: append ao fim com quebra de linha.

### Seção "Ligações" (no rodapé)

Exibida **sempre** que há links na nota (entrada ou saída). Estrutura:

```
Ligações
├─ ← Backlinks (notas que apontam para esta)
│  ├─ Nota A
│  ├─ Nota B (fantasma)
│  └─ …
└─ → Links de saída
   ├─ Nota C
   ├─ Nota D (fantasma — nota inexistente, toque cria)
   └─ …
```

- Cada item é um touchable que abre a nota correspondente (ou cria se fantasma).
- Fantasmas exibem badge/indicador visual (ex: ícone + ou cor diferente).
- Ordem segue `NoteLink.index` (ordem de aparição no conteúdo).

## Tela de Grafo (nova aba)

### Visão geral

Abagira navegável (tab/aba) na interface principal, ao lado de Inbox, Tags, etc. Renderiza um grafo interativo:

- **Nós:** notas (círculo) + tags (outro formato/cor).
- **Arestas:** links entre notas (linha) + associação nota–tag (outra cor/estilo).
- **Fantasmas:** nós desativados/esmaecidos com indicador visual.
- **Pastas:** NÃO são nós; são ignoradas na renderização.

### Canvas e física

- Implementado com Canvas Compose (sem SKRect ou dependência externa).
- Simulação force-directed simples:
  - Repulsão entre nós (Coulomb-like).
  - Atração entre nós conectados (spring-like).
  - Arrasto viscoso para estabilidade.
  - Loop de simulação a ~60 FPS, máx 10 iterações por frame para evitar travar.

**Limite conhecido:** ~300 nós total. Acima disso, degradação notável. Upgrade futuro: Barnes-Hut ou GPU-driven. Registrar esse limite na documentação de features.

### Gestos e interação

- **Pan:** dois dedos ou arrastar quando nenhum nó está selecionado.
- **Zoom:** pinça (pinch gesture).
- **Arrastar nó:** um dedo em um nó, arrasta ele pelo canvas enquanto a simulação continua.
- **Tocar nó (nota):** abre o editor da nota.
- **Tocar nó (tag):** filtra grafo mostrando apenas notas/tags conectadas àquela tag (toggle).
- **Tocar nó (fantasma):** abre tela de criação de nota com título pré-preenchido.

### Renderização

- Nós de nota: círculo (raio ~20dp), cor padrão (ex: azul).
- Nós de tag: quadrado (lado ~18dp), cor diferente (ex: verde).
- Nós fantasma: todos com opacidade reduzida (ex: 50%) + borda tracejada.
- Arestas nota–nota: linha reta, cor padrão (ex: cinza escuro).
- Arestas nota–tag: linha reta, cor diferente (ex: cinza claro).
- Labels de nó: título da nota/tag, truncado se muito longo, renderizado abaixo/ao lado do nó.

### Performance

- Atualização lazy: grafo recalculado quando há mudança (nova nota, link editado, exclusão). Não é real-time contínuo; atualiza uma vez por mudança.
- Culling simples: nós fora do viewport não recebem cálculo de física (apenas renderização skipped).
- State preservation: grafo lembra posição dos nós entre navegações (em ViewModel, não persistido).

## Fluxos principais

### Criar link digitando

```
Usuário abre editor
→ digita "ver [[Nota Existente]]"
→ ao salvar, parser extrai link
→ resolve `Nota Existente` (case-insensitive)
→ cria NoteLink(sourceId, targetId, "Nota Existente", index)
```

### Criar link via botão

```
Usuário abre editor
→ toca botão "ligar nota"
→ busca incremental abre
→ seleciona "Outra Nota"
→ `[[Outra Nota]]` inserido no cursor/fim
→ ao salvar, fluxo igual ao anterior
```

### Fantasma → nota real

```
Usuário referencia nota inexistente [[Futura Nota]]
→ link criado com targetId = NULL (fantasma)
→ usuário mais tarde cria "Futura Nota"
→ ao salvar, sistema resolve e atualiza NoteLink.targetId
→ editor mostra link como "normal" (não mais fantasma)
→ toque no grafo abre a nota
```

### Renomear

```
Usuário renomeia "Nota Antiga" → "Nota Nova"
→ sistema query todas as notas com links para "Nota Antiga"
→ substitui [[Nota Antiga]] → [[Nota Nova]] em cada
→ re-parse cada nota, atualiza NoteLink
→ backlinks apontam corretamente para "Nota Nova"
```

## Testes

### Unit tests

- **LinkParser:** extrai `[[...]]` corretamente; ignora `[texto normal]`; preserva espaços; múltiplos links; nenhum link.
- **NoteLink resolution:** query case-insensitive; encontra fantasmas; resolve fantasmas ao criar nota.
- **Rename logic:** substitui em múltiplas notas; transação atômica; não quebra em título com caracteres especiais.
- **Cascade delete:** deleta links de saída; converte targetId para NULL (fantasma).

### Instrumented tests

- **MigrationTestHelper:** versão 1 → 2 preserva dados; extrai links corretamente; fantasmas criados.
- **Editor: salvar com links:** NoteLink populado; order preservado.
- **Grafo: atualização:** nós aparecem/desaparecem; arestas recalculadas.

### Manual tests

- Gestos do grafo (pan, zoom, arrastar, tocar).
- Criar fantasma; clicar; criar nota real; ver resolução.
- Renomear nota; ver propagação em editor de notas que linkam.
- Performance com ~300 nós (monitor lag/FPS).

## Estimativa de esforço

- **Parser + lógica de links:** 2–3 dias (unit tests inclusos).
- **Migração Room:** 1–2 dias.
- **Seção Ligações no editor:** 1 dia.
- **Tela de Grafo:** 4–5 dias (simulação + gestos).
- **Testes (unit + instrumented):** 2–3 dias.
- **Testes manuais + polish:** 1–2 dias.

**Total estimado:** 11–16 dias de trabalho (1.5–2.5 semanas, dependendo de paralelismo).

## Fase 2 — checklist

- [ ] LinkParser (extração de `[[...]]`)
- [ ] NoteLink DAO
- [ ] Room migration v1 → v2
- [ ] Lógica de salvar nota (recalcula links)
- [ ] Lógica de renomear nota (propaga)
- [ ] Lógica de criar nota com fantasmas pendentes
- [ ] Botão "ligar nota" no editor
- [ ] Seção "Ligações" (backlinks + links de saída)
- [ ] Tela de Grafo (canvas + simulação)
- [ ] Gestos do grafo (pan, zoom, arrastar, tocar)
- [ ] Testes unit (LinkParser, rename, fantasma)
- [ ] Teste migração Room
- [ ] Testes instrumented (grafo, links)
- [ ] Testes manuais (gestos, performance)
