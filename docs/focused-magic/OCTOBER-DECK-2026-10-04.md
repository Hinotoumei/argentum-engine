# October deck repair verification

## Final source verification, October 4

### Manual startup correction

The user screenshot exposed `ai INVALID_ACTION: Mulligan phase not complete`. Earlier live acceptance auto-kept both seats and did not cover a human leaving the opening hand undecided. The client scheduled AI actions from opening-state resyncs before both seats finished mulligans. The provider now tracks each seat's mulliganComplete message, waits for both, requests fresh state, and gates automated/manual gameplay actions until that state arrives. London bottom-card selection also clears readiness and cancels scheduled actions.

Four startup regressions fail against the previous provider and pass with the repair; all 55 provider tests pass. Real Chrome against the deployed backend verifies all three profiles while the human opening hand remains pending: zero AI actions during the pause, actual Keep-button click, then both confirmations and an authoritative AI land play. The full 75-image/three-deck acceptance passed again afterward. See october-manual-mulligan-browser.json and the refreshed october-live-acceptance.json. No backend source or InuYasha code changed in this correction.

The replacement frontend is Focused_Magic_frontend_2026-10-04_mulligan_fix.zip, 804236 bytes, SHA256 e363bd3ad1f0b7e47d8f10c283e21d8ad13457c39951b199189da4fddf14e6dc. It must replace the previous frontend upload; public deployment of this correction remains unverified. The existing Drive file ID is retained.

Latest deployed acceptance: c4ce24869bbca3bbb11d315a9f8d5621938aa1be is live in dep-db1aitbncjis73bubmog. The updated local frontend against this deployed backend passed all 75 distinct October/mandatory/legacy printing image decodes, all three exact deck starts, confirmed authoritative AI land plays, real player life totals and hand state, and displayed hand images. Catalog admission also found no missing identities among the 84 names across all fixtures; that catalog result alone is not gameplay evidence. Full match completion is not asserted. See october-live-acceptance.json. The public Cloudflare frontend still requires upload and acceptance.

The final root-layout frontend ZIP is 803824 bytes, SHA256 f34bbfaeedded903af9c97dcb3b5d1e097932a767fa32f8821fef64d28de0905. CRC validation passed; the release manifest and deployment instructions now describe the direct-source backend and verified scopes. Drive file 1xiIehMm696gOSOpgMiH7051KjF2VZBFu was updated in place and its size and Software - Tikunari parent were read back successfully. InuYasha source is unchanged. Earlier package hashes below are preserved historical deliveries.

The printing CORS repair was published as 96771a71278767b7e742389c49714d3c26ae24a7 and deployment dep-db1aef2d0e5s73ers8tg reached live. All three exact deck profiles started and produced authoritative life totals, hand state, and confirmed AI land plays in the deck diagnosis. This diagnosis intentionally does not establish image acceptance: the browser's full image check found Library of Leng's stale image URL as the only failed asset. Scryfall's canonical LEA257 data supplies its replacement. The rebuilt snapshot/serialization/lint/counter gate passed in 2m36s, and the LEA snapshot diff changes only Library of Leng's imageUri. The failure is preserved in october-live-leng-image-failure.log. Final all-image acceptance remains required after publication and deployment.

The batch was published as 65408e0618b1c11f944b3dc267fcdc4543e98a68 and Render deployment dep-db1a4oc9v7es73el1ht0 reached live. The first updated-client acceptance exposed a missing cross-origin mapping for /api/printings; browser art requests failed although catalog admission was reachable. The read-only printing endpoint now has the same GET/OPTIONS policy as the card endpoints. Six real HTTP regressions cover ordinary, split, and comma-containing names from the public Pages origin and the local client. The complete server gate passed in 8m48s: 568 passed, 13 skipped, zero failures/errors. The original live failure is preserved in october-live-printing-cors-failure.log. Redeployed acceptance remains required.

The repair batch now has 83 passing dedicated gameplay cases: the earlier 54 and 29 additional support-card cases. Kasmina's corrupted type-line and loyalty punctuation were corrected. Her four scenarios verify printed subtype, scry, the granted ability on Way of the Pyromancer's Jace, Fractal counters, and the granted ultimate's activating-source color restriction and free cast.

