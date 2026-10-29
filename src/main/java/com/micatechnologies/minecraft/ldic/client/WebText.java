package com.micatechnologies.minecraft.ldic.client;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plain text from the web for themes: a news line, a message of the day. Fetched in the
 * background through {@link WebCache} (so it also works offline from the last copy) and
 * refreshed every {@link #REFRESH_MS}. Until the first fetch finishes, the text is empty.
 */
public final class WebText {

    private static final long REFRESH_MS = 10 * 60_000;
    private static final int MAX_CHARS = 16_384;

    private static final class Entry {
        volatile String text = "";
        volatile String[] lines = new String[0];
        volatile long fetched;
        volatile boolean fetching;
    }

    private final WebCache web;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public WebText(WebCache web) {
        this.web = web;
    }

    public String text(String url) {
        return entry(url).text;
    }

    /** Paragraph {@code i} (wrapping); empty if there are none yet. */
    public String line(String url, int i) {
        String[] lines = entry(url).lines;
        return lines.length == 0 ? "" : lines[((i % lines.length) + lines.length) % lines.length];
    }

    public int lineCount(String url) {
        return entry(url).lines.length;
    }

    private Entry entry(String url) {
        Entry e = entries.computeIfAbsent(url, u -> new Entry());
        long now = System.currentTimeMillis();
        if (!e.fetching && now - e.fetched > REFRESH_MS && AssetResolver.isWeb(url)) {
            e.fetching = true;
            e.fetched = now;
            web.pool().submit(() -> {
                try {
                    String s = new String(web.fetch(url), StandardCharsets.UTF_8);
                    if (s.length() > MAX_CHARS) {
                        s = s.substring(0, MAX_CHARS);
                    }
                    e.text = s.trim();
                    e.lines = paragraphs(s);
                } catch (Exception ex) {
                    // Leave the previous text; WebCache already logged the reason.
                } finally {
                    e.fetching = false;
                }
            });
        }
        return e;
    }

    /** Paragraphs separated by blank lines; lines within one are joined with spaces. */
    static String[] paragraphs(String text) {
        String t = text.trim();
        if (t.isEmpty()) {
            return new String[0];
        }
        String[] parts = t.split("\\r?\\n\\s*\\r?\\n\\s*");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim().replaceAll("\\s*\\r?\\n\\s*", " ");
        }
        return parts;
    }
}
