package com.micatechnologies.minecraft.ldic.inventory;

/**
 * Where everything goes on the full-screen creative inventory (Decision 14), in GUI pixels:
 * a top bar (title, search, sort, toggle), a tab sidebar on the left, the player's inventory on
 * the right and an item grid filling the rest. The sidebar shrinks to icons when the screen is
 * narrow; {@link #compute} returns null when even that leaves less than vanilla's 9 x 5 grid.
 *
 * <p>Slot positions are the 16 x 16 item corner, as Minecraft's {@code Slot} wants them; each
 * slot's 18 x 18 cell starts one pixel up and left.</p>
 */
public final class CreativeLayout {

    public static final int CELL = 18;
    static final int MARGIN = 6;
    static final int GAP = 4;
    static final int PAD = 5;
    static final int TOP_H = 20;
    static final int SIDEBAR_W = 112;
    static final int SIDEBAR_COMPACT_W = 26;
    static final int SCROLL_W = 6;
    static final int HEADER_H = 11;
    public static final int PLAYER_W = PAD * 2 + 9 * CELL;

    /** A rectangle; {@code right} and {@code bottom} are exclusive. */
    public static final class Rect {
        public final int x;
        public final int y;
        public final int w;
        public final int h;

        public Rect(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }

        public boolean contains(int px, int py) {
            return px >= x && py >= y && px < x + w && py < y + h;
        }
    }

    public final Rect top;
    public final Rect sidebar;
    public final Rect grid;
    public final Rect player;
    public final Rect search;
    public final Rect sort;
    public final Rect toggle;
    public final Rect scrollbar;
    public final boolean compact;
    public final int cols;
    public final int rows;
    /** First grid slot (top-left item corner). */
    public final int gridX;
    public final int gridY;
    /** Armour column: four slots downwards from here, helmet first. */
    public final int armorX;
    public final int armorY;
    public final int offhandX;
    public final int offhandY;
    public final int trashX;
    public final int trashY;
    /** The player model's feet, centred. */
    public final int modelX;
    public final int modelY;
    public final int mainX;
    public final int mainY;
    public final int hotbarX;
    public final int hotbarY;

    private CreativeLayout(int width, int height, boolean compact) {
        this.compact = compact;
        top = new Rect(MARGIN, MARGIN, width - 2 * MARGIN, TOP_H);
        int bodyY = top.bottom() + GAP;
        int bodyH = height - MARGIN - bodyY;
        sidebar = new Rect(MARGIN, bodyY, compact ? SIDEBAR_COMPACT_W : SIDEBAR_W, bodyH);

        int px = width - MARGIN - PLAYER_W;
        int cellX = px + PAD;
        int contentY = bodyY + PAD + HEADER_H;
        armorX = cellX + 1;
        armorY = contentY + 1;
        trashX = cellX + 8 * CELL + 1;
        trashY = contentY + 1;
        offhandX = trashX;
        offhandY = contentY + 3 * CELL + 1;
        modelX = cellX + CELL + (7 * CELL) / 2;
        modelY = contentY + 4 * CELL - 4;
        mainX = cellX + 1;
        mainY = contentY + 4 * CELL + 6 + 1;
        hotbarX = mainX;
        hotbarY = mainY + 3 * CELL + 4;
        player = new Rect(px, bodyY, PLAYER_W, hotbarY - 1 + CELL + PAD - bodyY);

        int gx = sidebar.right() + GAP;
        grid = new Rect(gx, bodyY, px - GAP - gx, bodyH);
        int usableW = grid.w - 2 * PAD - SCROLL_W - 2;
        cols = Math.max(0, usableW / CELL);
        rows = Math.max(0, (grid.h - 2 * PAD) / CELL);
        gridX = grid.x + PAD + Math.max(0, (usableW - cols * CELL) / 2) + 1;
        gridY = grid.y + PAD + 1;
        scrollbar = new Rect(grid.right() - PAD - SCROLL_W, grid.y + PAD, SCROLL_W, rows * CELL);

        toggle = new Rect(top.right() - 4 - 12, top.y + 4, 12, 12);
        sort = new Rect(toggle.x - 3 - 16, top.y + 4, 16, 12);
        search = new Rect(grid.x, top.y + 4, sort.x - 4 - grid.x, 12);
    }

    private boolean fits(int height) {
        return cols >= 9 && rows >= 5 && player.bottom() <= height - MARGIN && search.w >= 60;
    }

    /** The layout for a screen of this size, or null when it is too small for a full screen. */
    public static CreativeLayout compute(int width, int height) {
        CreativeLayout full = new CreativeLayout(width, height, false);
        if (full.fits(height)) {
            return full;
        }
        CreativeLayout compact = new CreativeLayout(width, height, true);
        return compact.fits(height) ? compact : null;
    }
}
