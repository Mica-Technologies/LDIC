# Actions

An `action` is what clicking an element does. Write it as:

- a name, for actions with no arguments: `"action": "quit"`;
- an object: `"action": { "type": "open", "screen": "options" }`;
- an array, to run several in order: `"action": [ {"type": "sound", "sound": "minecraft:block.note.pling"}, "back" ]`.

Arguments marked *text* accept `{expression}` parts.

| Type | Arguments | Does |
| --- | --- | --- |
| `open` | `screen` *text* | Opens a screen of this theme, or a built-in one (below) |
| `back` | | Returns to the previous theme screen |
| `open_link` | `url` *text*, `confirm` (default `true`) | Opens a web page, asking first unless `confirm` is `false` |
| `connect` | `address` *text*, `name` *text* | Joins a server (`host` or `host:port`) |
| `load_world` | `folder` *text* | Loads a singleplayer world by its folder name in `saves/` |
| `open_folder` | `path` *text* | Opens a folder inside the game directory (`screenshots`, `resourcepacks`, …) |
| `mod_config` | `modid` *text* | Opens a mod's config screen; without `modid` on the [mod list](mods.md), the selected mod's |
| `sound` | `sound` *text*, `volume`, `pitch` | Plays a sound event |
| `set` | `name`, `value` *expr* | Sets a [`state`](theme-files.md#top-level-keys) value |
| `toggle` | `name` | Flips a bool `state` value |
| `reload` | | Reloads the theme from disk |
| `option` | `name` *text* | Cycles a toggle or choice game option (`VIEW_BOBBING`, `GUI_SCALE`, ...) and saves it |
| `difficulty` | | Cycles the world difficulty (only the singleplayer host, while it is not locked) |
| `classic_options` | | Opens vanilla's own options screen instead of the themed one |
| `resume` | | Closes the [pause menu](pause.md) and returns to the game |
| `disconnect` | | Saves and quits to the title screen (singleplayer), or disconnects from the server |
| `cheats` | | Turns Allow Cheats on or off in the singleplayer world, saved with the world (vanilla only offers it under Open to LAN, for the session) |
| `classic_pause` | | Opens vanilla's own pause menu instead of the themed one |
| `quit` | | Quits the game |

## Built-in screens for `open`

`singleplayer`, `create_world`, `multiplayer`, `video`, `controls`, `sounds`, `skin`, `chat`,
`language`, `resource_packs`, `mods`, `snooper`, `credits`, `realms`, and `options` (the
[themed options screen](options.md) if the theme has one, else vanilla's). In a world also
`advancements`, `statistics`, `lan` (singleplayer only) and `mod_options`. Plus `main` (this
theme's title screen) and any screen your theme defines. A theme screen with the same name as a
built-in one wins, except `options`, which always opens as the options screen.

## Tabs with state

```json
"state": { "tab": 0 },
"screens": { "main": { "elements": [
  { "type": "button", "text": "News",    "action": { "type": "set", "name": "tab", "value": "0" } },
  { "type": "button", "text": "Changes", "y": 24, "action": { "type": "set", "name": "tab", "value": "1" } },
  { "type": "image", "image": "news.png",    "visible": "tab == 0", "x": 210 },
  { "type": "image", "image": "changes.png", "visible": "tab == 1", "x": 210 }
] } }
```

A failing action, such as a missing world or an unknown screen name, is logged and does nothing
else.

## World selection

`world_play`, `world_create`, `world_edit`, `world_delete`, `world_recreate` and `world_sort` work on the [world selection](worlds.md) screen.
`mod_sort` (and `mod_config` without a `modid`) work on the [mod list](mods.md).

## Server list

`server_join`, `server_direct`, `server_add`, `server_edit`, `server_delete` and `server_refresh` work on the [server list](servers.md).
