package com.micatechnologies.minecraft.ldic.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The entries mods have added to LDIC's menus. Get it from {@link LdicApi#menus()}.
 *
 * <p>Safe to call from any thread. An entry's suppliers and actions always run on the client
 * thread.</p>
 */
public final class MenuRegistry {

    private final Map<MenuLocation, List<MenuEntry>> entries = new EnumMap<>(MenuLocation.class);

    MenuRegistry() {
        for (MenuLocation l : MenuLocation.values()) {
            entries.put(l, new ArrayList<>());
        }
    }

    /**
     * Adds an entry to a screen. An entry with the same id already at that location is replaced
     * in place, so registering again (after a config reload, say) never duplicates it.
     */
    public synchronized void add(MenuLocation location, MenuEntry entry) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(entry, "entry");
        List<MenuEntry> list = entries.get(location);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(entry.id())) {
                list.set(i, entry);
                return;
            }
        }
        list.add(entry);
    }

    /** Removes an entry; returns whether it was there. */
    public synchronized boolean remove(MenuLocation location, String id) {
        Objects.requireNonNull(location, "location");
        return entries.get(location).removeIf(e -> e.id().equals(id));
    }

    /**
     * The entries at a location in display order: by {@link MenuEntry#order()}, then in the
     * order they were added. A copy; hidden entries are included.
     */
    public synchronized List<MenuEntry> entries(MenuLocation location) {
        Objects.requireNonNull(location, "location");
        List<MenuEntry> out = new ArrayList<>(entries.get(location));
        // A stable sort: entries with the same order keep their registration order.
        out.sort((a, b) -> Integer.compare(a.order(), b.order()));
        return Collections.unmodifiableList(out);
    }
}
