# October deck repair verification

The uploaded October 3 list is preserved as a selectable 60-card, 30-identity profile. The printed split name is normalized to `Commit // Memory`. Other profiles remain available. Provider coverage checks use the selected deck's identities.

## Verified changes

- Native human modal targets, attackers, and optional blockers use the selection dialog. Blocking retains server-provided required assignments and block limits.
- Missing card art is fetched from the provider's real `/api/cards/{name}/printings` endpoint. Late image insertion preserves counters, creature statistics, and X controls.
- Notion Thief is a native Dragon's Maze definition. Draw replacement targets its source controller. The first-draw exception uses the active player's draw-step entry count; it no longer accidentally exempts the first draw of an instruction outside that step.

## Evidence

- Action control tests: 51 passed.
- Real Chrome selection test: uploaded deck count and identities, printing-data image fallback, counters/statistics/X preservation, discard confirmation and cancellation passed.
- Real Chrome against local Argentum: Faithless Looting draw/discard, Way of the Pyromancer granted loyalty/mana ability, and Prismari Command damage/Treasure modes and targets passed.
- Native backend: 7 Notion Thief, 10 Blood Scrivener, and 2 Faithless Looting scenarios passed. Gradle succeeded in 10m 46s. Notion cases cover both casters, opposing replacement sources, draw-step entry snapshots after earlier draws, Looting's subsequent discard, and source removal.

## Outstanding

Erebos's Intervention and Commit // Memory were absent from the prior source inventory. Erebos implementation and scenarios are in progress. Commit // Memory needs proper Aftermath restrictions, library placement, and both-face gameplay tests. Source presence is not gameplay proof. The generated deck audit explicitly inventories source files, not card correctness.

The browser tests above use the local provider. They do not establish deployed Render or Cloudflare acceptance. Full deck gameplay, broader engine regression gates, and live acceptance remain outstanding.
