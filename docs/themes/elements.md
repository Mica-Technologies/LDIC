# Elements

Every element is an object with a `type`. Any property marked *expr* also takes an
[expression](expressions.md) string, re-evaluated every frame. *Text* properties take text with
`{expression}` parts, such as `"Hello {username}"`. Colours are `"#RRGGBB"`, `"#AARRGGBB"`, or a
number or expression giving `0xAARRGGBB`.

## Common properties

| Property | Type | Default | Meaning |
| --- | --- | --- | --- |
| `type` | name | required | One of the types below |
| `id` | string | | Lets themes that extend this one change or remove it |
| `anchor`, `pivot` | anchor | `top_left`, = anchor | See [Layout](layout.md) |
| `x`, `y` | number, `%`, *expr* | `0` | Offset from the anchor |
| `width`, `height` | number, `%`, *expr* | depends on type | Size |
| `visible` | bool *expr* | `true` | Hidden elements and their children are skipped |
| `enabled` | bool *expr* | `true` | Disabled elements cannot be clicked |
| `opacity` | number *expr* | `1` | 0–1, multiplied into children |
| `rotation` | number *expr* | `0` | Degrees clockwise around the pivot |
| `scale` | number *expr* | `1` | Around the pivot, children included |
| `z` | number | `0` | Draw order among siblings (higher on top) |
| `children` | array | | Elements laid out inside this one |
| `action` | [action](actions.md) | | What a click does; makes the element interactive |
| `tooltip` | text | | Shown while hovered |
| `sound_hover`, `sound_press` | text | | Sound event names, such as `minecraft:ui.button.click` |
| `hover_time` | number | `0.15` | Seconds for `hover_t` to ease from 0 to 1 |

