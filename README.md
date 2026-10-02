# TCost

Client-side NEI raw-material route planner targeting **GTNH 2.9.0-beta-3** (Minecraft 1.7.10, GregTech 5.09.54.132, NEI 2.8.130-GTNH).

## Install and use

Download the normal JAR from Releases into your instance's `mods` directory. Do not install a `-dev`, `-sources`, or `-javadoc` JAR. No server installation is required.

- Hover an item in NEI and press **F8** to open a separate customization GUI for that item. F8 also opens general options in game. Remap it under Minecraft Controls.
- **Ctrl+F8** or **Rules → Hover raw costs** toggles the hover tooltip. It starts **off**, saves immediately, and requires no restart.
- **Machines:** click a tier to cycle from unavailable through ULV/LV/MV/etc. Click the count to cycle 1, 2, 4, 8, 16, 32, 64. Crafting and furnace start enabled; GregTech machines start unavailable. Rows are processing recipe maps, not detected blocks in your world.
- **Rules:** choose materials, total EU, base-speed time, balanced custom weights, or fewer recipe operations. Apply the target quantity and weights.
- **Materials:** search items/fluids, select one, mark it as a raw endpoint (stop expanding), and set its cost weight. Items default to cost 1 each; fluid defaults to cost 1 per bucket (0.001 per mB).
- **Recipes:** pin the target's preferred recipe. Clear it to let the evaluator choose. Special recipes need an explicit availability declaration with the Allow button. Inspect those requirements in NEI first.
- **Report:** raw quantities, total EU, base-speed time, unresolved inputs, batch surplus, and the chosen processing chain. Search, scroll, or page through long lists.

The NEI right-hand grid contains items, not unique recipes. Hover cost uses the configured profile and preferred/selected route for the hovered item. Preferences are applied recursively to ingredients. Use Materials to select an intermediate ingredient before choosing its preferred recipe.

## Calculation boundaries

The first alpha evaluates deterministic GregTech recipe maps and assembly-line recipes, standard shaped/shapeless crafting and ore-dictionary variants, and furnace recipes. Chance recipes and custom dynamic crafting handlers are skipped; their known outputs are marked unresolved rather than pretending they are raw resources. Other mod processing is outside this initial adapter. Inputs without an indexed producing recipe are assumed source materials; explicitly mark your intended stopping points.

The evaluator compares complete alternatives recursively under a bounded search. It is a **local route heuristic**, not a global minimum solver. It rounds whole batches, shows unused surplus, and does not reuse surplus across sibling branches, credit byproducts, or optimize multi-output production jointly. Results can therefore exceed the true globally minimal bill of materials.

Time is a **base-speed estimate**: processing stages are summed serially, identical machines can divide recipe batches, and ingredient branches are not globally scheduled. Machine tier filters recipe eligibility; it does not model overclocking, perfect overclocks, multiblock discounts, or custom parallel hatches. EU is base recipe EU including map amperage; vanilla furnace fuel is not included. Tools/non-consumed GT inputs are excluded from recurring consumption, and setup/infrastructure cost is not included. Container-item crafting requires explicit special approval and is conservatively counted without returned-container credits.

Metadata/special-value/special-item recipes require explicit approval rather than voltage alone implying a cleanroom, coils, temperature, research, etc. This declaration is per recipe; it does not verify physical machine conditions. Saved settings and preferences are global to the Minecraft instance at `config/tcost.json`. Search depth and visit bounds can also be edited there (defaults: 32 / 12000). Recipe preferences use hashed structural fingerprints and may need reselection after pack updates.

## Build

Use a JDK 25 build environment; Gradle/GTNH tooling provisions the Minecraft Java toolchain. Run `bash scripts/test-core.sh` and `./gradlew spotlessApply build`. Install the reobfuscated normal JAR from `build/libs`.

GitHub Actions runs the core regression checks, compiles/reobfuscates the mod, then creates the prerelease and attaches JAR artifacts. No release is published when compilation or core checks fail. The initial release still requires in-game testing; a successful build does not establish pack runtime compatibility.

Built for Cake / thecakeisalie05. Template/build tooling originates from GTNewHorizons ExampleMod1.7.10; see LICENSE and LICENSE-template.
