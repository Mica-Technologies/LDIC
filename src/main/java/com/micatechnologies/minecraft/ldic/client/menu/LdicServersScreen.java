package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.client.render.Gfx;
import com.micatechnologies.minecraft.ldic.client.render.ListHooks;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiMultiplayer;
import com.micatechnologies.minecraft.ldic.mixin.AccessorServerListEntryNormal;
import com.micatechnologies.minecraft.ldic.mixin.AccessorServerSelectionList;
import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import com.micatechnologies.minecraft.ldic.ui.UiNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ServerListEntryLanDetected;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.gui.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.LanServerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * The themed server list: the menu theme's {@code servers} screen with a {@code list} element of
 * server cards (icon, name, message of the day in its own colours, players, ping, version and
 * mod-compatibility warnings) and the LAN games found on the network, with a search box.
 *
 * <p>It extends {@link GuiMultiplayer} and runs all of vanilla's logic: the saved list, LAN
 * discovery, pinging, and the add / edit / delete / direct-connect / refresh flows with their
 * confirmations, by pressing vanilla's own (hidden) buttons. Vanilla's entries are still "drawn"
 * every frame with colour writes off, because that is where vanilla starts each ping and decodes
 * each icon. Other mods' buttons stay, in the theme's foreign_buttons area.</p>
 */
public final class LdicServersScreen extends GuiMultiplayer implements ThemeScreenDriver.Host, ListHooks {

    static final String SCREEN = "servers";
    private static final ResourceLocation UNKNOWN_SERVER = new ResourceLocation("textures/misc/unknown_server.png");
    // Vanilla's button ids (GuiMultiplayer.createButtons).
    private static final int JOIN = 1;
    private static final int DIRECT = 4;
    private static final int ADD = 3;
    private static final int EDIT = 7;
    private static final int DELETE = 2;
    private static final int REFRESH = 8;
    /** Vanilla's network protocol for 1.12.2: other values are a different game version. */
    private static final int PROTOCOL = 340;

    private final MenuManager manager;
    private final ThemeScreenDriver driver;
    private final Map<Integer, GuiButton> vanillaButtons = new HashMap<>();
    private final List<Integer> shown = new ArrayList<>();
    private GuiTextField search;
    private double scroll;
    private long lastClickMs;
    private int lastClickIndex = -1;
    private String tooltip;
    private int mouseX;
    private int mouseY;
    private int heldButton = -1;
    private double lx;
    private double ly;
    private double lw;
    private double lh;
    private double rowsTop;
    private double rowH = 40;
    private double rowGap = 4;
    private boolean hasSearch;

    LdicServersScreen(MenuManager manager, ThemeDef theme, GuiScreen parent) {
        super(parent);
        this.manager = manager;
        this.driver = new ThemeScreenDriver(manager, theme, SCREEN, this, false);
    }

    // ----------------------------------------------------------------------------- state

    private ServerSelectionList list() {
        return ((AccessorGuiMultiplayer) (Object) this).ldic$getServerListSelector();
    }

    private List<ServerListEntryNormal> saved() {
        return ((AccessorServerSelectionList) list()).ldic$getSaved();
    }

    private List<ServerListEntryLanDetected> lan() {
        return ((AccessorServerSelectionList) list()).ldic$getLan();
    }

    /** Vanilla's index for a LAN entry: saved servers, then the scan row, then LAN games. */
    private int lanIndex(int i) {
        return saved().size() + 1 + i;
    }

    int selectedIndex() {
        return list() == null ? -1 : list().getSelected();
    }

    boolean hasSelection() {
        int i = selectedIndex();
        return i >= 0 && i != saved().size() && i <= saved().size() + lan().size();
    }

    boolean selectionEditable() {
        int i = selectedIndex();
        return i >= 0 && i < saved().size();
    }

    int serverCount() {
        return saved().size();
    }

