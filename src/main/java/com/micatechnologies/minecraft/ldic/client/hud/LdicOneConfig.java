package com.micatechnologies.minecraft.ldic.client.hud;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Button;
import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.HUD;
import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.core.ConfigUtils;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.Tags;
import com.micatechnologies.minecraft.ldic.client.LdicClient;
import com.micatechnologies.minecraft.ldic.client.LdicSettings;
import com.micatechnologies.minecraft.ldic.client.hud.elements.ActiveEffectsHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.ArmourHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.BiomeHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.BlockAboveHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.ClickCounterHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.CompassHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.CoordsHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.CpsHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.CustomTextHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.DayCounterHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.DirectionHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.DurabilityHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.EffectsHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.FpsHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.FtiHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.GameModeHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.GearHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.HealthHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.HeightLimitHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.HungerHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.LightLevelHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.LookingAtBlockHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.MemoryHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.PerfGraphHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.PingHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.PitchHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.PlaceCountHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.PlaytimeHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.RealLifeDateHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.ResourcePackHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.SaturationHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.ServerIpHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.SpeedHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.TimeHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.TpsHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.VitalsHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.XpHud;
import com.micatechnologies.minecraft.ldic.client.hud.elements.YawHud;
import com.micatechnologies.minecraft.ldic.client.hud.presets.HudPresets;
import com.micatechnologies.minecraft.ldic.theme.Json;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * LDIC's OneConfig page: the hotbar and inventory settings, and the HUD elements (Decision 16:
 * the general-purpose ones moved here from SUM; SUM keeps the ones that read its server data).
 *
 * <p>Hotbar and inventory settings live in {@code config/ldic/player.json} (with the pack's
 * defaults in settings.json), which LDIC's own Config screen also edits. This page mirrors
 * them: its fields are loaded from {@link LdicSettings} after OneConfig reads its file, and
 * every change here is written straight back. HUD positions and looks are OneConfig's, in
 * {@code ldic.json} under the OneConfig profile.</p>
 *
 * <p>HUD field names are the ones SUM used, so a player's saved layout moves over: on the first
 * boot with no {@code ldic.json}, the matching entries are copied from SUM's {@code sum.json}.</p>
 *
 * <p>Only constructed when OneConfig is present ({@link OneConfigSupport}).</p>
 */
public class LdicOneConfig extends Config {

    /** {@code transient}: OneConfig's Gson skips transient fields only, and a static self-reference would recurse. */
    public static transient LdicOneConfig INSTANCE;

    private static final transient String[] STYLE_KEYS = {"modern", "vanilla"};

    // ---------------------------------------------------------------- hotbar and inventory

    @Dropdown(name = "Style", category = "Hotbar", options = {"Modern", "Vanilla"})
    public int hotbarStyle = 0;

    @Slider(name = "Size (%)", category = "Hotbar", min = 50, max = 200, step = 5)
    public float hotbarScale = 100;

    @Switch(name = "Auto-hide", category = "Hotbar", subcategory = "Auto-hide",
            description = "Slide the hotbar away once you have been moving for a moment; slot changes, pickups and damage bring it back.")
    public boolean hotbarAutoHide = false;

    @Slider(name = "Hide After Moving (Seconds)", category = "Hotbar", subcategory = "Auto-hide", min = 0, max = 10)
    public float hotbarHideDelay = 2;

    @Switch(name = "Full Screen", category = "Inventory",
            description = "Creative's item grid fills the screen; other inventories scale up.")
    public boolean inventoryFullScreen = false;

    @Switch(name = "Scale Other Mods' Screens", category = "Inventory",
            description = "Also scale up other mods' inventories in full-screen mode.")
    public boolean inventoryScaleModScreens = false;

    @Switch(name = "Themed Inventories", category = "Inventory",
            description = "Draw inventories with the menu theme's inventory screens, when it has them.")
    public boolean inventoryThemed = true;

    // ------------------------------------------------------------------------------- HUDs

    @HUD(name = "Coordinates", category = "HUDs", subcategory = "Information")
    public CoordsHud coordsHud = new CoordsHud();

    @HUD(name = "FPS", category = "HUDs", subcategory = "Performance")
    public FpsHud fpsHud = new FpsHud();

    @HUD(name = "Facing Direction", category = "HUDs", subcategory = "Information")
    public DirectionHud directionHud = new DirectionHud();

    @HUD(name = "Clock", category = "HUDs", subcategory = "Information")
    public TimeHud timeHud = new TimeHud();

