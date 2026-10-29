# Music

Any theme, loading screen or menu, can play music:

```json
"music": {
  "tracks": ["music/theme.ogg", "music/ambience.ogg"],
  "volume": 0.8,
  "loop": true,
  "shuffle": false,
  "fade_in": 2,
  "fade_out": 3,
  "continue_into_menu": true
}
```

| Key | Default | Meaning |
| --- | --- | --- |
| `tracks` | required | Ogg Vorbis (`.ogg`) files, relative to the theme file |
| `volume` | `1` | 0–1 |
| `loop` | `true` | Start over after the last track |
| `shuffle` | `false` | Random order each pass |
| `fade_in`, `fade_out` | `2`, `3` | Seconds |
| `continue_into_menu` | `false` | Loading themes: keep playing on the main menu instead of fading out when loading ends |

## How it plays

- **Loading music starts with the window.** It plays on its own thread through Java's audio
  system, not Minecraft's, which does not exist yet while the game loads.
- **Volume follows the game's sliders.** The track volume is multiplied by the game's
  Master and Music settings (live), and by the player's `music_volume` in
  [`config/ldic/player.json`](../help/settings.md#playerjson). Setting that to 0 turns LDIC music off for that player.
- **One at a time.** Menu music does not start while loading music continues into the menu.
  While LDIC music plays, vanilla's menu music stays quiet.
- **It stops for the game.** All LDIC music fades out when the player enters a world.

Only Ogg Vorbis is supported, the format Minecraft itself uses. Convert MP3 or WAV with any audio
tool (Audacity: *File → Export → Ogg Vorbis*).
