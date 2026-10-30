# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Purpose

LDIC (Limitless Development: Immersive Client) is a client-only Forge 1.12.2 mod that owns what a
player sees before reaching a world: a themeable **loading screen** (drawn on Forge's splash
thread) and a fully customizable **main menu** (replacing `GuiMainMenu`). It is the Mica
replacement for Custom Loading Screen (AlexIIL) and Custom Main Menu (lumien231). Their behaviour
and config formats were studied; none of their code or built-in themes is copied.

User docs (the wiki) are in `docs/` and published to https://mica-technologies.github.io/LDIC/.

## Commands

There is no `java` on PATH; prefix every Gradle call with a JDK from `~/.jdks`:

```sh
# Build (runs unit tests and the reobfJar manifest guard in addon.gradle)
JAVA_HOME="$HOME/.jdks/azul-17.0.19" ./gradlew build

# Unit tests only (JUnit 5)
JAVA_HOME="$HOME/.jdks/azul-17.0.19" ./gradlew test

# Dev client (logs: run/logs/latest.log)
JAVA_HOME="$HOME/.jdks/azul-17.0.19" ./gradlew runClient

# Wiki (needs a Python venv with docs/requirements.txt); CI builds with --strict
mkdocs build --strict
```

Never build while a `runClient` is live: it corrupts the running classpath. Stop the dev client
first (it is the `java.exe` whose command line contains both `LDIC` and `GradleStart`).

## Architecture

Pure Java (no Minecraft types, fully unit-tested):
- `expr`: the theme expression language. Typed parser that resolves names to `Slot`s at compile
  time and folds constants; `Library` is the built-in function set.
- `theme`: `ThemeLoader` (extends chains, asset path rewriting to `theme://<location>/...`),
  `JsonMerge` (merge by id, at any depth; a changed `type` replaces), `ThemeCompiler` (validates
  against `ElementSchema`, never throws, reports to `Problems` with JSON paths).
- `ui`: `UiTree` evaluates and draws a screen in one pass per frame (factories share one template
  definition, so evaluation and drawing must stay in the same pass).
- `loading.ProgressModel`: Forge's bar stack to a weighted, monotonic percentage and ETA.
- `inventory`: `ItemQuery` (creative search syntax), `ItemSort` (stable tab orders),
  `HotbarVisibility` (auto-hide state), `CreativeLayout` (full-screen creative rectangles).

Minecraft-facing (`client`):
- `render`: all drawing goes through `Gfx.gl()`, a per-thread `GlBackend`: `McGl` (GlStateManager +
  Tessellator) on the main thread, `RawGl` (plain GL11) on the loading thread. Never call
  GlStateManager or the Tessellator from loading-screen code. `ThemeRenderer` draws every element
  type for both screens.
