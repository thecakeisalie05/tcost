package io.github.thecakeisalie05.tcost.api;

import java.util.List;
import io.github.thecakeisalie05.tcost.model.RecipeEdge;

public interface RecipeIndex {
    List<RecipeEdge> recipesFor(String outputKey);
    boolean isRawMaterial(String key);
}
