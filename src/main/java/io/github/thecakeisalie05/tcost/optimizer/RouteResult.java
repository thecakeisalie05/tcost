package io.github.thecakeisalie05.tcost.optimizer;

import io.github.thecakeisalie05.tcost.model.CostVector;

public final class RouteResult {

    public final CostVector cost;
    public final boolean bounded;
    public final int expandedNodes;

    public RouteResult(CostVector cost, boolean bounded, int expandedNodes) {
        this.cost = cost;
        this.bounded = bounded;
        this.expandedNodes = expandedNodes;
    }
}
