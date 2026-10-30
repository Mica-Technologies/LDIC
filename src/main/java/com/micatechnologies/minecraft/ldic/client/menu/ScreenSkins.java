package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.client.render.Gfx;
import com.micatechnologies.minecraft.ldic.client.render.SkinHooks;
import com.micatechnologies.minecraft.ldic.expr.Expr;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiCreateWorld;
import com.micatechnologies.minecraft.ldic.theme.ElementDef;
import com.micatechnologies.minecraft.ldic.theme.ScreenDef;
import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import com.micatechnologies.minecraft.ldic.ui.UiNode;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiWorldEdit;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

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
        final List<GuiTextField> fields;
        int mouseX;
        int mouseY;

        Skin(GuiScreen gui, ThemeDef theme, String screen) {
            this.gui = gui;
            this.theme = theme;
            this.driver = new ThemeScreenDriver(manager, theme, screen, this, false);
            this.fields = textFields(gui);
            collectPlaced(theme.screen(screen).elements);
        }

        private void collectPlaced(List<ElementDef> defs) {
            for (ElementDef d : defs) {
                if ("vanilla_button".equals(d.type)) {
                    try {
                        placed.add(Integer.parseInt(d.statik("button", "-1").trim()));
                    } catch (NumberFormatException ignored) {
                        // The compiler already reported it.
                    }
                }
                collectPlaced(d.children);
            }
        }

        double ui() {
            return theme.uiScale(gui.width, gui.height);
        }

        GuiButton button(int id) {
            for (GuiButton b : ((com.micatechnologies.minecraft.ldic.mixin.AccessorGuiScreen) gui).ldic$getButtonList()) {
                if (b.id == id) {
                    return b;
                }
            }
            return null;
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
            for (GuiButton b : ((com.micatechnologies.minecraft.ldic.mixin.AccessorGuiScreen) gui).ldic$getButtonList()) {
                if (!placed.contains(b.id) || !isVanillaButton(b)) {
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
            int id;
            try {
                id = Integer.parseInt(n.def.statik("button", "-1").trim());
            } catch (NumberFormatException e) {
                return null;
            }
            GuiButton b = button(id);
            if (b == null || !b.visible) {
                return null;
            }
            double ui = ui();
            b.x = (int) Math.round(n.x * ui);
            b.y = (int) Math.round(n.y * ui);
            b.width = (int) Math.round(n.width * ui);
            b.height = (int) Math.round(n.height * ui);
            boolean hover = mouseX >= b.x && mouseY >= b.y && mouseX < b.x + b.width && mouseY < b.y + b.height;
            n.enabled = b.enabled;
            n.hoverT += ((hover && b.enabled ? 1 : 0) - n.hoverT) * 0.35;
            return b.displayString;
        }

        @Override
        public void vanillaField(UiNode n) {
            int i;
            try {
                i = Integer.parseInt(n.def.statik("field", "0").trim());
            } catch (NumberFormatException e) {
                return;
            }
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
                for (GuiButton b : ((com.micatechnologies.minecraft.ldic.mixin.AccessorGuiScreen) gui).ldic$getButtonList()) {
                    if (placed.contains(b.id) && isVanillaButton(b)) {
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
            manager.bindings().setSkin(((com.micatechnologies.minecraft.ldic.mixin.AccessorGuiScreen) gui).ldic$getButtonList(), fields, text, flags);
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

    /** The screen's text fields, in the order its class declares them. */
    private static List<GuiTextField> textFields(GuiScreen gui) {
        List<GuiTextField> out = new ArrayList<>();
        for (Class<?> c = gui.getClass(); c != null && c != GuiScreen.class; c = c.getSuperclass()) {
            List<GuiTextField> own = new ArrayList<>();
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() == GuiTextField.class && !Modifier.isStatic(f.getModifiers())) {
                    try {
                        f.setAccessible(true);
                        Object v = f.get(gui);
                        if (v != null) {
                            own.add((GuiTextField) v);
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
            current.fields.clear();
            current.fields.addAll(textFields(gui));
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
