package com.micatechnologies.minecraft.ldic.client.menu;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.EnumDifficulty;

/**
 * Game options by name, for themes: vanilla {@link GameSettings.Options} by enum name ({@code FOV},
 * {@code RENDER_DISTANCE}, {@code GUI_SCALE}, {@code VIEW_BOBBING}, ...) and sound volumes as
 * {@code sound:<category>} ({@code sound:master}, {@code sound:music}, ...).
 *
 * <p>Values go through vanilla's own setters, so stepping, side effects (a GUI scale change,
 * the sound engine picking up a volume) and saving behave exactly as on vanilla's screens.</p>
 */
public final class GameOptions {

    private GameOptions() {
    }

    private static GameSettings settings() {
        return Minecraft.getMinecraft().gameSettings;
    }

    private static SoundCategory sound(String name) {
        if (!name.toLowerCase(Locale.ROOT).startsWith("sound:")) {
            return null;
        }
        return SoundCategory.getByName(name.substring("sound:".length()).toLowerCase(Locale.ROOT));
    }

    private static GameSettings.Options option(String name) {
        try {
            return GameSettings.Options.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Whether {@code name} is an option LDIC knows. */
    public static boolean exists(String name) {
        return sound(name) != null || option(name) != null;
    }

    /**
     * The option as a number 0..1: a slider position for float options and volumes, 1/0 for
     * on/off options, and the 0-based choice index for the rest.
     */
    public static double value(String name) {
        SoundCategory s = sound(name);
        if (s != null) {
            return settings().getSoundLevel(s);
        }
        GameSettings.Options o = option(name);
        if (o == null) {
            return 0;
        }
        if (o.isFloat()) {
            return o.normalizeValue(settings().getOptionFloatValue(o));
        }
        if (o.isBoolean()) {
            return settings().getOptionOrdinalValue(o) ? 1 : 0;
        }
        // Multiple-choice options have no numeric view here; themes show them with option_text.
        return 0;
    }

    /** Sets a float option or volume from a 0..1 slider position. */
    public static void setFraction(String name, double fraction) {
        float f = (float) Math.max(0, Math.min(1, fraction));
        SoundCategory s = sound(name);
        if (s != null) {
            settings().setSoundLevel(s, f);
            return;
        }
        GameSettings.Options o = option(name);
        if (o != null && o.isFloat()) {
            float v = o.denormalizeValue(f);
            settings().setOptionFloatValue(o, v);
        }
    }

    /** Cycles an on/off or multiple-choice option, as clicking its vanilla button does. */
    public static void cycle(String name) {
        GameSettings.Options o = option(name);
        if (o != null && !o.isFloat()) {
            settings().setOptionValue(o, 1);
        }
    }

    public static boolean isFloat(String name) {
        GameSettings.Options o = option(name);
        return sound(name) != null || (o != null && o.isFloat());
    }

    /** The option's label and value as vanilla shows it ("FOV: 70", "Music: 100%"). */
    public static String text(String name) {
        SoundCategory s = sound(name);
        if (s != null) {
            float v = settings().getSoundLevel(s);
            String label = I18n.format("soundCategory." + s.getName());
            return label + ": " + (v == 0 ? I18n.format("options.off") : Math.round(v * 100) + "%");
        }
        GameSettings.Options o = option(name);
        return o == null ? name : settings().getKeyBinding(o);
    }

    public static void save() {
        settings().saveOptions();
    }

    public static boolean inWorld() {
        return Minecraft.getMinecraft().world != null;
    }

    /** Vanilla only lets the singleplayer host change difficulty, and not once it is locked. */
    public static boolean canChangeDifficulty() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.world != null && mc.isSingleplayer() && !mc.world.getWorldInfo().isHardcoreModeEnabled()
                && !mc.world.getWorldInfo().isDifficultyLocked();
    }

    public static String difficultyText() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            return "";
        }
        return I18n.format("options.difficulty") + ": " + I18n.format(mc.world.getDifficulty().getTranslationKey());
    }

    public static void cycleDifficulty() {
        if (!canChangeDifficulty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        mc.world.getWorldInfo().setDifficulty(EnumDifficulty.byId(mc.world.getDifficulty().getId() + 1));
    }
}
