# HUD

With [OneConfig](https://polyfrost.org/projects/oneconfig/) in the pack, LDIC adds a set of
information HUDs and its own OneConfig page. Open OneConfig with **Right Shift** (or
`/oneconfig`) and pick **Limitless Development: Immersive Client**.

Without OneConfig, LDIC works as usual and these two parts are simply off; the log says so.

## The HUDs

Each is a small text box you drag into place in OneConfig's **Edit HUD** screen, with its own
options (colours, units, which parts to show) on its settings page.

| Group | HUDs |
| --- | --- |
| Information | Coordinates, Facing Direction, Clock, Biome, Day Counter, Real-Life Date, Server IP, Resource Pack, Game Mode, Block Above, Height Limit, Looking-At Block, Light Level |
| Player | Pitch, Yaw, Speed, Saturation, Armor Durability, Health, Hunger, Experience, Active Effects, Tool Durability |
| Performance | FPS, Frame Time, TPS, Memory, Ping |
| Counters | CPS, Click Counter, Blocks Placed, Session Playtime |
| Custom Text | Ten slots for your own text, with `%placeholders%`: `%player%`, `%x%`, `%y%`, `%z%`, `%dim%`, `%biome%`, `%server%`, `%time%`, `%realtime%`, `%date%`, `%fps%`, `%direction%` |

## Presets

**HUD Presets** on LDIC's page apply a **style** (size, background, border, text shadow) to every
HUD at once, and a **layout** that switches HUDs on and places them. Both are starting points:
drag and tweak freely afterwards; applying a preset again overwrites those tweaks.

## Moving from SUM

These HUDs used to be part of SUM. SUM keeps the ones that read its own server data (wallet,
bank, vault, phone number, pocket, plot info, jobs, loyalty, nearest roamer, border distance,
now playing).

- On the first start with LDIC's page, a player's positions and options for the moved HUDs are
  copied from SUM's settings, so nothing has to be set up again.
- SUM's and LDIC's presets have the same names and positions, and each leaves room for the
  other's rows, so applying the same layout on both pages gives one tidy layout.

## Hotbar and inventory settings

LDIC's page also has the [hotbar](hotbar.md) and [inventory](full-screen.md) settings. They are
the same settings as LDIC's own Config screen (Mods list) and are saved in
`config/ldic/player.json` either way.
