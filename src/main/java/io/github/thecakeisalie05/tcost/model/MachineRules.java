package io.github.thecakeisalie05.tcost.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MachineRules {

    private final Map<String, Integer> maxTierByMachine = new LinkedHashMap<>();

    public void setMaxTier(String machine, int tier) {
        maxTierByMachine.put(machine, tier);
    }

    public boolean allows(RecipeEdge recipe) {
        Integer maxTier = maxTierByMachine.get(recipe.machine);
        return maxTier != null && recipe.tier <= maxTier;
    }

    public Map<String, Integer> snapshot() {
        return Collections.unmodifiableMap(maxTierByMachine);
    }
}
