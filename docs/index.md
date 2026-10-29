# LDIC: Limitless Development Immersive Client

LDIC lets a Minecraft 1.12.2 modpack decide what players see before they reach a world: the
**main menu** and, soon, the **loading screen**. Both are driven by **themes**: JSON files a pack
ships in its config folder, with layout, images, fonts, animation and actions all described
there.

![The horizon theme](assets/screenshots/menu-horizon.png){ .shot }

<div class="grid cards" markdown>

-   :material-rocket-launch: **Get started**

    ---

    Install LDIC and make the menu your own in a few minutes.

    [:octicons-arrow-right-24: Installing](start/installing.md) ·
    [Your first theme](start/first-theme.md)

-   :material-palette: **Themes**

    ---

    Built-in themes, the theme file format, every element and action.

    [:octicons-arrow-right-24: Built-in themes](themes/built-in.md) ·
    [Elements](themes/elements.md)

-   :material-function-variant: **Expressions**

    ---

    Animate anything and bind it to live values with LDIC's expression language.

    [:octicons-arrow-right-24: Expressions](themes/expressions.md) ·
    [Game values](themes/game-values.md)

-   :material-progress-clock: **Loading screen**

    ---

    Cinematic and traditional loading screens with real progress, music and an optional
    photosensitivity notice. In development.

    [:octicons-arrow-right-24: Loading screen](loading/index.md)

</div>

## What LDIC does

- **Replaces the title screen** with a theme. Use a built-in theme as-is, extend one, or build
  your own from scratch.
- **Keeps other mods working.** Buttons other mods add to the main menu are kept by default, and a
  theme can move them into an area, hide them, or put its own button on top of one.
- **Never locks players out.** A broken theme falls back to the built-in default, and if that fails
  too, to the vanilla menu. Problems are logged with the file and the exact JSON path.
- **Reloads live.** Edit your theme and the open menu updates within a second.

LDIC is a client-only mod. A client with it can join any server, and servers do not need it.

!!! note "Coming from Custom Main Menu or Custom Loading Screen?"
    LDIC does the same jobs, but with its own theme format, which is not compatible with the old
    configs. [Your first theme](start/first-theme.md) shows how a typical Custom Main Menu layout looks
    in LDIC.
