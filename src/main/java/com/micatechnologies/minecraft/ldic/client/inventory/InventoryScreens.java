package com.micatechnologies.minecraft.ldic.client.inventory;

import com.micatechnologies.minecraft.ldic.client.LdicClient;
import com.micatechnologies.minecraft.ldic.client.LdicSettings;
import com.micatechnologies.minecraft.ldic.inventory.CreativeLayout;
import com.micatechnologies.minecraft.ldic.mixin.AccessorGuiContainer;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiRepair;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Full-screen inventories (Decision 14) and the button and key that toggle them.
 *
 * <ul>
 *   <li>Creative opens {@link LdicCreativeScreen}, whose grid fills the screen.</li>
 *   <li>Vanilla's other inventory screens (and other mods', if the pack allows it) scale up to
 *   the largest whole pixel size that fits. The screen is initialised with a smaller width and
 *   height, so its own layout and mouse handling work in the larger units unchanged;
 *   {@code MixinForgeHooksClient} scales its drawing to match.</li>
 * </ul>
 *
 * The toggle button sits just right of the panel's top-right corner. It is drawn after the
 * screen and its clicks are taken before the screen sees them, so it never enters a screen's
 * button list and a click on it never counts as a click outside the panel (which would throw
 * the held stack).
 */
public final class InventoryScreens {

    /** Room left around a scaled panel, in its own pixels. */
    private static final int MARGIN = 24;
    /** Extra width for screens with a recipe book beside them. */
    private static final int RECIPE_BOOK = 160;
    private static final int BUTTON = 12;

    private static final Map<GuiScreen, Float> FACTORS = new WeakHashMap<>();
    private static InventoryScreens instance;

    private final LdicSettings settings;
    private final KeyBinding toggleKey = new KeyBinding("key.ldic.inventory_full", KeyConflictContext.GUI,
            Keyboard.KEY_NONE, "key.categories.ldic");

    public InventoryScreens(LdicSettings settings) {
        this.settings = settings;
        instance = this;
        ClientRegistry.registerKeyBinding(toggleKey);
    }

    public static InventoryScreens instance() {
        return instance;
    }

    // ------------------------------------------------------------------------- scaling

    /** The GL scale a screen is drawn at; 1 for screens LDIC leaves alone. */
    public static float factor(GuiScreen screen) {
        if (screen == null) {
            return 1;
        }
        Float k = FACTORS.get(screen);
        return k == null ? 1 : k;
    }

    public static int mouseX(GuiScreen screen, int mouseX) {
        if (factor(screen) == 1) {
            return mouseX;
        }
        return Mouse.getX() * screen.width / Minecraft.getMinecraft().displayWidth;
    }

    public static int mouseY(GuiScreen screen, int mouseY) {
        if (factor(screen) == 1) {
            return mouseY;
        }
        return screen.height - Mouse.getY() * screen.height / Minecraft.getMinecraft().displayHeight - 1;
    }

    private boolean scalable(GuiScreen gui) {
        if (!(gui instanceof GuiContainer) || gui instanceof GuiContainerCreative || gui instanceof LdicCreativeScreen) {
            return false;
        }
        return gui.getClass().getName().startsWith("net.minecraft.client.gui.") || settings.inventoryScaleModScreens;
    }

    /** Pixels per GUI unit a scaled screen would use, or 0 if it would be no bigger than now. */
    private static int fullScale(GuiContainer gui) {
        Minecraft mc = Minecraft.getMinecraft();
        AccessorGuiContainer a = (AccessorGuiContainer) gui;
        int extra = gui instanceof GuiInventory || gui instanceof GuiCrafting ? RECIPE_BOOK : 0;
        int f = Math.min(mc.displayWidth / (a.ldic$getXSize() + extra + MARGIN), mc.displayHeight / (a.ldic$getYSize() + MARGIN));
        return f > new ScaledResolution(mc).getScaleFactor() ? f : 0;
    }

    private static boolean creativeFits() {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        return CreativeLayout.compute(sr.getScaledWidth(), sr.getScaledHeight()) != null;
    }

    /** Whether the toggle has anything to do on this screen. */
    private boolean toggleApplies(GuiScreen gui) {
        if (gui instanceof LdicCreativeScreen) {
            return true;
        }
        if (gui instanceof GuiContainerCreative) {
            return creativeFits();
        }
        return scalable(gui) && (factor(gui) != 1 || fullScale((GuiContainer) gui) > 0);
    }

