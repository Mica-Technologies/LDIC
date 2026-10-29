# Layout

Every element is a box positioned inside its parent: the screen for top-level elements, or the
group it is a child of.

## Anchor, pivot and offset

- `anchor`: the point **in the parent** the element is positioned from.
- `pivot`: the point **of the element** that sits on that spot. Defaults to the anchor.
- `x`, `y`: offset from the anchor, in layout units. Positive is right and down.

Both take a name, or `[fx, fy]` fractions (0 = left/top, 1 = right/bottom):

| Name | Point |
| --- | --- |
| `top_left` (default), `top`, `top_right` | Top edge |
| `left`, `center`, `right` | Middle |
| `bottom_left`, `bottom`, `bottom_right` | Bottom edge |

Because the pivot follows the anchor, `"anchor": "bottom_right"` with no offset puts an element
flush in the bottom-right corner, and `"anchor": "center"` centres it. Define your own names
under the theme's `anchors`: `"anchors": { "third": [0.333, 0.5] }`.

## Sizes

`width` and `height` accept:

- a number of layout units: `200`;
- a percentage of the parent: `"50%"`;
- any [expression](expressions.md): `"pw / 3 - 8"`, where `pw`/`ph` are the parent's width and height.

Defaults: buttons are 200×20, text is measured from its content, and everything else fills
its parent (`100%`).

## Rotation, scale and opacity

`rotation` (degrees, clockwise) and `scale` act around the pivot and apply to children too.
`opacity` (0–1) multiplies down the tree, so fading a group fades everything in it.
Clicks follow `scale` (a scaled-up button is clickable where you see it). They do not follow
`rotation`.

## Design size and scaling

By default one layout unit is one GUI pixel, like vanilla, so the player's GUI Scale setting
decides how big everything is. A theme can instead lay itself out for a design size and scale to
the window:

```json
"design": { "mode": "height", "height": 400, "min": 0.5, "max": 3 }
```

| `mode` | Scale |
| --- | --- |
| `none` | 1 (GUI pixels, default) |
| `fit` | Largest that fits the whole design size in the window |
| `fill` | Smallest that covers the window |
| `width` / `height` | Match the design width / height exactly |

`min`/`max` clamp the scale. Inside a scaled theme, `screen_width`/`screen_height` are in layout
units, and `ui_scale` is the factor. Text in TTF and system fonts is drawn at the real screen
resolution at any scale, so it stays sharp.
