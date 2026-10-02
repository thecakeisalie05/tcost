package io.github.thecakeisalie05.tcost.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import io.github.thecakeisalie05.tcost.model.OptimizationMode;
import io.github.thecakeisalie05.tcost.optimizer.OptimizationWeights;
import io.github.thecakeisalie05.tcost.optimizer.SearchLimits;

public final class TCostConfig {

    public boolean hoverEnabled = false;
    public OptimizationMode mode = OptimizationMode.MATERIALS;
    public final OptimizationWeights weights = new OptimizationWeights();
    public final SearchLimits limits = new SearchLimits();

    private final Configuration forgeConfig;

    private TCostConfig(Configuration forgeConfig) {
        this.forgeConfig = forgeConfig;
    }

    public static TCostConfig load(File file) {
        Configuration cfg = new Configuration(file);
        cfg.load();
        TCostConfig out = new TCostConfig(cfg);
        out.read();
        if (cfg.hasChanged()) cfg.save();
        return out;
    }

    public void read() {
        hoverEnabled = forgeConfig.getBoolean(
            "enableNeiHoverSummary",
            "display",
            false,
            "Show cached TCost summaries while hovering items in the NEI item panel.");

        String rawMode = forgeConfig.getString(
            "optimizationMode",
            "optimizer",
            OptimizationMode.MATERIALS.name(),
            "MATERIALS, ENERGY, TIME, or BALANCED.");

        try {
            mode = OptimizationMode.valueOf(rawMode.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            mode = OptimizationMode.MATERIALS;
        }

        weights.materials = forgeConfig.getFloat(
            "materialWeight", "optimizer", 1F, 0F, 1000000F, "Balanced mode material weight.");
        weights.energy = forgeConfig.getFloat(
            "energyWeight", "optimizer", 0F, 0F, 1000000F, "Balanced mode EU weight.");
        weights.time = forgeConfig.getFloat(
            "timeWeight", "optimizer", 0F, 0F, 1000000F, "Balanced mode time weight.");

        limits.maxDepth = forgeConfig.getInt(
            "maxDepth", "search", 18, 1, 128, "Maximum recursive recipe depth.");
        limits.maxExpandedNodes = forgeConfig.getInt(
            "maxExpandedNodes", "search", 5000, 100, 1000000, "Maximum expanded nodes per analysis.");
        limits.maxRecipesPerItem = forgeConfig.getInt(
            "maxRecipesPerItem", "search", 32, 1, 1024, "Maximum alternative recipes evaluated per item.");
    }

    public void save() {
        forgeConfig.get("display", "enableNeiHoverSummary", false).set(hoverEnabled);
        forgeConfig.get("optimizer", "optimizationMode", OptimizationMode.MATERIALS.name()).set(mode.name());
        forgeConfig.get("optimizer", "materialWeight", 1D).set(weights.materials);
        forgeConfig.get("optimizer", "energyWeight", 0D).set(weights.energy);
        forgeConfig.get("optimizer", "timeWeight", 0D).set(weights.time);
        forgeConfig.get("search", "maxDepth", 18).set(limits.maxDepth);
        forgeConfig.get("search", "maxExpandedNodes", 5000).set(limits.maxExpandedNodes);
        forgeConfig.get("search", "maxRecipesPerItem", 32).set(limits.maxRecipesPerItem);
        forgeConfig.save();
    }
}
