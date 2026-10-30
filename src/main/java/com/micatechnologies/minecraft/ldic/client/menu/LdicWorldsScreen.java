package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.client.render.Gfx;
import com.micatechnologies.minecraft.ldic.client.render.ListHooks;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiListWorldSelection;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiListWorldSelectionEntry;
import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import com.micatechnologies.minecraft.ldic.ui.UiNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiListWorldSelection;
import net.minecraft.client.gui.GuiListWorldSelectionEntry;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.WorldSummary;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * The themed world selection: the menu theme's {@code worlds} screen with a {@code list}
 * element of world cards (icon, name, when it was last played, game mode, hardcore, cheats,
 * version warnings), a search box and a sort order.
 *
 * <p>It extends {@link GuiWorldSelection} so mods hooking that screen still find it, and acts
 * through vanilla's own list entries: playing, deleting, editing and re-creating a world run
 * exactly vanilla's code, with its confirmations and version warnings. Only the drawing and the
 * list's input are LDIC's.</p>
 */
public final class LdicWorldsScreen extends GuiWorldSelection implements ThemeScreenDriver.Host, ListHooks {

    static final String SCREEN = "worlds";
    private static final ResourceLocation ICON_MISSING = new ResourceLocation("textures/misc/unknown_server.png");
    private static final String[] SORTS = {"recent", "name"};
    private static String sort = "recent";

    private final MenuManager manager;
    private final ThemeScreenDriver driver;
    private GuiListWorldSelection worlds;
    private final List<GuiListWorldSelectionEntry> shown = new ArrayList<>();
    private GuiTextField search;
    @Nullable
    private String selectedFolder;
    private double scroll;
    private long lastClickMs;
    private String lastClickFolder;
    private String tooltip;

    // The list's box and rows from the last frame, in layout units, for hit-testing.
    private double lx;
    private double ly;
    private double lw;
    private double lh;
    private double rowsTop;
    private boolean hasSearch;
    private int mouseX;
    private int mouseY;
    private int heldButton = -1;
    private double rowH = 40;
    private double rowGap = 4;

    LdicWorldsScreen(MenuManager manager, ThemeDef theme, GuiScreen parent) {
        super(parent);
        this.manager = manager;
        this.driver = new ThemeScreenDriver(manager, theme, SCREEN, this, false);
    }

    // ----------------------------------------------------------------------------- state

    /** The selected world's vanilla entry, or null. */
    @Nullable
    GuiListWorldSelectionEntry selected() {
        for (GuiListWorldSelectionEntry e : entries()) {
            if (summary(e).getFileName().equals(selectedFolder)) {
                return e;
            }
        }
        return null;
    }

    boolean hasSelection() {
        return selected() != null;
    }

    int worldCount() {
        return entries().size();
    }

    static String sortName() {
        return sort;
    }

    private List<GuiListWorldSelectionEntry> entries() {
        return worlds == null ? new ArrayList<>() : ((AccessorGuiListWorldSelection) worlds).ldic$getEntries();
    }

    private static WorldSummary summary(GuiListWorldSelectionEntry e) {
        return ((AccessorGuiListWorldSelectionEntry) e).ldic$getSummary();
    }

    private static String name(WorldSummary s) {
        String n = s.getDisplayName();
        return n == null || n.isEmpty() ? s.getFileName() : n;
    }

    /** Rebuilds the visible rows from the search text and sort order. */
    private void refilter() {
        shown.clear();
        String q = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        for (GuiListWorldSelectionEntry e : entries()) {
            WorldSummary s = summary(e);
            if (q.isEmpty() || name(s).toLowerCase(Locale.ROOT).contains(q) || s.getFileName().toLowerCase(Locale.ROOT).contains(q)) {
                shown.add(e);
            }
        }
        if ("name".equals(sort)) {
            shown.sort(Comparator.comparing(e -> name(summary(e)).toLowerCase(Locale.ROOT)));
        } else {
            shown.sort(Comparator.comparingLong((GuiListWorldSelectionEntry e) -> summary(e).getLastTimePlayed()).reversed());
        }
    }

    void cycleSort() {
        int i = java.util.Arrays.asList(SORTS).indexOf(sort);
        sort = SORTS[(i + 1) % SORTS.length];
        refilter();
    }

