# Loading screen

!!! warning "In development"
    The loading screen is the next part of LDIC being built. This page describes what is planned,
    so pack makers can prepare art and music. Details may change before the first release.

LDIC will replace Forge's startup screen with a theme, using the same format, elements and
expressions as the [main menu](../themes/theme-files.md).

## Planned

- **Real progress.** Overall percentage, the current stage (construction, pre-init, textures and
  models, init, post-init) and the mod or step being worked on, for bars, text and scrolling
  logs. The stage order is correct for 1.12, where textures and models load before init.
- **No added load time.** The screen draws on Forge's own splash thread, with a frame cap, and
  never adds pauses or extra resource reloads.
- **Cinematic themes.** Full-screen artwork that slowly pans and crossfades between scenes,
  captions, letterboxing, and a smooth fade into the main menu.
- **Loading music.** Packs can ship music and sounds (OGG) that play while the game loads, with
  volume, looping and a crossfade into the menu. Players can turn it off.
- **Photosensitivity notice.** An optional, pack-enabled warning shown before anything animated,
  with a reduced-motion choice that is remembered.
- **Fallback.** If a loading theme fails, Forge's normal splash screen takes over.

## Variables a loading theme will have

The menu's [game values](../themes/game-values.md) plus `progress` (0–1), `stage`, `status`,
`sub_status`, the Forge progress bars, and `tip()` lines from a tips file.
