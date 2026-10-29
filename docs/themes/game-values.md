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
| `reduced_motion` | bool | The player asked for less motion (`player.reduced_motion` in [settings](../help/settings.md)) |

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

Every element also has its own values (`hover`, `hover_t`, `pw`, …); see
[Elements](elements.md#common-properties).
