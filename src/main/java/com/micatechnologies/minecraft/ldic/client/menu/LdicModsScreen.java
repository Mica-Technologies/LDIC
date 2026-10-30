package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.client.render.Gfx;
import com.micatechnologies.minecraft.ldic.client.render.ListHooks;
import com.micatechnologies.minecraft.ldic.client.render.LdicFont;
import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import com.micatechnologies.minecraft.ldic.ui.UiNode;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.GuiModList;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ModMetadata;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * The themed mod list: the menu theme's {@code mod_list} screen, with a {@code list} element of
 * mod cards (logo, name, version, an update badge), a search box and a sort order, and a
 * {@code mod_info} element for the selected mod (logo, version, id, state, authors, credits,
 * links, child mods, available update and its changes, description).
 *
 * <p>It extends Forge's {@link GuiModList} so mods hooking that screen still find it. It lists
 * what Forge's does (active mods that are not another mod's child), and opens a mod's config
 * screen through the mod's own gui factory, as Forge does.</p>
 */
public final class LdicModsScreen extends GuiModList implements ThemeScreenDriver.Host, ListHooks {

    static final String SCREEN = "mod_list";
    private static final String[] SORTS = {"normal", "a_z", "z_a"};
    private static String sort = "normal";

    private final MenuManager manager;
    private final ThemeScreenDriver driver;
    private final GuiScreen parent;
    private final List<ModContainer> shown = new ArrayList<>();
    private GuiTextField search;
    @Nullable
    private ModContainer selected;
    private double scroll;
    private double infoScroll;
    private int mouseX;
    private int mouseY;
    private int heldButton = -1;

    // The list's box and rows from the last frame, in layout units, for hit-testing.
    private double lx;
    private double ly;
    private double lw;
    private double lh;
    private double rowsTop;
    private boolean hasSearch;
    private double rowH = 32;
    private double rowGap = 3;
    // The info panel's box and its links from the last frame.
    private double ix;
    private double iy;
    private double iw;
    private double ih;
    private double infoContent;
    private final List<double[]> linkBoxes = new ArrayList<>();
    private final List<String> linkUrls = new ArrayList<>();

    /** Logos by mod id, loaded as they are first shown (null when the mod has none). */
    private final Map<String, Logo> logos = new HashMap<>();

    private static final class Logo {
        final ResourceLocation location;
        final DynamicTexture texture;
        final int width;
        final int height;

        Logo(ResourceLocation location, DynamicTexture texture, int width, int height) {
            this.location = location;
            this.texture = texture;
            this.width = width;
            this.height = height;
        }
    }

    LdicModsScreen(MenuManager manager, ThemeDef theme, GuiScreen parent) {
        super(parent);
        this.manager = manager;
        this.parent = parent;
        this.driver = new ThemeScreenDriver(manager, theme, SCREEN, this, false);
    }

    // ----------------------------------------------------------------------------- state

    boolean hasSelection() {
        return selected != null;
    }

    /** Whether the selected mod has a config screen (Forge's Config button). */
    boolean configurable() {
        if (selected == null) {
            return false;
        }
        IModGuiFactory f = FMLClientHandler.instance().getGuiFactoryFor(selected);
        return f != null && f.hasConfigGui();
    }

    static String sortName() {
        return sort;
    }

    int modCount() {
        return topLevel().size();
    }

    private static List<ModContainer> topLevel() {
        List<ModContainer> out = new ArrayList<>();
        for (ModContainer m : Loader.instance().getActiveModList()) {
            if (m.getMetadata() == null || m.getMetadata().parentMod == null) {
                out.add(m);
            }
        }
        return out;
    }

    private static String plain(String s) {
        return TextFormatting.getTextWithoutFormattingCodes(s == null ? "" : s);
    }

    /** Rebuilds the visible rows from the search text and sort order. */
    private void refilter() {
        shown.clear();
        String q = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        for (ModContainer m : topLevel()) {
            if (q.isEmpty() || plain(m.getName()).toLowerCase(Locale.ROOT).contains(q)
                    || m.getModId().toLowerCase(Locale.ROOT).contains(q)) {
                shown.add(m);
            }
        }
        if (!"normal".equals(sort)) {
            shown.sort((a, b) -> plain(a.getName()).compareToIgnoreCase(plain(b.getName())));
            if ("z_a".equals(sort)) {
                java.util.Collections.reverse(shown);
            }
        }
    }

    void cycleSort() {
        int i = java.util.Arrays.asList(SORTS).indexOf(sort);
        sort = SORTS[(i + 1) % SORTS.length];
        refilter();
    }

    void openConfig() {
        if (selected == null) {
            return;
        }
        try {
            IModGuiFactory f = FMLClientHandler.instance().getGuiFactoryFor(selected);
            if (f != null && f.hasConfigGui()) {
                mc.displayGuiScreen(f.createConfigGui(this));
            }
        } catch (Exception e) {
            Ldic.LOGGER.error("LDIC: could not open the config screen of {}", selected.getModId(), e);
        }
    }