    void press(int id) {
        GuiButton b = vanillaButtons.get(id);
        if (b == null) {
            return;
        }
        // Vanilla's buttons enable themselves from the selection (selectServer); respect that.
        if (id == JOIN && !hasSelection() || (id == EDIT || id == DELETE) && !selectionEditable()) {
            return;
        }
        b.enabled = true;
        try {
            super.actionPerformed(b);
        } catch (IOException e) {
            com.micatechnologies.minecraft.ldic.Ldic.LOGGER.warn("LDIC: server action {} failed", id, e);
        }
    }

    void join() {
        press(JOIN);
    }

    void direct() {
        press(DIRECT);
    }

    void add() {
        press(ADD);
    }

    void edit() {
        press(EDIT);
    }

    void delete() {
        press(DELETE);
    }

    void refresh() {
        press(REFRESH);
    }

    private void refilter() {
        shown.clear();
        String q = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        List<ServerListEntryNormal> saved = saved();
        for (int i = 0; i < saved.size(); i++) {
            ServerData d = saved.get(i).getServerData();
            if (q.isEmpty() || d.serverName.toLowerCase(Locale.ROOT).contains(q) || d.serverIP.toLowerCase(Locale.ROOT).contains(q)) {
                shown.add(i);
            }
        }
        List<ServerListEntryLanDetected> lan = lan();
        for (int i = 0; i < lan.size(); i++) {
            LanServerInfo info = lan.get(i).getServerData();
            if (q.isEmpty() || info.getServerMotd().toLowerCase(Locale.ROOT).contains(q)) {
                shown.add(lanIndex(i));
            }
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
        mc.displayGuiScreen(((AccessorGuiMultiplayer) (Object) this).ldic$getParentScreen());
    }

    // ---------------------------------------------------------------------- vanilla screen

    @Override
    public void initGui() {
        // Vanilla sets everything up (saved list, LAN discovery, its list and buttons); its
        // buttons are then set aside, so only other mods' buttons (added after this) are shown.
        super.initGui();
        vanillaButtons.clear();
        for (GuiButton b : buttonList) {
            vanillaButtons.put(b.id, b);
        }
        buttonList.clear();
        String text = search == null ? "" : search.getText();
        search = new GuiTextField(0, fontRenderer, 0, 0, 200, 12);
        search.setMaxStringLength(64);
        search.setText(text);
        driver.init();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        driver.close();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        search.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        tooltip = null;
        refilter();
        runVanillaEntries(partialTicks);
        manager.bindings().setServers(hasSelection(), selectionEditable(), serverCount());
        driver.draw(mouseX, mouseY, partialTicks);
        if (tooltip != null) {
            List<String> lines = new ArrayList<>();
            for (String l : tooltip.split("\n")) {
                lines.add(l);
            }
            drawHoveringText(lines, mouseX, mouseY);
        }
    }

    /** Runs vanilla's per-frame entry code (starting pings, decoding icons) without drawing it. */
    private void runVanillaEntries(float partialTicks) {
        GlStateManager.colorMask(false, false, false, false);
        try {
            List<ServerListEntryNormal> saved = saved();
            for (int i = 0; i < saved.size(); i++) {
                saved.get(i).drawEntry(i, -1000, -1000, 220, 32, -5000, -5000, false, partialTicks);
            }
        } finally {
            GlStateManager.colorMask(true, true, true, true);
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
            c.search(search, lx, ly, lw, hover, I18n.format("ldic.servers.search"));
        }
        rowH = n.def.num("row_height", 40);
        rowGap = n.def.num("gap", 4);
        double viewH = ly + lh - rowsTop;
        // One more row at the end: vanilla's "scanning for LAN games".
        int rows = shown.size() + 1;
        double content = rows * (rowH + rowGap) - rowGap;
        scroll = Math.max(0, Math.min(Math.max(0, content - viewH), scroll));
        ListCards.clip(lx, rowsTop, lw, viewH, ui);
        boolean listHover = mx >= lx && mx < lx + lw && my >= rowsTop && my < rowsTop + viewH;
        double w = lw - (content > viewH ? 4 : 0);
        for (int r = 0; r < rows; r++) {
            double y = rowsTop + r * (rowH + rowGap) - scroll;
            if (y + rowH < rowsTop || y > rowsTop + viewH) {
                continue;
            }
            boolean hover = listHover && my >= y && my < y + rowH;
            if (r == shown.size()) {
                drawScanRow(c, lx, y, w, rowH);
                continue;
            }
            int index = shown.get(r);
            if (index < saved().size()) {
                drawServer(c, saved().get(index), index, lx, y, w, rowH, hover, mx, my);
            } else {
                drawLan(c, lan().get(index - saved().size() - 1), index, lx, y, w, rowH, hover);
            }
        }
        ListCards.unclip();
        c.scrollbar(lx + lw, rowsTop, viewH, content, scroll);
    }

    private void drawServer(ListCards c, ServerListEntryNormal e, int index, double x, double y, double w, double h,
                            boolean hover, double mx, double my) {
        ServerData d = e.getServerData();
        boolean sel = index == selectedIndex();
        c.card(x, y, w, h, hover, sel);
        double pad = 4;
        double icon = h - pad * 2;
        AccessorServerListEntryNormal a = (AccessorServerListEntryNormal) e;
        mc.getTextureManager().bindTexture(a.ldic$getIcon() != null ? a.ldic$getIconLocation() : UNKNOWN_SERVER);
        Gfx.texturedQuad(x + pad, y + pad, x + pad + icon, y + pad + icon, 0, 0, 1, 1, 0xFFFFFFFFL, c.opacity);
        long text = c.colour("text_colour", 0xFFF0F0F0L);
        long detail = c.colour("detail_colour", 0xFF9AA3AEL);
        long accent = c.colour("accent_colour", 0xFF4FC3F7L);
        double tx = x + pad + icon + 6;

        // Right side: the ping (or its state), then players or a version warning.
        double right = x + w - 6;
        double by = y + 5;
        boolean otherVersion = d.version != PROTOCOL && d.pinged && d.pingToServer >= 0;
        String state;
        long stateColour;
        if (!d.pinged || d.pingToServer == -2L) {
            state = I18n.format("multiplayer.status.pinging");
            stateColour = detail;
        } else if (d.pingToServer < 0) {
            state = I18n.format("ldic.servers.offline");
            stateColour = 0xFFEF5350L;
        } else {
            state = d.pingToServer + " ms";
            stateColour = d.pingToServer < 150 ? 0xFF66BB6AL : d.pingToServer < 300 ? 0xFFC0CA33L : d.pingToServer < 600 ? 0xFFFFCA28L : 0xFFEF5350L;
        }
        double left = c.badge(state, right, by, stateColour);
        if (hover && mx >= left && mx < right && my >= by && my < by + c.size) {
            tooltip = d.pingToServer >= 0 ? d.pingToServer + " ms" : state;
        }
        right = left;
        if (otherVersion) {
            right = c.badge(d.gameVersion.replaceAll("§.", ""), right, by, 0xFFEF5350L);
            if (hover && mx >= right && my >= by && my < by + c.size) {
                tooltip = "§c" + I18n.format(d.version > PROTOCOL ? "multiplayer.status.client_out_of_date" : "multiplayer.status.server_out_of_date");
            }
        } else if (d.populationInfo != null && !d.populationInfo.isEmpty()) {
            String players = d.populationInfo.replaceAll("§.", "");
            double pl = c.badge(players, right, by, accent);
            if (hover && mx >= pl && mx < right && my >= by && my < by + c.size && d.playerList != null && !d.playerList.isEmpty()) {
                tooltip = d.playerList;
            }
            right = pl;
        }
        // Forge's own marker (modded server, compatible or not, blocked) and its tooltip, below the badges.
        GlStateManager.color(1, 1, 1, 1);
        int fx = (int) Math.round(x);
        int fy = (int) Math.round(y + 12);
        int fw = (int) Math.round(w - 4);
        String forge = net.minecraftforge.fml.client.FMLClientHandler.instance().enhanceServerListEntry(e, d, fx, fw, fy,
                (int) Math.round(mx - fx), (int) Math.round(my - fy));
        if (hover && forge != null) {
            tooltip = forge;
        }
        Gfx.beginUi();

        c.text(c.fit(d.serverName, c.size, right - tx - 4), tx, y + 5, c.size, text);
        // The message of the day keeps its own colours and fonts, so it is drawn with the game's font.
        String motd = net.minecraftforge.fml.client.FMLClientHandler.instance().fixDescription(d.serverMOTD == null ? "" : d.serverMOTD);
        double ms = c.size * 0.7;
        float k = (float) (ms / 8);
        List<String> lines = fontRenderer.listFormattedStringToWidth(motd, (int) ((x + w - tx - 6) / k));
        GlStateManager.pushMatrix();
        GlStateManager.translate(tx, y + 7 + c.size, 0);
        GlStateManager.scale(k, k, 1);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            fontRenderer.drawString(lines.get(i), 0, i * 9, (int) detail);
        }
        GlStateManager.popMatrix();
        GlStateManager.color(1, 1, 1, 1);
        Gfx.beginUi();
    }

