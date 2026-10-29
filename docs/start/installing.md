# Installing

1. Download the latest `ldic-<version>.jar` from the
   [releases page](https://github.com/Mica-Technologies/LDIC/releases).
2. Put it in your instance's `mods/` folder. LDIC needs Forge for Minecraft 1.12.2 and
   [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixin-booter) (most modern 1.12.2
   packs already have it).
3. Start the game. The main menu now uses the built-in **classic** theme, which looks like vanilla.

On first start LDIC creates `config/ldic/`:

| File | What it is |
| --- | --- |
| `config/ldic/menu.json` | Your main menu theme. It starts as a one-line theme that extends `builtin:classic`. |
| `config/ldic/settings.json` | Switches that are not part of a theme. See [Settings](../help/settings.md). |
| `config/ldic/themes/` | Optional: full themes, one folder each, that `menu.json` can extend. |
| `config/ldic/cache/` | Images downloaded from the web, so menus still show them offline. Safe to delete. |

## For modpacks

Ship `config/ldic/menu.json` (and any images, fonts or theme folders it uses) with your pack. Set
your pack's name and version in `config/ldic/settings.json`. Themes can show them, and a
launcher can write them on update.

!!! tip "Do not ship Custom Main Menu alongside LDIC"
    Both replace the title screen. If both are installed, whichever runs last wins.
