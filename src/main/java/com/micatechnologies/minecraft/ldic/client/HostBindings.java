package com.micatechnologies.minecraft.ldic.client;

import static com.micatechnologies.minecraft.ldic.expr.Type.BOOL;
import static com.micatechnologies.minecraft.ldic.expr.Type.NUMBER;
import static com.micatechnologies.minecraft.ldic.expr.Type.STRING;

import com.micatechnologies.minecraft.ldic.expr.Function;
import com.micatechnologies.minecraft.ldic.expr.Scope;
import com.micatechnologies.minecraft.ldic.client.menu.GameOptions;
import com.micatechnologies.minecraft.ldic.expr.Slot;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;

/**
 * The game-facing names a menu theme can read: live values updated each frame and functions
 * over game state. The pure library ({@code sin}, {@code mix}, ...) comes from
 * {@link Scope#withLibrary()}; everything here needs Minecraft.
 *
 * <p>Keep docs/themes.md in step with this list.</p>
 */
public final class HostBindings {

    public final Scope scope = Scope.withLibrary().child();

    private final Slot time = scope.declare("time", NUMBER);
    private final Slot delta = scope.declare("delta", NUMBER);
    private final Slot frame = scope.declare("frame", NUMBER);
    private final Slot fps = scope.declare("fps", NUMBER);
    private final Slot screenTime = scope.declare("screen_time", NUMBER);
    private final Slot screenWidth = scope.declare("screen_width", NUMBER);
    private final Slot screenHeight = scope.declare("screen_height", NUMBER);
    private final Slot guiWidth = scope.declare("gui_width", NUMBER);
    private final Slot guiHeight = scope.declare("gui_height", NUMBER);
    private final Slot guiScale = scope.declare("gui_scale", NUMBER);
    private final Slot uiScale = scope.declare("ui_scale", NUMBER);
    private final Slot mouseX = scope.declare("mouse_x", NUMBER);
    private final Slot mouseY = scope.declare("mouse_y", NUMBER);
    private final Slot memUsed = scope.declare("mem.used", NUMBER);
    private final Slot memTotal = scope.declare("mem.total", NUMBER);
    private final Slot memMax = scope.declare("mem.max", NUMBER);
    private final Slot memFree = scope.declare("mem.free", NUMBER);
    private final Slot screenId = scope.declare("screen", STRING);
    private final Slot inWorld = scope.declare("in_world", BOOL);
    private final Slot foreignButtons = scope.declare("foreign_buttons", NUMBER);
    private final Slot singleplayer = scope.declare("singleplayer", BOOL);
    private final Slot lanOpen = scope.declare("lan_open", BOOL);
    private final Slot cheats = scope.declare("cheats", BOOL);
    private final Slot status = scope.declare("status", STRING);
    private final Slot statusDetail = scope.declare("status_detail", STRING);
    private final Slot statusProgress = scope.declare("status_progress", NUMBER);

    // Inventory screens: the container's title, the player's state, and named areas (see setInventory).
    private final Slot containerTitle = scope.declare("container_title", STRING);
    private final Slot playerHealth = scope.declare("player_health", NUMBER);
    private final Slot playerMaxHealth = scope.declare("player_max_health", NUMBER);
    private final Slot playerFood = scope.declare("player_food", NUMBER);
    private final Slot playerArmor = scope.declare("player_armor", NUMBER);
    private final Slot playerLevel = scope.declare("player_level", NUMBER);
    private final Slot playerXp = scope.declare("player_xp", NUMBER);
    private Map<String, double[]> areas = new HashMap<>();
    private List<GuiButton> skinButtons = Collections.emptyList();
    private List<GuiTextField> skinFields = Collections.emptyList();
    private Map<String, String> skinText = new HashMap<>();
    private Map<String, Boolean> skinFlags = new HashMap<>();
    private Map<String, Double> screenValues = new HashMap<>();
    // The world selection screen.
    private final Slot worldSelected = scope.declare("world_selected", BOOL);
    private final Slot worldCount = scope.declare("world_count", NUMBER);
    private final Slot worldSort = scope.declare("world_sort", STRING);
    // The server list screen.
    private final Slot serverSelected = scope.declare("server_selected", BOOL);
    private final Slot serverEditable = scope.declare("server_editable", BOOL);
    private final Slot serverCount = scope.declare("server_count", NUMBER);
    // The mod list screen.
    private final Slot modSelected = scope.declare("mod_selected", BOOL);
    private final Slot modConfigurable = scope.declare("mod_configurable", BOOL);
    private final Slot modSort = scope.declare("mod_sort", STRING);

    private final Map<String, SimpleDateFormat> dateFormats = new HashMap<>();
    private String splash = "";
    private int frames;

