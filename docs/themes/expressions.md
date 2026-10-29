# Expressions

Most theme properties accept an **expression**, a small formula evaluated every frame. That is how
themes animate (`"opacity": "ease_out(screen_time / 0.5)"`), react (`"x": "6 * hover_t"`) and
show live values (`"text": "{mem.used} MiB"`).

Expressions are typed: **numbers**, **bools** (`true`/`false`) and **strings** (`'single quoted'`
or `"double"`, escaped inside JSON). Colours are numbers (`0xAARRGGBB`) and can be written
`#RRGGBB` or `#AARRGGBB` inside expressions too. A type mistake, an unknown name or a typo is
reported when the theme loads, with the position, never while it is running.

## Operators

From loosest to tightest:

| Operators | Meaning |
| --- | --- |
| `c ? a : b` | If `c` then `a` else `b` |
| `\|\|`, `&&` | Or, and (bools, short-circuit) |
| `\|`, `^`, `&` | Bitwise on numbers; or, xor, and on bools |
| `==`, `!=` | Equality (same types) |
| `<`, `>`, `<=`, `>=` | Comparison (numbers) |
| `<<`, `>>`, `>>>` | Bit shifts (`>>>` treats the value as an unsigned 32-bit colour) |
| `+`, `-` | Add, subtract; `+` joins text when either side is a string |
| `*`, `/`, `%` | Multiply, divide, remainder |
| `-x`, `!x`, `~x` | Negate, not, bitwise not |
| `a ** b` | Power (right to left: `2 ** 3 ** 2` is 512) |

`a.f(b)` is another way to write `f(a, b)`: `status.upper()`, `mem.used.max(512)`.
Numbers join text without a trailing `.0`: `'v' + 2` is `v2`, and `1/3` shows as `0.3333`.

## Text with expressions

In *text* properties, expressions go in braces: `"Loaded {mod_count} mods in {fixed(time, 1)}s"`.
A literal brace is doubled: `{{` and `}}`.

## Functions

Angles are in **degrees**. Colour channels are 0–255.

**Maths:** `abs` `floor` `ceil` `round` `trunc` `fract` `sign` `sqrt` `cbrt` `exp` `log` `log10`
`pow(x, y)` `mod(x, m)` (always positive) `min(…)` `max(…)` `clamp(x, lo, hi)` `saturate(x)`
`lerp(a, b, t)` `inv_lerp(a, b, x)` `remap(x, a, b, c, d)` `step(edge, x)` `smoothstep(lo, hi, x)`
`hypot(x, y)` `sin` `cos` `tan` `asin` `acos` `atan` `atan2(y, x)` `sinh` `cosh` `tanh`
`radians` `degrees`, constants `pi` `tau` `e`.

**Waves** over time `t` with period `p` seconds, all 0–1: `wave(t, p)` (smooth), `saw(t, p)`
(ramp), `triangle(t, p)`, `square(t, p)`.

**Easing** (input clamped to 0–1): `ease_in` `ease_out` `ease_in_out` `ease_in_cubic`
`ease_out_cubic` `ease_in_out_cubic` `ease_out_back` `ease_out_elastic` `ease_out_bounce`
`ease_out_expo`.

**Random:** `random(seed)` and `random(seed, i)` give 0–1, `random_int(seed, max)` gives
0…max−1, and `noise(x)` is smooth 0–1 noise. All are deterministic: the same seed always gives
the same value.

**Text:** `len` `upper` `lower` `trim` `substr(s, start)` `substr(s, start, end)` `char_at(s, i)`
`index_of(s, find)` `contains` `starts_with` `ends_with` `is_empty` `replace(s, a, b)`
`repeat(s, n)` `pad_left(s, width, fill)` `pad_right` `ellipsize(s, max)` `str(x)`
`fixed(x, places)` `percent(x)` (0.42 → `42%`) `num(s)` `pick('a|b|c', i)` `count('a|b|c')`
`if_else(c, a, b)`.

**Colours:** `rgb(r, g, b)` `rgba(r, g, b, a)` `argb(a, r, g, b)` `hex('#RRGGBB')`
`hsv(hue, sat, val)` `alpha` `red` `green` `blue` `with_alpha(c, 0..1)` `fade(c, 0..1)`
`mix(c1, c2, t)` `brighten(c, t)` `darken(c, t)`.

**Game values and functions** (`translate`, `mod_loaded`, `clock`, …) are listed in
[Game values](game-values.md).

## Your own functions

```json
"functions": {
  "enter(number i)": "ease_out_cubic(saturate((screen_time - 0.06 * i) / 0.45))",
  "label(string s, bool loud)": "loud ? s.upper() : s"
}
```

Parameter types are `number`, `bool` or `string` (untyped means number). A function can call
functions defined above it, but not itself.

## Constants, variables and state

- `constants` are computed once when the theme loads.
- `variables` are recomputed every frame, top to bottom.
- `state` values change only through `set` and `toggle` [actions](actions.md).

All three can be used anywhere below where they are defined.
