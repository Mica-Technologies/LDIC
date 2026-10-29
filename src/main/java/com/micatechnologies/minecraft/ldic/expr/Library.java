package com.micatechnologies.minecraft.ldic.expr;

import static com.micatechnologies.minecraft.ldic.expr.Type.BOOL;
import static com.micatechnologies.minecraft.ldic.expr.Type.NUMBER;
import static com.micatechnologies.minecraft.ldic.expr.Type.STRING;

import java.util.Locale;
import java.util.function.DoubleUnaryOperator;

/**
 * The built-in function library every theme can use. Everything here is pure (no IO, no game
 * state), so calls with constant arguments fold at compile time. Host bindings that read game
 * state ({@code mod_loaded}, {@code translate}, ...) are added by the client, not here.
 *
 * <p>Colours are {@code 0xAARRGGBB} numbers; channel arguments are 0-255 unless a name says
 * otherwise. Angles are in degrees.</p>
 */
public final class Library {

    private Library() {
    }

    public static void install(Scope s) {
        constants(s);
        maths(s);
        easing(s);
        random(s);
        strings(s);
        colours(s);
    }

    private static void constants(Scope s) {
        s.declare("pi", NUMBER).set(Math.PI);
        s.declare("tau", NUMBER).set(Math.PI * 2);
        s.declare("e", NUMBER).set(Math.E);
    }

    private static void num1(Scope s, String name, DoubleUnaryOperator op) {
        s.define(Function.num(name, true, a -> op.applyAsDouble(a[0].num()), NUMBER));
    }

