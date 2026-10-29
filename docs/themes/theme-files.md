# Theme files

A theme is one JSON object. Comments (`//`, `/* */`) are allowed, files are read as UTF-8, and
keys starting with `_` are ignored, so you can leave notes like `"_comment": "…"` anywhere. Avoid
trailing commas: in an array they read as an extra empty entry.

```json
{
  "ldic": 1,
  "name": "My Pack",
  "author": "Me",
  "extends": "builtin:horizon",
  "design": { "mode": "height", "height": 400 },
  "fonts": { "title": "fonts/Display.ttf" },
  "constants": { "accent": "#4FC3F7" },
  "state": { "tab": 0 },
  "functions": { "pulse(number period)": "wave(time, period)" },
  "variables": { "glow": "0.6 + 0.4 * pulse(3)" },
  "foreign_buttons": "keep",
  "screens": {
    "main":    { "elements": [ … ] },
    "credits": { "elements": [ … ], "on_escape": "back" }
  }
}
```

## Top-level keys

| Key | Meaning |
| --- | --- |
| `ldic` | Format version. Always `1` for now. |
| `name`, `author`, `description` | Shown in logs and future theme pickers. |
| `extends` | Another theme to build on: `builtin:<name>`, or `<name>` for `config/ldic/themes/<name>/theme.json`. See [Extending](#extending-a-theme). |
| `design` | How layout units map to the screen. See [Layout](layout.md#design-size-and-scaling). |
| `fonts` | Short names for fonts: `"title": "fonts/Display.ttf"`, then `"font": "title"` on elements. |
| `constants` | Values computed **once** when the theme loads. |
| `state` | Values that actions can change (`set`, `toggle`). Initial values are JSON literals. |
| `functions` | Your own functions. See [Expressions](expressions.md#your-own-functions). |
| `variables` | Values recomputed **every frame**, in order; each can use the ones above it. |
| `foreign_buttons` | `"keep"` (default) or `"hide"`: what happens to buttons other mods add. |
| `flashing` | `true` if the theme flashes or strobes; see [Photosensitivity notice](../loading/notices.md). |
| `forge_update_notice` | Menu: `false` hides Forge's "new version available" line (default `true`). Packs pin their Forge version, so modern themes usually hide it. |
| `music` | Background music; see [Music](../loading/music.md). |
| `sequence`, `crossfade` | Loading themes: screens shown before `main`; see [Loading screen](../loading/index.md#screens-before-the-loading-screen). |
| `screens` | Named screens. `main` is required; it is the title screen. |
| `elements` | Shorthand for `screens.main.elements`. |

Each screen has `elements` (drawn in order, later on top; `z` overrides), and optionally
`on_open` and `on_escape` [actions](actions.md). Without `on_escape`, Escape goes back to the
previous screen. On `main`, it does nothing.

## Where files are found

| Written as | Means |
| --- | --- |
| `logo.png`, `images/bg.jpg` | Relative to the theme file that wrote it. For `config/ldic/menu.json` that is `config/ldic/`. |
| `minecraft:textures/gui/widgets.png` | A resource-pack asset (`namespace:path`). |
| `https://example.com/news.png` | Downloaded in the background and cached in `config/ldic/cache/`. |

Relative paths stay tied to the file that wrote them even when another theme extends it, so a
built-in theme's images keep working in yours. A path cannot climb out of `config/ldic` with `..`.

Images can be PNG, JPEG, GIF (first frame), or BMP. Fonts can be `.ttf` or `.otf`.

## Extending a theme

`extends` loads the other theme first, then merges yours over it:

- Objects merge key by key. Setting a key to `null` deletes it. An object whose `type` is
  different (an action changing from `open` to `connect`) replaces the old one instead.
- **Elements merge by `id`.** An element whose `id` matches one in the parent, at any depth inside
  groups, is changed in place with only the keys you give. A new `id` adds an element at the end
  of that list.
- `"remove": ["id", …]` on a screen deletes those elements, wherever they are nested.
- Any other array you write replaces the parent's.

Chains are allowed (`a` extends `b` extends `builtin:classic`), up to 8 deep. A loop is reported
and broken.
