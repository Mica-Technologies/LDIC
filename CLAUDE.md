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

Minecraft-facing (`client`):
- `render`: all drawing goes through `Gfx.gl()`, a per-thread `GlBackend`: `McGl` (GlStateManager +
  Tessellator) on the main thread, `RawGl` (plain GL11) on the loading thread. Never call
  GlStateManager or the Tessellator from loading-screen code. `ThemeRenderer` draws every element
  type for both screens.
- `menu`: `MenuManager` (theme load, GuiOpenEvent swap, music, server ticks), `LdicMenuScreen`
  (extends `GuiMainMenu` on purpose so other mods' button hooks keep working; theme elements never
  enter `buttonList`), `Actions`.
- `loading.LoadingScreen`: wraps Forge's splash runnable (via `MixinSplashProgress`), keeps
  Forge's lock/mutex/pause protocol, falls back to Forge's runnable on any failure.
- `audio`: Ogg Vorbis via the JOrbis classes Minecraft ships, played through Java Sound (never
  OpenAL: a second AL context would break game audio).

Mixins (`mixins.ldic.json`, all in `"client"`): `MixinSplashProgress` (thread takeover + null-safe
texture deletes in `finish()`), `MixinMinecraft` (lifts vanilla's 30 fps menu cap for LDIC
screens). `LdicCoreMod` registers the config through MixinBooter's `IEarlyMixinLoader`.

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
  loading. LDIC rewrites `settings.json` (pretty-printed) on every launch, so write the whole file
  rather than patching lines. Copy a built-in theme into `run/config/ldic/themes/<x>-dev/` and point
  `run/config/ldic/menu.json` at it for live reload. MCMCP (jar in `run/mods`) can screenshot the
  menu.
- Keep theme parsing, layout and expression code free of Minecraft types.
- Third-party reference source goes in `example-source/` (gitignored), never into `src/`.
- CI workflows come from `dev-configurations/minecraft-mod-workflows/template/`.