    private static void maths(Scope s) {
        num1(s, "abs", Math::abs);
        num1(s, "floor", Math::floor);
        num1(s, "ceil", Math::ceil);
        num1(s, "round", x -> (double) Math.round(x));
        num1(s, "sign", Math::signum);
        num1(s, "sqrt", Math::sqrt);
        num1(s, "cbrt", Math::cbrt);
        num1(s, "exp", Math::exp);
        num1(s, "log", Math::log);
        num1(s, "log10", Math::log10);
        num1(s, "fract", x -> x - Math.floor(x));
        num1(s, "trunc", x -> x < 0 ? Math.ceil(x) : Math.floor(x));
        num1(s, "sin", x -> Math.sin(Math.toRadians(x)));
        num1(s, "cos", x -> Math.cos(Math.toRadians(x)));
        num1(s, "tan", x -> Math.tan(Math.toRadians(x)));
        num1(s, "asin", x -> Math.toDegrees(Math.asin(x)));
        num1(s, "acos", x -> Math.toDegrees(Math.acos(x)));
        num1(s, "atan", x -> Math.toDegrees(Math.atan(x)));
        num1(s, "sinh", Math::sinh);
        num1(s, "cosh", Math::cosh);
        num1(s, "tanh", Math::tanh);
        num1(s, "radians", Math::toRadians);
        num1(s, "degrees", Math::toDegrees);
        s.define(Function.num("atan2", true, a -> Math.toDegrees(Math.atan2(a[0].num(), a[1].num())), NUMBER, NUMBER));
        s.define(Function.num("pow", true, a -> Math.pow(a[0].num(), a[1].num()), NUMBER, NUMBER));
        s.define(Function.num("mod", true, a -> {
            double m = a[1].num();
            double r = a[0].num() % m;
            return r < 0 ? r + Math.abs(m) : r;
        }, NUMBER, NUMBER));
        s.define(Function.numVariadic("min", a -> {
            double m = a[0].num();
            for (int i = 1; i < a.length; i++) {
                m = Math.min(m, a[i].num());
            }
            return m;
        }, NUMBER));
        s.define(Function.numVariadic("max", a -> {
            double m = a[0].num();
            for (int i = 1; i < a.length; i++) {
                m = Math.max(m, a[i].num());
            }
            return m;
        }, NUMBER));
        s.define(Function.num("clamp", true, a -> clamp(a[0].num(), a[1].num(), a[2].num()), NUMBER, NUMBER, NUMBER));
        s.define(Function.num("saturate", true, a -> clamp(a[0].num(), 0, 1), NUMBER));
        s.define(Function.num("lerp", true, a -> lerp(a[0].num(), a[1].num(), a[2].num()), NUMBER, NUMBER, NUMBER));
        s.define(Function.num("inv_lerp", true, a -> {
            double lo = a[0].num();
            double hi = a[1].num();
            return hi == lo ? 0 : (a[2].num() - lo) / (hi - lo);
        }, NUMBER, NUMBER, NUMBER));
        s.define(Function.num("remap", true, a -> {
            double lo = a[1].num();
            double hi = a[2].num();
            double t = hi == lo ? 0 : (a[0].num() - lo) / (hi - lo);
            return lerp(a[3].num(), a[4].num(), t);
        }, NUMBER, NUMBER, NUMBER, NUMBER, NUMBER));
        s.define(Function.num("step", true, a -> a[1].num() < a[0].num() ? 0 : 1, NUMBER, NUMBER));
        s.define(Function.num("smoothstep", true, a -> {
            double lo = a[0].num();
            double hi = a[1].num();
            double t = hi == lo ? (a[2].num() < lo ? 0 : 1) : clamp((a[2].num() - lo) / (hi - lo), 0, 1);
            return t * t * (3 - 2 * t);
        }, NUMBER, NUMBER, NUMBER));
        s.define(Function.num("hypot", true, a -> Math.hypot(a[0].num(), a[1].num()), NUMBER, NUMBER));
        s.define(Function.num("if_else", true, a -> a[0].bool() ? a[1].num() : a[2].num(), BOOL, NUMBER, NUMBER));
        s.define(Function.num("num", true, a -> a[0].bool() ? 1 : 0, BOOL));
        s.define(Function.num("num", true, a -> {
            try {
                return Double.parseDouble(a[0].str().trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }, STRING));
        // Periodic waves of period p seconds over time t, all ranging 0..1.
        s.define(Function.num("wave", true, a -> 0.5 - 0.5 * Math.cos(2 * Math.PI * a[0].num() / a[1].num()), NUMBER, NUMBER));
        s.define(Function.num("saw", true, a -> {
            double x = a[0].num() / a[1].num();
            return x - Math.floor(x);
        }, NUMBER, NUMBER));
        s.define(Function.num("triangle", true, a -> {
            double x = a[0].num() / a[1].num();
            double f = x - Math.floor(x);
            return 1 - Math.abs(2 * f - 1);
        }, NUMBER, NUMBER));
        s.define(Function.num("square", true, a -> {
            double x = a[0].num() / a[1].num();
            return x - Math.floor(x) < 0.5 ? 1 : 0;
        }, NUMBER, NUMBER));
    }

    private static void easing(Scope s) {
        num1(s, "ease_in", t -> {
            t = clamp(t, 0, 1);
            return t * t;
        });
        num1(s, "ease_out", t -> {
            t = clamp(t, 0, 1);
            return 1 - (1 - t) * (1 - t);
        });
        num1(s, "ease_in_out", t -> {
            t = clamp(t, 0, 1);
            return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
        });
        num1(s, "ease_in_cubic", t -> {
            t = clamp(t, 0, 1);
            return t * t * t;
        });
        num1(s, "ease_out_cubic", t -> {
            t = clamp(t, 0, 1);
            return 1 - Math.pow(1 - t, 3);
        });
        num1(s, "ease_in_out_cubic", t -> {
            t = clamp(t, 0, 1);
            return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
        });
        num1(s, "ease_out_back", t -> {
            t = clamp(t, 0, 1);
            double c1 = 1.70158;
            double c3 = c1 + 1;
            return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
        });
        num1(s, "ease_out_elastic", t -> {
            t = clamp(t, 0, 1);
            if (t == 0 || t == 1) {
                return t;
            }
            return Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * (2 * Math.PI / 3)) + 1;
        });
        num1(s, "ease_out_bounce", Library::bounce);
        num1(s, "ease_out_expo", t -> {
            t = clamp(t, 0, 1);
            return t == 1 ? 1 : 1 - Math.pow(2, -10 * t);
        });
    }

    private static double bounce(double t) {
        t = clamp(t, 0, 1);
        double n1 = 7.5625;
        double d1 = 2.75;
        if (t < 1 / d1) {
            return n1 * t * t;
        } else if (t < 2 / d1) {
            t -= 1.5 / d1;
            return n1 * t * t + 0.75;
        } else if (t < 2.5 / d1) {
            t -= 2.25 / d1;
            return n1 * t * t + 0.9375;
        }
        t -= 2.625 / d1;
        return n1 * t * t + 0.984375;
    }

    private static void random(Scope s) {
        // Deterministic: the same seed always gives the same value, so a theme can pick a
        // random tip per launch by seeding with a value it fixes once (a constant).
        s.define(Function.num("random", true, a -> unit(hash(Double.doubleToLongBits(a[0].num()))), NUMBER));
        s.define(Function.num("random", true, a -> unit(hash(Double.doubleToLongBits(a[0].num()) * 31 + (long) a[1].num())), NUMBER, NUMBER));
        s.define(Function.num("random_int", true, a -> {
            double max = a[1].num();
            return max <= 0 ? 0 : Math.floor(unit(hash(Double.doubleToLongBits(a[0].num()))) * max);
        }, NUMBER, NUMBER));
        // Smooth 1D value noise in 0..1.
        s.define(Function.num("noise", true, a -> {
            double x = a[0].num();
            long i = (long) Math.floor(x);
            double f = x - i;
            double u = f * f * (3 - 2 * f);
            return lerp(unit(hash(i)), unit(hash(i + 1)), u);
        }, NUMBER));
    }

    private static void strings(Scope s) {
        s.define(Function.num("len", true, a -> a[0].str().length(), STRING));
        s.define(Function.str("upper", true, a -> a[0].str().toUpperCase(Locale.ROOT), STRING));
        s.define(Function.str("lower", true, a -> a[0].str().toLowerCase(Locale.ROOT), STRING));
        s.define(Function.str("trim", true, a -> a[0].str().trim(), STRING));
        s.define(Function.str("substr", true, a -> {
            String str = a[0].str();
            int start = clampIndex(a[1].num(), str.length());
            return str.substring(start);
        }, STRING, NUMBER));
        s.define(Function.str("substr", true, a -> {
            String str = a[0].str();
            int start = clampIndex(a[1].num(), str.length());
            int end = Math.max(start, clampIndex(a[2].num(), str.length()));
            return str.substring(start, end);
        }, STRING, NUMBER, NUMBER));
        s.define(Function.str("char_at", true, a -> {
            String str = a[0].str();
            int i = (int) a[1].num();
            return i < 0 || i >= str.length() ? "" : String.valueOf(str.charAt(i));
        }, STRING, NUMBER));
        s.define(Function.num("index_of", true, a -> a[0].str().indexOf(a[1].str()), STRING, STRING));
        s.define(Function.bool("contains", true, a -> a[0].str().contains(a[1].str()), STRING, STRING));
        s.define(Function.bool("starts_with", true, a -> a[0].str().startsWith(a[1].str()), STRING, STRING));
        s.define(Function.bool("ends_with", true, a -> a[0].str().endsWith(a[1].str()), STRING, STRING));
        s.define(Function.bool("is_empty", true, a -> a[0].str().isEmpty(), STRING));
        s.define(Function.str("replace", true, a -> a[0].str().replace(a[1].str(), a[2].str()), STRING, STRING, STRING));
        s.define(Function.str("repeat", true, a -> {
            int n = (int) Math.max(0, Math.min(10000, a[1].num()));
            StringBuilder sb = new StringBuilder();
            String str = a[0].str();
            for (int i = 0; i < n; i++) {
                sb.append(str);
            }
            return sb.toString();
        }, STRING, NUMBER));
        s.define(Function.str("pad_left", true, a -> pad(a[0].str(), (int) a[1].num(), a[2].str(), true), STRING, NUMBER, STRING));
        s.define(Function.str("pad_right", true, a -> pad(a[0].str(), (int) a[1].num(), a[2].str(), false), STRING, NUMBER, STRING));
        s.define(Function.str("ellipsize", true, a -> {
            String str = a[0].str();
            int max = (int) Math.max(0, a[1].num());
            return str.length() <= max ? str : str.substring(0, max) + "...";
        }, STRING, NUMBER));
        s.define(Function.str("str", true, a -> a[0].asString(), NUMBER));
        s.define(Function.str("str", true, a -> a[0].asString(), BOOL));
        s.define(Function.str("fixed", true, a -> {
            int places = (int) clamp(a[1].num(), 0, 10);
            return String.format(Locale.ROOT, "%." + places + "f", a[0].num());
        }, NUMBER, NUMBER));
        s.define(Function.str("percent", true, a -> Math.round(clamp(a[0].num(), 0, 1) * 100) + "%", NUMBER));
        s.define(Function.str("if_else", true, a -> a[0].bool() ? a[1].str() : a[2].str(), BOOL, STRING, STRING));
        // pick('a|b|c', i): the i-th entry (wrapping) of a '|'-separated list.
        s.define(Function.str("pick", true, a -> {
            String[] items = a[0].str().split("\\|", -1);
            int i = (int) Math.floor(a[1].num());
            return items[((i % items.length) + items.length) % items.length];
        }, STRING, NUMBER));
        s.define(Function.num("count", true, a -> a[0].str().isEmpty() ? 0 : a[0].str().split("\\|", -1).length, STRING));
    }

    private static void colours(Scope s) {
        s.define(Function.num("rgb", true, a -> argb(255, a[0].num(), a[1].num(), a[2].num()), NUMBER, NUMBER, NUMBER));
        s.define(Function.num("rgba", true, a -> argb(a[3].num(), a[0].num(), a[1].num(), a[2].num()), NUMBER, NUMBER, NUMBER, NUMBER));
        s.define(Function.num("argb", true, a -> argb(a[0].num(), a[1].num(), a[2].num(), a[3].num()), NUMBER, NUMBER, NUMBER, NUMBER));
        s.define(Function.num("alpha", true, a -> ((long) a[0].num() >>> 24) & 0xFF, NUMBER));
        s.define(Function.num("red", true, a -> ((long) a[0].num() >>> 16) & 0xFF, NUMBER));
        s.define(Function.num("green", true, a -> ((long) a[0].num() >>> 8) & 0xFF, NUMBER));
        s.define(Function.num("blue", true, a -> (long) a[0].num() & 0xFF, NUMBER));
        // with_alpha(colour, 0..1): the same colour at the given opacity.
        s.define(Function.num("with_alpha", true, a -> {
            long c = (long) a[0].num() & 0xFFFFFFL;
            long alpha = Math.round(clamp(a[1].num(), 0, 1) * 255);
            return (double) ((alpha << 24) | c);
        }, NUMBER, NUMBER));
        // fade(colour, 0..1): multiplies the colour's existing alpha.
        s.define(Function.num("fade", true, a -> {
            long c = (long) a[0].num();
            long alpha = Math.round(((c >>> 24) & 0xFF) * clamp(a[1].num(), 0, 1));
            return (double) ((alpha << 24) | (c & 0xFFFFFFL));
        }, NUMBER, NUMBER));
        s.define(Function.num("mix", true, a -> mixColour((long) a[0].num(), (long) a[1].num(), clamp(a[2].num(), 0, 1)), NUMBER, NUMBER, NUMBER));
        s.define(Function.num("brighten", true, a -> {
            long c = (long) a[0].num();
            return mixColour(c, (c & 0xFF000000L) | 0xFFFFFFL, clamp(a[1].num(), 0, 1));
        }, NUMBER, NUMBER));
        s.define(Function.num("darken", true, a -> {
            long c = (long) a[0].num();
            return mixColour(c, c & 0xFF000000L, clamp(a[1].num(), 0, 1));
        }, NUMBER, NUMBER));
        // hsv(hue degrees, saturation 0..1, value 0..1), opaque.
        s.define(Function.num("hsv", true, a -> hsv(a[0].num(), a[1].num(), a[2].num()), NUMBER, NUMBER, NUMBER));
        s.define(Function.num("hex", true, a -> {
            Long c = parseHexColour(a[0].str());
            return c == null ? 0xFFFF00FFL : c;
        }, STRING));
    }

    // -------------------------------------------------------------------------------------

    /**
     * Parses {@code #RGB}, {@code #RRGGBB} or {@code #AARRGGBB} (the {@code #} is optional);
     * returns null when the text is not a colour. Colours without alpha are opaque.
     */
    public static Long parseHexColour(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.startsWith("#")) {
            t = t.substring(1);
        } else {
            return null;
        }
        for (int i = 0; i < t.length(); i++) {
            if (Character.digit(t.charAt(i), 16) < 0) {
                return null;
            }
        }
        switch (t.length()) {
            case 3: {
                long r = Character.digit(t.charAt(0), 16) * 17L;
                long g = Character.digit(t.charAt(1), 16) * 17L;
                long b = Character.digit(t.charAt(2), 16) * 17L;
                return 0xFF000000L | (r << 16) | (g << 8) | b;
            }
            case 6:
                return 0xFF000000L | Long.parseLong(t, 16);
            case 8:
                return Long.parseLong(t, 16);
            default:
                return null;
        }
    }

