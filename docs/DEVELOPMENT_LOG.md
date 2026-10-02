# Development record

## 2026-10-02 — Initial TCost alpha

- Task: raw material totals accessible from NEI in GTNH 2.9 beta 3.
- Human product decisions: separate customization screen; per-machine voltage tiers; optional hover costs with immediate toggle; objectives for time, EU, materials and other routes; repository `tcost`; JAR delivered in GitHub Release.
- Agent: Codex in ChatGPT Work. No delegated agents.
- Implementation: exact pack-version source inspection; data-only bounded recursive route engine; GregTech/crafting/furnace indexing; profile persistence; asynchronous cached tooltip queries; independent options GUI; build/release CI.
- Validation: core regression suite and CI compilation/reobfuscation; live pack runtime acceptance remains for Connor.
- Environment intervention: initial local Gradle download could not connect from Java. Source retrieval via GitHub worked. CI provides the full dependency/build environment.
- Known limitations and scope boundaries are described in README and release notes rather than hidden by a cheapest-route claim.
- Final acceptance: pending in-game user validation.
