package com.micatechnologies.minecraft.ldic.inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Orders a creative tab's items. Every order is stable: items that tie keep the tab's own
 * order, so sorting by mod keeps each mod's items the way that mod listed them. Descending
 * reverses the whole order, ties included.
 */
public final class ItemSort {

    public enum Order {
        /** The tab's own order (registration order for most tabs). */
        DEFAULT("default"),
        NAME("name"),
        MOD("mod"),
        ID("id");

        public final String key;

        Order(String key) {
            this.key = key;
        }

        public Order next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public static Order byKey(String key) {
            for (Order o : values()) {
                if (o.key.equalsIgnoreCase(key)) {
                    return o;
                }
            }
            return DEFAULT;
        }
    }

    /** A tab's chosen order. */
    public static final class Pref {
        public static final Pref DEFAULT = new Pref(Order.DEFAULT, false);

        public final Order order;
        public final boolean descending;

        public Pref(Order order, boolean descending) {
            this.order = order;
            this.descending = descending;
        }

        public boolean isDefault() {
            return order == Order.DEFAULT && !descending;
        }

        public Pref nextOrder() {
            return new Pref(order.next(), descending);
        }

        public Pref flipped() {
            return new Pref(order, !descending);
        }
    }

    private static final class Keyed<T> {
        final T item;
        final int index;
        final String key;
        final String key2;

        Keyed(T item, int index, String key, String key2) {
            this.item = item;
            this.index = index;
            this.key = key;
            this.key2 = key2;
        }
    }

    private ItemSort() {
    }

    /** Returns the items in the given order; the input list is not changed. */
    public static <T> List<T> sort(List<T> items, Pref pref, Function<? super T, ? extends ItemQuery.Facts> facts) {
        if (pref.isDefault()) {
            return new ArrayList<>(items);
        }
        List<Keyed<T>> keyed = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            String key = "";
            String key2 = "";
            if (pref.order != Order.DEFAULT) {
                ItemQuery.Facts f = facts.apply(item);
                switch (pref.order) {
                    case NAME:
                        key = ItemQuery.stripFormatting(nonNull(f.name()));
                        break;
                    case MOD:
                        key = nonNull(f.modName());
                        key2 = nonNull(f.modId());
                        break;
                    default:
                        key = nonNull(f.id());
                        break;
                }
            }
            keyed.add(new Keyed<>(item, i, key, key2));
        }
        Comparator<Keyed<T>> c = Comparator.<Keyed<T>, String>comparing(k -> k.key, ItemSort::compareNatural)
                .thenComparing(k -> k.key2, ItemSort::compareNatural)
                .thenComparingInt(k -> k.index);
        keyed.sort(pref.descending ? c.reversed() : c);
        List<T> out = new ArrayList<>(keyed.size());
        for (Keyed<T> k : keyed) {
            out.add(k.item);
        }
        return out;
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }

    /**
     * Case-insensitive comparison where runs of digits compare by value, so "Wool 2" comes
     * before "Wool 10".
     */
    static int compareNatural(String a, String b) {
        String x = a.toLowerCase(Locale.ROOT);
        String y = b.toLowerCase(Locale.ROOT);
        int i = 0;
        int j = 0;
        while (i < x.length() && j < y.length()) {
            char cx = x.charAt(i);
            char cy = y.charAt(j);
            if (Character.isDigit(cx) && Character.isDigit(cy)) {
                int si = i;
                int sj = j;
                while (i < x.length() && Character.isDigit(x.charAt(i))) {
                    i++;
                }
                while (j < y.length() && Character.isDigit(y.charAt(j))) {
                    j++;
                }
                String nx = trimZeros(x.substring(si, i));
                String ny = trimZeros(y.substring(sj, j));
                if (nx.length() != ny.length()) {
                    return nx.length() - ny.length();
                }
                int c = nx.compareTo(ny);
                if (c != 0) {
                    return c;
                }
            } else {
                if (cx != cy) {
                    return cx - cy;
                }
                i++;
                j++;
            }
        }
        return (x.length() - i) - (y.length() - j);
    }

    private static String trimZeros(String digits) {
        int k = 0;
        while (k < digits.length() - 1 && digits.charAt(k) == '0') {
            k++;
        }
        return digits.substring(k);
    }
}
