# The LDIC API

Other mods can add their own entries to the title screen, the pause menu and the options
screen, and describe a settings page that LDIC draws for them. The entry and the page look like
the rest of the menu in whatever theme a pack uses, and still work when there is no theme at all.

A mod that only adds a plain button to those screens (in `InitGuiEvent`, or by mixin) needs
none of this: LDIC keeps such buttons working as they are. The API is for a cleaner fit.

## Setting up

LDIC is a client-only mod. Compile against its jar from the
[releases](https://github.com/Mica-Technologies/LDIC/releases) without bundling it:

```groovy
dependencies {
    compileOnly files('libs/ldic-<version>.jar')
}
```

The release jar works as it is in a development workspace: nothing in the API's own method
signatures is a Minecraft member, so there is nothing to deobfuscate.

Use only the classes in `com.micatechnologies.minecraft.ldic.api`. Everything else is LDIC's
internals and changes without notice.

Call the API from **client-side code only** (a client proxy, or behind
`event.getSide().isClient()`). If LDIC is optional for your mod, keep every use of the API in a
class of its own and only touch that class when LDIC is there:

```java
@Mod.EventHandler
public void init(FMLInitializationEvent event) {
    if (event.getSide().isClient() && Loader.isModLoaded("ldic")) {
        LdicCompat.register();   // the only class that imports the LDIC API
    }
}
```

Registration works at any time and in any load order: before LDIC's own pre-init, during init,
or later while the game runs. `LdicApi.API_VERSION` (1 for now) goes up when something is added.

## Entries

An entry is a labelled item on one of three screens: `MenuLocation.MAIN` (the title screen),
`MenuLocation.PAUSE` and `MenuLocation.OPTIONS`.

```java
LdicApi.menus().add(MenuLocation.PAUSE,
    MenuEntry.builder("mymod:map")
        .label(() -> I18n.format("mymod.menu.map"))
        .tooltip("Open the city map")
        .opens(parent -> new MyMapScreen(parent))
        .build());
```

| Builder call | Meaning |
| --- | --- |
| `builder(id)` | The entry's id. Use `modid:name`. Adding the same id to the same screen again replaces it in place. |
| `label(text)` / `label(supplier)` | Required. A supplier is read every frame, so it can follow the language or a setting. |
| `tooltip(text)` / `tooltip(supplier)` | Hover text. |
| `order(n)` | Position among other mods' entries; lower first, 1000 by default. Entries with the same order keep the order they were added in. |
| `visibleWhen(check)` | Shows the entry only while the check is true (only in multiplayer, only for operators, ...). |
| `enabledWhen(check)` | Greys the entry out while the check is false. |
| `opens(parent -> screen)` | Opens your own screen. `parent` is the screen the entry is on, to go back to. |
| `page(page)` | Opens a [page](#pages) that LDIC draws. |
| `runs(code)` | Runs code on the client thread. |

An entry needs exactly one of `opens`, `page` and `runs`. `LdicApi.menus().remove(location, id)`
takes one away again.

Where an entry appears:

- **In a theme that places entries** (the pause menus of the built-in Horizon, Launcher and
  Premiere themes), as one more item in the theme's own menu.
- **In any other theme**, as a flat button in the theme's accent colour, in the area the theme
  gives to other mods' buttons.
- **On a vanilla screen** (the menu theme is off, or the player opened the classic screen), as an
  ordinary button at the top right.

So an entry is always reachable. On the title screen a pack can hide other mods' buttons with
`"foreign_buttons": "hide"` in its theme; that hides entries there too, unless the theme places
them itself. Entries are never hidden on the pause and options screens.

Your suppliers and actions always run on the client thread. If one throws, LDIC logs it once and
carries on; it does not take the menu down.

## Pages

A page is a list of rows. LDIC draws it, scrolls it and handles the mouse and keyboard, so your
mod has no GUI code and the page matches the pack's theme.

```java
MenuPage page = MenuPage.builder("My Mod")
    .section("Display")
    .toggle("Fancy mode", cfg::isFancy, cfg::setFancy)
        .tooltip("Draws the extra detail")
    .slider("Range", 2, 32, 1, cfg::getRange, v -> cfg.setRange((int) v), v -> (int) v + " chunks")
    .slider("Opacity", 0, 1, 0, cfg::getOpacity, cfg::setOpacity)
        .enabledWhen(cfg::isFancy)
    .choice("Quality", Arrays.asList("Low", "Medium", "High"), cfg::getQuality, cfg::setQuality)
    .section("Data")
    .text(() -> cfg.count() + " places saved")
    .button("Reset to defaults", cfg::reset)
    .onClose(cfg::save)
    .build();

LdicApi.menus().add(MenuLocation.OPTIONS,
    MenuEntry.builder("mymod:settings").label("My Mod").page(page).build());
```

| Row | Call | Notes |
| --- | --- | --- |
| Heading | `section(label)` | |
| Text | `text(text)` / `text(supplier)` | A supplier is read every frame. |
| Switch | `toggle(label, get, set)` | Shown as `Label: ON` / `Label: OFF`, translated. |
| Slider | `slider(label, min, max, step, get, set)` | A step above 0 snaps to multiples of it from `min`; 0 is continuous. An optional last argument formats the value. |
| Choice | `choice(label, options, get, set)` | By position in `options`; a click moves to the next and wraps. |
| Button | `button(label, code)` | |

`tooltip(text)` and `enabledWhen(check)` apply to the row added just before them.

Getters are read every frame. A setter runs as soon as the player changes the row; a slider's
only runs when the snapped value actually changes, so a drag does not flood it. Save in
`onClose`, which runs each time the page closes.

One page can be used by several entries, such as the same settings from the pause menu and the
options screen.

With a menu theme in play the page is the theme's `mod_page` screen (theme authors: see
[Entries and pages from mods](../themes/mod-menus.md)). Without one it is a plain vanilla-style
screen with the same rows.

## Trying it without a second mod

With `"debug": true` in `config/ldic/settings.json`, LDIC registers a sample entry and a sample
page on all three screens the way another mod would.
