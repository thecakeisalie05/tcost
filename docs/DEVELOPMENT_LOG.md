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

### Build attempts

1. Initial full CI build: failed resource processing because `mcmod.info` used `${version}` instead of `${modVersion}`; corrected.
2. Second full CI build: Java compilation and reobfuscation succeeded; required checks found wildcard imports and the executable regression runner was incorrectly placed in Gradle test sources with no discoverable unit tests. Imports were made explicit; the standalone suite was moved to `tests/core` and remains executed before every CI build.
3. Concurrent repository edits changed CI while builds ran; work was integrated onto current main without force pushing or removing the bootstrap core.
