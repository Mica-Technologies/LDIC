package com.micatechnologies.minecraft.ldic.client;

import com.micatechnologies.minecraft.ldic.Ldic;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.ServerPinger;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.FMLClientHandler;

/**
 * Live status of servers a theme asks about, using the same pinger as the multiplayer screen.
 *
 * <p>A server is pinged the first time a theme reads it, then again every
 * {@link #REFRESH_MS} while the theme keeps reading it. Pings run on a small background pool;
 * {@link #tick()} must be called on the main thread each tick to process the network replies,
 * as the multiplayer screen does.</p>
 */
public final class ServerStatus {

    private static final long REFRESH_MS = 30_000;

    public static final class Entry {
        final ServerData data;
        volatile long lastPing;
        volatile boolean pinging;
        volatile boolean failed;

        Entry(String address) {
            this.data = new ServerData(address, address, false);
        }

        /** Answered a ping and is reachable. */
        public boolean online() {
            return !failed && data.pingToServer >= 0 && data.populationInfo != null && !data.populationInfo.isEmpty();
        }

        public int players() {
            return part(0);
        }

        public int maxPlayers() {
            return part(1);
        }

        private int part(int i) {
            if (!online()) {
                return 0;
            }
            String plain = TextFormatting.getTextWithoutFormattingCodes(data.populationInfo);
            String[] parts = plain == null ? new String[0] : plain.split("/");
            try {
                return parts.length == 2 ? Integer.parseInt(parts[i].trim()) : 0;
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public String motd() {
            return online() && data.serverMOTD != null ? data.serverMOTD : "";
        }

        /** Round trip in milliseconds, or -1 while unknown or offline. */
        public long ping() {
            return online() ? data.pingToServer : -1;
        }
    }

    private final ServerPinger pinger = new ServerPinger();
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final ExecutorService pool = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "LDIC server status");
        t.setDaemon(true);
        return t;
    });

    private boolean forgeReady;

    /** The status of {@code address} ({@code host} or {@code host:port}), starting a ping if due. */
    public Entry get(String address) {
        if (!forgeReady) {
            // Forge stores modded-server data from each ping reply in maps it only creates when
            // the Multiplayer screen is first built; a ping before that throws an NPE inside the
            // reply handler. Create them once, the same way that screen does.
            FMLClientHandler.instance().setupServerList();
            forgeReady = true;
        }
        Entry e = entries.computeIfAbsent(address, Entry::new);
        long now = System.currentTimeMillis();
        if (!e.pinging && now - e.lastPing > REFRESH_MS) {
            e.pinging = true;
            e.lastPing = now;
            pool.submit(() -> {
                try {
                    e.failed = false;
                    pinger.ping(e.data);
                } catch (Exception ex) {
                    e.failed = true;
                    Ldic.LOGGER.debug("LDIC: could not ping {}: {}", address, ex.toString());
                } finally {
                    e.pinging = false;
                }
            });
        }
        return e;
    }

    /** Processes ping replies. Main thread, once a tick. */
    public void tick() {
        pinger.pingPendingNetworks();
    }
}