    @HUD(name = "Biome", category = "HUDs", subcategory = "Information")
    public BiomeHud biomeHud = new BiomeHud();

    @HUD(name = "Day Counter", category = "HUDs", subcategory = "Information")
    public DayCounterHud dayCounterHud = new DayCounterHud();

    @HUD(name = "Real-Life Date", category = "HUDs", subcategory = "Information")
    public RealLifeDateHud realLifeDateHud = new RealLifeDateHud();

    @HUD(name = "Server IP", category = "HUDs", subcategory = "Information")
    public ServerIpHud serverIpHud = new ServerIpHud();

    @HUD(name = "Resource Pack", category = "HUDs", subcategory = "Information")
    public ResourcePackHud resourcePackHud = new ResourcePackHud();

    @HUD(name = "Game Mode", category = "HUDs", subcategory = "Information")
    public GameModeHud gameModeHud = new GameModeHud();

    @HUD(name = "Block Above", category = "HUDs", subcategory = "Information")
    public BlockAboveHud blockAboveHud = new BlockAboveHud();

    @HUD(name = "Height Limit", category = "HUDs", subcategory = "Information")
    public HeightLimitHud heightLimitHud = new HeightLimitHud();

    @HUD(name = "Looking-At Block", category = "HUDs", subcategory = "Information")
    public LookingAtBlockHud lookingAtBlockHud = new LookingAtBlockHud();

    @HUD(name = "Light Level", category = "HUDs", subcategory = "Information")
    public LightLevelHud lightLevelHud = new LightLevelHud();

    // Player-state HUDs

    @HUD(name = "Pitch", category = "HUDs", subcategory = "Player")
    public PitchHud pitchHud = new PitchHud();

    @HUD(name = "Yaw", category = "HUDs", subcategory = "Player")
    public YawHud yawHud = new YawHud();

    @HUD(name = "Speed", category = "HUDs", subcategory = "Player")
    public SpeedHud speedHud = new SpeedHud();

    @HUD(name = "Saturation", category = "HUDs", subcategory = "Player")
    public SaturationHud saturationHud = new SaturationHud();

    @HUD(name = "Armor Durability", category = "HUDs", subcategory = "Player")
    public ArmourHud armourHud = new ArmourHud();

    @HUD(name = "Health", category = "HUDs", subcategory = "Player")
    public HealthHud healthHud = new HealthHud();

    @HUD(name = "Hunger", category = "HUDs", subcategory = "Player")
    public HungerHud hungerHud = new HungerHud();

    @HUD(name = "Experience", category = "HUDs", subcategory = "Player")
    public XpHud xpHud = new XpHud();

    @HUD(name = "Active Effects", category = "HUDs", subcategory = "Player")
    public ActiveEffectsHud activeEffectsHud = new ActiveEffectsHud();

    @HUD(name = "Tool Durability", category = "HUDs", subcategory = "Player")
    public DurabilityHud durabilityHud = new DurabilityHud();

    // Performance HUDs

    @HUD(name = "Memory", category = "HUDs", subcategory = "Performance")
    public MemoryHud memoryHud = new MemoryHud();

    @HUD(name = "Ping", category = "HUDs", subcategory = "Performance")
    public PingHud pingHud = new PingHud();

    @HUD(name = "TPS", category = "HUDs", subcategory = "Performance")
    public TpsHud tpsHud = new TpsHud();

    @HUD(name = "Frame Time", category = "HUDs", subcategory = "Performance")
    public FtiHud ftiHud = new FtiHud();

    // Counter HUDs — backed by HudStateTracker (registered by OneConfigSupport).

    @HUD(name = "CPS", category = "HUDs", subcategory = "Counters")
    public CpsHud cpsHud = new CpsHud();

    @HUD(name = "Click Counter", category = "HUDs", subcategory = "Counters")
    public ClickCounterHud clickCounterHud = new ClickCounterHud();

    @HUD(name = "Blocks Placed", category = "HUDs", subcategory = "Counters")
    public PlaceCountHud placeCountHud = new PlaceCountHud();

    @HUD(name = "Session Playtime", category = "HUDs", subcategory = "Counters")
    public PlaytimeHud playtimeHud = new PlaytimeHud();

