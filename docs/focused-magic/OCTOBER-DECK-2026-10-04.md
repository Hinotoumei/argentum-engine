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
- Erebos's Intervention: five native scenarios passed, with X=0 and X=2 shrink/life, zero optional targets, two cards from different owners' graveyards, and rejection of three targets at X=1. Gradle succeeded in 6m 22s. Definition uses existing dynamic/modal/target vocabulary. Scryfall's official ruling about losing the creature target was retrieved and reviewed; a dedicated target-invalidation scenario is still needed.

## Outstanding

Erebos's Intervention and Commit // Memory were absent from the prior source inventory. Erebos now has a compiled definition and five passing scenarios; its broader acceptance is incomplete. Commit // Memory remains absent and needs proper Aftermath restrictions, library placement, and both-face gameplay tests. Source presence is not gameplay proof. The generated deck audit explicitly inventories source files, not card correctness.

The initial full gate stopped on three Syphon Mind failures. The user authorized their repair; the original three tests and two Library of Leng tests passed afterward. The subsequent full engine plus all card scenario gate completed successfully in 49m 22s: 16,825 tests, zero skips, failures, or errors. Erebos's Intervention now passes six scenarios, including invalidated creature targeting and no life gain. The catalog snapshot/serialization and lint gate then passed in 2m 22s. Reviewed snapshot diffs contain only the new Notion Thief and Erebos's Intervention trees in DGM and THB. The unrelated FUT snapshot is excluded from publication.

Verified UI changes were published to the existing fork branch at `b1dedb9c1e92b5f6efc147fc712aac218882b209`. The corresponding local commit is `8b8929b371`. Publication does not prove production deployment. Do not force-push the local branch: earlier API publication and local equivalent commits have different histories.

The browser tests above use the local provider. They do not establish deployed Render or Cloudflare acceptance. Full deck gameplay, broader engine regression gates, and live acceptance remain outstanding.
