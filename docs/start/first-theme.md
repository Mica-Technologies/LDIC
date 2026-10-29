# Your first menu theme

Open `config/ldic/menu.json`. It starts like this:

```json
{
  "ldic": 1,
  "extends": "builtin:classic"
}
```

Leave the game running on the main menu while you edit: LDIC reloads the theme within a second of
every save. You can also press ++ctrl+r++ on the menu.

## Switch to another built-in theme

Change `builtin:classic` to `builtin:horizon` and save. See [Built-in themes](../themes/built-in.md)
for what each one looks like.

## Change one thing

Every element in a built-in theme has an `id`. A theme that extends another can change any element
by repeating its `id` with only the properties you want to change:

```json
{
  "ldic": 1,
  "extends": "builtin:horizon",
  "screens": {
    "main": {
      "elements": [
        { "id": "title", "text": "My Pack", "colour": "#FFD54F" }
      ]
    }
  }
}
```

Remove elements with `remove`:

```json
"screens": { "main": { "remove": ["clock", "date"] } }
```

## Add your own logo and a server button

Put `logo.png` next to `menu.json`. Paths in a theme are relative to the file that names them.

```json
{
  "ldic": 1,
  "extends": "builtin:horizon",
  "screens": {
    "main": {
      "remove": ["title", "subtitle"],
      "elements": [
        { "id": "logo", "type": "image", "image": "logo.png", "fit": "contain",
          "anchor": "top_left", "x": 48, "y": 24, "width": 256, "height": 96 },
        { "id": "join", "type": "button", "style": "flat", "radius": 4,
          "text": "Join the server", "anchor": "bottom_left", "x": 48, "y": -48,
          "width": 180, "height": 24, "colour": "#C01144CC", "hover_colour": "#FF1E5BE6",
          "action": { "type": "connect", "address": "play.example.com", "name": "Example" } }
      ]
    }
  }
}
```

## A Custom Main Menu layout in LDIC

Custom Main Menu positions everything from a named alignment point plus an offset. In LDIC,
that is `anchor` plus `x`/`y`, with `pivot` saying which corner of the element sits on the point.
Custom Main Menu always used the element's top-left corner, so set `"pivot": "top_left"`:

| Custom Main Menu | LDIC |
| --- | --- |
| `"alignment": "center", "posX": -128, "posY": -55` | `"anchor": "center", "pivot": "top_left", "x": -128, "y": -55` |
| `"texture": "minecraft:textures/gui/gear.png"` with 128×256 normal/hover halves | `"style": "image", "image": "minecraft:textures/gui/gear.png", "sheet_rows": 2` |
| `"action": {"type": "openGui", "gui": "options"}` | `"action": {"type": "open", "screen": "options"}` |
| `"action": {"type": "connectToServer", "ip": "…"}` | `"action": {"type": "connect", "address": "…"}` |
| `"action": {"type": "quit"}` | `"action": "quit"` |

Next: learn the [theme file](../themes/theme-files.md) structure and the [elements](../themes/elements.md).
