package dev.arkvisuals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/** Persisted client-only visual preferences. */
public final class VisualConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("arkvisuals.json");
    private static final int[] PALETTE = {0xA978F7, 0x45E8FF, 0xFF72D2, 0xA8F06A, 0xFFD166, 0x7C9CFF, 0xFFFFFF, 0xFF7B65};

    private final Map<VisualFeature, Boolean> features = new EnumMap<>(VisualFeature.class);
    private int paletteIndex = 0;
    private int density = 2;
    private int scale = 2;
    private int hudCorner = 0;

    public VisualConfig() {
        for (VisualFeature feature : VisualFeature.values()) features.put(feature, false);
        features.put(VisualFeature.MAGIC_TRAIL, true);
        features.put(VisualFeature.AURA_MOTES, true);
        features.put(VisualFeature.COORDINATES, true);
        features.put(VisualFeature.HOTBAR_ACCENT, true);
        features.put(VisualFeature.LOW_HEALTH_PULSE, true);
        features.put(VisualFeature.FOLIAGE_WHISPERS, true);
        features.put(VisualFeature.WATER_SHIMMER, true);
        features.put(VisualFeature.FAUNA_AURA, true);
    }

    public boolean enabled(VisualFeature feature) { return features.getOrDefault(feature, false); }
    public void toggle(VisualFeature feature) { features.put(feature, !enabled(feature)); save(); }
    public int color() { return PALETTE[Math.floorMod(paletteIndex, PALETTE.length)]; }
    public int paletteIndexForParticle() { return Math.floorMod(paletteIndex, PALETTE.length); }
    public String colorName() { return switch (Math.floorMod(paletteIndex, PALETTE.length)) {
        case 0 -> "Аметист"; case 1 -> "Лагуна"; case 2 -> "Орхидея"; case 3 -> "Мята";
        case 4 -> "Золото"; case 5 -> "Сумерки"; case 6 -> "Лунный"; default -> "Коралл";
    }; }
    public void nextColor() { paletteIndex = (paletteIndex + 1) % PALETTE.length; save(); }
    public int density() { return density; }
    public void cycleDensity() { density = (density + 1) % 4; save(); }
    public int scale() { return scale; }
    public void cycleScale() { scale = (scale + 1) % 4; save(); }
    public int hudCorner() { return hudCorner; }
    public void cycleHudCorner() { hudCorner = (hudCorner + 1) % 4; save(); }
    public String cornerName() { return new String[]{"Слева сверху", "Справа сверху", "Слева снизу", "Справа снизу"}[hudCorner]; }

    public void save() {
        JsonObject root = new JsonObject();
        JsonObject enabled = new JsonObject();
        features.forEach((feature, value) -> enabled.addProperty(feature.name(), value));
        root.add("features", enabled);
        root.addProperty("palette", paletteIndex);
        root.addProperty("density", density);
        root.addProperty("scale", scale);
        root.addProperty("hudCorner", hudCorner);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(root));
        } catch (IOException e) {
            System.err.println("[ArkVisuals] Не удалось сохранить настройки: " + e.getMessage());
        }
    }

    public static VisualConfig load() {
        VisualConfig config = new VisualConfig();
        if (!Files.exists(FILE)) return config;
        try {
            JsonObject root = GSON.fromJson(Files.readString(FILE), JsonObject.class);
            if (root == null) return config;
            if (root.has("features") && root.get("features").isJsonObject()) {
                JsonObject stored = root.getAsJsonObject("features");
                for (VisualFeature feature : VisualFeature.values())
                    if (stored.has(feature.name())) config.features.put(feature, stored.get(feature.name()).getAsBoolean());
            }
            config.paletteIndex = bounded(root, "palette", 0, PALETTE.length - 1);
            config.density = bounded(root, "density", 2, 3);
            config.scale = bounded(root, "scale", 2, 3);
            config.hudCorner = bounded(root, "hudCorner", 0, 3);
        } catch (Exception e) {
            System.err.println("[ArkVisuals] Конфиг поврежден; используются настройки по умолчанию: " + e.getMessage());
        }
        return config;
    }

    private static int bounded(JsonObject root, String key, int fallback, int max) {
        if (!root.has(key)) return fallback;
        try { return Math.max(0, Math.min(max, root.get(key).getAsInt())); }
        catch (RuntimeException ignored) { return fallback; }
    }
}
