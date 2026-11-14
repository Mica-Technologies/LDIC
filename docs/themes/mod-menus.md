# Entries and pages from mods

Other mods can add entries to the title screen, the pause menu and the options screen through
the [LDIC API](../mods/api.md), and describe settings pages that LDIC draws. A theme needs to do
nothing for either to work. This page is for a theme that wants them to look its own way.

## Entries

By default each entry is a flat button in the theme's accent colour, placed with other mods'
buttons: in the screen's [`foreign_buttons`](elements.md#foreign_buttons) area, and under the
theme's `"foreign_buttons": "hide"` on the title screen like any of them.

A theme draws entries itself with a [factory](elements.md#factory) over these values:

| Value | Meaning |
| --- | --- |
| `menu_entry_count(location)` | How many entries are showing on `'main'`, `'pause'` or `'options'` |
| `menu_entry_label(location, i)` | Entry `i`'s text |
| `menu_entry_tooltip(location, i)` | Its hover text, empty for none |
| `menu_entry_enabled(location, i)` | Whether it can be clicked |

and the `menu_entry` action, which runs entry `index` of a `location`:

```json
{ "id": "mod_entries", "type": "factory", "count": "menu_entry_count('pause')", "max": 8,
  "template": { "id": "mod_entry", "type": "button", "y": "200 + 26 * index", "width": 220, "height": 22,
    "text": "{menu_entry_label('pause', index)}",
    "tooltip": "{menu_entry_tooltip('pause', index)}",
    "enabled": "menu_entry_enabled('pause', index)",
    "action": { "type": "menu_entry", "location": "pause", "index": "index" } } }
```

The theme then owns as many entries as that factory's `max`: those get no button. Entries past
it still get one, in the `foreign_buttons` area, so a mod's entry is never out of reach however
many there are. (A `menu_entry` action on an element outside a factory means the theme places
entries one by one, and it owns them all.) The built-in themes with a pause
menu all do this there: Horizon adds up to eight entries to its list above the Disconnect button,
in columns of four; Launcher adds up to six to its card's grid; Premiere shows up to six in a row
above its bottom bar.

## Pages

A mod's page is the theme's **`mod_page`** screen. Every theme has one: LDIC merges a base layer
under each menu theme, and its `mod_page` is a centred panel with a title, the rows and a Done
button, using the theme's accent through `accent_colour()`.

To restyle it, override its elements by id in your theme, as you would a parent theme's:

```json
"fonts": { "page_font": "fonts/Body.ttf", "page_font_bold": "fonts/Heading.ttf" },
"screens": { "mod_page": { "elements": [
  { "id": "page_backdrop", "colour": "#FF101010", "colour_bottom": "#FF202020" }
] } }
```

The ids: `page_backdrop` (outside a world), `page_dim` (over a world), `page_panel`, and inside
it `page_title`, `page_rule`, `page_rows` (the factory), `page_done` and `page_scroll_hint`. Each
row is `page_row`, holding `page_row_section`, `page_row_text`, `page_row_button` (switches,
choices and buttons) and `page_row_slider`. The fonts `page_font` and `page_font_bold` are the
page's two fonts.

To build the screen from nothing instead, these values describe the page. Rows are numbered
from the first one in view, so a factory's `index` is the row number.

| Value | Meaning |
| --- | --- |
| `page_title()` | The page's title |
| `page_rows()` | How many rows the page has |
| `page_fit(n)` | Tells LDIC the screen has room for `n` rows; gives how many to draw. Use it as the factory's `count`. |
| `page_scroll()`, `page_more()` | The first row in view, and whether there are rows below the last |
| `page_row_is(i, kind)` | Whether row `i` is a `'section'`, `'text'`, `'toggle'`, `'slider'`, `'choice'` or `'button'` |
| `page_row_kind(i)` | The same as a string |
| `page_row_label(i)` | The row's label |
| `page_row_text(i)` | Label and value together: `Fancy mode: ON`, `Range: 16 chunks` |
| `page_row_value(i)` | A slider's position, 0 to 1 |
| `page_row_on(i)` | Whether a switch is on |
| `page_row_enabled(i)`, `page_row_tooltip(i)` | Whether the row can be changed, and its hover text |

The `page_row` action clicks row `index` (flips a switch, moves a choice on, runs a button). A
[slider](elements.md#slider) with `"row": "index"` sets that row when dragged. The mouse wheel
scrolls the page, and `back` (or Escape) closes it.

`accent_colour()` works on every screen: the theme's `accent` constant, or the pack's inventory
accent when the theme has none.