    // Ten fixed custom-text slots. Each is independently positioned, scaled, and
    // shown/hidden via OneConfig's standard HUD UI; the @Text option on each lets the
    // user paint whatever string they want — including %placeholder% tokens like
    // %player%, %x%, %y%, %z%, %dim%, %biome%, %server%, %time%, %realtime%, %date%,
    // %fps%, %direction%. See TextPlaceholders for the full catalog.
    //
    // A fixed-N pattern (rather than the upstream dynamic HudList) keeps the OneConfig
    // wire-up simple — no custom option type for dynamic-sized lists. With placeholder
    // substitution, ten slots cover ~all realistic use cases since one slot can render
    // arbitrarily many live values.

    // Drawn HUDs: the same information, more visual and more compact. Colours follow the menu
    // theme's HUD colours.

    @HUD(name = "Compass Tape", category = "HUDs", subcategory = "Visual")
    public CompassHud compassHud = new CompassHud();

    @HUD(name = "Vitals Bars", category = "HUDs", subcategory = "Visual")
    public VitalsHud vitalsHud = new VitalsHud();

    @HUD(name = "Gear Durability", category = "HUDs", subcategory = "Visual")
    public GearHud gearHud = new GearHud();

    @HUD(name = "Effect Rings", category = "HUDs", subcategory = "Visual")
    public EffectsHud effectsHud = new EffectsHud();

    @HUD(name = "Frame Time Graph", category = "HUDs", subcategory = "Visual")
    public PerfGraphHud perfGraphHud = new PerfGraphHud();

    @HUD(name = "Custom Text 1", category = "HUDs", subcategory = "Custom Text")
    public CustomTextHud customText1 = new CustomTextHud();

    @HUD(name = "Custom Text 2", category = "HUDs", subcategory = "Custom Text")
    public CustomTextHud customText2 = new CustomTextHud();

    @HUD(name = "Custom Text 3", category = "HUDs", subcategory = "Custom Text")
    public CustomTextHud customText3 = new CustomTextHud();

    @HUD(name = "Custom Text 4", category = "HUDs", subcategory = "Custom Text")
    public CustomTextHud customText4 = new CustomTextHud();

    @HUD(name = "Custom Text 5", category = "HUDs", subcategory = "Custom Text")
    public CustomTextHud customText5 = new CustomTextHud();

    @HUD(name = "Custom Text 6", category = "HUDs", subcategory = "Custom Text")
    public CustomTextHud customText6 = new CustomTextHud();

    @HUD(name = "Custom Text 7", category = "HUDs", subcategory = "Custom Text")
    public CustomTextHud customText7 = new CustomTextHud();

    @HUD(name = "Custom Text 8", category = "HUDs", subcategory = "Custom Text")
    public CustomTextHud customText8 = new CustomTextHud();

    @HUD(name = "Custom Text 9", category = "HUDs", subcategory = "Custom Text")
    public CustomTextHud customText9 = new CustomTextHud();

    @HUD(name = "Custom Text 10", category = "HUDs", subcategory = "Custom Text")
    public CustomTextHud customText10 = new CustomTextHud();

    // ---------------------------------------------------------------------------- presets
    // The option literals MUST match HudPresets.STYLE_NAMES / LAYOUT_NAMES in order (and SUM's
    // copy, so one preset name means the same place in both mods).

    @Dropdown(name = "Style Preset", category = "HUD Presets", subcategory = "Style",
            options = {
                "Default", "Minimal Brackets", "Clean Professional", "Stylish Glass",
                "Compact Light", "Retro Terminal", "Realistic Game HUD", "High Contrast",
                "Theme", "Compact Glass", "Compact Pill"
            })
    public int stylePresetIndex = HudPresets.styleIndexOf("Realistic Game HUD");

    @Button(name = "Apply Style", text = "Apply", category = "HUD Presets", subcategory = "Style")
    public void applyStylePreset() {
        HudPresets.applyStyle(stylePresetIndex, this);
    }

    @Dropdown(name = "Layout Preset", category = "HUD Presets", subcategory = "Layout",
            options = {
                "Off (All Hidden)", "Vanilla+", "Survival Essentials", "Speedrunner",
                "PvP Focus", "Builder", "Explorer", "Performance Watcher", "Time Tracker",
                "Minimal Top", "Server Op", "Urban Builder", "City Explorer", "Architect",
                "Cityscape Photographer", "Explorer 2 (SUM)", "Combat Pro", "Visual Compact"
            })
    public int layoutPresetIndex = HudPresets.layoutIndexOf("Explorer 2 (SUM)");

    @Button(name = "Apply Layout", text = "Apply", category = "HUD Presets", subcategory = "Layout")
    public void applyLayoutPreset() {
        HudPresets.applyLayout(layoutPresetIndex, this);
    }

