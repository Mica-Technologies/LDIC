package com.micatechnologies.minecraft.ldic.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TimeAgoTest {

    private static final long NOW = 1_800_000_000_000L;
    private static final long MIN = 60_000L;

    private static String key(long agoMs) {
        TimeAgo.Ago a = TimeAgo.of(NOW - agoMs, NOW);
        return a == null ? null : a.key + ":" + a.amount;
    }

    @Test
    void picksTheLargestWholeUnit() {
        assertEquals("ldic.ago.now:0", key(30_000));
        assertEquals("ldic.ago.minute:1", key(MIN));
        assertEquals("ldic.ago.minutes:59", key(59 * MIN + 59_000));
        assertEquals("ldic.ago.hour:1", key(60 * MIN));
        assertEquals("ldic.ago.hours:23", key(23 * 60 * MIN));
        assertEquals("ldic.ago.yesterday:1", key(30 * 60 * MIN));
        assertEquals("ldic.ago.days:6", key(6 * 24 * 60 * MIN));
    }

    @Test
    void aWeekOrUnknownMeansShowTheDate() {
        assertNull(key(7 * 24 * 60 * MIN));
        assertNull(TimeAgo.of(0, NOW));
    }

    @Test
    void theFutureCountsAsNow() {
        assertEquals("ldic.ago.now:0", key(-5 * MIN));
    }
}
