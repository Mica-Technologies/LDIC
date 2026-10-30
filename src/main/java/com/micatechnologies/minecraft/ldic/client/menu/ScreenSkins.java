package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.client.render.SkinHooks;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiButton;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiCreateWorld;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiDisconnected;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiGameOver;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiOptionsRowListRow;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiScreen;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiSlot;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiYesNo;
import com.micatechnologies.minecraft.ldic.theme.ElementDef;
import com.micatechnologies.minecraft.ldic.theme.ScreenDef;
import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import com.micatechnologies.minecraft.ldic.ui.UiNode;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiCustomizeSkin;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiOptionsRowList;
import net.minecraft.client.gui.GuiScreenOptionsSounds;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.gui.GuiScreenServerList;
import net.minecraft.client.gui.GuiShareToLan;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.ScreenChatOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenAddServer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiWorldEdit;
import net.minecraft.client.gui.GuiChat;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import net.minecraftforge.fml.client.config.GuiConfig;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiConfig;
import net.minecraft.client.gui.GuiSleepMP;
import net.minecraft.client.gui.advancements.GuiScreenAdvancements;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiScreenAdvancements;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiStats;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * Skins: vanilla screens drawn by a theme screen while they keep all of their own logic.
 *
 * <p>The vanilla screen is set up as usual (its buttons, text fields and state), and then only
 * its drawing is replaced: the theme screen draws instead, with {@code vanilla_button} and
 * {@code vanilla_field} elements that move the screen's own buttons and text fields into the
 * theme's layout and draw them in its style. Clicking and typing still go to the vanilla screen
 * and its widgets, so nothing about how the screen works changes. A vanilla button the theme
 * does not place is shown in the theme's foreign_buttons area (with other mods' buttons), so it
 * always stays usable. Vanilla's buttons are still "drawn" each frame with colour writes off,
 * which keeps their hover state and slider dragging working.</p>
 *
 * <p>Screens: {@link GuiCreateWorld} ({@code world_create}), {@link GuiWorldEdit}
 * ({@code world_edit}) and the others in {@link #SKINS}; the advancement screen draws its map and
 * tabs through {@link AdvancementMap}.</p>
 */
public final class ScreenSkins {

    /** Vanilla screen class (exactly; subclasses are other mods' screens) to theme screen id. */
    private static final Map<Class<?>, String> SKINS = new LinkedHashMap<>();
    /** Strings and flags particular to a screen, for {@code screen_text} / {@code screen_flag}. */
    private static final Map<Class<?>, Function<GuiScreen, Map<String, Object>>> EXTRAS = new HashMap<>();

    static {
        SKINS.put(GuiCreateWorld.class, "world_create");
        SKINS.put(GuiWorldEdit.class, "world_edit");
        SKINS.put(GuiScreenAddServer.class, "server_edit");
        SKINS.put(GuiScreenServerList.class, "server_direct");
        SKINS.put(GuiShareToLan.class, "lan_share");
        SKINS.put(GuiGameOver.class, "death");
        SKINS.put(GuiDisconnected.class, "disconnected");
        SKINS.put(GuiYesNo.class, "confirm");
        SKINS.put(GuiScreenOptionsSounds.class, "options_sounds");
        SKINS.put(ScreenChatOptions.class, "options_chat");
        SKINS.put(GuiCustomizeSkin.class, "options_skin");
        SKINS.put(GuiLanguage.class, "options_language");
        SKINS.put(GuiVideoSettings.class, "options_video");
        SKINS.put(GuiControls.class, "options_controls");
        SKINS.put(GuiScreenResourcePacks.class, "packs");
        SKINS.put(GuiScreenAdvancements.class, "advancement_tree");
        SKINS.put(GuiStats.class, "player_stats");
        SKINS.put(GuiChat.class, "chat_bar");
        SKINS.put(GuiSleepMP.class, "chat_bed");
        EXTRAS.put(GuiStats.class, s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("loading", ((AccessorGuiStats) s).ldic$isLoading());
            return m;
        });
        EXTRAS.put(GuiScreenAdvancements.class, s -> {
            AccessorGuiScreenAdvancements a = (AccessorGuiScreenAdvancements) s;
            Map<String, Object> m = new HashMap<>();
            m.put("empty", a.ldic$getSelectedTab() == null);
            m.put("tab", a.ldic$getSelectedTab() == null ? "" : a.ldic$getSelectedTab().getTitle());
            m.put("tabs", Integer.toString(a.ldic$getTabs().size()));
            return m;
        });
        EXTRAS.put(GuiGameOver.class, s -> {
            Map<String, Object> m = new HashMap<>();
            ITextComponent cause = ((AccessorGuiGameOver) s).ldic$getCause();
            m.put("cause", cause == null ? "" : cause.getUnformattedText());
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            m.put("score", player == null ? "0" : Integer.toString(player.getScore()));
            m.put("hardcore", Minecraft.getMinecraft().world != null && Minecraft.getMinecraft().world.getWorldInfo().isHardcoreModeEnabled());
            return m;
        });
        EXTRAS.put(GuiDisconnected.class, s -> {
            AccessorGuiDisconnected a = (AccessorGuiDisconnected) s;
            Map<String, Object> m = new HashMap<>();
            m.put("reason", a.ldic$getReason());
            m.put("message", a.ldic$getMessage() == null ? "" : a.ldic$getMessage().getUnformattedText());
            return m;
        });
        EXTRAS.put(GuiYesNo.class, s -> {
            AccessorGuiYesNo a = (AccessorGuiYesNo) s;
            Map<String, Object> m = new HashMap<>();
            m.put("line1", a.ldic$getLine1());
            m.put("line2", a.ldic$getLine2());
            return m;
        });
        EXTRAS.put(GuiCreateWorld.class, s -> {
            AccessorGuiCreateWorld a = (AccessorGuiCreateWorld) s;
            Map<String, Object> m = new HashMap<>();
            m.put("folder", a.ldic$getSaveDirName());
            m.put("mode_hint1", a.ldic$getGameModeDesc1());
            m.put("mode_hint2", a.ldic$getGameModeDesc2());
            m.put("more_options", a.ldic$isMoreOptions());
            WorldType type = WorldType.WORLD_TYPES[Math.max(0, a.ldic$getWorldTypeIndex())];
            m.put("world_type_info", type != null && type.hasInfoNotice() ? I18n.format(type.getInfoTranslationKey()) : "");
            return m;
        });
    }

    private final MenuManager manager;
    private Skin current;

    ScreenSkins(MenuManager manager) {
        this.manager = manager;
    }

    /** One skinned screen: its driver and what it offers the theme. */
    private final class Skin implements ThemeScreenDriver.Host, SkinHooks {
        final GuiScreen gui;
        final ThemeDef theme;
        final ThemeScreenDriver driver;
        final Set<Integer> placed = new HashSet<>();
        final List<GuiTextField> fields = new ArrayList<>();
        final List<GuiSlot> lists = new ArrayList<>();
        /** A vanilla_grid takes every vanilla button not placed or excluded. */
        boolean grid;
        final Set<Integer> gridExclude = new HashSet<>();
        /** Widths the screen gave its buttons, before the skin moved them (wide ones span a grid row). */
        final Map<GuiButton, Integer> initialWidths = new IdentityHashMap<>();
        final Map<GuiButton, Double> hover = new IdentityHashMap<>();
        final Map<GuiTextField, SkinHooks.Field> fieldStates = new IdentityHashMap<>();
        /** The advancement screen's map and tabs; null on other screens. */
        final AdvancementMap advancements;
        int mouseX;
        int mouseY;

        Skin(GuiScreen gui, ThemeDef theme, String screen) {
            this.gui = gui;
            this.advancements = gui instanceof GuiScreenAdvancements ? new AdvancementMap((GuiScreenAdvancements) gui) : null;
            this.theme = theme;
            this.driver = new ThemeScreenDriver(manager, theme, screen, this, false);
            collectPlaced(theme.screen(screen).elements);
            refresh();
        }

        /** After the screen (re)built its widgets. */
        void refresh() {
            fields.clear();
            fieldStates.clear();
            fields.addAll(declared(gui, GuiTextField.class));
            lists.clear();
            lists.addAll(declared(gui, GuiSlot.class));
            initialWidths.clear();
            for (GuiButton b : buttonList()) {
                initialWidths.put(b, b.width);
            }
        }

        private void collectPlaced(List<ElementDef> defs) {
            for (ElementDef d : defs) {
                if ("vanilla_button".equals(d.type)) {
                    int id = id(d.statik("button", "-1"));
                    if (id != Integer.MIN_VALUE) {
                        placed.add(id);
                    }
                } else if ("vanilla_grid".equals(d.type)) {
                    grid = true;
                    for (String s : d.statik("exclude", "").split(",")) {
                        int id = id(s);
                        if (id != Integer.MIN_VALUE) {
                            gridExclude.add(id);
                        }
                    }
                }
                collectPlaced(d.children);
            }
        }

        List<GuiButton> buttonList() {
            return ((AccessorGuiScreen) gui).ldic$getButtonList();
        }

        /** Whether the theme draws this button (placed by id, or in a grid). */
        boolean claimed(GuiButton b) {
            return isVanillaButton(b) && (placed.contains(b.id) || grid && !gridExclude.contains(b.id));
        }

        double ui() {
            return theme.uiScale(gui.width, gui.height);
        }

        GuiButton button(int id) {
            for (GuiButton b : buttonList()) {
                if (b.id == id && isVanillaButton(b)) {
                    return b;
                }
            }
            return null;
        }

        /** Moves a vanilla button to a box in layout units; its eased hover amount. */
        double place(GuiButton b, double x, double y, double w, double h) {
            double ui = ui();
            b.x = (int) Math.round(x * ui);
            b.y = (int) Math.round(y * ui);
            b.width = (int) Math.round(w * ui);
            b.height = (int) Math.round(h * ui);
            boolean over = b.enabled && mouseX >= b.x && mouseY >= b.y && mouseX < b.x + b.width && mouseY < b.y + b.height;
            double t = hover.getOrDefault(b, 0.0);
            t += ((over ? 1 : 0) - t) * 0.35;
            hover.put(b, t);
            return t;
        }

        // Host

        @Override
        public GuiScreen gui() {
            return gui;
        }

        /** Buttons the theme did not place: other mods', and any vanilla ones it left out. */
        @Override
        public List<GuiButton> buttons() {
            List<GuiButton> out = new ArrayList<>();
            for (GuiButton b : buttonList()) {
                if (!claimed(b)) {
                    out.add(b);
                }
            }
            return out;
        }

        @Override
        public void back() {
            // Escape, as the screen itself handles it.
            try {
                ObfuscationReflectionHelper.findMethod(GuiScreen.class, "func_73869_a", void.class, char.class, int.class)
                        .invoke(gui, (char) 0, Keyboard.KEY_ESCAPE);
            } catch (ReflectiveOperationException | RuntimeException e) {
                Ldic.LOGGER.warn("LDIC: could not go back from {}", gui.getClass().getName(), e);
            }
        }

        @Override
        public boolean mayHideForeignButtons() {
            return false;
        }

        // SkinHooks

        @Override
        public String vanillaButton(UiNode n) {
            GuiButton b = button(id(n.def.statik("button", "-1")));
            if (b == null || !b.visible) {
                return null;
            }
            n.hoverT = place(b, n.x, n.y, n.width, n.height);
            n.enabled = b.enabled;
            return b.displayString;
        }

        @Override
        public double vanillaSlider(UiNode n) {
            GuiButton b = button(id(n.def.statik("button", "-1")));
            return b == null ? -1 : sliderValue(b);
        }

        @Override
        public List<Cell> vanillaGrid(UiNode n) {
            ElementDef d = n.def;
            int columns = Math.max(1, (int) d.num("columns", 2));
            double rowH = d.num("row_height", 20);
            double gap = d.num("gap", 4);
            double cellW = (n.width - gap * (columns - 1)) / columns;
            List<Cell> cells = new ArrayList<>();
            int col = 0;
            double y = n.y;
            for (GuiButton b : buttonList()) {
                if (!b.visible || !isVanillaButton(b) || placed.contains(b.id) || gridExclude.contains(b.id)) {
                    continue;
                }
                boolean wide = columns > 1 && initialWidths.getOrDefault(b, b.width) > 200;
                if (wide && col > 0) {
                    col = 0;
                    y += rowH + gap;
                }
                double x = n.x + col * (cellW + gap);
                double w = wide ? n.width : cellW;
                double t = place(b, x, y, w, rowH);
                cells.add(new Cell(x, y, w, rowH, b.displayString, b.enabled, t, sliderValue(b)));
                col = wide ? columns : col + 1;
                if (col >= columns) {
                    col = 0;
                    y += rowH + gap;
                }
            }
            return cells;
        }

        @Override
        public void vanillaList(UiNode n, Consumer<Cell> buttons) {
            String which = n.def.statik("list", "0");
            GuiSlot list;
            if ("shown".equals(which)) {
                // A screen that swaps between lists (statistics): the one it shows now.
                list = gui instanceof GuiStats ? ((AccessorGuiStats) gui).ldic$getShownList() : null;
            } else {
                int i = id(which);
                list = i >= 0 && i < lists.size() ? lists.get(i) : null;
            }
            if (list == null) {
                return;
            }
            // The list is laid out and drawn in the theme's units (so its rows match the theme's
            // scale); the clicks the screen forwards to it are converted (MixinGuiListExtended).
            // With gui_pixels it stays at GUI scale: lists whose rows place their own widgets from
            // the screen's size and take clicks the mixin never sees (Forge config entries).
            double ui = ui();
            boolean guiPixels = Boolean.parseBoolean(n.def.statik("gui_pixels", "false"));
            double k = guiPixels ? ui : 1;
            double unit = guiPixels ? 1 : ui;
            int x = (int) Math.round(n.x * k);
            int y = (int) Math.round(n.y * k);
            int w = (int) Math.round(n.width * k);
            int h = (int) Math.round(n.height * k);
            // Vanilla centres most lists on width / 2 from x 0 (clicks assume it); lists that
            // vanilla itself placed from a left edge (resource packs) keep that convention.
            if (LEFT_ALIGNED.computeIfAbsent(list, l -> l.left != 0)) {
                list.left = x;
                list.width = w;
            } else {
                list.left = 0;
                list.width = 2 * x + w;
            }
            list.right = x + w;
            list.top = y;
            list.bottom = y + h;
            SKINNED.put(list, unit);
            if (list instanceof GuiOptionsRowList) {
                // Option rows place their buttons once, from the screen's centre; follow the list.
                GuiOptionsRowList rows = (GuiOptionsRowList) list;
                int centre = list.left + list.width / 2;
                for (int r = 0; r < ((AccessorGuiSlot) list).ldic$getSize(); r++) {
                    AccessorGuiOptionsRowListRow row = (AccessorGuiOptionsRowListRow) rows.getListEntry(r);
                    GuiButton a = row.ldic$getButtonA();
                    GuiButton b = row.ldic$getButtonB();
                    if (a != null) {
                        a.x = centre - 155;
                    }
                    if (b != null) {
                        b.x = centre + 5;
                    }
                }
            }
            Minecraft mc = Minecraft.getMinecraft();
            double s = new ScaledResolution(mc).getScaleFactor() * unit;
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor((int) Math.floor(x * s), (int) Math.floor(mc.displayHeight - (y + h) * s),
                    (int) Math.ceil(w * s), (int) Math.ceil(h * s));
            drawingList = true;
            listButtons = buttons;
            if (guiPixels) {
                GlStateManager.pushMatrix();
                GlStateManager.scale(1 / ui, 1 / ui, 1);
            }
            try {
                list.drawScreen((int) (mouseX / unit), (int) (mouseY / unit), mc.getRenderPartialTicks());
            } finally {
                if (guiPixels) {
                    GlStateManager.popMatrix();
                }
                drawingList = false;
                listButtons = null;
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        }

        @Override
        public int vanillaChecked(UiNode n) {
            GuiButton b = button(id(n.def.statik("button", "-1")));
            return b instanceof GuiCheckBox ? ((GuiCheckBox) b).isChecked() ? 1 : 0 : -1;
        }

        @Override
        public SkinHooks.Field vanillaField(UiNode n) {
            int i = id(n.def.statik("field", "0"));
            if (i < 0 || i >= fields.size() || !fields.get(i).getVisible()) {
                return null;
            }
            GuiTextField f = fields.get(i);
            SkinHooks.Field s = fieldStates.computeIfAbsent(f, k -> new SkinHooks.Field());
            // Vanilla's box covers the theme's, so its own click handling takes the focus.
            double ui = ui();
            f.setEnableBackgroundDrawing(false);
            f.x = (int) Math.round(n.x * ui);
            f.y = (int) Math.round(n.y * ui);
            f.width = (int) Math.round(n.width * ui);
            f.height = (int) Math.round(n.height * ui);
            s.text = f.getText();
            s.cursor = f.getCursorPosition();
            s.selection = f.getSelectionEnd();
            s.focused = f.isFocused();
            return s;
        }

        @Override
        public void advancementMap(UiNode n) {
            if (advancements != null) {
                advancements.drawMap(n, ui(), mouseX, mouseY);
            }
        }

        @Override
        public void advancementTabs(UiNode n, Consumer<Cell> paint) {
            if (advancements != null) {
                advancements.drawTabs(n, ui(), mouseX, mouseY, paint);
            }
        }

        /** After vanilla handled a click: the cursor goes where the theme drew the text. */
        void fieldClicked(int mouseX, int mouseY) {
            double ui = ui();
            double x = mouseX / ui;
            double y = mouseY / ui;
            for (Map.Entry<GuiTextField, SkinHooks.Field> e : fieldStates.entrySet()) {
                SkinHooks.Field s = e.getValue();
                GuiTextField f = e.getKey();
                if (s.hit != null && f.isFocused() && x >= s.x && x < s.x + s.width && y >= s.y && y < s.y + s.height) {
                    f.setCursorPosition(s.hit.applyAsInt(x));
                }
            }
        }

        void draw(int mouseX, int mouseY, float partialTicks) {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            Minecraft mc = Minecraft.getMinecraft();
            if (gui instanceof GuiConfig) {
                configBeforeDraw((GuiConfig) gui);
            }
            // Vanilla's own per-frame button code (hover, slider drags), drawing nothing.
            GlStateManager.colorMask(false, false, false, false);
            try {
                for (GuiButton b : buttonList()) {
                    if (claimed(b)) {
                        b.drawButton(mc, mouseX, mouseY, partialTicks);
                    }
                }
            } finally {
                GlStateManager.colorMask(true, true, true, true);
            }
            Map<String, String> text = new HashMap<>();
            Map<String, Boolean> flags = new HashMap<>();
            Function<GuiScreen, Map<String, Object>> extras = gui instanceof GuiConfig ? ScreenSkins::configExtras : EXTRAS.get(gui.getClass());
            if (extras != null) {
                for (Map.Entry<String, Object> e : extras.apply(gui).entrySet()) {
                    if (e.getValue() instanceof Boolean) {
                        flags.put(e.getKey(), (Boolean) e.getValue());
                    } else if (e.getValue() != null) {
                        text.put(e.getKey(), e.getValue().toString());
                    }
                }
            }
            if (advancements != null) {
                advancements.hidePageButtons(buttonList());
            }
            manager.bindings().setSkin(buttonList(), fields, text, flags);
            driver.draw(mouseX, mouseY, partialTicks);
            if (advancements != null) {
                advancements.drawTooltip();
            }
            if (gui instanceof GuiConfig) {
                configTooltips((GuiConfig) gui, mouseX, mouseY, partialTicks);
            }
            if (gui instanceof GuiChat) {
                // As vanilla's chat screen: the hover box of the chat text under the mouse.
                ITextComponent hovered = mc.ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY());
                if (hovered != null && hovered.getStyle().getHoverEvent() != null) {
                    ((AccessorGuiScreen) gui).ldic$handleComponentHover(hovered, mouseX, mouseY);
                }
            }
        }
    }

    /** Buttons of Minecraft's and Forge's config screens' own classes (the screens' buttons), not other mods'. */
    private static boolean isVanillaButton(GuiButton b) {
        String c = b.getClass().getName();
        return c.startsWith("net.minecraft.") || c.startsWith("net.minecraftforge.fml.client.config.");
    }

    /**
     * The theme screen for a vanilla screen: its exact class in {@link #SKINS}, or
     * {@code forge_config} for a Forge config screen (mods subclass GuiConfig to fill it in) whose
     * classes leave its drawing alone; one that draws itself is left as it is.
     */
    private static String screenFor(GuiScreen gui) {
        String screen = SKINS.get(gui.getClass());
        if (screen != null || !(gui instanceof GuiConfig)) {
            return screen;
        }
        for (Class<?> c = gui.getClass(); c != GuiConfig.class; c = c.getSuperclass()) {
            for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                String name = m.getName();
                if (name.equals("drawScreen") || name.equals("func_73863_a")) {
                    return null;
                }
            }
        }
        return "forge_config";
    }

    /** A Forge config screen's titles, for screen_text. */
    private static Map<String, Object> configExtras(GuiScreen s) {
        GuiConfig c = (GuiConfig) s;
        Map<String, Object> m = new HashMap<>();
        m.put("title", c.title == null ? "" : c.title);
        m.put("title2", c.titleLine2 == null ? "" : c.titleLine2);
        return m;
    }

    /** What GuiConfig's own drawScreen does before drawing: Undo and Reset follow the entries. */
    private static void configBeforeDraw(GuiConfig c) {
        AccessorGuiConfig a = (AccessorGuiConfig) c;
        if (c.entryList == null || a.ldic$getApplyGlobally() == null) {
            return;
        }
        boolean global = a.ldic$getApplyGlobally().isChecked();
        boolean any = c.entryList.areAnyEntriesEnabled(global);
        a.ldic$getUndoAll().enabled = any && c.entryList.hasChangedEntry(global);
        a.ldic$getDefaultAll().enabled = any && !c.entryList.areAllEntriesDefault(global);
    }

    /** What GuiConfig's own drawScreen does after drawing: the entries' and buttons' tooltips. */
    private static void configTooltips(GuiConfig c, int mouseX, int mouseY, float partialTicks) {
        AccessorGuiConfig a = (AccessorGuiConfig) c;
        if (c.entryList == null) {
            return;
        }
        c.entryList.drawScreenPost(mouseX, mouseY, partialTicks);
        if (a.ldic$getUndoHover() != null && a.ldic$getUndoHover().checkHover(mouseX, mouseY)) {
            c.drawToolTip(java.util.Arrays.asList(I18n.format("fml.configgui.tooltip.undoAll").split("\n")), mouseX, mouseY);
        }
        if (a.ldic$getResetHover() != null && a.ldic$getResetHover().checkHover(mouseX, mouseY)) {
            c.drawToolTip(java.util.Arrays.asList(I18n.format("fml.configgui.tooltip.resetAll").split("\n")), mouseX, mouseY);
        }
        if (a.ldic$getCheckBoxHover() != null && a.ldic$getCheckBoxHover().checkHover(mouseX, mouseY)) {
            c.drawToolTip(java.util.Arrays.asList(I18n.format("fml.configgui.tooltip.applyGlobally").split("\n")), mouseX, mouseY);
        }
    }

    /** A button or field number from a theme; MIN_VALUE when it is not one. */
    private static int id(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    /** Lists drawn by a skin (their scroll bar sits at their right edge; see MixinGuiSlot). */
    private static final Map<GuiSlot, Double> SKINNED = new WeakHashMap<>();
    private static final Map<GuiSlot, Boolean> LEFT_ALIGNED = new WeakHashMap<>();
    private static boolean drawingList;
    private static Consumer<SkinHooks.Cell> listButtons;
    private static final Map<GuiButton, Double> LIST_HOVER = new WeakHashMap<>();
    private static final Map<Class<?>, Field> SLIDER_FIELDS = new HashMap<>();

    /** Whether a skin is drawing a list right now (it skips its dirt background). */
    public static boolean drawingList() {
        return drawingList;
    }

    /** A mouse coordinate in GUI pixels converted to a skinned list's units (unchanged for others). */
    public static int toList(GuiSlot list, int v) {
        Double ui = SKINNED.get(list);
        return ui == null ? v : (int) (v / ui);
    }

    /**
     * Draws a vanilla button inside a skinned list in the theme's style; false to let vanilla draw
     * it (no skin list drawing, a list without style_buttons, or another mod's button). Vanilla's
     * per-frame slider update still runs, with colour writes off.
     */
    public static boolean drawListButton(GuiButton b, Minecraft mc, int mouseX, int mouseY) {
        if (!drawingList || listButtons == null || !b.visible || !isVanillaButton(b)) {
            return false;
        }
        GlStateManager.colorMask(false, false, false, false);
        try {
            ((AccessorGuiButton) b).ldic$mouseDragged(mc, mouseX, mouseY);
        } finally {
            GlStateManager.colorMask(true, true, true, true);
        }
        boolean over = b.enabled && mouseX >= b.x && mouseY >= b.y && mouseX < b.x + b.width && mouseY < b.y + b.height;
        double t = LIST_HOVER.getOrDefault(b, 0.0);
        t += ((over ? 1 : 0) - t) * 0.35;
        LIST_HOVER.put(b, t);
        listButtons.accept(new SkinHooks.Cell(b.x, b.y, b.width, b.height, b.displayString, b.enabled, t, sliderValue(b)));
        return true;
    }

    /** Whether a skin placed this list. */
    public static boolean skinned(GuiSlot list) {
        return SKINNED.containsKey(list);
    }

    /**
     * A vanilla slider's position (0 to 1), or -1 for other buttons: the first float the button's
     * own classes declare (GuiOptionSlider's sliderValue, the sound sliders' volume, GuiSlider's
     * position).
     */
    private static double sliderValue(GuiButton b) {
        Field f = SLIDER_FIELDS.computeIfAbsent(b.getClass(), c -> {
            for (Class<?> k = c; k != GuiButton.class && k != null; k = k.getSuperclass()) {
                for (Field x : k.getDeclaredFields()) {
                    if (x.getType() == float.class && !Modifier.isStatic(x.getModifiers())) {
                        x.setAccessible(true);
                        return x;
                    }
                }
            }
            return null;
        });
        try {
            return f == null ? -1 : f.getFloat(b);
        } catch (IllegalAccessException e) {
            return -1;
        }
    }

    /** The screen's fields of a type, in the order its classes declare them. */
    private static <T> List<T> declared(GuiScreen gui, Class<T> type) {
        List<T> out = new ArrayList<>();
        for (Class<?> c = gui.getClass(); c != null && c != GuiScreen.class; c = c.getSuperclass()) {
            List<T> own = new ArrayList<>();
            for (Field f : c.getDeclaredFields()) {
                if (type.isAssignableFrom(f.getType()) && !Modifier.isStatic(f.getModifiers())) {
                    try {
                        f.setAccessible(true);
                        Object v = f.get(gui);
                        if (v != null) {
                            own.add(type.cast(v));
                        }
                    } catch (IllegalAccessException ignored) {
                        // Leave it out; the theme can still show the others.
                    }
                }
            }
            out.addAll(0, own);
        }
        return out;
    }

    private Skin skinFor(GuiScreen gui) {
        return current != null && current.gui == gui ? current : null;
    }

    // ---------------------------------------------------------------------------- events

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.getGui();
        String screen = screenFor(gui);
        ThemeDef theme = manager.theme();
        if (screen == null || theme == null || !manager.settings().menuEnabled || !manager.settings().skinsEnabled) {
            return;
        }
        ScreenDef def = theme.screen(screen);
        if (def == null) {
            return;
        }
        if (current == null || current.gui != gui || current.theme != theme) {
            current = new Skin(gui, theme, screen);
        } else {
            current.refresh();
        }
        current.driver.init();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        Skin skin = skinFor(event.getGui());
        if (skin == null) {
            return;
        }
        event.setCanceled(true);
        try {
            skin.draw(event.getMouseX(), event.getMouseY(), event.getRenderPartialTicks());
        } catch (RuntimeException e) {
            Ldic.LOGGER.error("LDIC: skin for {} failed; showing the vanilla screen", skin.gui.getClass().getName(), e);
            current = null;
            AdvancementMap.reset();
        }
    }

    /** Clicks on the theme's own interactive elements (vanilla's buttons get theirs as usual). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        Skin skin = skinFor(event.getGui());
        if (skin == null) {
            return;
        }
        int button = Mouse.getEventButton();
        GuiScreen gui = event.getGui();
        Minecraft mc = Minecraft.getMinecraft();
        int x = Mouse.getEventX() * gui.width / mc.displayWidth;
        int y = gui.height - Mouse.getEventY() * gui.height / mc.displayHeight - 1;
        if (skin.advancements != null) {
            int wheel = Mouse.getEventDWheel();
            if (wheel != 0 && skin.advancements.wheel(x, y, wheel, skin.ui())) {
                event.setCanceled(true);
                return;
            }
        }
        if (button < 0) {
            return;
        }
        boolean taken = Mouse.getEventButtonState() ? skin.driver.mouseClicked(x, y, button) : skin.driver.mouseReleased(x, y, button);
        if (taken) {
            event.setCanceled(true);
        }
    }

    /** A left click in a text field: vanilla focused it; place its cursor by the theme's text. */
    @SubscribeEvent
    public void onMousePost(GuiScreenEvent.MouseInputEvent.Post event) {
        Skin skin = skinFor(event.getGui());
        if (skin == null || Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) {
            return;
        }
        GuiScreen gui = event.getGui();
        Minecraft mc = Minecraft.getMinecraft();
        skin.fieldClicked(Mouse.getEventX() * gui.width / mc.displayWidth,
                gui.height - Mouse.getEventY() * gui.height / mc.displayHeight - 1);
    }

    @SubscribeEvent
    public void onOpen(net.minecraftforge.client.event.GuiOpenEvent event) {
        if (current != null && event.getGui() != current.gui) {
            current.driver.close();
            AdvancementMap.reset();
            current = null;
        }
    }
}
