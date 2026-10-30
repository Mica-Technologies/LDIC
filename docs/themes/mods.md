# Mod list

A menu theme's `mod_list` screen replaces Forge's mod list (from the title screen's Mods button or
the pause menu's Mod Options). It lists what Forge's list does: every loaded mod that is not part
of another mod. A mod's config screen opens through the mod's own config factory, as in Forge's
list. Other mods' buttons stay, in the theme's [`foreign_buttons`](elements.md#foreign_buttons)
area. The screen is still a Forge mod list, so mods that look for that screen still find it.

The built-in **horizon**, **launcher** and **premiere** themes have one; **classic** keeps
Forge's screen. A pack turns it off with `"menu": { "themed_mods": false }` in
[settings.json](../help/settings.md).

## The list and the details

A [`list`](elements.md#list) element with `"source": "mods"` draws each mod as a card: its logo
(or its initial on a coloured tile, when it has no logo or its logo is a wide banner), name and
version, and an "Update available" badge when Forge's update check found a newer version. The
search box filters by name or mod id.

A [`mod_info`](elements.md#mod_info) element shows the selected mod: its logo, name, version, mod
id and state, authors, credits, website, the mods it includes, the update link, its description
and the update's changes. Links open after vanilla's link confirmation (when that is on). The wheel
scrolls it.

- Click a mod to select it; Up and Down move the selection, Ctrl+F searches.

## Actions and values

| Action | Does |
| --- | --- |
| `mod_config` | Without a `modid`: opens the selected mod's config screen |
| `mod_sort` | Switches the order: load order, A to Z, Z to A |

| Value | Type | Meaning |
| --- | --- | --- |
| `mod_selected` | bool | A mod is selected |
| `mod_configurable` | bool | The selected mod has a config screen (use it for `enabled` on Config) |
| `mod_sort` | string | `normal`, `a_z` or `z_a` |
