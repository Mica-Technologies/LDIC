package com.micatechnologies.minecraft.ldic.loading;

import java.util.List;

/**
 * Turns Forge's stack of progress bars into one honest percentage.
 *
 * <p>Forge's outermost bar, "Loading", has seven steps (constructing mods, pre-init, the engine
 * with textures and models, init, post-init, finishing, completing). They take very different
 * amounts of time, so each step gets a weight. By default the weights are a typical pack's
 * split; after the first launch they are the time each step really took last time, which makes
 * the bar move at an even speed and gives a real ETA. Inside the current step, nested bars
 * (mods, textures, models) give the fraction done.</p>
 *
 * <p>The value shown never goes backwards and eases toward the target, so the bar does not
 * jump when a nested bar is replaced.</p>
 *
 * <p>No Minecraft types: the client feeds it bar snapshots, which keeps it unit-testable.</p>
 */
public final class ProgressModel {

    /** One progress bar as Forge reports it: {@code step} items begun out of {@code steps}. */
    public static final class Bar {
        public final String title;
        public final String message;
        public final int step;
        public final int steps;

        public Bar(String title, String message, int step, int steps) {
            this.title = title == null ? "" : title;
            this.message = message == null ? "" : message;
            this.step = step;
            this.steps = steps;
        }
    }

    public static final int STAGES = 7;

    /** Typical share of load time per top-level step, used until real timings exist. */
    static final double[] DEFAULT_WEIGHTS = {8, 20, 40, 14, 12, 3, 3};

    private static final String[] STAGE_NAMES = {
        "Constructing mods",
        "Pre-initialization",
        "Loading textures and models",
        "Initialization",
        "Post-initialization",
        "Finishing up",
        "Starting the game"
    };

    private final double[] weights;
    private final double expectedTotal;
    private final double[] stageStart = new double[STAGES + 1];
    private final double[] stageSeconds = new double[STAGES];

    private int stage;
    private double raw;
    private double shown;
    private String status = "Starting";
    private String subStatus = "";
    private String task = "";

    /**
     * @param lastTimings seconds each stage took last launch, or null for the defaults
     */
    public ProgressModel(double[] lastTimings) {
        weights = new double[STAGES];
        double total = 0;
        boolean usable = lastTimings != null && lastTimings.length == STAGES;
        if (usable) {
            for (double t : lastTimings) {
                usable &= t >= 0 && !Double.isNaN(t);
                total += Math.max(0, t);
            }
            usable &= total > 1;
        }
        for (int i = 0; i < STAGES; i++) {
            // A floor keeps a step that took ~0 s last time from collapsing to an instant jump.
            weights[i] = usable ? Math.max(lastTimings[i], total * 0.01) : DEFAULT_WEIGHTS[i];
        }
        expectedTotal = usable ? total : -1;
    }

    /**
     * Updates from the current bar stack (outermost first) at {@code now} seconds since loading
     * began, easing the shown value by {@code dt} seconds.
     */
    public void update(List<Bar> bars, double now, double dt) {
        Bar top = null;
        int topIndex = -1;
        for (int i = 0; i < bars.size(); i++) {
            if (bars.get(i).title.equals("Loading") && bars.get(i).steps == STAGES) {
                top = bars.get(i);
                topIndex = i;
                break;
            }
        }
        if (top == null) {
            raw = 0;
            status = bars.isEmpty() ? "Starting" : bars.get(0).title;
            subStatus = bars.isEmpty() ? "" : bars.get(bars.size() - 1).message;
            task = bars.isEmpty() ? "" : bars.get(bars.size() - 1).title;
        } else {
            int s = Math.max(1, Math.min(STAGES, top.step));
            if (s != stage) {
                for (int k = Math.max(1, stage); k < s; k++) {
                    // Close every stage we passed (one may be skipped between frames).
                    stageSeconds[k - 1] = now - stageStart[k];
                    stageStart[k + 1] = now;
                }
                if (stage == 0) {
                    stageStart[s] = now;
                }
                stage = s;
            }
            double inner = fraction(bars, topIndex + 1);
            double done = 0;
            double total = 0;
            for (int i = 0; i < STAGES; i++) {
                total += weights[i];
                if (i < s - 1) {
                    done += weights[i];
                }
            }
            raw = (done + weights[s - 1] * inner) / total;
            status = STAGE_NAMES[s - 1];
            Bar deepest = bars.get(bars.size() - 1);
            subStatus = deepest == top ? "" : deepest.message;
            task = deepest == top ? "" : deepest.title;
        }
        raw = clamp01(raw);
        double target = Math.max(shown, raw);
        shown = dt <= 0 ? target : shown + (target - shown) * Math.min(1, dt * 6);
    }

    /** Fraction done of the bar at {@code i} including its nested bars; 0 if there is none. */
    static double fraction(List<Bar> bars, int i) {
        if (i >= bars.size()) {
            return 0;
        }
        Bar b = bars.get(i);
        if (b.steps <= 0) {
            return 0;
        }
        double begun = Math.max(0, Math.min(b.steps, b.step));
        double f = (Math.max(0, begun - 1) + (begun > 0 ? fraction(bars, i + 1) : 0)) / b.steps;
        return clamp01(f);
    }

    /** Records the end of loading so the last stage's time is known. */
    public void finish(double now) {
        if (stage >= 1) {
            stageSeconds[stage - 1] = now - stageStart[stage];
        }
    }

    /** Seconds per stage measured this launch; to save for next time. */
    public double[] stageSeconds() {
        return stageSeconds.clone();
    }

    /** Whether every stage was seen, so the timings are worth saving. */
    public boolean complete() {
        return stage == STAGES;
    }

    /** Eased, never-decreasing progress 0..1. */
    public double progress() {
        return shown;
    }

    public double rawProgress() {
        return raw;
    }

    /** 1..7 once Forge's main bar exists, else 0. */
    public int stage() {
        return stage;
    }

    public String status() {
        return status;
    }

    public String subStatus() {
        return subStatus;
    }

    public String task() {
        return task;
    }

    /**
     * Seconds left: the share of last launch's time that is still to go. Progress is weighted
     * by those same times, so this is simply the remaining fraction of the expected total.
     * -1 when unknown (first launch).
     */
    public double eta() {
        return expectedTotal <= 0 ? -1 : expectedTotal * (1 - raw);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
