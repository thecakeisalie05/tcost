# TCost

TCost is a GT: New Horizons 2.9.0-beta-3 client-side recipe cost and route analysis mod.

## Goals

- Compute recursively-expanded raw material costs for craftable items.
- Respect a player-defined set of available machines and voltage tiers.
- Select routes by lowest raw material cost, lowest total EU, lowest completion time, or a weighted balanced score.
- Keep the configuration UI separate from NEI.
- Optionally show a compact cached cost summary while hovering items in NEI's right-side item panel.
- Never claim a globally optimal route when search bounds or unresolved recipes prevent that conclusion.

## Target

- Minecraft 1.7.10
- Forge 10.13.4.1614
- GT: New Horizons 2.9.0-beta-3

## Current status

Core route-analysis engine and persistent settings model are implemented. GT5U and NEI adapters are being kept isolated so beta-3-specific API work does not contaminate the optimizer core.
