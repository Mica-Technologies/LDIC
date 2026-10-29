# How LDIC works

For contributors. The code lives under `com.micatechnologies.minecraft.ldic`.

## Layers

| Package | Depends on Minecraft? | Job |
| --- | --- | --- |
| `expr` | No | The expression language: lexer, type-checking parser, nodes, built-in library |
| `theme` | No (Gson only) | Loading theme JSON, `extends` merging, compiling to definitions, the element schema |
| `ui` | No | The live element tree: layout, hover, focus, factories, hit testing |
| `client.render` | Yes | GL drawing, textures, fonts, panorama |
| `client.menu` | Yes | The main-menu screen, actions, theme loading and reload |
| `client` | Yes | Settings, host bindings (game values), asset paths, web cache |

The first three are plain Java and fully unit-tested (`./gradlew test`): the theme format and
layout maths can be checked without starting the game.

## A theme's life

1. **Load.** `ThemeLoader` reads `menu.json` and each theme it `extends`. It rewrites each file's
   relative asset paths to `theme://<location>/<path>`, then merges the chain with `JsonMerge`.
2. **Compile.** `ThemeCompiler` checks every key against `ElementSchema` and compiles values to
   typed expressions against a scope chain: the built-in library, then the client's
   `HostBindings`, then the theme's constants, state, functions and variables, then one child
   scope per element for its local values. Problems are collected with JSON paths; nothing throws.
3. **Run.** `UiTree` walks the tree once per frame. For each node it writes the node's local slots,
   evaluates its layout and properties, draws it, then visits its children. Factory copies share one
   template definition, which is why evaluating and drawing happen in the same pass.
4. **Draw.** `MenuRenderer` turns nodes into GL calls through `GlStateManager`, so vanilla's state
   cache stays correct.

## Expressions are compiled, not interpreted

Names resolve to `Slot` objects at compile time, and constant subtrees fold. Evaluating a number
or bool expression does not allocate. The host updates slots (`time`, `mouse_x`, …) once a frame,
before the tree pass.

## The menu screen

`LdicMenuScreen` extends `GuiMainMenu` so that mods adding buttons in
`InitGuiEvent` (which check `instanceof GuiMainMenu`) keep working. It skips everything vanilla
does in that class. Theme elements are never put in `buttonList`, so other mods only ever see
their own buttons there.

## Why not reuse Custom Main Menu or Custom Loading Screen?

LDIC replaces both, but shares no code with either. Custom Loading Screen is GPL-3.0, and both
have long-standing design limits: no scaling, silent failures, encoding bugs, and, in Custom
Loading Screen, deliberate sleeps and repeated resource reloads that slow every launch. LDIC's
format and engine were designed from the start to avoid those.
