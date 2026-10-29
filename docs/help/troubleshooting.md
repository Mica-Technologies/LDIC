# Troubleshooting

## "LDIC: N theme problems" at the top of the menu

The theme loaded, but something in it was wrong. LDIC skipped the bad part and carried on. Every
problem is in `logs/latest.log` as a line starting `LDIC theme`, with the file and the exact
place, for example:

```text
LDIC theme error in config/ldic/menu.json at screens.main.elements[3](play).x: unknown name 'buton_top' at 0: buton_top
```

That reads: in the fourth element of the main screen (id `play`), its `x` uses a name that does
not exist. Set `"debug": true` in `config/ldic/settings.json` to list all problems on the
menu itself.

| Kind of problem | What LDIC does |
| --- | --- |
| Unknown or bad property value | Uses the default for that property |
| Unknown element type, element without `type` | Skips the element (and its children) |
| Bad expression | Uses the property's default |
| Invalid JSON in `menu.json` | Uses `builtin:classic` instead |
| No usable theme at all | Shows the vanilla menu |

## An image shows as a magenta and black checkerboard

The image could not be loaded. The log has `LDIC could not load image '…'` with the reason.
Check:

- the path is relative to the theme file that names it;
- the file name's capitals match exactly (Linux and macOS care);
- resource-pack paths look like `namespace:path/to/file.png`, and the pack is enabled.

## A web image never appears

It is downloaded in the background. If the download fails and there is no cached copy, it turns
into the checkerboard and the log says why (HTTP error, timeout, larger than 16 MiB).

## A button another mod adds is missing

Check `"foreign_buttons"` at the top of the theme (is it `"hide"`?) and whether a theme button
`wrap`s it. Buttons other mods add stay where the mod puts them unless the theme has a
[`foreign_buttons`](../themes/elements.md#foreign_buttons) element.

## My edits do not show

The menu reloads automatically when `config/ldic/menu.json` or anything in `config/ldic/themes/`
changes, and ++ctrl+r++ on the menu forces it. Built-in themes inside the LDIC jar do not reload;
extend them instead of editing them.
