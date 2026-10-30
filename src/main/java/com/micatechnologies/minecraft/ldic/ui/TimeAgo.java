package com.micatechnologies.minecraft.ldic.ui;

/**
 * "Just now", "5 minutes ago", "Yesterday": how long ago something happened, for lists of
 * worlds and servers. Returns language keys and an amount so the caller translates; past a
 * week the caller shows a date instead.
 */
public final class TimeAgo {

    /** A language key and the number it takes (unused by keys without one). */
    public static final class Ago {
        public final String key;
        public final long amount;

        Ago(String key, long amount) {
            this.key = key;
            this.amount = amount;
        }
    }

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    private TimeAgo() {
    }

    /** Null when {@code then} is a week or more ago (or unknown): show the date. */
    public static Ago of(long then, long now) {
        if (then <= 0) {
            return null;
        }
        long d = Math.max(0, now - then);
        if (d < MINUTE) {
            return new Ago("ldic.ago.now", 0);
        }
        if (d < HOUR) {
            long m = d / MINUTE;
            return new Ago(m == 1 ? "ldic.ago.minute" : "ldic.ago.minutes", m);
        }
        if (d < DAY) {
            long h = d / HOUR;
            return new Ago(h == 1 ? "ldic.ago.hour" : "ldic.ago.hours", h);
        }
        if (d < 2 * DAY) {
            return new Ago("ldic.ago.yesterday", 1);
        }
        if (d < 7 * DAY) {
            return new Ago("ldic.ago.days", d / DAY);
        }
        return null;
    }
}
