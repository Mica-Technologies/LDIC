package com.micatechnologies.minecraft.ldic.client.loading;

import static com.micatechnologies.minecraft.ldic.expr.Type.NUMBER;
import static com.micatechnologies.minecraft.ldic.expr.Type.STRING;

import com.micatechnologies.minecraft.ldic.client.LdicSettings;
import com.micatechnologies.minecraft.ldic.expr.Function;
import com.micatechnologies.minecraft.ldic.expr.Scope;
import com.micatechnologies.minecraft.ldic.expr.Slot;
import com.micatechnologies.minecraft.ldic.loading.ProgressModel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;

/**
 * The names a loading-screen theme can read. Shares the menu's general names (time, screen
 * size, memory, clock, pack name) so a theme author learns one vocabulary, and adds progress.
 *
 * <p>Only things that are safe from the loading-screen thread before mods have loaded: no
 * translations (the language manager is not ready), no resource manager.</p>
 *
 * <p>Keep docs/loading/index.md in step with this list.</p>
 */
final class LoadingBindings {

    final Scope scope = Scope.withLibrary().child();

    private final Slot time = scope.declare("time", NUMBER);
    private final Slot screenTime = scope.declare("screen_time", NUMBER);
    private final Slot delta = scope.declare("delta", NUMBER);
    private final Slot frame = scope.declare("frame", NUMBER);
    private final Slot screenWidth = scope.declare("screen_width", NUMBER);
    private final Slot screenHeight = scope.declare("screen_height", NUMBER);
    private final Slot guiWidth = scope.declare("gui_width", NUMBER);
    private final Slot guiHeight = scope.declare("gui_height", NUMBER);
    private final Slot guiScale = scope.declare("gui_scale", NUMBER);
    private final Slot uiScale = scope.declare("ui_scale", NUMBER);
    private final Slot memUsed = scope.declare("mem.used", NUMBER);
    private final Slot memTotal = scope.declare("mem.total", NUMBER);
    private final Slot memMax = scope.declare("mem.max", NUMBER);
    private final Slot memFree = scope.declare("mem.free", NUMBER);

    private final Slot progress = scope.declare("progress", NUMBER);
    private final Slot progressRaw = scope.declare("progress_raw", NUMBER);
    private final Slot stage = scope.declare("stage", NUMBER);
    private final Slot status = scope.declare("status", STRING);
    private final Slot subStatus = scope.declare("sub_status", STRING);
    private final Slot task = scope.declare("task", STRING);
    private final Slot eta = scope.declare("eta", NUMBER);
    private final Slot modsFound = scope.declare("mods_found", NUMBER);

    private final List<ProgressModel.Bar> bars = new ArrayList<>();
    private final List<String> tips;
    private final int tipOffset;
    private final Map<String, SimpleDateFormat> dateFormats = new HashMap<>();
    private int frames;

    LoadingBindings(LdicSettings settings, List<String> tips) {
        this.tips = tips;
        this.tipOffset = tips.isEmpty() ? 0 : new Random().nextInt(tips.size());
        Minecraft mc = Minecraft.getMinecraft();
        scope.declare("stage_count", NUMBER).set(ProgressModel.STAGES);
        scope.declare("mc_version", STRING).set(ForgeVersion.mcVersion);
        scope.declare("forge_version", STRING).set(ForgeVersion.getVersion());
        scope.declare("username", STRING).set(mc.getSession().getUsername());
        scope.declare("pack_name", STRING).set(settings.packName);
        scope.declare("pack_version", STRING).set(settings.packVersion);

        // tip() is one tip picked at random for this launch; tip(i) walks the list from there,
        // so a theme can rotate with tip(floor(time / 8)).
        scope.define(Function.str("tip", false, a -> tip(0)));
        scope.define(Function.str("tip", false, a -> tip((int) Math.floor(a[0].num())), NUMBER));
        scope.define(Function.num("tip_count", false, a -> this.tips.size()));
        scope.define(Function.num("bar_count", false, a -> bars.size()));
        scope.define(Function.str("bar_title", false, a -> bar(a[0].num()) == null ? "" : bar(a[0].num()).title, NUMBER));
        scope.define(Function.str("bar_message", false, a -> bar(a[0].num()) == null ? "" : bar(a[0].num()).message, NUMBER));
        scope.define(Function.num("bar_progress", false, a -> {
            ProgressModel.Bar b = bar(a[0].num());
            return b == null || b.steps <= 0 ? 0 : Math.max(0, b.step - 1) / (double) b.steps;
        }, NUMBER));
        DateFormat clock = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
        scope.define(Function.str("clock", false, a -> clock.format(new Date())));
        DateFormat longDate = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault());
        scope.define(Function.str("long_date", false, a -> longDate.format(new Date())));
        scope.define(Function.str("date", false, a -> {
            SimpleDateFormat f = dateFormats.computeIfAbsent(a[0].str(), p -> {
                try {
                    return new SimpleDateFormat(p);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            });
            return f == null ? "bad date format" : f.format(new Date());
        }, STRING));
        // Seconds as m:ss, for ETAs and elapsed time.
        scope.define(Function.str("duration", true, a -> {
            long s = Math.max(0, Math.round(a[0].num()));
            return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
        }, NUMBER));
    }

    private String tip(int i) {
        if (tips.isEmpty()) {
            return "";
        }
        int n = tips.size();
        return tips.get(((tipOffset + i) % n + n) % n);
    }

    private ProgressModel.Bar bar(double i) {
        int idx = (int) i;
        return idx >= 0 && idx < bars.size() ? bars.get(idx) : null;
    }

    void update(double seconds, double dt, double width, double height, double gw, double gh, double gs, double ui,
                ProgressModel model, List<ProgressModel.Bar> current) {
        time.set(seconds);
        screenTime.set(seconds);
        delta.set(dt);
        frame.set(frames++);
        screenWidth.set(width);
        screenHeight.set(height);
        guiWidth.set(gw);
        guiHeight.set(gh);
        guiScale.set(gs);
        uiScale.set(ui);
        Runtime rt = Runtime.getRuntime();
        double mib = 1024.0 * 1024.0;
        memTotal.set(Math.round(rt.totalMemory() / mib));
        memFree.set(Math.round(rt.freeMemory() / mib));
        memUsed.set(Math.round((rt.totalMemory() - rt.freeMemory()) / mib));
        memMax.set(Math.round(rt.maxMemory() / mib));

        bars.clear();
        bars.addAll(current);
        progress.set(model.progress());
        progressRaw.set(model.rawProgress());
        stage.set(model.stage());
        status.set(model.status());
        subStatus.set(model.subStatus());
        task.set(model.task());
        eta.set(model.eta());
        if ((frames & 15) == 0) {
            try {
                // Read from the loading thread while the main thread fills the list: a stale or
                // failed read is fine, the next one is 16 frames away.
                modsFound.set(Loader.instance().getModList().size());
            } catch (RuntimeException ignored) {
                // ConcurrentModificationException while mods are being discovered.
            }
        }
    }
}
