# Settings

`config/ldic/settings.json` holds the switches that are not part of a theme. LDIC rewrites it on
start with every key present, so you can see what exists.

```json
{
  "menu": { "enabled": true },
  "pack": { "name": "", "version": "" },
  "debug": false
}
```

| Key | Default | Meaning |
| --- | --- | --- |
| `menu.enabled` | `true` | `false` leaves the vanilla main menu alone |
| `pack.name` | `""` | Your pack's name; themes show it as `pack_name` |
| `pack.version` | `""` | Your pack's version; themes show it as `pack_version` |
| `debug` | `false` | List every theme problem on the menu, not just a count |

A launcher can write `pack.name` and `pack.version` when it installs or updates a pack.