- `menu`: `MenuManager` (theme load, GuiOpenEvent swaps, music, server ticks).
  `ThemeScreenDriver` runs a theme screen (drawing, input, slider drags, other mods' buttons,
  links) inside a vanilla screen class: `LdicMenuScreen` extends `GuiMainMenu`, `LdicPauseScreen`
  extends `GuiIngameMenu` and
  `LdicOptionsScreen` extends `GuiOptions` (likewise `LdicWorldsScreen`, `LdicServersScreen` and
  `LdicModsScreen`, which extends Forge's `GuiModList`), on purpose, so other mods' `instanceof` button hooks
  keep working. Theme elements never enter `buttonList`; other mods' buttons are never hidden on
  the options or pause screens. `Actions` runs theme actions; `GameOptions` maps option names to
  `GameSettings`.
- `loading.LoadingScreen`: wraps Forge's splash runnable (via `MixinSplashProgress`), keeps
  Forge's lock/mutex/pause protocol, falls back to Forge's runnable on any failure.
- `audio`: Ogg Vorbis via the JOrbis classes Minecraft ships, played through Java Sound (never
  OpenAL: a second AL context would break game audio).
- `inventory`: `CreativeSearch` (filter + sort pass over a creative tab's list), `LdicCreativeScreen`
  + `LdicCreativeContainer` (full-screen creative; player-slot clicks go through
  `player.inventoryContainer` with a `CreativeCrafting` listener, as vanilla's Inventory tab does),
  `InventoryScreens` (full-screen toggle, creative swap, survival scaling by shrinking the screen's
  width/height at init), `InventoryStyle` (shared look; accent from the menu theme's `accent`).
- `hud.HotbarHud`: modern hotbar, size and auto-hide. One GL transform per bottom-group element,
  pushed in its overlay `Pre` and popped in `Post`; nothing pushed at size 1 and fully shown.
- `LdicConfigScreen` (Mods list Config): hotbar and inventory settings, saved to `player.json`.
- `hud` (OneConfig, compileOnly; the pack provides it): `OneConfigSupport` starts `LdicOneConfig`
  only when OneConfig's classes exist (nothing else may reference OneConfig types).
  `LdicOneConfig` mirrors the hotbar/inventory settings to and from `LdicSettings`, owns the HUD
  elements moved from SUM (`hud.elements`, field names kept so `migrateFromSum` can copy a
  player's `sum.json` entries) and the presets (`hud.presets`; SUM has a copy for its own HUDs:
  keep names, order and positions in step, and both leave room for the other mod's rows).
  OneConfig's own GUI polls the real mouse, so MCMCP cannot click inside it.
- `menu.InventoryTheme`: the menu theme's `inventory`/`crafting`/`container`/`creative` screens drawn
  in place of an inventory's background and labels (the screen keeps all its logic), at one layout
  unit per GUI pixel. Slot groups and named areas reach the theme through `area_x(name)` etc. and
  the `slots`/`player_model` elements (`render.InventoryHooks`; null on other screens). Furnaces,
  brewing, enchanting, anvils and mod screens are never themed (their backgrounds hold logic).

Mixins (`mixins.ldic.json`, all in `"client"`): `Accessor*` (vanilla screen fields LDIC reads),
`MixinSplashProgress` (thread takeover + null-safe texture deletes in `finish()`), `MixinMinecraft`
(lifts vanilla's 30 fps menu cap for LDIC screens; redirects the world start/stop sleeps so the
themed world-loading screen keeps drawing), `MixinLoadingScreenRenderer` (draws `menu.WorldLoading`
in place of the dirt progress screen), `MixinGuiContainerCreative` (tab search box + sort button),
`MixinGuiContainer` (theme in place of background + labels; full-screen toggle click before the
screen's own), `MixinForgeHooksClient`
(scales a full-screen inventory's drawing and converts the mouse for it and both draw events),
`MixinGuiIngameForge` (held-item name follows the hotbar's size and slide), `MixinGuiSlot` and
`MixinGuiListExtended` (skinned lists: no dirt, theme-scale clicks), `MixinGuiAdvancementTab` and
`MixinGuiAdvancement` (an advancement tab's 234x113 view sized to the skin's box, via
`menu.AdvancementMap`), `MixinGuiScreenAdvancements` (skin tab clicks), `MixinGuiButtonExt` (Forge
config buttons in skinned lists; named for dev and obfuscated, as a Forge class). `LdicCoreMod`
registers the config through MixinBooter's `IEarlyMixinLoader`.

Plans and decisions live in `docs/AGENT-PLANS/MASTER_PLAN.md` (gitignored by design).

## Conventions & gotchas

- `buildscript.properties` keeps `modVersion =` empty: CI pins it to the release tag.
- Built-in themes live in `src/main/resources/assets/ldic/themes/<id>/theme.json`;
  `BuiltinThemesTest` compiles each against stubs of the host bindings. When adding a host binding
  (`HostBindings`, `LoadingBindings`), add it to the stub there and to `docs/themes/game-values.md`
  or `docs/loading/index.md`.
- The loading screen's `minecraft` font is `BitmapFont` (ASCII only): keep non-ASCII characters out
  of loading themes that use it.
- Dev testing: with `"debug": true` in `run/config/ldic/settings.json` the loading screen saves
  `run/screenshots/ldic-loading-NNs.png` every 2 s and `loading.hold_seconds` keeps it up after
  loading. LDIC never rewrites an existing `settings.json`; player preferences live in `player.json`.
  Copy a built-in theme into `run/config/ldic/themes/<x>-dev/` and point
  `run/config/ldic/menu.json` at it for live reload. MCMCP (jar in `run/mods`) can screenshot the
  menu.
- A theme screen id that matches an `open` action name (`resource_packs`, `video`, `language`, ...
  in `Actions`) wins over the vanilla screen, so skin screen ids must avoid those names.
- Keep theme parsing, layout and expression code free of Minecraft types.
- Third-party reference source goes in `example-source/` (gitignored), never into `src/`.
- CI workflows come from `dev-configurations/minecraft-mod-workflows/template/`.
