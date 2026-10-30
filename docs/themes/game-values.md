# Game values

Names LDIC provides to every menu theme, on top of the [built-in functions](expressions.md#functions).

## Live values

| Name | Type | Value |
| --- | --- | --- |
| `time` | number | Seconds since LDIC started. Keeps counting across screens, so slideshows and panoramas do not restart. |
| `screen_time` | number | Seconds since this screen opened. Use it for entrance animations. |
| `delta` | number | Seconds since the previous frame |
| `frame`, `fps` | number | Frame counter, current frames per second |
| `screen` | string | Id of the open theme screen |
| `screen_width`, `screen_height` | number | Screen size in layout units |
| `gui_width`, `gui_height`, `gui_scale` | number | Screen size in GUI pixels, and Minecraft's GUI scale |
| `ui_scale` | number | Layout units → GUI pixels (see [design](layout.md#design-size-and-scaling)) |
| `mouse_x`, `mouse_y` | number | Pointer position in layout units |
| `mem.used`, `mem.total`, `mem.max`, `mem.free` | number | Java memory in MiB |

## Fixed values

| Name | Type | Value |
| --- | --- | --- |
| `mc_version` | string | `1.12.2` |
| `forge_version` | string | Forge version |
| `mod_count` | number | Mods loaded |
| `username` | string | Player name |
| `pack_name`, `pack_version` | string | From [settings](../help/settings.md) |
| `is_demo` | bool | Demo mode |
| `reduced_motion` | bool | The player asked for less motion (`reduced_motion` in [`player.json`](../help/settings.md#playerjson)) |
| `in_world` | bool | The screen was opened from the pause menu (a world is loaded) |
| `foreign_buttons` | number | How many of other mods' buttons are showing on this screen |
| `singleplayer` | bool | The world is a singleplayer world |
| `lan_open` | bool | The singleplayer world is open to LAN |
| `cheats` | bool | The player can use cheat commands in this world (singleplayer: the world's Allow Cheats setting) |
| `status`, `status_detail`, `status_progress` | string, string, number | On the [world-loading screen](world-loading.md): what the game is doing, the step, and 0–1 or -1 |

## Functions

| Function | Returns |
| --- | --- |
| `translate('key')` | The localised string for a language key, such as `menu.singleplayer` |
| `has_translation('key')` | Whether that key exists |
| `mod_loaded('modid')` | Whether a mod is installed |
| `splash()` | A random vanilla splash line, chosen once per load (with vanilla's holiday ones) |
| `branding()` | Forge's version and mod-count lines, one per line |
| `clock()` | The time in the computer's regional format: `3:45 PM`, or `15:45` |
| `long_date()` | The date in the regional long format: `Thursday, October 29, 2026` |
| `date('pattern')` | Date and time with a [Java pattern](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html): `date('yyyy-MM-dd')` |
| `month()`, `day()`, `hour()` | Numbers, for seasonal art: `"visible": "month() == 12"` |
| `option_value(name)`, `option_text(name)`, `option_on(name)` | A game option: slider position or 1/0, vanilla's label, and on/off; see [Options screen](options.md#game-options-by-name) |
| `difficulty_text()`, `can_change_difficulty()` | The world difficulty label, and whether this player may change it |
| `server_online('host:port')` | Whether the server answers. Pinged in the background, every 30 s while a theme reads it. |
| `server_players(address)`, `server_max_players(address)` | Player counts (0 while unknown) |
| `server_motd(address)` | The server's message of the day, with its colours |
| `server_ping(address)` | Round trip in ms, -1 while unknown |
| `web_text(url)` | A text file from the web, cached (also offline) and refreshed every 10 minutes; empty until it arrives |
| `web_line(url, i)`, `web_line_count(url)` | Its paragraphs (separated by blank lines), wrapping: `web_line(url, floor(time / 8))` rotates them |
| `area_x(name)`, `area_y(name)`, `area_w(name)`, `area_h(name)`, `has_area(name)` | On [inventory screens](inventory.md): where a named area is, in GUI pixels (0 when it is absent) |

On inventory screens there are also `container_title` (string: "Chest", "Large Chest", a renamed
shulker box's name) and the player's `player_health`, `player_max_health`, `player_food`,
`player_armor`, `player_level` and `player_xp` (0–1 through the level).

Every element also has its own values (`hover`, `hover_t`, `pw`, …); see
[Elements](elements.md#common-properties).

On the [world selection](worlds.md) screen there are also `world_selected` (bool), `world_count` (number) and `world_sort` (string).
On the [mod list](mods.md): `mod_selected` and `mod_configurable` (bool) and `mod_sort` (string).

On the [server list](servers.md) there are also `server_selected`, `server_editable` (bool) and `server_count` (number).

On [skinned screens](skins.md) there are also `vanilla_text(id)`, `vanilla_visible(id)`, `vanilla_enabled(id)`, `field_visible(i)`, `screen_text(name)` and `screen_flag(name)`.
On [furnaces, brewing stands and anvils](inventory.md#furnaces-brewing-stands-and-anvils), `screen_text(name)`, `screen_flag(name)` and `screen_value(name)` (a number) carry their progress, fuel and cost.