    static double clamp(double x, double lo, double hi) {
        return x < lo ? lo : (x > hi ? hi : x);
    }

    static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static int clampIndex(double i, int len) {
        return (int) clamp(Math.floor(i), 0, len);
    }

    private static String pad(String s, int width, String fill, boolean left) {
        if (fill.isEmpty() || s.length() >= width) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() + s.length() < width) {
            sb.append(fill);
        }
        sb.setLength(width - s.length());
        return left ? sb + s : s + sb;
    }

    private static double argb(double a, double r, double g, double b) {
        long al = Math.round(clamp(a, 0, 255));
        long rl = Math.round(clamp(r, 0, 255));
        long gl = Math.round(clamp(g, 0, 255));
        long bl = Math.round(clamp(b, 0, 255));
        return (double) ((al << 24) | (rl << 16) | (gl << 8) | bl);
    }

    public static double mixColour(long c1, long c2, double t) {
        double a = lerp((c1 >>> 24) & 0xFF, (c2 >>> 24) & 0xFF, t);
        double r = lerp((c1 >>> 16) & 0xFF, (c2 >>> 16) & 0xFF, t);
        double g = lerp((c1 >>> 8) & 0xFF, (c2 >>> 8) & 0xFF, t);
        double b = lerp(c1 & 0xFF, c2 & 0xFF, t);
        return argb(a, r, g, b);
    }

    private static double hsv(double h, double s, double v) {
        h = ((h % 360) + 360) % 360 / 60;
        s = clamp(s, 0, 1);
        v = clamp(v, 0, 1);
        int i = (int) Math.floor(h);
        double f = h - i;
        double p = v * (1 - s);
        double q = v * (1 - s * f);
        double t = v * (1 - s * (1 - f));
        double r;
        double g;
        double b;
        switch (i) {
            case 0:
                r = v; g = t; b = p;
                break;
            case 1:
                r = q; g = v; b = p;
                break;
            case 2:
                r = p; g = v; b = t;
                break;
            case 3:
                r = p; g = q; b = v;
                break;
            case 4:
                r = t; g = p; b = v;
                break;
            default:
                r = v; g = p; b = q;
                break;
        }
        return argb(255, r * 255, g * 255, b * 255);
    }

    /** SplitMix64 finaliser: a cheap, well-distributed 64-bit hash. */
    private static long hash(long x) {
        x += 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    private static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }
}