    public HostBindings(LdicSettings settings, ServerStatus servers, WebText web) {
        Minecraft mc = Minecraft.getMinecraft();
        scope.declare("mc_version", STRING).set(net.minecraftforge.common.ForgeVersion.mcVersion);
        scope.declare("forge_version", STRING).set(ForgeVersion.getVersion());
        scope.declare("mod_count", NUMBER).set(Loader.instance().getActiveModList().size());
        scope.declare("username", STRING).set(mc.getSession().getUsername());
        scope.declare("pack_name", STRING).set(settings.packName);
        scope.declare("pack_version", STRING).set(settings.packVersion);
        scope.declare("is_demo", BOOL).set(mc.isDemo());
        scope.declare("reduced_motion", BOOL).set(settings.reducedMotion);

        scope.define(Function.bool("mod_loaded", false, a -> Loader.isModLoaded(a[0].str()), STRING));
        scope.define(Function.str("translate", false, a -> I18n.format(a[0].str()), STRING));
        scope.define(Function.bool("has_translation", false, a -> I18n.hasKey(a[0].str()), STRING));
        scope.define(Function.str("splash", false, a -> splash));
        // Game options, for the options screen (see GameOptions for the names).
        scope.define(Function.num("option_value", false, a -> GameOptions.value(a[0].str()), STRING));
        scope.define(Function.str("option_text", false, a -> GameOptions.text(a[0].str()), STRING));
        scope.define(Function.bool("option_on", false, a -> GameOptions.value(a[0].str()) >= 0.5, STRING));
        scope.define(Function.str("difficulty_text", false, a -> GameOptions.difficultyText()));
        scope.define(Function.bool("can_change_difficulty", false, a -> GameOptions.canChangeDifficulty()));
        // Live server status, pinged in the background (see ServerStatus).
        scope.define(Function.bool("server_online", false, a -> servers.get(a[0].str()).online(), STRING));
        scope.define(Function.num("server_players", false, a -> servers.get(a[0].str()).players(), STRING));
        scope.define(Function.num("server_max_players", false, a -> servers.get(a[0].str()).maxPlayers(), STRING));
        scope.define(Function.str("server_motd", false, a -> servers.get(a[0].str()).motd(), STRING));
        scope.define(Function.num("server_ping", false, a -> servers.get(a[0].str()).ping(), STRING));
        // Text from the web (news, messages), fetched in the background and cached.
        scope.define(Function.str("web_text", false, a -> web.text(a[0].str()), STRING));
        scope.define(Function.str("web_line", false, a -> web.line(a[0].str(), (int) Math.floor(a[1].num())), STRING, NUMBER));
        scope.define(Function.num("web_line_count", false, a -> web.lineCount(a[0].str()), STRING));
        // Forge's branding lines (Minecraft, MCP, Forge, mod counts), top line first.
        scope.define(Function.str("branding", false, a -> String.join("\n", FMLCommonHandler.instance().getBrandings(true))));
        scope.define(Function.str("date", false, a -> {
            String fmt = a[0].str();
            SimpleDateFormat f = dateFormats.get(fmt);
            if (f == null) {
                try {
                    f = new SimpleDateFormat(fmt);
                } catch (IllegalArgumentException e) {
                    return "bad date format";
                }
                dateFormats.put(fmt, f);
            }
            return f.format(new Date());
        }, STRING));
        // The time in the system's own regional style (12-hour with AM/PM where that is the norm).
        DateFormat clock = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
        scope.define(Function.str("clock", false, a -> clock.format(new Date())));
        DateFormat longDate = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault());
        scope.define(Function.str("long_date", false, a -> longDate.format(new Date())));
        scope.define(Function.num("month", false, a -> Calendar.getInstance().get(Calendar.MONTH) + 1));
        scope.define(Function.num("day", false, a -> Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));
        scope.define(Function.num("hour", false, a -> Calendar.getInstance().get(Calendar.HOUR_OF_DAY)));
        // Inventory screens: a named rectangle ("panel", "player", "container", ...), 0 when absent.
        scope.define(Function.num("area_x", false, a -> area(a[0].str(), 0), STRING));
        scope.define(Function.num("area_y", false, a -> area(a[0].str(), 1), STRING));
        scope.define(Function.num("area_w", false, a -> area(a[0].str(), 2), STRING));
        scope.define(Function.num("area_h", false, a -> area(a[0].str(), 3), STRING));
        scope.define(Function.bool("has_area", false, a -> areas.containsKey(a[0].str()), STRING));
        // Skinned vanilla screens: their own buttons (by id) and text fields (by order), and strings
        // and flags particular to the screen (see ScreenSkins).
        scope.define(Function.str("vanilla_text", false, a -> {
            GuiButton b = skinButton(a[0].num());
            return b == null ? "" : b.displayString;
        }, NUMBER));
        scope.define(Function.bool("vanilla_visible", false, a -> {
            GuiButton b = skinButton(a[0].num());
            return b != null && b.visible;
        }, NUMBER));
        scope.define(Function.bool("vanilla_enabled", false, a -> {
            GuiButton b = skinButton(a[0].num());
            return b != null && b.visible && b.enabled;
        }, NUMBER));
        scope.define(Function.bool("field_visible", false, a -> {
            int i = (int) a[0].num();
            return i >= 0 && i < skinFields.size() && skinFields.get(i).getVisible();
        }, NUMBER));
        scope.define(Function.str("screen_text", false, a -> skinText.getOrDefault(a[0].str(), ""), STRING));
        scope.define(Function.num("screen_value", false, a -> screenValues.getOrDefault(a[0].str(), 0.0), STRING));
        scope.define(Function.bool("screen_flag", false, a -> skinFlags.getOrDefault(a[0].str(), false), STRING));
    }

    private GuiButton skinButton(double id) {
        for (GuiButton b : skinButtons) {
            if (b.id == (int) id) {
                return b;
            }
        }
        return null;
    }

    /** What a skinned vanilla screen offers this frame. */
    public void setSkin(List<GuiButton> buttons, List<GuiTextField> fields,
                        Map<String, String> text, Map<String, Boolean> flags) {
        skinButtons = buttons;
        skinFields = fields;
        skinText = text;
        skinFlags = flags;
    }

    private double area(String name, int i) {
        double[] r = areas.get(name);
        return r == null ? 0 : r[i];
    }

    /** The world selection screen's state this frame. */
    public void setWorlds(boolean selected, int count, String sort) {
        worldSelected.set(selected);
        worldCount.set(count);
        worldSort.set(sort);
    }

    /** The mod list screen's state this frame: a mod selected, one with a config screen, the sort. */
    public void setMods(boolean selected, boolean configurable, String sort) {
        modSelected.set(selected);
        modConfigurable.set(configurable);
        modSort.set(sort);
    }

    /** The server list screen's state this frame: a row selected, a saved (editable) one, how many saved. */
    public void setServers(boolean selected, boolean editable, int count) {
        serverSelected.set(selected);
        serverEditable.set(editable);
        serverCount.set(count);
    }

    /** What an inventory screen shows this frame: its title and named areas (x, y, w, h). */
    /** A screen's own strings, switches and numbers (an inventory's progress, an anvil's cost), for screen_text etc. */
    public void setScreenValues(Map<String, String> text, Map<String, Boolean> flags, Map<String, Double> values) {
        skinText = text;
        skinFlags = flags;
        screenValues = values;
    }

    public void setInventory(String title, Map<String, double[]> areas) {
        containerTitle.set(title == null ? "" : title);
        this.areas = areas;
    }

    /** How many of other mods' buttons are showing on the current screen. */
    /** What the world-loading screen is doing: a headline, a detail line, and 0..1 or -1 when unknown. */
    public void setStatus(String headline, String detail, double progress) {
        status.set(headline == null ? "" : headline);
        statusDetail.set(detail == null ? "" : detail);
        statusProgress.set(progress);
    }

    public void setForeignButtons(int count) {
        foreignButtons.set(count);
    }

    public void setSplash(String splash) {
        this.splash = splash == null ? "" : splash;
    }

    /** Updates the per-frame values. Sizes are in layout units except the gui_* ones. */
    public void update(double seconds, double dt, double screenSeconds, String screen,
                       double width, double height, double gw, double gh, double gs, double ui,
                       double mx, double my) {
        time.set(seconds);
        delta.set(dt);
        frame.set(frames++);
        fps.set(Minecraft.getDebugFPS());
        screenTime.set(screenSeconds);
        screenId.set(screen);
        screenWidth.set(width);
        screenHeight.set(height);
        guiWidth.set(gw);
        guiHeight.set(gh);
        guiScale.set(gs);
        uiScale.set(ui);
        mouseX.set(mx);
        mouseY.set(my);
        inWorld.set(GameOptions.inWorld());
        Minecraft mcNow = Minecraft.getMinecraft();
        singleplayer.set(mcNow.isSingleplayer());
        lanOpen.set(mcNow.isSingleplayer() && mcNow.getIntegratedServer() != null && mcNow.getIntegratedServer().getPublic());
        cheats.set(mcNow.player != null && mcNow.player.canUseCommand(2, ""));
        if (mcNow.player != null) {
            playerHealth.set(mcNow.player.getHealth());
            playerMaxHealth.set(mcNow.player.getMaxHealth());
            playerFood.set(mcNow.player.getFoodStats().getFoodLevel());
            playerArmor.set(mcNow.player.getTotalArmorValue());
            playerLevel.set(mcNow.player.experienceLevel);
            playerXp.set(mcNow.player.experience);
        }
        Runtime rt = Runtime.getRuntime();
        double mib = 1024.0 * 1024.0;
        memTotal.set(Math.round(rt.totalMemory() / mib));
        memFree.set(Math.round(rt.freeMemory() / mib));
        memUsed.set(Math.round((rt.totalMemory() - rt.freeMemory()) / mib));
        memMax.set(Math.round(rt.maxMemory() / mib));
    }
}
