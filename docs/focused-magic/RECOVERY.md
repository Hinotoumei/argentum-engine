# Focused Magic source recovery

Recovered on 2026-09-29 from the user-supplied v0.6.9 archive.

- Original ZIP: `recovery/original-v0.6.9.zip`
- ZIP SHA-256: `81bec2d917515286dc55c04784003918bab584a07af697d27b6d5a3284a272e1`
- Decoded deployed payload: `recovery/deployed-payload/`
- Decoded archive SHA-256: `5dffa810521a6ac588f552eae186521e787328c326b440c6d4921d8c59de777d`
- Original deployment pin: `e4fcc1621812670c58c001a80df9c71eced69a1c`
- Fork starting commit: `842023e60cfd25075c9a6a7f77945515112496d8`
- Destination: existing `Hinotoumei/argentum-engine` branch `focused-magic`.

The original archive preserves the frontend, exact deck fixtures, historical deployment recipe, and original patches. The decoded deployed payload preserves the exact backend patch and ten overrides. Those backend changes have been reconstructed as direct source changes in this checkout. The original Base64 payload is evidence only; it is not a build input for the fork.

Recovery is not gameplay acceptance. The recovered Gemstone implementation contains card-specific engine logic and an unreported counter mutation that require repair. Scalding Tarn and Faithless Looting already have native definitions; their gameplay and Focused Magic adapter decisions still require behavioral tests. Scope remains the exact Modern 60 versus Dimir Tempo 60 main decks, with 42 unique identities, as recorded in the latest checkpoint.

No upstream writes or Render changes have been made. The September 29 recovered baseline passed the project build excluding the accepted legacy `:mtgish-tooling:test` limitation. That baseline result does not verify subsequent September 30 changes or establish live gameplay acceptance.

## September 30 continuation

The newer repair packet is preserved in `recovery/2026-09-30/`; its complete ZIP has SHA-256 `19b611fdc55d6b23680711f3024d1d88c28af3d7ec7391ebc9206dde04455eb9`. The prepared frontend is direct source under `focused-magic-app/`. Archive preservation and frontend adoption were pushed to the existing branch in commit `ac5eb85c7f`.

The September 30 Magic regression build passed (`build -x :mtgish-tooling:test`), retaining the accepted legacy mtgish-only exclusion. The final test-only strengthening for normal Caverns land play also passed all twelve Caverns scenarios:

- Force of Will: three alternate-cost scenarios passed, including rejection of exiling the spell itself and a nonblue card. The self-exile rejection repairs cost validation. Assay reads the counter line but declines the alternate-cost line; this is not a whole-card Assay pass.
- Daze: Island return and both payment outcomes passed; a Forest cannot pay the alternate cost. Assay declined its reading, so scenario evidence remains necessary.
- Gray Ogre: the existing printed card casts and deals two unblocked combat damage in its dedicated scenario. The packet's historical `Grey Ogre` spelling remains in the preserved archive.
- Gemstone Caverns: all twelve scenarios passed on retry, including the one-card-hand and exiled-second-Caverns edge cases. The initial full-catalog fixture timed out in its first scenario; the retry uses only the relevant cards. Opening-hand options are now generic source data, and both SDK and suspended-game serialization checks passed. The 4,043-test engine suite and complete 2,030-test 2003–2007 card scenario suite passed, including the existing Leyline of the Void scenarios. The complete later-era regression suites also passed.
- Scalding Tarn: all four scenarios passed, covering Island, Mountain and dual-land eligibility, fail-to-find, activation life/sacrifice costs, invalid selection rejection, untapped entry and shuffling.
- Faithless Looting: both normal-cast and flashback scenarios passed, proving newly drawn cards are available to discard and that the flashback spell is exiled.

Consign to Memory now has a native MH3 definition and repeatable replicate payment, enumeration, client submission and copy resolution. Sixteen mechanic scenarios and four card scenarios passed, including copying a spell whose original was countered, retaining its latest targets, and serializing suspended copy decisions. The complete build with refreshed Assay installation passed in 1 hour 28 minutes 31 seconds; see `REPLICATE-VERIFICATION.md` and `replicate-test-results.json` for coverage and explicit limits. Assay declines both Consign lines and does not establish whole-card agreement. The other remaining September 30 card matrix, provider decisions in real games, and live acceptance remain unfinished. A card's presence in the corpus or registry is not gameplay verification. The printed InuYasha color and basic direct-attack repair was published in commit `13790f53eb`. Its dedicated JavaScript and Python checks and actual browser verification are recorded in `INUYASHA-COLOR-VERIFICATION.md`. Full turn timing and card-text effects remain unfinished; this frontend result does not establish Magic gameplay acceptance.

