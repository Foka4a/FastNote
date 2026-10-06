# Quick Notes Knowledge Graph — manual verification checklist

Run on a real device or emulator (API 26+), installed **over** the MVP build (v1 database) first.

## Testes instrumentados nunca executados nesta branch (sem device)

Rodar com um device/emulador conectado (`adb devices`) e registrar o resultado no PR:

```bash
./gradlew :app:connectedDebugAndroidTest
```

- [ ] `MigrationTest`
- [ ] `NoteRepositoryLinksTest`
- [ ] `NoteRepositoryImplTest`
- [ ] `LinksSectionTest`
- [ ] `QuickNotesNavHostTest`

Gestos do grafo (pan, pinça, arrastar nó) também nunca foram exercitados em device.

## Passo a passo manual

- [ ] Upgrade from the MVP build keeps every note; notes that already had `[[...]]` show them under "Ligações"
- [ ] Typing `[[Nota Existente]]` (different case/accents) and saving lists it under "Links de saída"; the target shows a backlink
- [ ] `[[Futura Nota]]` shows as "fantasma"; tapping it opens a new note titled "Futura Nota"; saving turns the link normal
- [ ] "Ligar nota" inserts `[[Título]]` at the cursor after tapping into the text, and on a new last line otherwise
- [ ] Renaming a linked note rewrites `[[Antigo]]` in the linking notes (reopen them to check)
- [ ] Deleting a linked note turns its incoming links into ghosts (editor + graph)
- [ ] Undo of that deletion (snackbar "DESFAZER") restores the note with the same id and the links resolve again (no ghosts)
- [ ] Links rows in "Ligações" are easy to tap (>= 48dp); TalkBack reads ghosts as "<título>, nota fantasma"
- [ ] Graph: one-finger pan on empty space, pinch zoom (content stays under the pinch centre), drag a node (neighbours follow), tap note opens editor
- [ ] Graph: tap a tag filters to its notes/tags (tag turns yellow); tap it again restores the full graph
- [ ] Graph: ghost nodes are faded with a dashed border; tapping one opens a prefilled new note
- [ ] Graph: TalkBack announces a single element "Grafo com N nós" (no per-node focus)
- [ ] Graph: leaving the tab and coming back keeps node positions
- [ ] Graph: with ~300 nodes (generate notes with links) panning/zooming stays usable; note FPS/lag in the PR
- [ ] Known minors (deferred): simulation wakes only when the finger lifts after pan/zoom; culled (off-screen) nodes stay still during a long pan
