# Full-screen inventory

Every inventory can switch between its regular size and a full-screen one. The small button
just right of the inventory's top-right corner switches, and the choice is remembered. You can
also bind a key to it: **Full-screen inventory** in Controls, under LDIC (unbound by default).

## Creative

![The full-screen creative inventory](../assets/screenshots/creative-full.png){ .shot }

In full-screen mode the creative inventory becomes one large screen:

- **Every tab in a sidebar**, with its icon and name. There are no tab pages, so packs with
  dozens of tabs stay usable. Scroll the sidebar with the mouse wheel. On a narrow screen it
  shows icons only (hover for the name).
- **One search box** for the current tab, with the same [search syntax](creative.md#search-syntax).
  The search stays as you change tabs, so `@quark` shows Quark's items tab by tab. Press
  **T** or **Ctrl+F** to type, **Esc** or **Enter** to stop.
- **The sort button** and the item count, as on the regular screen.
- **An item grid as large as the screen allows.** Scroll with the wheel (Shift scrolls a page),
  or drag the scrollbar.
- **Your whole inventory beside it**: armour, offhand, main inventory and hotbar, plus your
  player and a trash slot. Items go straight where you want them, with no Inventory tab.

Clicking the grid works as in vanilla (click to pick up, middle-click for a full stack, number
keys to fill a hotbar slot, Q to throw), with one addition: **Shift-click puts a full stack
straight into your inventory.** Drop a held item on the trash slot to delete it; Shift-click the
trash with nothing held to clear your whole inventory, as vanilla's does.

If the window is too small for the full screen (less than vanilla's 9 by 5 grid would fit),
the regular creative inventory opens instead.

## Survival and other inventories

Survival's inventory, chests, furnaces, crafting tables and Minecraft's other inventory screens
keep their layout and **scale up** to the largest whole pixel size that fits the window, so
nothing blurs. The button only appears when that is bigger than your GUI scale already draws
it.

Other mods' inventory screens stay as they are unless the pack (or the player, in LDIC's
settings) turns on **Scale mod screens**. It is off by default because some mods draw their own
overlays in ways that cannot follow the scale.

## Settings

```json
"inventory": { "full_screen": false, "scale_mod_screens": false, "accent": "#4FC3F7" }
```

`accent` is the colour LDIC's inventory and hotbar use when the menu theme has no `accent`
constant. It is a pack setting (`settings.json`) only.
