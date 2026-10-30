# Skins for other screens

Some vanilla screens are **skinned** rather than replaced: the screen keeps all of its own logic
(its buttons, text fields, state and every click and key), and the menu theme only redraws it.
Two elements bring the screen's own widgets into the theme's layout:

- [`vanilla_button`](elements.md#vanilla_button) takes one of the screen's buttons by its id,
  moves it into the element's box and draws it with the button styles. Its label, whether it is
  enabled or shown, and what clicking it does stay the screen's.
- [`vanilla_field`](elements.md#vanilla_field) takes one of the screen's text fields (by its order
  in the screen, from 0) and draws it in a themed box. Typing goes to the field as before.

A vanilla button a skin does not place is not lost: it appears in the theme's
`foreign_buttons` area with other mods' buttons (or where the screen put it).

| Theme screen | Skins | Buttons (id) | Fields |
| --- | --- | --- | --- |
| `world_create` | Create New World | 0 Create, 1 Cancel, 2 Game Mode, 3 More World Options / Done, 4 Generate Structures, 5 World Type, 6 Allow Cheats, 7 Bonus Chest, 8 Customize | 0 name, 1 seed |
| `world_edit` | Edit World | 0 Save, 1 Cancel, 3 Reset Icon, 4 Open Folder | 0 name |

![The horizon theme's Create New World](../assets/screenshots/theme-create-world.png){ .shot }

The built-in **horizon**, **launcher** and **premiere** themes skin both; **classic** leaves them
vanilla. A pack turns skins off with `"menu": { "themed_screens": false }` in
[settings.json](../help/settings.md).

## Values

| Function | Returns |
| --- | --- |
| `vanilla_text(id)` | A button's label |
| `vanilla_visible(id)`, `vanilla_enabled(id)` | Whether a button is shown, and usable |
| `field_visible(i)` | Whether a text field is shown |
| `screen_text(name)`, `screen_flag(name)` | Strings and switches particular to the screen (below) |

Create New World: `screen_text('folder')` (the save folder name), `screen_text('mode_hint1')` and
`'mode_hint2'` (the game mode's description), `screen_text('world_type_info')` (the world type's
notice, if any) and `screen_flag('more_options')` (the More World Options page is showing; the
screen shows and hides its own buttons to match, so lay both pages out and let `visible` follow).