Interactive elements (those with an `action`, a `tooltip` or a `wrap`) can be reached with
++tab++ and the arrow keys and pressed with ++enter++. Inside an element's own expressions you
can use `hover`, `hover_t` (eased 0–1), `pressed`, `focused`, `pw`/`ph` (parent size),
`elem_width`/`elem_height`, and `parent_hover`/`parent_hover_t` (the nearest interactive
ancestor's state: handy for a highlight bar inside a button).

## rect (also `group`, `panel`)

A filled box. With no colour it is an invisible container for children.

| Property | Type | Default | Meaning |
| --- | --- | --- | --- |
| `colour` | colour *expr* | transparent | Fill (top or left edge of a gradient) |
| `colour_bottom` | colour *expr* | | Second gradient colour (bottom or right edge) |
| `gradient` | `vertical` / `horizontal` | `vertical` | Gradient direction (horizontal needs `radius` 0) |
| `radius` | number *expr* | `0` | Rounded corners |
| `border_colour` | colour *expr* | | Outline colour |
| `border_width` | number *expr* | `1` | Outline width |

## image

| Property | Type | Default | Meaning |
| --- | --- | --- | --- |
| `image` | text | | The image file ([paths](theme-files.md#where-files-are-found)) |
| `hover_image` | text | | Shown instead while hovered or focused |
| `images` | array of text | | A slideshow: crossfades through these |
| `duration`, `fade` | number *expr* | `6`, `1` | Slideshow seconds per image, and crossfade seconds |
| `shuffle` | true/false | `false` | Random slideshow order each time the screen opens |
| `fit` | `stretch` `cover` `contain` `center` `tile` | `stretch` | How the image fills the box |
| `tint` | colour *expr* | white | Multiplied into the image |
| `uv` | `[u0, v0, u1, v1]` *expr* | whole image | Part of the image to show (0–1) |
| `frames`, `fps` | number *expr* | `1`, `10` | Animation: the image is a vertical strip of frames |
| `frame` | number *expr* | | Pick the frame yourself instead of by `fps` |
| `filter` | `linear` / `nearest` | `linear` | `nearest` keeps pixel art crisp |
| `ken_burns` | number *expr* | `0` | Slideshows: each slide slowly zooms and pans by up to this fraction (0.05–0.15 looks cinematic) |

For a slow "Ken Burns" pan and zoom on a slideshow, set `ken_burns`: every slide gets its own
movement over its own lifetime, so slide changes never jump.

## text

| Property | Type | Default | Meaning |
| --- | --- | --- | --- |
| `text` | text | `""` | `\n` starts a new line; `§` colour codes work |
| `font` | name | `minecraft` | `minecraft`, `sans`, `serif`, `mono`, a `.ttf`/`.otf` path, or a name from `fonts`; add `:bold`, `:italic` or `:bold_italic` |
| `size` | number *expr* | `9` | Line height in layout units (the vanilla font is 9) |
| `colour` | colour *expr* | white | |
| `shadow` | bool *expr* | `true` | Vanilla-style drop shadow |
| `align` | `left` `center` `right` | `left` | Alignment of lines within the box |
| `wrap` | true/false | `false` | Wrap at `width` (set a width) |
| `line_spacing` | number *expr* | `1` | Multiplier on `size` between lines |

## panorama

A rotating six-image cube background.

| Property | Type | Default | Meaning |
| --- | --- | --- | --- |
| `images` | text | vanilla panorama | Path with `#` where the face number 0–5 goes |
| `speed` | number *expr* | `2` | Degrees per second of turn |
| `angle` | number *expr* | | Set the turn yourself instead of `speed` |
| `pitch` | number *expr* | gentle bob | Degrees looking down |
| `fov` | number *expr* | `85` | Field of view |
| `blur` | number *expr* | `0` | 0 sharp; 1–6 renders at ½…1/64 resolution for a soft look |
| `tint` | colour *expr* | white | |

Faces 0–5 are front, right, back, left, top, bottom, the same as vanilla's
`panorama_0.png`…`panorama_5.png`.

## button

| Property | Type | Default | Meaning |
| --- | --- | --- | --- |
| `text` | text | `""` | Label |
| `style` | `vanilla` `flat` `image` `none` | `vanilla` | `vanilla` uses the resource pack's `widgets.png` at any size |
| `font`, `size`, `shadow` | | `minecraft`, `9`, `true` | As for text |
| `text_align` | `left` `center` `right` | `center` | |
| `padding` | number *expr* | `6` | Space at the aligned edge, and between icon and text |
| `text_colour`, `hover_text_colour`, `disabled_text_colour` | colour *expr* | vanilla colours | Text colour eases between normal and hover |
| `colour`, `hover_colour`, `pressed_colour`, `disabled_colour` | colour *expr* | | Fill for `flat` |
| `border_colour`, `hover_border_colour`, `border_width`, `radius` | | | Outline and corners for `flat` |
| `image` | text | | For `image`: a sheet of rows (normal, hover, disabled, pressed) |
| `sheet_rows` | number *expr* | `2` | Rows in that sheet |
| `icon`, `icon_size`, `icon_tint` | | , `16`, white | An icon left of the text (or alone, centred) |
| `wrap` | string | | Put this button over another mod's button, by id or class name; see below |

Buttons default to 200×20 and play `minecraft:ui.button.click` when pressed unless they set
`sound_press`.

## factory

Spawns copies of a `template` element: a scrolling log line per status message, or a row of
icons from a count.

| Property | Meaning |
| --- | --- |
| `trigger` | *expr*. A new copy appears at start and every time this value changes. |
| `count` | *expr*. Alternatively: keep exactly this many copies. |
| `max` | Most copies alive (default 32, up to 1024); the oldest go first. |
| `keep` | `{ "name": expr }`: values captured when each copy is created. |
| `destroy_when` | bool *expr*. Evaluated per copy; true removes it. |
| `template` | The element to copy. |

Inside the template: `index` (0 = newest with `trigger`; position with `count`), `count`,
`age` (seconds since created) and every `keep` name.

A scrolling status log for the loading screen (`status` is a loading-screen value):

```json
{ "type": "factory", "trigger": "status", "max": 12, "keep": { "line": "status" },
  "destroy_when": "age > 20",
  "template": { "type": "text", "text": "{line}", "anchor": "bottom_left", "x": 8,
                "y": "-8 - index * 11", "opacity": "1 - index / 12" } }
```

## foreign_buttons

Where buttons added by **other mods** go. With `layout` `row` or `column` they are lined up
inside this element's box (`spacing`, `align` `start`/`center`/`end`). With `keep` they stay where
the mod put them. Without this element they also stay where the mod put them, and the theme-level
`"foreign_buttons": "hide"` hides them all.

To put your own button over a specific mod button, give it `"wrap": "<id or class name>"`. The mod's
button is hidden, and clicking yours clicks it, firing the mod's event handlers exactly once. The
id or class shows in the log when a wrap target is not found.

## slots

[Inventory screens](inventory.md) only. Draws a cell behind every slot of a group, at the slot's
own position: the game decides where slots are, the theme decides how they look. The element's
own box is not used.

| Property | Type | Default | Meaning |
| --- | --- | --- | --- |
| `group` | `all` / `player` / `main` / `hotbar` / `armor` / `offhand` / `crafting` / `result` / `container` / `grid` | `all` | Which slots; `player` is main + hotbar + armour + offhand, `grid` the full-screen creative item grid |
| `size` | number *expr* | `18` | Cell size, centred on the 16-pixel item |
| `colour`, `border_colour`, `border_width`, `radius` | | `#50000000`, none, `1`, `0` | As for rect |
| `selected_colour`, `selected_border_colour` | colour *expr* | as unselected | For the player's selected hotbar slot |
| `image`, `tint`, `filter` | | | An image drawn in each cell (`nearest` by default, for pixel art) |

## player_model

[Inventory screens](inventory.md) only. The player, standing in the element's box (the box height
sets the size), turning to look at the pointer unless `follow_mouse` is `false`.

## vanilla_button

[Skinned screens](skins.md) only. One of the screen's own buttons, by `button` (its id), moved into
this box and drawn with every [button](#button) property except `text`: the label, enabled state
and what a click does stay the screen's. Not drawn while the screen hides the button. A slider shows
its position as a bar in `slider_colour`, and a check box (Forge's Apply globally) a small box at
its left, filled in `slider_colour` when ticked (leave room with `padding` and `text_align: "left"`).

## vanilla_grid

[Skinned screens](skins.md) only. Every vanilla button the theme did not place with
`vanilla_button`, except the ids in `exclude` (comma-separated), in `columns` (2) of `row_height`
(20) with `gap` (4); a button vanilla made wider than 200 px spans its row. Drawn with every
[button](#button) property except `text`; sliders also draw their position in `slider_colour`.

## vanilla_list

[Skinned screens](skins.md) only. One of the screen's scrolling lists, by `list` (its order in the
screen, from 0), laid out in this box at the theme's scale with vanilla's dirt background off; the
scroll bar sits at the right edge. Draw a panel behind it yourself. `style_buttons: true` draws
the vanilla buttons inside its rows (video settings, key bindings) with this element's
[button](#button) properties instead of vanilla's look.

`list: "shown"` is the list a screen that swaps between several is showing now (statistics).
`gui_pixels: true` keeps the list at GUI scale (its rows are
not scaled with the theme), for lists whose rows place their own controls (Forge config entries).

## vanilla_field

[Skinned screens](skins.md) only. One of the screen's text fields, by `field` (its order in the
screen, from 0), in a themed box: `colour`, `border_colour`, `focus_colour`, `radius`, `padding`.
The text is drawn in the theme's `font` at `size` and `text_colour`, with the selection in
`selection_colour` and a blinking cursor, and scrolls to keep the cursor in view. Clicking (which
places the cursor by the drawn text) and typing stay the field's.

## advancement_map

The advancements skin only. The selected tab's map (its background, lines and advancement frames,
drawn as in vanilla) in a view the size of this box, instead of vanilla's small window. Drag to
move it; the wheel scrolls it (sideways with shift held). Hovering an advancement shows its box
over everything else. No properties of its own: draw a panel behind it yourself.

## advancement_tabs

The advancements skin only. Every advancement tab as a [button](#button) in a scrolling column, with
the tab's icon at the left (leave room with `padding` and `text_align: "left"`); the selected tab
looks hovered. `row_height` (22), `gap` (3), `icon_size` (16). Clicking a tab selects it; the wheel
scrolls the column.

## mod_info

[Mod list](mods.md) only: the selected mod's logo, name (in `title_font`, else `font`), version,
id and state, authors, credits, website, included mods, update link, description and the update's
changes, scrolling with the wheel. `font`, `title_font`, `size`, `text_colour`, `detail_colour`,
`accent_colour` (version badge and links). With nothing selected it shows a hint.

## list

World, server and mod list screens only: the [worlds](worlds.md), [servers](servers.md) or
[mods](mods.md) as cards, drawn by the screen in
the element's box, with a search box across the top. The screen handles clicks, scrolling and keys.

| Property | Type | Default | Meaning |
| --- | --- | --- | --- |
| `source` | `worlds` / `servers` / `mods` | `worlds` | What the list shows
| `row_height`, `gap`, `radius` | number *expr* | `40`, `4`, `3` | Card height, space between cards, corner radius |
| `colour`, `hover_colour`, `selected_colour` | colour *expr* | dark, darker, darkest | Card backgrounds |
| `border_colour`, `selected_border_colour` | colour *expr* | faint white, `#4FC3F7` | Card edges |
| `accent_colour` | colour *expr* | `#4FC3F7` | Game-mode badge, search focus |
| `text_colour`, `detail_colour` | colour *expr* | white, grey | Names; second lines and hints |
| `font`, `size` | | `minecraft`, `9` | As for text |
| `search` | `true` / `false` | `true` | The search box |
| `empty_text` | text | | Shown when there is nothing to list |

## slider

Drag to set a value. Bound to a game option with `option`, it reads and sets that option; see
[Options screen](options.md#game-options-by-name) for the names. Default size 200×20.

| Property | Type | Default | Meaning |
| --- | --- | --- | --- |
| `option` | name | | The game option to set, such as `FOV` or `sound:music` |
| `value` | number *expr* | the option's | Knob position 0–1 |
| `text` | text | the option's label | Centred over the slider |
| `style` | `flat` / `vanilla` | `flat` | `vanilla` draws the resource pack's slider |
| `track_colour`, `fill_colour`, `knob_colour` | colour *expr* | | `flat` colours |
| `knob_width`, `radius` | number *expr* | `4`, `2` | |
| `font`, `size`, `text_colour`, `shadow` | | | As for text |

## include

Not drawn itself: it is replaced by a group holding copies of another screen's elements, all of
them or those listed in `ids`. Its own keys (anchor, size, `visible`, `opacity`, …) apply to that
group.

```json
{ "type": "include", "screen": "main", "ids": ["panorama", "shade"], "visible": "!in_world" }
```