    public LdicOneConfig() {
        // The Forge mod's own name: OneConfig pairs its pages with Forge mods by name, and would
        // otherwise list LDIC twice (this page, and a card for the Mods-list Config screen).
        super(new Mod(Tags.MODNAME, ModType.UTIL_QOL), "ldic.json");
        INSTANCE = this;
        // Only observable before initialize(), which writes a file when there is none.
        boolean firstBoot = !ConfigUtils.getProfileFile(configFile).exists();
        boolean migrated = firstBoot && migrateFromSum();
        initialize();
        if (firstBoot && !migrated) {
            // Fresh install, no SUM layout to keep: the pack-standard presets.
            HudPresets.applyStyle(stylePresetIndex, this);
            HudPresets.applyLayout(layoutPresetIndex, this);
        }
        pullFromSettings();
        addDependency("hotbarHideDelay", "hotbarAutoHide");
        for (String option : new String[] {"hotbarStyle", "hotbarScale", "hotbarAutoHide", "hotbarHideDelay",
                "inventoryFullScreen", "inventoryScaleModScreens", "inventoryThemed"}) {
            addListener(option, this::pushToSettings);
        }
    }

    /** Shows the current player.json values (after LDIC's own screen or a toggle changed them). */
    public void pullFromSettings() {
        LdicSettings s = LdicClient.settings();
        if (s == null) {
            return;
        }
        hotbarStyle = "vanilla".equals(s.hotbarStyle) ? 1 : 0;
        hotbarScale = (float) (s.hotbarScale * 100);
        hotbarAutoHide = s.hotbarAutoHide;
        hotbarHideDelay = (float) s.hotbarHideDelay;
        inventoryFullScreen = s.inventoryFullScreen;
        inventoryScaleModScreens = s.inventoryScaleModScreens;
        inventoryThemed = s.inventoryThemed;
    }

    private void pushToSettings() {
        LdicSettings s = LdicClient.settings();
        if (s == null) {
            return;
        }
        s.hotbarStyle = STYLE_KEYS[Math.max(0, Math.min(STYLE_KEYS.length - 1, hotbarStyle))];
        s.hotbarScale = Math.max(0.5, Math.min(2, Math.round(hotbarScale) / 100.0));
        s.hotbarAutoHide = hotbarAutoHide;
        s.hotbarHideDelay = Math.round(hotbarHideDelay * 10) / 10.0;
        s.inventoryFullScreen = inventoryFullScreen;
        s.inventoryScaleModScreens = inventoryScaleModScreens;
        s.inventoryThemed = inventoryThemed;
        s.saveHotbarAndInventory(LdicClient.configDir());
    }

    /**
     * Copies this page's HUDs (and the selected presets) from SUM's {@code sum.json}, where they
     * lived before, into a new {@code ldic.json}; true when there was something to copy.
     */
    private boolean migrateFromSum() {
        File sum = ConfigUtils.getProfileFile("sum.json");
        if (!sum.isFile()) {
            return false;
        }
        try {
            JsonElement root = Json.parse(new String(Files.readAllBytes(sum.toPath()), StandardCharsets.UTF_8));
            if (!root.isJsonObject()) {
                return false;
            }
            JsonObject from = root.getAsJsonObject();
            JsonObject to = new JsonObject();
            for (Field f : LdicOneConfig.class.getDeclaredFields()) {
                boolean wanted = f.isAnnotationPresent(HUD.class)
                        || f.getName().equals("stylePresetIndex") || f.getName().equals("layoutPresetIndex");
                if (wanted && from.has(f.getName())) {
                    to.add(f.getName(), from.get(f.getName()));
                }
            }
            if (to.size() == 0) {
                return false;
            }
            File target = ConfigUtils.getProfileFile(configFile);
            target.getParentFile().mkdirs();
            Files.write(target.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(to).getBytes(StandardCharsets.UTF_8));
            Ldic.LOGGER.info("LDIC: moved {} HUD settings from SUM's sum.json to ldic.json", to.size());
            return true;
        } catch (IOException | RuntimeException e) {
            Ldic.LOGGER.warn("LDIC: could not read SUM's HUD settings from {}; starting with the default HUD layout", sum, e);
            return false;
        }
    }

    /** As SUM found: OneConfig drops STATIC from Gson's exclusions, and HUDs' static helpers then recurse on save. */
    @Override
    protected GsonBuilder addGsonOptions(GsonBuilder builder) {
        return super.addGsonOptions(builder).excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT);
    }
}
