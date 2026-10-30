# LDIC: Limitless Development: Immersive Client

A client-only Minecraft **1.12.2 Forge** mod for modpacks that want to control what players see:
a themeable **loading screen**, a fully customizable **main menu**, themed **options**, **pause**
and **world-loading** screens, full-screen **inventories** with search and sorting, and a
redesigned **hotbar**.

![GitHub Release](https://img.shields.io/github/v/release/Mica-Technologies/LDIC?sort=semver&display_name=tag&style=for-the-badge&logo=GitHub)
![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/Mica-Technologies/LDIC/total?style=for-the-badge&label=Downloads)

LDIC is Mica Technologies' replacement for Custom Loading Screen (AlexIIL) and Custom Main Menu
(lumien231). It is a clean-room implementation with its own theme format: their behaviour was
studied, but none of their code, config samples or textures were copied.

**Documentation:** https://mica-technologies.github.io/LDIC/

![The horizon menu theme](docs/assets/screenshots/menu-horizon.png)

## Features

### Loading screen

- Takes over Forge's splash thread from its first frame, so the screen animates smoothly
  through every loading stage. Falls back to Forge's own splash if anything goes wrong.
- Accurate progress: Forge's nested progress bars become one weighted, never-backwards
  percentage, with an ETA learned from the previous launch.
- Built-in themes (cinematic, traditional, minimal, console), or your own: images,
  fonts, slideshows, animation, music (Ogg Vorbis) and loading tips.
- Optional logo card and photosensitivity notice before loading; hold Space to skip.
- An F3 details panel (stages, mods, memory) that remembers whether it was open.

### Main menu, options, pause and world loading

- Themes are JSON: anchored layout that survives resizing and GUI scale changes, buttons,
  images, panoramas, slideshows, server status, web news, music and actions.
- A full expression language for animation and live values (time, player, servers, memory),
  compiled once per theme load and sandboxed.
- `extends` lets a theme inherit from a built-in one and change only what it needs.
- Live reload while you edit, with every theme problem reported by file and JSON path,
  never as a crash.
- A themed **world selection**: worlds as cards with icons, last played, game-mode, hardcore,
  cheats and version badges, search and sort, while play, create, edit, delete and re-create run
  vanilla's own code.
- A themed **server list**: servers as cards with icon, message of the day, players, ping and
  version warnings, LAN games and search, while every vanilla flow (add, edit, delete, direct
  connect, refresh, LAN) runs vanilla's own code.
- A themed **mod list**: mods as cards with logos and update badges, search and sort, and a
  details panel (authors, links, description, update changes) with each mod's Config button.
- **Skins** for other vanilla screens (create and edit world, add server, direct connect, open to
  LAN, death, disconnected, confirmations, the chat bar, sound, chat, skin, language, video, controls,
  resource packs, statistics, advancements with a full-window map, signs, books and Forge's config
  screens with their list editors): the screen
  keeps all its logic and the theme redraws it, placing the screen's own buttons, text fields and
  lists.
- Other mods' buttons keep working: LDIC's screens extend the vanilla screen classes, and
  LDIC never hides a mod's button on the options or pause screens.

### Inventory

![The full-screen creative inventory](docs/assets/screenshots/creative-full.png)

- **Full-screen inventories**, toggled by a button beside the panel (or a key) and remembered.
  Creative becomes one large screen: every tab in a sidebar (no tab pages), one search box,
  an item grid as large as the window allows, and the whole player inventory beside it, where
  Shift-click drops a full stack straight in. Survival, chests, furnaces and the rest scale up
  to the largest crisp size that fits.
- Search box on **every** creative tab, not only Search Items (click the title or press Ctrl+F).
- Richer search everywhere: several words in any order, `@mod`, `#tooltip`, `-exclude`.
- **Survival search and sort**: a search box above every survival inventory dims what does not
  match; a sort button merges and orders your inventory or a chest with ordinary clicks, so it
  is safe on any server.
- Sort any tab by tab order, name, mod or item id, ascending or descending. Each tab
  remembers its choice. Useful in modpacks, where registration order leaves tabs in disarray.
- **Themed inventories**: the menu theme can draw the survival inventory, crafting table, chests,
  furnaces (flame and progress), brewing stands (fuel, progress, bubbles), anvils (name box, cost)
  and the full-screen creative inventory (panels, slot cells, labels, the player) as theme
  screens, while the game keeps all their behaviour. The built-in modern themes include them.

### Hotbar

- A **modern** hotbar (translucent, with a gliding accent selection, the same size as vanilla's),
  or vanilla's.
- **Size** from 50% to 200%, independent of GUI scale. Health, food, armour, air, experience and
  the held item's name scale with it and keep their places.
- **Auto-hide**: once you have been moving for a moment, the hotbar group slides away; changing
  slot, picking things up, taking damage or opening a screen brings it back. A key (H) shows or
  hides it at any time.
- Settings in game under **Mods > LDIC > Config**.

See the wiki: [creative search and sort](https://mica-technologies.github.io/LDIC/inventory/creative/),
[full-screen inventory](https://mica-technologies.github.io/LDIC/inventory/full-screen/),
[hotbar](https://mica-technologies.github.io/LDIC/inventory/hotbar/),
[inventory theme screens](https://mica-technologies.github.io/LDIC/themes/inventory/).

### HUD (with OneConfig)

- Five **visual HUDs**: a compass tape, vitals bars, gear durability, effect rings and a frame-time
  graph, coloured by the menu theme.
- 32 information HUDs (coordinates, facing, biome, clock, day, health, food, armour, effects,
  tool durability, FPS, frame time, TPS, memory, ping, speed, light level, counters and more)
  plus ten custom-text slots with `%placeholders%`, each dragged into place in OneConfig's
  editor; eleven compact styles (one follows the menu theme's HUD colours) and layout presets
  apply in one click.
- LDIC's OneConfig page also holds the hotbar and inventory settings.
- These HUDs moved here from SUM; a player's saved layout comes with them. SUM keeps the HUDs
  that read its own server data, and the shared presets leave room for each other's rows.
- Needs [OneConfig](https://polyfrost.org/projects/oneconfig/) in the pack (Alto ships it). Without
  it, LDIC works as before with these two parts off.

## Installing

Drop the jar into `mods/`. It needs [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixin-booter).
LDIC is client-only: servers do not need it, and it joins any server.

Packs configure LDIC in `config/ldic/`: `settings.json` holds the pack's switches and
`player.json` holds each player's own choices, which pack updates never overwrite. See
[Settings](https://mica-technologies.github.io/LDIC/help/settings/).

## Building

There is no `java` on the PATH in the usual dev setup; point Gradle at a JDK 17+:

```sh
JAVA_HOME=/path/to/jdk17 ./gradlew build      # compile, unit tests, jar in build/libs/
JAVA_HOME=/path/to/jdk17 ./gradlew test       # unit tests only
JAVA_HOME=/path/to/jdk17 ./gradlew runClient  # dev client
```

The build uses [GregTechCEu Buildscripts](https://github.com/GregTechCEu/Buildscripts)
(RetroFuturaGradle), like the other Mica Technologies 1.12.2 mods. The mod itself targets Java 8.

## Architecture at a glance

```
com.micatechnologies.minecraft.ldic
├── expr/        # the theme expression language: typed parser, constant folding, built-ins
├── theme/       # theme loading, extends chains, merging, validation (never throws)
├── ui/          # evaluates and draws a theme screen in one pass per frame
├── loading/     # ProgressModel: Forge's bar stack to a weighted percentage and ETA
├── inventory/   # search queries, item ordering, hotbar visibility, full-screen layout
├── mixin/       # splash takeover, world-loading screens, inventories, hotbar, accessors
└── client/      # everything that touches Minecraft
    ├── render/      # one GL backend per thread (GlStateManager on the main thread, raw GL11 on the splash thread)
    ├── menu/        # menu, options and pause screens, actions, world loading
    ├── loading/     # the loading screen on Forge's splash thread
    ├── inventory/   # creative search and sort, full-screen screens and scaling
    ├── hud/         # the hotbar
    └── audio/       # Ogg Vorbis through Java Sound, never OpenAL
```

`expr`, `theme`, `ui`, `loading` and `inventory` use no Minecraft types, so the parts most likely
to be subtly wrong (expressions, theme merging, layout, progress, search and sort) are
unit-tested on a plain JVM.

## CI

- **Pull requests:** compile and unit tests.
- **Push to `main`:** builds and publishes a pre-release with checksums. A manual dispatch cuts a
  full `YYYY.MM.DD` release.
- The wiki in `docs/` is built with MkDocs (`--strict`) and published to GitHub Pages.

## Credits

| Project | License | How LDIC uses it |
| --- | --- | --- |
| [OneConfig](https://github.com/Polyfrost/OneConfig) by Polyfrost | LGPL-3.0 + Additional Terms | The HUD elements and LDIC's settings page are built on it (compile-only; the pack provides it). The loading screen and menu do not use it. |
| [EvergreenHUD](https://github.com/Polyfrost/EvergreenHUD) by Polyfrost | GPL-3.0 | Design reference for the HUD-element catalog (one OneConfig HUD per piece of information), by way of SUM, where these HUDs started. No code reused. |
| Custom Main Menu (lumien231), Custom Loading Screen (AlexIIL) | MIT, GPL-3.0 | Behaviour and config shapes studied; LDIC is a clean-room replacement with none of their code, samples or textures. |

## License

LGPL 2.1; see [LICENSE](LICENSE).
