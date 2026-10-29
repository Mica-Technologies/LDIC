package com.micatechnologies.minecraft.ldic.client;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.Tags;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Downloads web assets on background threads, with a disk cache in {@code config/ldic/cache/}.
 *
 * <p>A cached copy younger than {@link #FRESH_MS} is used without asking the network. An older
 * one is used if the download fails, so a menu still shows its images offline. Requests carry a
 * User-Agent (some hosts refuse Java's default), time out, and are capped in size.</p>
 */
public final class WebCache {

    private static final long FRESH_MS = TimeUnit.HOURS.toMillis(6);
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final int MAX_BYTES = 16 * 1024 * 1024;

    private final File dir;
    private final ExecutorService pool = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "LDIC web assets");
        t.setDaemon(true);
        return t;
    });

    public WebCache(File configDir) {
        this.dir = new File(configDir, "cache");
    }

    public ExecutorService pool() {
        return pool;
    }

    /** Fetches synchronously (call from a pool thread). Throws when neither network nor cache has it. */
    public byte[] fetch(String url) throws IOException {
        File cached = new File(dir, sha1(url) + ".bin");
        if (cached.isFile() && System.currentTimeMillis() - cached.lastModified() < FRESH_MS) {
            return Files.readAllBytes(cached.toPath());
        }
        try {
            byte[] data = download(url);
            if (!dir.isDirectory() && !dir.mkdirs()) {
                Ldic.LOGGER.warn("Could not create {}", dir);
            } else {
                Files.write(cached.toPath(), data);
            }
            return data;
        } catch (IOException e) {
            if (cached.isFile()) {
                Ldic.LOGGER.warn("Using cached copy of {} ({})", url, e.toString());
                return Files.readAllBytes(cached.toPath());
            }
            throw e;
        }
    }

    private static byte[] download(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(CONNECT_TIMEOUT_MS);
        c.setReadTimeout(READ_TIMEOUT_MS);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "LDIC/" + Tags.VERSION + " (Minecraft 1.12.2)");
        try {
            int code = c.getResponseCode();
            if (code / 100 != 2) {
                throw new IOException("HTTP " + code + " for " + url);
            }
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    if (out.size() > MAX_BYTES) {
                        throw new IOException("larger than " + (MAX_BYTES >> 20) + " MiB: " + url);
                    }
                }
                return out.toByteArray();
            }
        } finally {
            c.disconnect();
        }
    }

    private static String sha1(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