    private void select(ModContainer m) {
        if (m != selected) {
            selected = m;
            infoScroll = 0;
        }
    }

    @Nullable
    private Logo logo(ModContainer m) {
        if (logos.containsKey(m.getModId())) {
            return logos.get(m.getModId());
        }
        Logo logo = null;
        String file = m.getMetadata() == null ? "" : m.getMetadata().logoFile;
        if (file != null && !file.isEmpty()) {
            try {
                // As Forge's list: the mod's resource pack image, else the logo file on the classpath.
                BufferedImage image = null;
                IResourcePack pack = FMLClientHandler.instance().getResourcePackFor(m.getModId());
                if (pack != null) {
                    image = pack.getPackImage();
                } else {
                    InputStream in = GuiModList.class.getResourceAsStream(file);
                    if (in != null) {
                        image = TextureUtil.readBufferedImage(in);
                    }
                }
                if (image != null) {
                    DynamicTexture t = new DynamicTexture(image);
                    ResourceLocation loc = mc.getTextureManager().getDynamicTextureLocation("ldic_modlogo", t);
                    logo = new Logo(loc, t, image.getWidth(), image.getHeight());
                }
            } catch (IOException | RuntimeException e) {
                // No logo then.
            }
        }
        logos.put(m.getModId(), logo);
        return logo;
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
        freeLogos();
        mc.displayGuiScreen(parent);
    }

    // ---------------------------------------------------------------------- vanilla screen

    /** Forge's widgets are not made; the theme draws everything. */
    @Override
    public void initGui() {
        String text = search == null ? "" : search.getText();
        search = new GuiTextField(0, fontRenderer, 0, 0, 200, 12);
        search.setMaxStringLength(64);
        search.setText(text);
        refilter();
        driver.init();
    }

    @Override
    public void onGuiClosed() {
        driver.close();
    }

