package io.github.thecakeisalie05.tcost.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RecipeEdge {

    public final String id;
    public final String outputKey;
    public final double outputAmount;
    public final String machine;
    public final int tier;
    public final long eut;
    public final long durationTicks;

    private final List<Ingredient> inputs;

    public RecipeEdge(
        String id,
        String outputKey,
        double outputAmount,
        String machine,
        int tier,
        long eut,
        long durationTicks,
        List<Ingredient> inputs) {
        this.id = id;
        this.outputKey = outputKey;
        this.outputAmount = outputAmount;
        this.machine = machine;
        this.tier = tier;
        this.eut = eut;
        this.durationTicks = durationTicks;
        this.inputs = new ArrayList<>(inputs);
    }

    public List<Ingredient> inputs() {
        return Collections.unmodifiableList(inputs);
    }

    public long euPerOperation() {
        return Math.max(0L, eut) * Math.max(0L, durationTicks);
    }
}
