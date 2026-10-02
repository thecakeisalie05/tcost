package io.github.thecakeisalie05.tcost.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CostVector {

    private final Map<String, Double> rawMaterials = new LinkedHashMap<>();
    private long totalEu;
    private long totalTicks;
    private int unresolved;

    public void addMaterial(String key, double amount) {
        if (amount <= 0) return;
        rawMaterials.put(key, rawMaterials.containsKey(key) ? rawMaterials.get(key) + amount : amount);
    }

    public void add(CostVector other, double multiplier) {
        for (Map.Entry<String, Double> e : other.rawMaterials.entrySet()) {
            addMaterial(e.getKey(), e.getValue() * multiplier);
        }
        totalEu += Math.round(other.totalEu * multiplier);
        totalTicks += Math.round(other.totalTicks * multiplier);
        unresolved += other.unresolved;
    }

    public Map<String, Double> rawMaterials() {
        return Collections.unmodifiableMap(rawMaterials);
    }

    public double materialUnits() {
        double total = 0;
        for (double value : rawMaterials.values()) total += value;
        return total;
    }

    public long totalEu() {
        return totalEu;
    }

    public long totalTicks() {
        return totalTicks;
    }

    public int unresolved() {
        return unresolved;
    }

    public void addEu(long eu) {
        totalEu += Math.max(0, eu);
    }

    public void addTicks(long ticks) {
        totalTicks += Math.max(0, ticks);
    }

    public void markUnresolved() {
        unresolved++;
    }
}
