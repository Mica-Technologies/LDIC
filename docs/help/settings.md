# Settings

`config/ldic/settings.json` holds the switches that are not part of a theme. LDIC rewrites it on
start with every key present, so you can see what exists.

```json
{
  "menu": { "enabled": true, "fade_in": 0.6 },
  "loading": { "enabled": true, "fps": 60, "hold_seconds": 0 },
  "photosensitivity_notice": { "enabled": false, "always": false, "seconds": 7 },
  "player": { "reduced_motion": false, "music_volume": 1 },
  "pack": { "name": "", "version": "" },
  "debug": false
}
```

| Key | Default | Meaning |
| --- | --- | --- |
| `menu.enabled` | `true` | `false` leaves the vanilla main menu alone |
| `menu.fade_in` | `0.6` | Seconds the first menu after loading fades in from black; `0` for none |
| `loading.enabled` | `true` | `false` leaves Forge's loading screen alone |
| `loading.fps` | `60` | Loading-screen frame cap (10–240). Lower leaves more CPU for loading. |
| `loading.hold_seconds` | `0` | With `debug`: keep the loading screen up this long after loading, to preview a theme |
| `photosensitivity_notice.*` | off | See [Photosensitivity notice](../loading/notices.md) |
| `player.reduced_motion` | `false` | Ask themes to keep motion to a minimum |
| `player.music_volume` | `1` | LDIC music volume (0 turns it off), on top of the game's Master and Music sliders |
| `pack.name` | `""` | Your pack's name; themes show it as `pack_name` |
| `pack.version` | `""` | Your pack's version; themes show it as `pack_version` |
| `debug` | `false` | List every theme problem on the menu; save loading-screen screenshots |

A launcher can write `pack.name` and `pack.version` when it installs or updates a pack. The
`player` keys are for players. A pack should ship them at their defaults.

## Other files LDIC keeps

| File | What it is |
| --- | --- |
| `config/ldic/menu.json`, `loading.json` | The menu and loading themes |
| `config/ldic/notices.json` | Optional reworded notices |
| `config/ldic/tips.txt` | Loading tips, one per line |
| `config/ldic/cache/loading-timings.json` | How long each loading step took last launch (for progress and ETA) |
| `config/ldic/cache/seen.json` | Notices and once-only intros this player has seen |
| `config/ldic/cache/*.bin` | Downloaded web images |
