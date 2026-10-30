package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.client.render.Gfx;
import com.micatechnologies.minecraft.ldic.client.render.SkinHooks;
import com.micatechnologies.minecraft.ldic.expr.Expr;
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
 * <p>Screens: {@link GuiCreateWorld} ({@code world_create}) and {@link GuiWorldEdit}
 * ({@code world_edit}); more follow the same pattern (see {@link #SKINS}).</p>
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
        int mouseX;
        int mouseY;

        Skin(GuiScreen gui, ThemeDef theme, String screen) {
            this.gui = gui;
            this.theme = theme;
            this.driver = new ThemeScreenDriver(manager, theme, screen, this, false);
            collectPlaced(theme.screen(screen).elements);
            refresh();
        }

        /** After the screen (re)built its widgets. */
        void refresh() {
            fields.clear();
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
            int i = id(n.def.statik("list", "0"));
            if (i < 0 || i >= lists.size()) {
                return;
            }
            GuiSlot list = lists.get(i);
            // The list is laid out and drawn in the theme's units (so its rows match the theme's
            // scale); the clicks the screen forwards to it are converted (MixinGuiListExtended).
            double ui = ui();
            int x = (int) Math.round(n.x);
            int y = (int) Math.round(n.y);
            int w = (int) Math.round(n.width);
            int h = (int) Math.round(n.height);
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
            SKINNED.put(list, ui);
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
            double s = new ScaledResolution(mc).getScaleFactor() * ui;
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor((int) Math.floor(x * s), (int) Math.floor(mc.displayHeight - (y + h) * s),
                    (int) Math.ceil(w * s), (int) Math.ceil(h * s));
            drawingList = true;
            listButtons = buttons;
            try {
                list.drawScreen((int) (mouseX / ui), (int) (mouseY / ui), mc.getRenderPartialTicks());
            } finally {
                drawingList = false;
                listButtons = null;
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        }

        @Override
        public void vanillaField(UiNode n) {
            int i = id(n.def.statik("field", "0"));
            if (i < 0 || i >= fields.size() || !fields.get(i).getVisible()) {
                return;
            }
            GuiTextField f = fields.get(i);
            ElementDef d = n.def;
            double radius = d.num("radius", 2);
            long fill = colour(d, "colour", 0x80000000L);
            long edge = f.isFocused() ? colour(d, "focus_colour", 0xFF4FC3F7L) : colour(d, "border_colour", 0x30FFFFFFL);
            Gfx.roundedRect(n.x, n.y, n.x + n.width, n.y + n.height, radius, fill, fill, n.opacity);
            Gfx.roundedBorder(n.x, n.y, n.x + n.width, n.y + n.height, radius, f.isFocused() ? 1 : 0.5, edge, n.opacity);
            double ui = ui();
            double pad = d.num("padding", 5);
            f.setEnableBackgroundDrawing(false);
            f.setTextColor((int) colour(d, "text_colour", 0xFFE0E0E0L));
            f.x = (int) Math.round((n.x + pad) * ui);
            f.width = (int) Math.round((n.width - 2 * pad) * ui);
            f.height = (int) Math.round(n.height * ui);
            f.y = (int) Math.round(n.y * ui + (n.height * ui - 8) / 2);
            GlStateManager.pushMatrix();
            GlStateManager.scale(1 / ui, 1 / ui, 1);
            f.drawTextBox();
            GlStateManager.popMatrix();
        }

        void draw(int mouseX, int mouseY, float partialTicks) {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            Minecraft mc = Minecraft.getMinecraft();
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
            Function<GuiScreen, Map<String, Object>> extras = EXTRAS.get(gui.getClass());
            if (extras != null) {
                for (Map.Entry<String, Object> e : extras.apply(gui).entrySet()) {
                    if (e.getValue() instanceof Boolean) {
                        flags.put(e.getKey(), (Boolean) e.getValue());
                    } else if (e.getValue() != null) {
                        text.put(e.getKey(), e.getValue().toString());
                    }
                }
            }
            manager.bindings().setSkin(buttonList(), fields, text, flags);
            driver.draw(mouseX, mouseY, partialTicks);
        }
    }

    /** Buttons of Minecraft's own classes (vanilla screens' buttons), not other mods'. */
    private static boolean isVanillaButton(GuiButton b) {
        return b.getClass().getName().startsWith("net.minecraft.");
    }

    private static long colour(ElementDef d, String prop, long fallback) {
        Expr e = d.prop(prop);
        return e == null ? fallback : (long) e.num();
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
        String screen = SKINS.get(gui.getClass());
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
        }
    }

    /** Clicks on the theme's own interactive elements (vanilla's buttons get theirs as usual). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        Skin skin = skinFor(event.getGui());
        int button = Mouse.getEventButton();
        if (skin == null || button < 0) {
            return;
        }
        GuiScreen gui = event.getGui();
        Minecraft mc = Minecraft.getMinecraft();
        int x = Mouse.getEventX() * gui.width / mc.displayWidth;
        int y = gui.height - Mouse.getEventY() * gui.height / mc.displayHeight - 1;
        boolean taken = Mouse.getEventButtonState() ? skin.driver.mouseClicked(x, y, button) : skin.driver.mouseReleased(x, y, button);
        if (taken) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onOpen(net.minecraftforge.client.event.GuiOpenEvent event) {
        if (current != null && event.getGui() != current.gui) {
            current.driver.close();
            current = null;
        }
    }
}
