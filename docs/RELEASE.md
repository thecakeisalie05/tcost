TCost alpha for GTNH 2.9.0-beta-3.

- Separate F8 options GUI: machine tier/count availability, optimization rules, material weights/raw endpoints, preferred recipes, and full report.
- Optional NEI item-grid raw-cost tooltip; disabled by default and toggleable with Ctrl+F8 or the Rules page.
- Deterministic GregTech, standard crafting/ore-dictionary, and furnace recipe adapters; bounded route evaluation with cycles, tier filtering, batch quantities, and unresolved-output handling.
- Materials, total EU, base-speed time, balanced weights, and recipe-operation objectives.

Install only the normal mod JAR in your client's mods directory. No server installation is required.

This is an initial alpha: compiled and regression-checked by CI, but not tested interactively in a running GTNH instance. Optimization is a bounded local heuristic. Time/EU use base recipe values; no overclocking, fuel costing, global scheduling, byproduct credits, or cross-branch surplus reuse. Recipes with special metadata require explicit availability approval. See README for full boundaries.
