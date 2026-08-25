# Quick Notes MVP — manual verification checklist

Run on a real device or emulator (API 26+). These paths aren't practically
covered by automated tests per the spec's risk section.

- [ ] First launch shows the overlay-permission onboarding screen
- [ ] Tapping "Liberar permissão" opens system Settings to the overlay screen
- [ ] After granting, relaunching the app skips onboarding and opens the Inbox
- [ ] Widget added to home screen shows: Texto, Voz, Inbox, and recent notes
- [ ] Tapping "Texto" on the widget shows the floating popup within ~1s, keyboard open
- [ ] Saving from the text popup closes it and the note appears in the Inbox
- [ ] Tapping "Voz" on the widget shows the recording indicator, then the transcription
- [ ] Voice capture behaves per the Settings choice (auto-save / review / continue editing)
- [ ] Tapping "Inbox" on the widget opens the app directly on the Inbox screen
- [ ] Tapping a recent note on the widget opens that note in the Editor
- [ ] Search finds a note by title, content, tag name, and folder name
- [ ] Favoriting and archiving a note moves it in/out of the Favorites/Archive screens
- [ ] On a device without a speech recognition engine, voice capture shows an error
      without crashing the app, and text capture still works