    @SubscribeEvent
    public void onInitGuiPre(GuiScreenEvent.InitGuiEvent.Pre event) {
        GuiScreen gui = event.getGui();
        FACTORS.remove(gui);
        if (!settings.inventoryFullScreen || !scalable(gui)) {
            return;
        }
        int f = fullScale((GuiContainer) gui);
        if (f == 0) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        gui.width = mc.displayWidth / f;
        gui.height = mc.displayHeight / f;
        FACTORS.put(gui, (float) f / new ScaledResolution(mc).getScaleFactor());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onGuiOpen(GuiOpenEvent event) {
        GuiScreen gui = event.getGui();
        Minecraft mc = Minecraft.getMinecraft();
        if (gui != null && gui.getClass() == GuiContainerCreative.class && settings.inventoryFullScreen
                && mc.player != null && mc.playerController != null && mc.playerController.isInCreativeMode()
                && creativeFits()) {
            event.setGui(new LdicCreativeScreen(mc.player));
        }
    }

    // ------------------------------------------------------------------------- toggle

    /** Flips full-screen mode and reopens the current inventory to match. */
    public void toggle(GuiScreen gui) {
        Minecraft mc = Minecraft.getMinecraft();
        settings.inventoryFullScreen = !settings.inventoryFullScreen;
        settings.saveHotbarAndInventory(LdicClient.configDir());
        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        if (gui instanceof LdicCreativeScreen || gui instanceof GuiContainerCreative) {
            // GuiOpenEvent picks the right creative screen for the new setting.
            mc.displayGuiScreen(new GuiContainerCreative(mc.player));
        } else {
            ScaledResolution sr = new ScaledResolution(mc);
            gui.setWorldAndResolution(mc, sr.getScaledWidth(), sr.getScaledHeight());
        }
    }

    /** Top-left of the toggle button in the screen's own units, or null when it has none. */
    private int[] buttonAt(GuiScreen gui) {
        if (gui instanceof LdicCreativeScreen || !toggleApplies(gui)) {
            return null; // the full creative screen draws its own toggle in its top bar
        }
        AccessorGuiContainer a = (AccessorGuiContainer) gui;
        int x = a.ldic$getGuiLeft() + a.ldic$getXSize() + 2;
        int y = a.ldic$getGuiTop() + (gui instanceof GuiContainerCreative ? 4 : 2);
        return x + BUTTON <= gui.width ? new int[] {x, y} : null;
    }

    /** Called from {@code MixinGuiContainer} before a screen handles a click; true when it was the toggle's. */
    public boolean clickToggle(GuiScreen gui, int mouseX, int mouseY, int button) {
        if (button != 0) {
            return false;
        }
        int[] at = buttonAt(gui);
        if (at != null && mouseX >= at[0] && mouseY >= at[1] && mouseX < at[0] + BUTTON && mouseY < at[1] + BUTTON) {
            toggle(gui);
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public void onKey(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        int key = toggleKey.getKeyCode();
        if (key == Keyboard.KEY_NONE || !Keyboard.getEventKeyState() || Keyboard.getEventKey() != key) {
            return;
        }
        GuiScreen gui = event.getGui();
        if (gui instanceof GuiRepair || !toggleApplies(gui) || typing(gui)) {
            return;
        }
        event.setCanceled(true);
        toggle(gui);
    }

    /** A focused text box gets the key instead (creative search). */
    private static boolean typing(GuiScreen gui) {
        if (gui instanceof LdicCreativeScreen) {
            return ((LdicCreativeScreen) gui).isTyping();
        }
        SurvivalSearch search = SurvivalSearch.instance();
        return gui instanceof GuiContainerCreative && CreativeSearch.isTyping(gui) || search != null && search.isTyping(gui);
    }

    @SubscribeEvent
    public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        GuiScreen gui = event.getGui();
        if (!(gui instanceof GuiContainer)) {
            return;
        }
        int[] at = buttonAt(gui);
        if (at == null) {
            return;
        }
        boolean hovered = event.getMouseX() >= at[0] && event.getMouseY() >= at[1]
                && event.getMouseX() < at[0] + BUTTON && event.getMouseY() < at[1] + BUTTON;
        drawToggle(at[0], at[1], BUTTON, hovered, settings.inventoryFullScreen && !(gui instanceof GuiContainerCreative));
        if (hovered) {
            gui.drawHoveringText(Arrays.asList(
                    I18n.format(settings.inventoryFullScreen && !(gui instanceof GuiContainerCreative)
                            ? "ldic.inventory.full.exit" : "ldic.inventory.full.enter"),
                    "§7" + keyHint()), event.getMouseX(), event.getMouseY());
        }
    }

    public String keyHint() {
        return toggleKey.getKeyCode() == Keyboard.KEY_NONE ? I18n.format("ldic.inventory.full.nokey")
                : I18n.format("ldic.inventory.full.key", toggleKey.getDisplayName());
    }

    /** The toggle: four corner marks pointing out (go full screen) or in (go back). */
    public static void drawToggle(int x, int y, int size, boolean hovered, boolean full) {
        int accent = InventoryStyle.accent();
        InventoryStyle.panel(x, y, x + size, y + size, hovered ? 0xE0202830 : InventoryStyle.PANEL,
                hovered ? accent : InventoryStyle.PANEL_EDGE);
        int c = hovered ? accent : InventoryStyle.TEXT;
        int a = 3;
        int l = x + 2;
        int t = y + 2;
        int r = x + size - 2;
        int b = y + size - 2;
        if (!full) {
            corner(l, t, 1, 1, a, c);
            corner(r - 1, t, -1, 1, a, c);
            corner(l, b - 1, 1, -1, a, c);
            corner(r - 1, b - 1, -1, -1, a, c);
        } else {
            corner(l + a - 1, t + a - 1, -1, -1, a, c);
            corner(r - a, t + a - 1, 1, -1, a, c);
            corner(l + a - 1, b - a, -1, 1, a, c);
            corner(r - a, b - a, 1, 1, a, c);
        }
    }

    /** An L of two arms {@code len} long from (x, y), running in directions dx and dy. */
    private static void corner(int x, int y, int dx, int dy, int len, int colour) {
        int x2 = x + dx * (len - 1);
        int y2 = y + dy * (len - 1);
        Gui.drawRect(Math.min(x, x2), y, Math.max(x, x2) + 1, y + 1, colour);
        Gui.drawRect(x, Math.min(y, y2), x + 1, Math.max(y, y2) + 1, colour);
    }
}
