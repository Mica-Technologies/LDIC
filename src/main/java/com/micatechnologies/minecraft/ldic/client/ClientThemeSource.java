package com.micatechnologies.minecraft.ldic.client;

import com.micatechnologies.minecraft.ldic.theme.ThemeSource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Theme documents from the LDIC jar ({@code builtin:<id>}) and from {@code config/ldic/themes/<id>/theme.json}.
 *
 * <p>Locations are {@code builtin/<id>} (assets under {@code /assets/ldic/themes/<id>/} in the
 * jar) and {@code config/<relative dir>} (assets under that folder of {@code config/ldic}).
 * {@link AssetResolver} turns those back into files and resources.</p>
 */
public final class ClientThemeSource implements ThemeSource {

    public static final String BUILTIN = "builtin:";

    private final File configDir;

    public ClientThemeSource(File configDir) {
        this.configDir = configDir;
    }

    @Override
    public Document open(String reference) throws IOException {
        if (reference.startsWith(BUILTIN)) {
            String id = reference.substring(BUILTIN.length());
            checkId(id);
            String resource = "/assets/ldic/themes/" + id + "/theme.json";
            try (InputStream in = ClientThemeSource.class.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new FileNotFoundException("no built-in theme '" + id + "'");
                }
                return new Document("builtin/" + id, reference, read(in));
            }
        }
        checkId(reference);
        File file = new File(configDir, "themes/" + reference + "/theme.json");
        if (!file.isFile()) {
            throw new FileNotFoundException("no theme at " + file);
        }
        return new Document("config/themes/" + reference, "config/ldic/themes/" + reference + "/theme.json", readFile(file));
    }

    /** A theme file directly inside {@code config/ldic}, such as {@code menu.json}. */
    public Document openConfigFile(String name) throws IOException {
        File file = new File(configDir, name);
        return new Document("config", "config/ldic/" + name, readFile(file));
    }

    private static void checkId(String id) throws FileNotFoundException {
        if (!id.matches("[A-Za-z0-9_.-]+") || id.contains("..")) {
            throw new FileNotFoundException("'" + id + "' is not a valid theme name");
        }
    }

    private static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static String read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