    /** The logos go with the screen (a config screen opened from it comes back to a new one). */
    private void freeLogos() {
        for (Logo l : logos.values()) {
            if (l != null) {
                mc.getTextureManager().deleteTexture(l.location);
            }
        }
        logos.clear();
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
        manager.bindings().setMods(hasSelection(), configurable(), sortName());
        driver.draw(mouseX, mouseY, partialTicks);
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
            c.search(search, lx, ly, lw, hover, I18n.format("ldic.mods.search"));
        }
        rowH = n.def.num("row_height", 32);
        rowGap = n.def.num("gap", 3);
        double viewH = ly + lh - rowsTop;
        double content = shown.size() * (rowH + rowGap) - rowGap;
        scroll = Math.max(0, Math.min(Math.max(0, content - viewH), scroll));
        if (shown.isEmpty()) {
            String empty = I18n.format("ldic.mods.none_match");
            c.text(empty, lx + (lw - c.width(empty, c.size)) / 2, rowsTop + 20, c.size, c.colour("detail_colour", 0xFF9AA3AEL));
            return;
        }
        ListCards.clip(lx, rowsTop, lw, viewH, ui);
        boolean listHover = mx >= lx && mx < lx + lw && my >= rowsTop && my < rowsTop + viewH;
        for (int i = 0; i < shown.size(); i++) {
            double y = rowsTop + i * (rowH + rowGap) - scroll;
            if (y + rowH < rowsTop || y > rowsTop + viewH) {
                continue;
            }
            boolean hover = listHover && my >= y && my < y + rowH;
            drawMod(c, shown.get(i), lx, y, lw - (content > viewH ? 4 : 0), rowH, hover);
        }
        ListCards.unclip();
        c.scrollbar(lx + lw, rowsTop, viewH, content, scroll);
    }

    private void drawMod(ListCards c, ModContainer m, double x, double y, double w, double h, boolean hover) {
        c.card(x, y, w, h, hover, m == selected);
        double pad = 4;
        double icon = h - pad * 2;
        drawIcon(c, m, x + pad, y + pad, icon);
        double tx = x + pad + icon + 6;
        long text = c.colour("text_colour", 0xFFF0F0F0L);
        long detail = c.colour("detail_colour", 0xFF9AA3AEL);
        double right = x + w - 6;
        if (outdated(m)) {
            right = c.badge(I18n.format("ldic.mods.update"), right, y + 5, c.colour("accent_colour", 0xFF4FC3F7L));
        }
        c.text(c.fit(m.getName(), c.size, right - tx - 4), tx, y + 5, c.size, text);
        double ds = c.size * 0.8;
        c.text(c.fit(m.getDisplayVersion(), ds, x + w - tx - 6), tx, y + 7 + c.size, ds, detail);
    }

    /** The mod's logo fitted in a square, or its initial on a tile coloured from its id. */
    private void drawIcon(ListCards c, ModContainer m, double x, double y, double s) {
        Logo logo = logo(m);
        if (logo != null && logo.width <= logo.height * 2) {
            double k = Math.min(s / logo.width, s / logo.height);
            double w = logo.width * k;
            double h = logo.height * k;
            mc.getTextureManager().bindTexture(logo.location);
            Gfx.texturedQuad(x + (s - w) / 2, y + (s - h) / 2, x + (s + w) / 2, y + (s + h) / 2, 0, 0, 1, 1, 0xFFFFFFFFL, c.opacity);
            return;
        }
        int hash = m.getModId().hashCode();
        long tile = 0xFF000000L | (0x303030 + ((hash & 0x7F) << 16) + (((hash >> 8) & 0x7F) << 8) + ((hash >> 16) & 0x7F));
        Gfx.roundedRect(x, y, x + s, y + s, 3, tile, tile, c.opacity);
        String name = plain(m.getName()).trim();
        String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
        double ts = s * 0.5;
        c.text(initial, x + (s - c.width(initial, ts)) / 2, y + (s - ts) / 2, ts, 0xFFFFFFFFL);
    }

    private static boolean outdated(ModContainer m) {
        ForgeVersion.CheckResult r = ForgeVersion.getResult(m);
        return r.status == ForgeVersion.Status.OUTDATED || r.status == ForgeVersion.Status.BETA_OUTDATED;
    }

    /** The row index under a point (layout units), or -1. */
    private int rowAt(double mx, double my) {
        if (mx < lx || mx >= lx + lw || my < rowsTop || my >= ly + lh) {
            return -1;
        }
        double off = my - rowsTop + scroll;
        int i = (int) Math.floor(off / (rowH + rowGap));
        if (i < 0 || i >= shown.size() || off - i * (rowH + rowGap) > rowH) {
            return -1;
        }
        return i;
    }

    // ------------------------------------------------------------------------------- info

    @Override
    public void drawDetails(UiNode n) {
        ListCards c = new ListCards(n, manager);
        double ui = ui();
        double mx = mouseX / ui;
        double my = mouseY / ui;
        ix = n.x;
        iy = n.y;
        iw = n.width;
        ih = n.height;
        linkBoxes.clear();
        linkUrls.clear();
        long text = c.colour("text_colour", 0xFFF0F0F0L);
        long detail = c.colour("detail_colour", 0xFF9AA3AEL);
        long accent = c.colour("accent_colour", 0xFF4FC3F7L);
        if (selected == null) {
            String hint = I18n.format("ldic.mods.pick");
            c.text(hint, ix + (iw - c.width(hint, c.size)) / 2, iy + ih / 2 - c.size / 2, c.size, detail);
            return;
        }
        ModContainer m = selected;
        ModMetadata meta = m.getMetadata();
        LdicFont titleFont = manager.fonts().get(n.def.statik("title_font", n.def.statik("font", "minecraft")));
        double size = c.size;
        double line = size * 1.35;
        infoScroll = Math.max(0, Math.min(Math.max(0, infoContent - ih), infoScroll));
        ListCards.clip(ix, iy, iw, ih, ui);
        double y = iy - infoScroll;
        double top = y;

        Logo logo = logo(m);
        if (logo != null) {
            double k = Math.min(Math.min(iw / logo.width, 64.0 / logo.height), 1.5);
            double w = logo.width * k;
            double h = logo.height * k;
            mc.getTextureManager().bindTexture(logo.location);
            Gfx.texturedQuad(ix, y, ix + w, y + h, 0, 0, 1, 1, 0xFFFFFFFFL, c.opacity);
            y += h + 8;
        }
        double ts = size * 1.6;
        for (String l : titleFont.wrap(m.getName(), iw, ts)) {
            titleFont.draw(l, ix, y, ts, text, false, c.opacity);
            y += ts * 1.2;
        }
        y += 2;
        String version = m.getDisplayVersion().equals(m.getVersion()) ? m.getVersion() : m.getDisplayVersion() + "  (" + m.getVersion() + ")";
        double badgeRight = ix + c.width(version, size * 0.75) + 6;
        c.badge(version, badgeRight, y, accent);
        c.text(m.getModId() + "  ·  " + Loader.instance().getModState(m), badgeRight + 6, y + 1, size * 0.85, detail);
        y += size + 10;

        if (meta == null || meta.autogenerated) {
            c.text(I18n.format("ldic.mods.no_info"), ix, y, size, 0xFFEF9A9AL);
            y += line;
        } else {
            y = field(c, I18n.format("ldic.mods.authors"), meta.getAuthorList(), y, line, text, detail);
            y = field(c, I18n.format("ldic.mods.credits"), meta.credits, y, line, text, detail);
            if (meta.url != null && !meta.url.isEmpty()) {
                y = link(c, I18n.format("ldic.mods.website"), meta.url, y, line, accent, detail, mx, my);
            }
            if (!meta.childMods.isEmpty()) {
                y = field(c, I18n.format("ldic.mods.children"), meta.getChildModList(), y, line, text, detail);
            }
        }
        ForgeVersion.CheckResult check = ForgeVersion.getResult(m);
        if (outdated(m)) {
            String url = check.url == null ? "" : check.url;
            y = url.isEmpty() ? field(c, I18n.format("ldic.mods.update"), check.target == null ? "" : check.target.toString(), y, line, accent, detail)
                    : link(c, I18n.format("ldic.mods.update"), url, y, line, accent, detail, mx, my);
        }
        if (meta != null && meta.description != null && !meta.description.isEmpty()) {
            y += 6;
            for (String l : c.font.wrap(meta.description, iw - 4, size)) {
                c.text(l, ix, y, size, text);
                y += line;
            }
        }
        if (outdated(m) && !check.changes.isEmpty()) {
            y += 6;
            c.text(I18n.format("ldic.mods.changes"), ix, y, size, accent);
            y += line;
            for (Map.Entry<?, String> e : check.changes.entrySet()) {
                c.text(e.getKey().toString(), ix, y, size, text);
                y += line;
                for (String l : c.font.wrap(e.getValue(), iw - 10, size * 0.9)) {
                    c.text(l, ix + 6, y, size * 0.9, detail);
                    y += line * 0.9;
                }
            }
        }
        ListCards.unclip();
        infoContent = y - top;
        c.scrollbar(ix + iw, iy, ih, infoContent, infoScroll);
    }

    /** A "Label: value" line (wrapped); skipped when the value is empty. */
    private double field(ListCards c, String label, String value, double y, double line, long text, long detail) {
        if (value == null || value.trim().isEmpty()) {
            return y;
        }
        double lw = c.width(label + "  ", c.size);
        c.text(label, ix, y, c.size, detail);
        for (String l : c.font.wrap(value, iw - lw - 4, c.size)) {
            c.text(l, ix + lw, y, c.size, text);
            y += line;
        }
        return y;
    }

    /** A "Label: url" line whose url opens (through vanilla's link confirmation) on a click. */
    private double link(ListCards c, String label, String url, double y, double line, long accent, long detail, double mx, double my) {
        double lw = c.width(label + "  ", c.size);
        c.text(label, ix, y, c.size, detail);
        String shown = c.fit(url, c.size, iw - lw - 4);
        double x0 = ix + lw;
        double x1 = x0 + c.width(shown, c.size);
        boolean hover = mx >= x0 && mx < x1 && my >= y && my < y + c.size && my >= iy && my < iy + ih;
        c.text(shown, x0, y, c.size, hover ? 0xFFFFFFFFL : accent);
        Gfx.rect(x0, y + c.size, x1, y + c.size + 0.5, hover ? 0xFFFFFFFFL : accent, hover ? 0xFFFFFFFFL : accent, c.opacity * (hover ? 1 : 0.5));
        linkBoxes.add(new double[] {x0, y, x1, y + c.size});
        linkUrls.add(url);
        return y + line;
    }

    private boolean inInfo(double mx, double my) {
        return mx >= ix && mx < ix + iw && my >= iy && my < iy + ih;
    }

    // ------------------------------------------------------------------------------ input

    /** Mouse input as GuiScreen does it; Forge's lists (never made) are not fed. */
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
            double ui = ui();
            if (inInfo(x / ui, y / ui)) {
                infoScroll -= Integer.signum(wheel) * 24;
            } else {
                scroll -= Integer.signum(wheel) * (rowH + rowGap);
            }
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
            select(shown.get(row));
            return;
        }
        if (mouseButton == 0 && inInfo(mx, my)) {
            for (int i = 0; i < linkBoxes.size(); i++) {
                double[] b = linkBoxes.get(i);
                if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) {
                    String url = linkUrls.get(i);
                    handleComponentClick(new TextComponentString(url)
                            .setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))));
                    return;
                }
            }
            return;
        }
        clickForeign(mouseX, mouseY, mouseButton);
    }

    /** Other mods' buttons, clicked as GuiScreen would. */
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
            int i = shown.indexOf(selected);
            i = Math.max(0, Math.min(shown.size() - 1, i + (keyCode == Keyboard.KEY_UP ? -1 : 1)));
            select(shown.get(i));
            double rowTop = i * (rowH + rowGap);
            double viewH = ly + lh - rowsTop;
            scroll = Math.min(rowTop, Math.max(scroll, rowTop + rowH - viewH));
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
            // A mod's link (vanilla's confirmation opens it and comes back here).
            super.confirmClicked(result, id);
        }
    }
}
