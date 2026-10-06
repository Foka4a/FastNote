# Git flow

## Branches

- `main` — sempre estável, representa releases.
- `develop` — branch de integração, onde as `feature/*` são mescladas.
- `feature/<nome>` — nova funcionalidade, nasce de `develop`, volta pra `develop`.
- `fix/<nome>` — correção de bug, nasce de `develop`, volta pra `develop`.
- `release/<versao>` — preparação de release, nasce de `develop`, volta pra `main` e `develop`.
- `hotfix/<nome>` — correção urgente em produção, nasce de `main`, volta pra `main` e `develop`.

## Commits

Formato:

```text
tipo[nome-da-branch]: descrição curta das alterações
```

Tipos:

- `feature` — nova funcionalidade
- `fix` — correção de bug
- `chore` — manutenção, configuração, dependências
- `docs` — documentação
- `refactor` — refatoração sem mudar comportamento

Exemplo:

```text
feature[quick-capture-widget]: adiciona overlay de captura de texto
docs[quick-notes-android-design]: adiciona spec de design do MVP
```
