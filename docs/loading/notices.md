# Logo card and photosensitivity notice

Before the loading screen, LDIC shows two short screens: a **logo card** (Mojang's logo, the way
the game itself opens) and a **photosensitivity notice**. Both are on by default, and a pack can
turn either off or reword it. The game loads behind them the whole time, so they cost no loading
time; when they end, the pack's loading theme takes over.

![The logo card](../assets/screenshots/loading-logo.png){ .shot }

## Settings

In `config/ldic/settings.json`:

```json
"logo_screen": { "enabled": true, "seconds": 3 },
"photosensitivity_notice": { "enabled": true, "always": true, "seconds": 5 },
"intro_skip": { "hold_seconds": 1.5 }
```

| Key | Default | Meaning |
| --- | --- | --- |
| `logo_screen.enabled` | `true` | Show the logo card |
| `logo_screen.seconds` | `3` | How long it stays up (1–30) |
| `photosensitivity_notice.enabled` | `true` | Show the notice |
| `photosensitivity_notice.always` | `true` | Every launch; `false` shows it only until the player has seen it once |
| `photosensitivity_notice.seconds` | `5` | How long it stays up (2–60) |
| `intro_skip.hold_seconds` | `1.5` | Holding **Space** this long skips the logo card or the notice; `0` turns skipping off |

Both screens are shown for their full time even when the game finishes loading sooner: the menu
waits for them. A player who does not want to wait holds Space; a bar under "Hold SPACE to skip"
fills while they hold it, and the next screen fades in. Each screen needs its own press, so one
long hold never skips both. Whether a player has seen the notice is stored in
`config/ldic/cache/seen.json`.

![The photosensitivity notice](../assets/screenshots/loading-notice.png){ .shot }

## Your own logo or wording

Copy the built-in file to `config/ldic/notices.json` and edit it. It is an ordinary theme with two
screens, `logo` and `photosensitivity`, so any element works: put your studio's logo on the card, or
reword the notice. A screen you leave out of your copy comes from the built-in one. The built-in
file is in the LDIC jar at `assets/ldic/themes/notices/theme.json`.

Values these screens can read, on top of the [loading screen's](index.md):

| Name | Type | Value |
| --- | --- | --- |
| `skip_hold` | number | How far the player has held Space, 0 to 1 |
| `skip_seconds` | number | `intro_skip.hold_seconds`; 0 when skipping is off |
| `loading_done` | bool | The game has finished loading and is waiting for this screen |

## Flashing themes and reduced motion

- A theme that flashes should say so with `"flashing": true` at its top level. LDIC then logs a
  warning if the notice is off, so a pack maker does not ship one by accident.
- Players can set `reduced_motion` in [`config/ldic/player.json`](../help/settings.md#playerjson). Themes read it as `reduced_motion`, and the
  built-in themes stop panning, zooming and spinning when it is on. Do the same in yours:
  `"ken_burns": "reduced_motion ? 0 : 0.1"`.