The broad regression completed in 1h16m52s with only the stale compiled Kasmina subtype assertion failing. After rebuilding, the targeted support-card/snapshot/lint/counter gate passed in 11m17s, and the entire affected 2017–2022 card module passed 1,147 tests in 2m22s. All other modules passed the broad run. This is combined verification, not a claim that one full invocation passed. The accepted mtgish-only exclusion remains. See october-final-full-results.json for scopes and log hashes.

Semantic snapshot review found only the ten intended card changes across AKH, DGM, DIS, FUT, HOB, SHM, SOK, STX, TSP, and USG. Provider JavaScript syntax also passed. Deployed backend and frontend acceptance remain separate requirements; the historical checkpoints below do not establish deployment.

The uploaded October 3 list is preserved as a selectable 60-card, 30-identity profile. The printed split name is normalized to `Commit // Memory`. Other profiles remain available. Provider coverage checks use the selected deck's identities.

## Verified changes

- Native human modal targets, attackers, and optional blockers use the selection dialog. Blocking retains server-provided required assignments and block limits.
- Missing card art uses the provider's query endpoint for split-card names containing slashes and its single-card path endpoint for names containing commas. This avoids encoded-slash rejection and Spring's comma-separated query binding. Late image insertion preserves counters, creature statistics, and X controls.
- Notion Thief is a native Dragon's Maze definition. Draw replacement targets its source controller. The first-draw exception uses the active player's draw-step entry count; it no longer accidentally exempts the first draw of an instruction outside that step.

## Evidence

- Action control tests: 51 passed.
- Real Chrome selection test: uploaded deck count and identities, printing-data image fallback, counters/statistics/X preservation, discard confirmation and cancellation passed.
- Real Chrome against local Argentum: Faithless Looting draw/discard, Way of the Pyromancer granted loyalty/mana ability, and Prismari Command damage/Treasure modes and targets passed.
- Native backend: 7 Notion Thief, 10 Blood Scrivener, and 2 Faithless Looting scenarios passed. Gradle succeeded in 10m 46s. Notion cases cover both casters, opposing replacement sources, draw-step entry snapshots after earlier draws, Looting's subsequent discard, and source removal.
- Erebos's Intervention: five native scenarios passed, with X=0 and X=2 shrink/life, zero optional targets, two cards from different owners' graveyards, and rejection of three targets at X=1. Gradle succeeded in 6m 22s. Definition uses existing dynamic/modal/target vocabulary. Scryfall's official ruling about losing the creature target was retrieved and reviewed; a dedicated target-invalidation scenario is still needed.

## Outstanding

The final regression gate, publication of this batch, deployed backend acceptance, and frontend release remain outstanding. Commit // Memory now has a native definition and 21 passing gameplay scenarios. Erebos's Intervention has six passing scenarios. Source presence is not gameplay proof. The generated deck audit explicitly inventories source files, not card correctness. Earlier checkpoints below preserve the previous failures and outstanding work at those times.

The initial full gate stopped on three Syphon Mind failures. The user authorized their repair; the original three tests and two Library of Leng tests passed afterward. The subsequent full engine plus all card scenario gate completed successfully in 49m 22s: 16,825 tests, zero skips, failures, or errors. Erebos's Intervention now passes six scenarios, including invalidated creature targeting and no life gain. The catalog snapshot/serialization and lint gate then passed in 2m 22s. Reviewed snapshot diffs contain only the new Notion Thief and Erebos's Intervention trees in DGM and THB. The unrelated FUT snapshot is excluded from publication.

Verified UI changes were published to the existing fork branch at `b1dedb9c1e92b5f6efc147fc712aac218882b209`. The corresponding local commit is `8b8929b371`. Publication does not prove production deployment. Do not force-push the local branch: earlier API publication and local equivalent commits have different histories.

The browser tests above use the local provider. They do not establish deployed Render or Cloudflare acceptance. Full deck gameplay, broader engine regression gates, and live acceptance remain outstanding.

## Continuing October 4 repair

Commit // Memory is now implemented locally with an Aftermath face, separate face costs and timing, and stack-departure exile replacement. The canonical AKH printing and existing NCC reprint were checked against Scryfall. Eighteen earlier scenarios passed; expanded face-permission tests and final metadata remain under verification.