The first September 30 full build failed when Adder-Staff Boggart's first gameplay test spent its timeout loading the full card catalog. An unchanged retry passed but spent 97.6 seconds in that test; a thread dump confirmed catalog discovery. With user authorization, the fixture now loads the shared catalog during spec setup instead of inside a timed gameplay test (commit `87ee1dfb5d`). The corrected full-suite run spent roughly 128 seconds preparing that spec, then its first gameplay test passed in 2.0 seconds; the following Ageless Sentinels tests also passed. The complete build subsequently passed in 36 minutes 27 seconds; a final Caverns-only run passed after strengthening its normal-play case to submit the actual PlayLand action. Assay's ALL and TSP set differential checks reported no divergences among covered cards; NEM reported a pre-existing Stronghold Machinist target-filter divergence, whose repair still awaits separate approval. Assay coverage declines are not passes.

## October 1 continuation

Native Fatal Push, Null Rod, and Izzet Charm now have 27 passing dedicated gameplay scenarios, original-printing metadata, and reviewed compiled snapshots. Izzet Charm is canonical in DDJ with its RTR reprint recorded separately; DDJ is incomplete and excluded from sealed play. The full build passed after the authorized catalog-setup repairs in Abattoir Ghoul and A Realm Reborn, retaining the accepted legacy mtgish-only test exclusion. Assay reports no divergences in AER, WTH, or DDJ among covered cards, but declines these three new cards; a decline is not whole-card proof. See `NEXT-CARD-VERIFICATION.md` and its result files for evidence and limits.

Hydroblast, Toxic Deluge, and Taste for Mayhem now have 20 passing dedicated native gameplay scenarios. Paid-life X resolution, real untargeted spell copies, both-player X badges, and normal/modal numeric damage text have eight passing engine regressions. The final full build passed with 19,524 passes, 24 skips, and zero failures/errors across 20 modules, excluding only the accepted legacy mtgish tests. User-approved catalog preloads repaired the AI factory and A Killer Among Us timed fixtures without changing assertions or timeouts. See `FOLLOWING-CARD-VERIFICATION.md` and its result files.

The adapter's modal-target and paid-life controls have 24 passing control/display tests. Real local-server browser scenarios verify paid-life Toxic Deluge, projected creature stats/deaths, and Prismari Command's separate targets for two selected modes. These use dev-scenario fixtures and normal adapter controls, not mandatory exact-deck admission. See `PROVIDER-CONTROL-VERIFICATION.md` and the captured screenshots.

Twelve cards in the September 30 remaining-card matrix still need native implementation and behavioral verification. Preparatory drafts for subsequent cards remain outside the build tree and are not verified implementations. Broader provider decisions, mandatory Constructed admission/start, deployment, and live acceptance remain unfinished.

Gathan Raiders, Jagged Poppet, and Cutthroat il-Dal now have 22 passing native scenarios, reviewed snapshots, exact metadata, and fresh printing checks. The full regression build passed with 19,546 passes and 24 skips across 20 modules, excluding only the accepted mtgish tests. A real local browser scenario verifies Gathan's face-down casting and discard-to-morph through normal controls. Assay reports no divergences among covered FUT/DIS cards but declines these three whole-card models. See `HELLBENT-CARD-VERIFICATION.md` and its result files. Mandatory exact-deck admission, deployment, and live acceptance remain unfinished.

The Biblioplex and Blood Scrivener now have 25 passing native scenarios, reviewed snapshots, exact original-printing metadata, and fresh printing checks. The full regression gate passed in 55 minutes 16 seconds with 19,571 passes, 24 skips, and zero failures/errors across 20 modules. An initial unrelated JevClientTest local HTTP timeout is preserved; its user-authorized unchanged retry passed, followed by the successful full gate. Assay agrees on all covered STX/DGM cards but declines these two whole-card models. See `LIBRARY-DRAW-CARD-VERIFICATION.md` and its result files. This native verification does not establish deployment or final provider/live acceptance.