    private void drawLan(ListCards c, ServerListEntryLanDetected e, int index, double x, double y, double w, double h, boolean hover) {
        LanServerInfo info = e.getServerData();
        c.card(x, y, w, h, hover, index == selectedIndex());
        long accent = c.colour("accent_colour", 0xFF4FC3F7L);
        double tx = x + 10;
        double right = c.badge(I18n.format("lanServer.title"), x + w - 6, y + 5, accent);
        c.text(c.fit(info.getServerMotd(), c.size, right - tx - 4), tx, y + 5, c.size, c.colour("text_colour", 0xFFF0F0F0L));
        c.text(info.getServerIpPort(), tx, y + 7 + c.size, c.size * 0.8, c.colour("detail_colour", 0xFF9AA3AEL));
    }

    private void drawScanRow(ListCards c, double x, double y, double w, double h) {
        int dots = (int) (System.currentTimeMillis() / 400 % 4);
        String s = I18n.format("lanServer.scanning") + "...".substring(0, dots);
        double size = c.size * 0.8;
        c.text(s, x + (w - c.width(I18n.format("lanServer.scanning") + "...", size)) / 2, y + (h - size) / 2, size,
                c.colour("detail_colour", 0xFF9AA3AEL));
    }

    /** Vanilla's index of the row under a point, or -1 (the scan row counts as nothing). */
    private int rowAt(double mx, double my) {
        if (mx < lx || mx >= lx + lw || my < rowsTop || my >= ly + lh) {
            return -1;
        }
        double off = my - rowsTop + scroll;
        int r = (int) Math.floor(off / (rowH + rowGap));
        if (r < 0 || r >= shown.size() || off - r * (rowH + rowGap) > rowH) {
            return -1;
        }
        return shown.get(r);
    }

    // ------------------------------------------------------------------------------ input

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
            scroll -= Integer.signum(wheel) * (rowH + rowGap);
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
            }
            search.setFocused(true);
            return;
        }
        search.setFocused(false);
        int index = rowAt(mx, my);
        if (index >= 0 && mouseButton == 0) {
            selectServer(index);
            long now = System.currentTimeMillis();
            boolean onIcon = mx < lx + rowH && index < saved().size();
            if (onIcon || index == lastClickIndex && now - lastClickMs < 250) {
                join();
            }
            lastClickMs = now;
            lastClickIndex = index;
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
            } else {
                search.textboxKeyTyped(typedChar, keyCode);
            }
            return;
        }
        if (keyCode == Keyboard.KEY_F && isCtrlKeyDown()) {
            search.setFocused(true);
            return;
        }
        if (keyCode == Keyboard.KEY_F5) {
            refresh();
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            join();
            return;
        }
        if (keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_DOWN) {
            // Vanilla's handling: arrows move the selection, Shift+arrows move a saved server.
            super.keyTyped(typedChar, keyCode);
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
            super.confirmClicked(result, id);
        }
    }
}
