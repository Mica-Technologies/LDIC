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

    private static final String[] THEMES = {"classic", "horizon"};

    private static Scope hostStub() {
        Scope s = Scope.withLibrary().child();
        for (String n : new String[] {"time", "delta", "frame", "fps", "screen_time", "screen_width", "screen_height",
                "gui_width", "gui_height", "gui_scale", "ui_scale", "mouse_x", "mouse_y",
                "mem.used", "mem.total", "mem.max", "mem.free", "mod_count"}) {
            s.declare(n, Type.NUMBER);
        }
        for (String n : new String[] {"screen", "mc_version", "forge_version", "username", "pack_name", "pack_version"}) {
            s.declare(n, Type.STRING);
        }
        s.declare("is_demo", Type.BOOL);
        s.define(Function.bool("mod_loaded", false, a -> false, Type.STRING));
        s.define(Function.str("translate", false, a -> a[0].str(), Type.STRING));
        s.define(Function.bool("has_translation", false, a -> true, Type.STRING));
        s.define(Function.str("splash", false, a -> "splash"));
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

    @Test
    void builtinThemesCompileCleanly() throws IOException {
        for (String id : THEMES) {
            Problems p = new Problems();
            ThemeLoader.Result r = ThemeLoader.load(JAR, JAR.open("builtin:" + id), p);
            assertNotNull(r, id + ": " + p.all());
            ThemeDef t = ThemeCompiler.compile(r.json, hostStub(), p);
            assertTrue(p.isEmpty(), id + ": " + p.all());
            assertNotNull(t.screen("main"), id);
        }
    }
}