    void play() {
        GuiListWorldSelectionEntry e = selected();
        if (e != null) {
            e.joinWorld();
        }
    }

    void create() {
        mc.displayGuiScreen(new GuiCreateWorld(this));
    }

    void edit() {
        GuiListWorldSelectionEntry e = selected();
        if (e != null) {
            e.editWorld();
        }
    }

    void delete() {
        GuiListWorldSelectionEntry e = selected();
        if (e != null) {
            e.deleteWorld();
        }
    }

    void recreate() {
        GuiListWorldSelectionEntry e = selected();
        if (e != null) {
            e.recreateWorld();
        }
    }

    // ------------------------------------------------------------------------------ Host

    @Override
    public GuiScreen gui() {
        return this;
    }

    @Override
    public List<GuiButton> buttons() {
        return buttonList;
    }

    @Override
    public void back() {
        mc.displayGuiScreen(prevScreen);
    }

    // ---------------------------------------------------------------------- vanilla screen

    @Override
    public void initGui() {
        title = I18n.format("selectWorld.title");
        // Vanilla's list, for its entries (world summaries, icons) and their actions; never drawn.
        worlds = new GuiListWorldSelection(this, mc, width, height, 32, height - 64, 36);
        String text = search == null ? "" : search.getText();
        search = new GuiTextField(0, fontRenderer, 0, 0, 200, 12);
        search.setMaxStringLength(64);
        search.setText(text);
        refilter();
        driver.init();
    }

    /** Vanilla's list calls this when an entry is selected; the selection is LDIC's here. */
    @Override
    public void selectWorld(@Nullable GuiListWorldSelectionEntry entry) {
    }

    @Override
    public void onGuiClosed() {
        driver.close();
    }

