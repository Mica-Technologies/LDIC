package com.micatechnologies.minecraft.ldic.client;

import static com.micatechnologies.minecraft.ldic.expr.Type.BOOL;
import static com.micatechnologies.minecraft.ldic.expr.Type.NUMBER;
import static com.micatechnologies.minecraft.ldic.expr.Type.STRING;

import com.micatechnologies.minecraft.ldic.expr.Function;
import com.micatechnologies.minecraft.ldic.expr.Scope;
import com.micatechnologies.minecraft.ldic.expr.Slot;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
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
        Runtime rt = Runtime.getRuntime();
        double mib = 1024.0 * 1024.0;
        memTotal.set(Math.round(rt.totalMemory() / mib));
        memFree.set(Math.round(rt.freeMemory() / mib));
        memUsed.set(Math.round((rt.totalMemory() - rt.freeMemory()) / mib));
        memMax.set(Math.round(rt.maxMemory() / mib));
    }
}
