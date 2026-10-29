# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Purpose

LDIC (Limitless Development: Immersive Client) is a client-only Forge 1.12.2 mod that owns what a
player sees before reaching a world: a themeable **loading screen** (replacing FML's
`SplashProgress`) and a fully customizable **main menu** (replacing `GuiMainMenu`). It is the
Mica replacement for Custom Loading Screen (AlexIIL) and Custom Main Menu (lumien231) in the
Mica modpacks (Alto first). Their behaviour and config formats were studied; none of their code or
built-in themes is copied.

## Commands

There is no `java` on PATH; prefix every Gradle call with a JDK from `~/.jdks`:

```sh
# Build (also runs the reobfJar manifest guard in addon.gradle)
JAVA_HOME="$HOME/.jdks/azul-17.0.19" ./gradlew build

# Run all tests (JUnit 5; none yet)
JAVA_HOME="$HOME/.jdks/azul-17.0.19" ./gradlew test

# Dev client (logs: run/logs/latest.log)
JAVA_HOME="$HOME/.jdks/azul-17.0.19" ./gradlew runClient
```

Never build while a `runClient` is live: it corrupts the running classpath.

## Architecture

- Stack: GregTechCEu buildscript (`build.gradle`, version 1784768619, do not edit; local changes go
  in `addon.gradle`) on RetroFuturaGradle 1.4.7 / Gradle 8.9, Jabel for modern syntax targeting Java 8.
- `Ldic` is the `@Mod` class: `clientSideOnly = true`, `acceptableRemoteVersions = "*"`.
- `mixin.LdicCoreMod` registers `mixins.ldic.json` through MixinBooter's `IEarlyMixinLoader`, in dev and
  production alike (the `MixinConfigs` manifest attribute is metadata only). The loading screen hooks in
  through mixins/coremod because `SplashProgress` starts before any mod is constructed.
- Plans and requirements live in `docs/AGENT-PLANS/` (gitignored by design).

## Conventions & gotchas

- `buildscript.properties` keeps `modVersion =` empty: CI pins it to the release tag.
- Every mixin goes in the `"client"` array of `mixins.ldic.json`; the config is hand-maintained.
- Keep theme parsing, layout and expression code free of Minecraft types so it stays unit-testable.
- Third-party reference source goes in `example-source/` (gitignored), never into `src/`.
- CI workflows come from `dev-configurations/minecraft-mod-workflows/template/`; keep them in line
  with that template rather than editing them freely.