    @Override
    public void updateScreen() {
        if (search != null) {
            search.updateCursorCounter();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        tooltip = null;
        manager.bindings().setWorlds(hasSelection(), worldCount(), sortName());
        driver.draw(mouseX, mouseY, partialTicks);
        if (tooltip != null) {
            List<String> lines = new ArrayList<>();
            for (String l : tooltip.split("\n")) {
                lines.add(l);
            }
            drawHoveringText(lines, mouseX, mouseY);
        }
    }

    private double ui() {
        return manager.theme() == null ? 1 : manager.theme().uiScale(width, height);
    }

    // ------------------------------------------------------------------------------- list

    @Override
    public void drawList(UiNode n) {
        ListCards c = new ListCards(n, manager);
        double ui = ui();
        double mx = mouseX / ui;
        double my = mouseY / ui;
        lx = n.x;
        ly = n.y;
        lw = n.width;
        lh = n.height;
        hasSearch = Boolean.parseBoolean(n.def.statik("search", "true"));
        rowsTop = ly + (hasSearch ? ListCards.SEARCH_H : 0);
        if (hasSearch) {
            boolean hover = mx >= lx && mx < lx + lw && my >= ly && my < ly + ListCards.SEARCH_H - 3;
            c.search(search, lx, ly, lw, hover, I18n.format("ldic.worlds.search"));
        }
        rowH = n.def.num("row_height", 40);
        rowGap = n.def.num("gap", 4);
        double gap = rowGap;
        double viewH = ly + lh - rowsTop;
        double content = shown.size() * (rowH + gap) - gap;
        scroll = Math.max(0, Math.min(Math.max(0, content - viewH), scroll));
        if (shown.isEmpty()) {
            String empty = !search.getText().isEmpty() ? I18n.format("ldic.worlds.none_match")
                    : n.def.str("empty_text", "").isEmpty() ? I18n.format("ldic.worlds.none") : n.def.str("empty_text", "");
            c.text(empty, lx + (lw - c.width(empty, c.size)) / 2, rowsTop + 20, c.size, c.colour("detail_colour", 0xFF9AA3AEL));
            return;
        }
        ListCards.clip(lx, rowsTop, lw, viewH, ui);
        boolean listHover = mx >= lx && mx < lx + lw && my >= rowsTop && my < rowsTop + viewH;
        for (int i = 0; i < shown.size(); i++) {
            double y = rowsTop + i * (rowH + gap) - scroll;
            if (y + rowH < rowsTop || y > rowsTop + viewH) {
                continue;
            }
            GuiListWorldSelectionEntry e = shown.get(i);
            boolean hover = listHover && my >= y && my < y + rowH;
            drawWorld(c, e, lx, y, lw - (content > viewH ? 4 : 0), rowH, hover, mx, my);
        }
        ListCards.unclip();
        c.scrollbar(lx + lw, rowsTop, viewH, content, scroll);
    }

    private void drawWorld(ListCards c, GuiListWorldSelectionEntry e, double x, double y, double w, double h,
                           boolean hover, double mx, double my) {
        WorldSummary s = summary(e);
        boolean sel = s.getFileName().equals(selectedFolder);
        c.card(x, y, w, h, hover, sel);
        double pad = 4;
        double icon = h - pad * 2;
        AccessorGuiListWorldSelectionEntry a = (AccessorGuiListWorldSelectionEntry) e;
        mc.getTextureManager().bindTexture(a.ldic$getIcon() != null ? a.ldic$getIconLocation() : ICON_MISSING);
        Gfx.texturedQuad(x + pad, y + pad, x + pad + icon, y + pad + icon, 0, 0, 1, 1, 0xFFFFFFFFL, c.opacity);
        if (hover && mx < x + pad + icon) {
            // As vanilla: the icon plays the world. Show a play mark over it.
            Gfx.rect(x + pad, y + pad, x + pad + icon, y + pad + icon, 0x80000000L, 0x80000000L, c.opacity);
            double cx = x + pad + icon / 2;
            double cy = y + pad + icon / 2;
            double t = icon * 0.18;
            for (double k = -t; k <= t; k += 0.5) {
                double len = (t - Math.abs(k)) * 1.6;
                Gfx.rect(cx - t * 0.6, cy + k, cx - t * 0.6 + len, cy + k + 0.5, 0xFFFFFFFFL, 0xFFFFFFFFL, c.opacity);
            }
        }
        double tx = x + pad + icon + 6;
        long text = c.colour("text_colour", 0xFFF0F0F0L);
        long detail = c.colour("detail_colour", 0xFF9AA3AEL);
        long accent = c.colour("accent_colour", 0xFF4FC3F7L);

        // Badges, right to left: warnings, then cheats, hardcore, game mode.
        double right = x + w - 6;
        double by = y + 5;
        if (s.requiresConversion()) {
            right = c.badge(I18n.format("selectWorld.conversion").replace("!", ""), right, by, 0xFFFFB74DL);
        } else if (s.markVersionInList()) {
            boolean newer = s.askToOpenWorld();
            double left = c.badge(s.getVersionName(), right, by, newer ? 0xFFEF5350L : 0xFFFFCA28L);
            if (hover && mx >= left && mx < right && my >= by && my < by + c.size) {
                tooltip = newer
                        ? "§c" + I18n.format("selectWorld.tooltip.fromNewerVersion1") + "\n§c" + I18n.format("selectWorld.tooltip.fromNewerVersion2")
                        : "§6" + I18n.format("selectWorld.tooltip.snapshot1") + "\n§6" + I18n.format("selectWorld.tooltip.snapshot2");
            }
            right = left;
        }
        if (!s.requiresConversion()) {
            if (s.getCheatsEnabled()) {
                right = c.badge(I18n.format("selectWorld.cheats"), right, by, detail);
            }
            if (s.isHardcoreModeEnabled()) {
                right = c.badge(I18n.format("gameMode.hardcore"), right, by, 0xFFEF5350L);
            } else {
                right = c.badge(I18n.format("gameMode." + s.getEnumGameType().getName()), right, by, accent);
            }
        }
        String name = c.fit(name(s), c.size, right - tx - 4);
        c.text(name, tx, y + 5, c.size, text);
        String when = ListCards.ago(s.getLastTimePlayed());
        String folder = s.getFileName().equals(name(s)) ? "" : "  ·  " + s.getFileName();
        double ds = c.size * 0.8;
        c.text(c.fit(when + folder, ds, x + w - tx - 6), tx, y + 7 + c.size, ds, detail);
    }

    /** The row index under a point (layout units), or -1. */
    private int rowAt(double mx, double my) {
        if (mx < lx || mx >= lx + lw || my < rowsTop || my >= ly + lh) {
            return -1;
        }
        double rowH = rowHeight();
        double gap = rowGap;
        double off = my - rowsTop + scroll;
        int i = (int) Math.floor(off / (rowH + gap));
        if (i < 0 || i >= shown.size() || off - i * (rowH + gap) > rowH) {
            return -1;
        }
        return i;
    }

    private double rowHeight() {
        return rowH;
    }

    // ------------------------------------------------------------------------------ input

    /** Mouse input as GuiScreen does it; vanilla's list (which polls the mouse) is never fed. */
    @Override
    public void handleMouseInput() throws IOException {
        int x = Mouse.getEventX() * width / mc.displayWidth;
        int y = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int button = Mouse.getEventButton();
        if (Mouse.getEventButtonState()) {
            heldButton = button;
            mouseClicked(x, y, button);
        } else if (button != -1) {
            heldButton = -1;
            mouseReleased(x, y, button);
        } else if (heldButton != -1) {
            mouseClickMove(x, y, heldButton, 0);
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scroll -= Integer.signum(wheel) * (rowHeight() + rowGap);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (driver.mouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }
        double ui = ui();
        double mx = mouseX / ui;
        double my = mouseY / ui;
        if (hasSearch && mx >= lx && mx < lx + lw && my >= ly && my < ly + ListCards.SEARCH_H - 3) {
            if (mouseButton == 1) {
                search.setText("");
                refilter();
            }
            search.setFocused(true);
            return;
        }
        search.setFocused(false);
        int row = rowAt(mx, my);
        if (row >= 0 && mouseButton == 0) {
            WorldSummary s = summary(shown.get(row));
            String folder = s.getFileName();
            selectedFolder = folder;
            long now = System.currentTimeMillis();
            boolean onIcon = mx < lx + rowHeight();
            if (onIcon || folder.equals(lastClickFolder) && now - lastClickMs < 250) {
                play();
            }
            lastClickMs = now;
            lastClickFolder = folder;
            return;
        }
        clickForeign(mouseX, mouseY, mouseButton);
    }

    /** Other mods' buttons, clicked as GuiScreen would (vanilla's own handler also drives its list). */
    private void clickForeign(int x, int y, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }
        for (GuiButton b : new ArrayList<>(buttonList)) {
            if (b.mousePressed(mc, x, y)) {
                GuiScreenEvent.ActionPerformedEvent.Pre pre = new GuiScreenEvent.ActionPerformedEvent.Pre(this, b, buttonList);
                if (MinecraftForge.EVENT_BUS.post(pre)) {
                    break;
                }
                pre.getButton().playPressSound(mc.getSoundHandler());
                if (equals(mc.currentScreen)) {
                    MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.ActionPerformedEvent.Post(this, pre.getButton(), buttonList));
                }
                break;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        driver.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        driver.mouseDragged(mouseX, mouseY);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (search.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                search.setFocused(false);
            } else if (search.textboxKeyTyped(typedChar, keyCode)) {
                refilter();
            }
            return;
        }
        if (keyCode == Keyboard.KEY_F && isCtrlKeyDown()) {
            search.setFocused(true);
            return;
        }
        if ((keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_DOWN) && !shown.isEmpty()) {
            int i = -1;
            for (int k = 0; k < shown.size(); k++) {
                if (summary(shown.get(k)).getFileName().equals(selectedFolder)) {
                    i = k;
                }
            }
            i = Math.max(0, Math.min(shown.size() - 1, i + (keyCode == Keyboard.KEY_UP ? -1 : 1)));
            selectedFolder = summary(shown.get(i)).getFileName();
            double rowTop = i * (rowHeight() + rowGap);
            double viewH = ly + lh - rowsTop;
            scroll = Math.min(rowTop, Math.max(scroll, rowTop + rowHeight() - viewH));
            return;
        }
        if ((keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) && hasSelection()) {
            play();
            return;
        }
        driver.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        // Only other mods' buttons are in buttonList; they act through ActionPerformedEvent.
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (!driver.confirmClicked(result, id)) {
            mc.displayGuiScreen(this);
        }
    }
}
