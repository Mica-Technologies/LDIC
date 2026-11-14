package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.api.MenuPage;
import java.util.List;
import net.minecraft.client.resources.I18n;

/**
 * A mod's {@link MenuPage} as a screen shows it: which rows are in view, and each row's text
 * and state read safely. The page's getters and setters are another mod's code, so a failure
 * in one is logged (once per page) and that row shows nothing, rather than taking the menu down.
 */
public final class PageView {

    private final MenuPage page;
    private final List<MenuPage.Row> rows;
    private int scroll;
    private int fit = Integer.MAX_VALUE;
    private boolean failureLogged;

    PageView(MenuPage page) {
        this.page = page;
        this.rows = page.rows();
    }

    public String title() {
        try {
            return page.title();
        } catch (RuntimeException e) {
            failed(e);
            return "";
        }
    }

    /** How many rows the page has in all. */
    public int rows() {
        return rows.size();
    }

    /** The index of the first row in view. */
    public int scroll() {
        return scroll;
    }

    /**
     * Tells the view how many rows the screen has room for; returns how many it should draw.
     * The wheel then stops at the last full view.
     */
    public int fit(int room) {
        fit = Math.max(1, room);
        clampScroll();
        return Math.min(fit, rows.size() - scroll);
    }

    /** Whether there are rows below the ones in view. */
    public boolean more() {
        return fit != Integer.MAX_VALUE && scroll + fit < rows.size();
    }

    void scrollBy(int delta) {
        scroll += delta;
        clampScroll();
    }

    private void clampScroll() {
        int max = fit == Integer.MAX_VALUE ? 0 : Math.max(0, rows.size() - fit);
        scroll = Math.max(0, Math.min(max, scroll));
    }

    /** The row at a position among the rows in view, or null. */
    private MenuPage.Row row(int visibleIndex) {
        int i = scroll + visibleIndex;
        return visibleIndex >= 0 && i < rows.size() ? rows.get(i) : null;
    }

    public String kind(int visibleIndex) {
        MenuPage.Row r = row(visibleIndex);
        return r == null ? "" : r.kind().key();
    }

    public String label(int visibleIndex) {
        MenuPage.Row r = row(visibleIndex);
        try {
            return r == null ? "" : r.label();
        } catch (RuntimeException e) {
            failed(e);
            return "";
        }
    }

    /** The row's label with its value: {@code Fancy mode: ON}, {@code Range: 16}. */
    public String text(int visibleIndex) {
        MenuPage.Row r = row(visibleIndex);
        try {
            return r == null ? "" : text(r);
        } catch (RuntimeException e) {
            failed(e);
            return "";
        }
    }

    public double value(int visibleIndex) {
        MenuPage.Row r = row(visibleIndex);
        try {
            return r == null ? 0 : r.fraction();
        } catch (RuntimeException e) {
            failed(e);
            return 0;
        }
    }

    public boolean on(int visibleIndex) {
        MenuPage.Row r = row(visibleIndex);
        try {
            return r != null && r.on();
        } catch (RuntimeException e) {
            failed(e);
            return false;
        }
    }

    public boolean enabled(int visibleIndex) {
        MenuPage.Row r = row(visibleIndex);
        try {
            return r != null && r.enabled();
        } catch (RuntimeException e) {
            failed(e);
            return false;
        }
    }

    public String tooltip(int visibleIndex) {
        MenuPage.Row r = row(visibleIndex);
        try {
            return r == null ? "" : r.tooltip();
        } catch (RuntimeException e) {
            failed(e);
            return "";
        }
    }

    void activate(int visibleIndex) {
        MenuPage.Row r = row(visibleIndex);
        try {
            if (r != null) {
                r.activate();
            }
        } catch (RuntimeException e) {
            failed(e);
        }
    }

    void setFraction(int visibleIndex, double fraction) {
        MenuPage.Row r = row(visibleIndex);
        try {
            if (r != null) {
                r.setFraction(fraction);
            }
        } catch (RuntimeException e) {
            failed(e);
        }
    }

    /** Moves a slider one key step up or down. */
    void nudge(int visibleIndex, int direction, boolean large) {
        MenuPage.Row r = row(visibleIndex);
        try {
            if (r != null) {
                double step = r.keyStep() * (large ? 5 : 1);
                r.setFraction(r.fraction() + direction * step);
            }
        } catch (RuntimeException e) {
            failed(e);
        }
    }

    void closed() {
        try {
            page.closed();
        } catch (RuntimeException e) {
            failed(e);
        }
    }

    /** A row's label with its value, as both the themed and the plain page show it. */
    static String text(MenuPage.Row r) {
        switch (r.kind()) {
            case TOGGLE:
                return r.label() + ": " + I18n.format(r.on() ? "options.on" : "options.off");
            case SLIDER:
            case CHOICE:
                return r.label() + ": " + r.valueText();
            default:
                return r.label();
        }
    }

    private void failed(RuntimeException e) {
        if (!failureLogged) {
            failureLogged = true;
            Ldic.LOGGER.error("LDIC: a mod's menu page failed; the row it belongs to is left blank (logged once per page)", e);
        }
    }
}
