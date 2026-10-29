package com.micatechnologies.minecraft.ldic.client.audio;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import java.io.IOException;
import java.io.InputStream;

/**
 * Streams an Ogg Vorbis file as 16-bit little-endian PCM, decoding with the JOrbis library
 * Minecraft already ships (inside paulscode's codecjorbis), so LDIC adds no dependency.
 *
 * <p>Single logical stream only (no chained Oggs), which is what music files are.</p>
 */
final class OggStream implements AutoCloseable {

    private static final int CHUNK = 4096;

    private final InputStream in;
    private final SyncState sync = new SyncState();
    private final StreamState stream = new StreamState();
    private final Page page = new Page();
    private final Packet packet = new Packet();
    private final Info info = new Info();
    private final Comment comment = new Comment();
    private final DspState dsp = new DspState();
    private final Block block = new Block(dsp);
    private final float[][][] pcm = new float[1][][];
    private int[] index;
    private boolean eos;

    OggStream(InputStream in) throws IOException {
        this.in = in;
        sync.init();
        readHeaders();
        dsp.synthesis_init(info);
        block.init(dsp);
        index = new int[info.channels];
    }

    int channels() {
        return info.channels;
    }

    int rate() {
        return info.rate;
    }

    private boolean fill() throws IOException {
        int at = sync.buffer(CHUNK);
        int n = in.read(sync.data, at, CHUNK);
        if (n <= 0) {
            sync.wrote(0);
            return false;
        }
        sync.wrote(n);
        return true;
    }

    private void readHeaders() throws IOException {
        while (sync.pageout(page) != 1) {
            if (!fill()) {
                throw new IOException("not an Ogg file");
            }
        }
        stream.init(page.serialno());
        info.init();
        comment.init();
        if (stream.pagein(page) < 0 || stream.packetout(packet) != 1 || info.synthesis_headerin(comment, packet) < 0) {
            throw new IOException("not an Ogg Vorbis file");
        }
        int headers = 1;
        while (headers < 3) {
            int r = sync.pageout(page);
            if (r == 0) {
                if (!fill()) {
                    throw new IOException("Ogg file ends inside its headers");
                }
                continue;
            }
            if (r < 0) {
                continue;
            }
            stream.pagein(page);
            while (headers < 3) {
                int p = stream.packetout(packet);
                if (p == 0) {
                    break;
                }
                if (p < 0) {
                    throw new IOException("corrupt Ogg header");
                }
                info.synthesis_headerin(comment, packet);
                headers++;
            }
        }
    }

    /**
     * Decodes into {@code out} as interleaved 16-bit little-endian samples scaled by
     * {@code gain}. Returns the number of bytes written, or -1 at the end of the stream.
     */
    int read(byte[] out, float gain) throws IOException {
        while (true) {
            int samples = dsp.synthesis_pcmout(pcm, index);
            if (samples > 0) {
                int channels = info.channels;
                int frames = Math.min(samples, out.length / (2 * channels));
                float[][] p = pcm[0];
                int o = 0;
                for (int i = 0; i < frames; i++) {
                    for (int c = 0; c < channels; c++) {
                        int v = (int) (p[c][index[c] + i] * 32767f * gain);
                        v = v > 32767 ? 32767 : (v < -32768 ? -32768 : v);
                        out[o++] = (byte) v;
                        out[o++] = (byte) (v >> 8);
                    }
                }
                dsp.synthesis_read(frames);
                return o;
            }
            if (eos) {
                return -1;
            }
            int p = stream.packetout(packet);
            if (p > 0) {
                if (block.synthesis(packet) == 0) {
                    dsp.synthesis_blockin(block);
                }
                continue;
            }
            if (p < 0) {
                continue;
            }
            int r = sync.pageout(page);
            if (r > 0) {
                stream.pagein(page);
                if (page.eos() != 0) {
                    // Drain this last page's packets, then stop.
                    while (stream.packetout(packet) > 0) {
                        if (block.synthesis(packet) == 0) {
                            dsp.synthesis_blockin(block);
                        }
                    }
                    eos = true;
                }
                continue;
            }
            if (r == 0 && !fill()) {
                eos = true;
            }
        }
    }

    @Override
    public void close() throws IOException {
        stream.clear();
        block.clear();
        dsp.clear();
        info.clear();
        sync.clear();
        in.close();
    }
}
