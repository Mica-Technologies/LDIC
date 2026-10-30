# Settings

LDIC has two settings files in `config/ldic/`:

- `settings.json` holds the **pack's** switches. A modpack ships it.
- `player.json` holds the **player's** own preferences. Packs do not ship it, so a pack update never
  resets a player's choices.

LDIC creates each one with its defaults if it is missing, and never rewrites an existing one, so a
launcher that manages `settings.json` does not see it change.

## settings.json

```json
{
  "menu": { "enabled": true, "fade_in": 0.6, "themed_options": true, "themed_pause": true, "themed_world_loading": true, "themed_worlds": true, "themed_servers": true, "themed_screens": true },
  "loading": { "enabled": true, "fps": 60, "hold_seconds": 0 },
  "logo_screen": { "enabled": true, "seconds": 3 },
  "photosensitivity_notice": { "enabled": true, "always": true, "seconds": 5 },
  "intro_skip": { "hold_seconds": 1.5 },
  "pack": { "name": "", "version": "" },
  "hotbar": { "style": "modern", "scale": 1.0, "auto_hide": false, "hide_delay": 2.0 },
  "inventory": { "full_screen": false, "scale_mod_screens": false, "themed": true, "accent": "#4FC3F7" },
  "debug": false
}
```

| Key | Default | Meaning |
| --- | --- | --- |
| `menu.enabled` | `true` | `false` leaves the vanilla main menu alone |
| `menu.fade_in` | `0.6` | Seconds the first menu after loading fades in from black; `0` for none |
| `menu.themed_options` | `true` | Use the menu theme's `options` screen in place of vanilla's, when the theme has one |
| `menu.themed_pause` | `true` | Use the menu theme's `pause` screen in place of vanilla's pause menu, when the theme has one |
| `menu.themed_world_loading` | `true` | Use the menu theme's `world_loading` screen while a world loads, when the theme has one |
| `menu.themed_worlds` | `true` | Use the menu theme's `worlds` screen in place of vanilla's world selection, when the theme has one |
| `menu.themed_servers` | `true` | Use the menu theme's `servers` screen in place of vanilla's server list, when the theme has one |
| `menu.themed_screens` | `true` | Draw other vanilla screens (Create New World, Edit World, settings pages, confirmations, statistics, advancements and more) with the menu theme's [skins](../themes/skins.md) for them, when it has them |
| `loading.enabled` | `true` | `false` leaves Forge's loading screen alone |
| `loading.fps` | `60` | Loading-screen frame cap (10–240). Lower leaves more CPU for loading. |
| `loading.hold_seconds` | `0` | With `debug`: keep the loading screen up this long after loading, to preview a theme |
| `logo_screen.*`, `photosensitivity_notice.*`, `intro_skip.*` | on | The logo card and notice before the loading screen; see [Logo card and notice](../loading/notices.md) |
| `pack.name` | `""` | Your pack's name; themes show it as `pack_name` |
| `pack.version` | `""` | Your pack's version; themes show it as `pack_version` |
| `hotbar.*` | modern, 1, off, 2 | The pack's defaults for the [hotbar](../inventory/hotbar.md): `style` (`modern` or `vanilla`), `scale` (0.5–2), `auto_hide`, `hide_delay` (seconds of movement before it hides). Players change them in game. |
| `inventory.full_screen` | `false` | Open inventories [full screen](../inventory/full-screen.md) by default |
| `inventory.scale_mod_screens` | `false` | Also scale up other mods' inventory screens in full-screen mode |
| `inventory.themed` | `true` | Draw the survival inventory, crafting table, chests and the full-screen creative inventory with the menu theme's [inventory screens](../themes/inventory.md), when it has them |
| `inventory.accent` | `#4FC3F7` | Accent for LDIC's inventory and hotbar when the menu theme has no `accent` constant |
| `debug` | `false` | List every theme problem on the menu, outline elements, save loading-screen and world-loading screenshots, log world-loading frame pacing |

## player.json

```json
{ "reduced_motion": false, "music_volume": 1, "loading_details": false }
```

| Key | Default | Meaning |
| --- | --- | --- |
| `reduced_motion` | `false` | Ask themes to keep motion to a minimum (no panning, zooming or spinning in the built-in themes) |
| `music_volume` | `1` | LDIC music volume, 0 to 1 (0 turns it off), on top of the game's Master and Music sliders |
| `loading_details` | `false` | The loading screen's F3 details panel is open; LDIC saves it when the player presses F3 |
| `hotbar`, `inventory` | the pack's | The player's own hotbar and full-screen choices, saved from LDIC's Config screen and the full-screen button; same keys as in settings.json |
| `creative_sort` | `{}` | Each creative tab's item order, saved when the player uses the sort button ([Creative search and sort](../inventory/creative.md)) |

## Other files LDIC keeps

| File | What it is |
| --- | --- |
| `config/ldic/menu.json`, `loading.json` | The menu and loading themes |
| `config/ldic/notices.json` | Optional reworded notices |
| `config/ldic/tips.txt` | Loading tips, one per line |
| `config/ldic/cache/loading-timings.json` | How long each loading step took last launch (for progress and ETA) |
| `config/ldic/cache/seen.json` | Notices and once-only intros this player has seen |
| `config/ldic/cache/*.bin` | Downloaded web images and text |