Inspection also found two actual Oracle mismatches: Bilbo incorrectly granted turn-long graveyard casting instead of a paid cast during its attack trigger; Gamble omitted random discard entirely. Both are corrected locally and require the current scenario gate. New separate scenarios cover Gamble, Lotus Petal, Infernal Tutor, One with Nothing, Keldon Megaliths, both shock lands, Sire of Insanity, Xander's Lounge and Starting Town.

The broad baseline gate finished in 36m43s and stopped at CounterTypeVocabularyTest: the recovered Dauthi Voidwalker uses VOID, absent from CounterType.KNOWN. VOID is now registered. The baseline engine and server suites passed, but this is not a final-code full-gate pass. Accepted mtgish-only failures remain excluded from the next broad gate.

The existing Render fork service has auto-deploy disabled and is still serving 894937bae23a7211bb91e98c350a086110d2830b. The public Cloudflare frontend also lacks the October deck and printing-image fallback. A reviewed frontend ZIP is ready at work/outputs/Focused_Magic_frontend_2026-10-04.zip; the upload route remains unavailable in this session. Neither backend publication nor local browser checks establish live completion.

### Current scenario and snapshot checkpoint

The expanded deck gate passed 52 native gameplay scenarios in 26m29s. Prismari Command subsequently passed two additional scenarios covering all four modes, bringing this batch to 54 cases. This includes 21 Commit // Memory and eight Bilbo cases. Card definition snapshots, serialization, lint, and counter vocabulary passed in the 6m28s release gate. Snapshot semantic review found changes only to Commit // Memory, Gamble, Bilbo, Infernal Tutor, Keldon Megaliths, One with Nothing, Sire of Insanity, and Prismari Command.

Canonical printing art is supplied for the five October deck definitions that lacked it. Prismari Command's printing rarity is corrected to rare. The Chrome image test now covers a split-card name with the query endpoint, alongside preserved counters, statistics and X. The 51 provider controls passed. These are local checks, not deployed acceptance.

The final full gate is running with only the accepted mtgish test exclusion. Its result must be recorded before publication. Fresh Assay AKH, HOB and USG comparisons report zero divergences among compared cards; parser declines and multi-face exclusions remain limitations, not passes.

All eight scoped Assay comparisons (AKH, HOB, USG, DIS, FUT, SOK, STX, DGM) finished with zero divergences. Direct explains decline Commit // Memory, Bilbo and Gamble; those declines do not validate their models. Dedicated gameplay scenarios provide the behavioral evidence. All five added canonical image URLs returned HTTP 200. The expanded Chrome test covers Bilbo's comma-containing name using actual provider printing data, and the 51 provider control tests passed again.

The current frontend package is `Focused_Magic_frontend_2026-10-04_card_art.zip`, 805767 bytes, SHA256 `c47927d104d171db65cab5388123ad1fa890b6cf7e43721fb2bc16c1dffecde8`. Archive CRC and root layout were checked. It is uploaded to the existing Software - Tikunari folder under Handoffs at https://drive.google.com/file/d/1xiIehMm696gOSOpgMiH7051KjF2VZBFu/view . Drive metadata readback confirms the file size and parent. This is a deliverable, not a Cloudflare deployment claim.

### Preserved final-gate failure and repair

The first final gate failed after 31m14s at two SDK checks: CounterTypeClientMirrorTest required VOID in the browser enum/display map, and CounterTypeTest's explicit post-legacy kind set omitted VOID. Both omissions came from the new vocabulary entry and are repaired. The printed spelling now has an explicit `void` assertion. AI, server and gym tests passed before the stop. The original failure log is preserved as `work/tooling/october-final-full-counter-failure.log`; this run is not a full-gate pass.

The older-profile image audit found Gemstone Caverns, Kasmina, Enigma Sage and Manamorphose also lacked image metadata and provider printings with art. Their canonical TSP274, STX196 and SHM211 printing artist/image metadata is now supplied. The fresh full gate runs SDK checks first and regenerates the expected metadata snapshots; semantic snapshot review remains required before publication. No gameplay assertion was weakened or excluded for these repairs.
