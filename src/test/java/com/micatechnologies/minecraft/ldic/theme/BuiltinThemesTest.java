package com.micatechnologies.minecraft.ldic.theme;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.ldic.expr.Function;
import com.micatechnologies.minecraft.ldic.expr.Scope;
import com.micatechnologies.minecraft.ldic.expr.Type;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Every built-in theme must compile with zero problems. The host scope here stubs the names
 * the client's HostBindings declares; keep the two lists in step.
 */
class BuiltinThemesTest {

    private static final String[] THEMES = {"classic", "horizon", "launcher", "premiere"};

    private static Scope hostStub() {
        Scope s = Scope.withLibrary().child();
        for (String n : new String[] {"time", "delta", "frame", "fps", "screen_time", "screen_width", "screen_height",
                "gui_width", "gui_height", "gui_scale", "ui_scale", "mouse_x", "mouse_y",
                "mem.used", "mem.total", "mem.max", "mem.free", "mod_count", "foreign_buttons"}) {
            s.declare(n, Type.NUMBER);
        }
        for (String n : new String[] {"screen", "mc_version", "forge_version", "username", "pack_name", "pack_version"}) {
            s.declare(n, Type.STRING);
        }
        s.declare("is_demo", Type.BOOL);
        s.declare("in_world", Type.BOOL);
        s.declare("singleplayer", Type.BOOL);
        s.declare("lan_open", Type.BOOL);
        s.declare("status", Type.STRING);
        s.declare("status_detail", Type.STRING);
        s.declare("status_progress", Type.NUMBER);
        s.define(Function.num("option_value", false, a -> 0.5, Type.STRING));
        s.define(Function.str("option_text", false, a -> "FOV: 70", Type.STRING));
        s.define(Function.bool("option_on", false, a -> true, Type.STRING));
        s.define(Function.str("difficulty_text", false, a -> "Difficulty: Easy"));
        s.define(Function.bool("can_change_difficulty", false, a -> true));
        s.declare("reduced_motion", Type.BOOL);
        s.define(Function.bool("mod_loaded", false, a -> false, Type.STRING));
        s.define(Function.str("translate", false, a -> a[0].str(), Type.STRING));
        s.define(Function.bool("has_translation", false, a -> true, Type.STRING));
        s.define(Function.str("splash", false, a -> "splash"));
        s.define(Function.bool("server_online", false, a -> true, Type.STRING));
        s.define(Function.num("server_players", false, a -> 3, Type.STRING));
        s.define(Function.num("server_max_players", false, a -> 20, Type.STRING));
        s.define(Function.str("server_motd", false, a -> "motd", Type.STRING));
        s.define(Function.num("server_ping", false, a -> 40, Type.STRING));
        s.define(Function.str("web_text", false, a -> "", Type.STRING));
        s.define(Function.str("web_line", false, a -> "", Type.STRING, Type.NUMBER));
        s.define(Function.num("web_line_count", false, a -> 0, Type.STRING));
        s.define(Function.str("branding", false, a -> "Minecraft 1.12.2"));
        s.define(Function.str("date", false, a -> "2026-01-01", Type.STRING));
        s.define(Function.str("clock", false, a -> "10:04 AM"));
        s.define(Function.str("long_date", false, a -> "Thursday, October 29, 2026"));
        s.define(Function.num("month", false, a -> 1));
        s.define(Function.num("day", false, a -> 1));
        s.define(Function.num("hour", false, a -> 12));
        return s;
    }

    private static final ThemeSource JAR = reference -> {
        String id = reference.startsWith("builtin:") ? reference.substring(8) : reference;
        try (InputStream in = BuiltinThemesTest.class.getResourceAsStream("/assets/ldic/themes/" + id + "/theme.json")) {
            if (in == null) {
                throw new FileNotFoundException(reference);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return new ThemeSource.Document("builtin/" + id, reference, new String(out.toByteArray(), StandardCharsets.UTF_8));
        }
    };

    private static final String[] LOADING_THEMES = {"minimal", "console", "cinematic", "traditional", "notices"};

    /** Stub of client.loading.LoadingBindings; keep in step with it. */
    private static Scope loadingStub() {
        Scope s = Scope.withLibrary().child();
        for (String n : new String[] {"time", "screen_time", "delta", "frame", "screen_width", "screen_height",
                "gui_width", "gui_height", "gui_scale", "ui_scale", "mem.used", "mem.total", "mem.max", "mem.free",
                "progress", "progress_raw", "stage", "stage_count", "eta", "mods_found", "skip_hold", "skip_seconds"}) {
            s.declare(n, Type.NUMBER);
        }
        for (String n : new String[] {"status", "sub_status", "task", "mc_version", "forge_version", "username", "pack_name", "pack_version"}) {
            s.declare(n, Type.STRING);
        }
        s.declare("reduced_motion", Type.BOOL);
        s.declare("loading_done", Type.BOOL);
        s.define(Function.str("tip", false, a -> "tip"));
        s.define(Function.str("tip", false, a -> "tip", Type.NUMBER));
        s.define(Function.num("tip_count", false, a -> 1));
        s.define(Function.num("bar_count", false, a -> 0));
        s.define(Function.str("bar_title", false, a -> "", Type.NUMBER));
        s.define(Function.str("bar_message", false, a -> "", Type.NUMBER));
        s.define(Function.num("bar_progress", false, a -> 0, Type.NUMBER));
        s.define(Function.str("clock", false, a -> "10:04 AM"));
        s.define(Function.str("long_date", false, a -> "Thursday, October 29, 2026"));
        s.define(Function.str("date", false, a -> "2026-01-01", Type.STRING));
        s.define(Function.str("duration", true, a -> "1:05", Type.NUMBER));
        return s;
    }

    @Test
    void builtinThemesCompileCleanly() throws IOException {
        for (String id : THEMES) {
            check(id, hostStub());
        }
        for (String id : LOADING_THEMES) {
            check(id, loadingStub());
        }
    }

    private static void check(String id, Scope scope) throws IOException {
        Problems p = new Problems();
        ThemeLoader.Result r = ThemeLoader.load(JAR, JAR.open("builtin:" + id), p);
        assertNotNull(r, id + ": " + p.all());
        ThemeDef t = ThemeCompiler.compile(r.json, scope, p);
        assertTrue(p.isEmpty(), id + ": " + p.all());
        assertNotNull(t.screen(id.equals("notices") ? "photosensitivity" : "main"), id);
    }
}
