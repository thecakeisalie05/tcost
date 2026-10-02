package io.github.thecakeisalie05.tcost.optimizer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.thecakeisalie05.tcost.api.RecipeIndex;
import io.github.thecakeisalie05.tcost.model.CostVector;
import io.github.thecakeisalie05.tcost.model.Ingredient;
import io.github.thecakeisalie05.tcost.model.MachineRules;
import io.github.thecakeisalie05.tcost.model.OptimizationMode;
import io.github.thecakeisalie05.tcost.model.RecipeEdge;

public final class RouteOptimizer {

    private final RecipeIndex index;
    private final MachineRules machines;
    private final SearchLimits limits;
    private final OptimizationWeights weights;

    private int expanded;
    private boolean bounded;

    public RouteOptimizer(
        RecipeIndex index,
        MachineRules machines,
        SearchLimits limits,
        OptimizationWeights weights) {
        this.index = index;
        this.machines = machines;
        this.limits = limits;
        this.weights = weights;
    }

    public RouteResult solve(String key, double amount, OptimizationMode mode) {
        expanded = 0;
        bounded = false;
        CostVector cost = solveInternal(key, amount, mode, 0, new HashSet<String>());
        return new RouteResult(cost, bounded, expanded);
    }

    private CostVector solveInternal(
        String key,
        double amount,
        OptimizationMode mode,
        int depth,
        Set<String> path) {

        if (amount <= 0) return new CostVector();

        if (index.isRawMaterial(key)) {
            CostVector raw = new CostVector();
            raw.addMaterial(key, amount);
            return raw;
        }

        if (depth >= limits.maxDepth || expanded >= limits.maxExpandedNodes || path.contains(key)) {
            bounded = true;
            CostVector unresolved = new CostVector();
            unresolved.addMaterial(key, amount);
            unresolved.markUnresolved();
            return unresolved;
        }

        List<RecipeEdge> recipes = index.recipesFor(key);
        CostVector best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        int considered = 0;

        Set<String> nextPath = new HashSet<>(path);
        nextPath.add(key);

        for (RecipeEdge recipe : recipes) {
            if (considered++ >= limits.maxRecipesPerItem) {
                bounded = true;
                break;
            }

            if (!machines.allows(recipe) || recipe.outputAmount <= 0) continue;

            expanded++;

            double operations = amount / recipe.outputAmount;
            CostVector candidate = new CostVector();
            candidate.addEu(Math.round(recipe.euPerOperation() * operations));
            candidate.addTicks(Math.round(recipe.durationTicks * operations));

            for (Ingredient input : recipe.inputs()) {
                CostVector child = solveInternal(
                    input.key,
                    input.amount * operations,
                    mode,
                    depth + 1,
                    nextPath);
                candidate.add(child, 1.0);
            }

            double score = score(candidate, mode);
            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }

        if (best != null) return best;

        CostVector unresolved = new CostVector();
        unresolved.addMaterial(key, amount);
        unresolved.markUnresolved();
        return unresolved;
    }

    private double score(CostVector c, OptimizationMode mode) {
        double penalty = c.unresolved() * weights.unresolved;
        switch (mode) {
            case MATERIALS:
                return c.materialUnits() + penalty;
            case ENERGY:
                return c.totalEu() + penalty;
            case TIME:
                return c.totalTicks() + penalty;
            case BALANCED:
            default:
                return c.materialUnits() * weights.materials
                    + c.totalEu() * weights.energy
                    + c.totalTicks() * weights.time
                    + penalty;
        }
    }
}
