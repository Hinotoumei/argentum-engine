# TIKUNARI / Focused Magic — Codex Repair Packet

## Authority / target
- Repository: `Hinotoumei/argentum-engine`
- Branch: `focused-magic`
- Last verified deployed base commit before this packet: `1e021883b1961f73ae39366518e131b43bd55993`.
- Re-read remote HEAD before editing. If it moved, reconcile; do not reset newer work.
- Existing Render service: `srv-datrs2rncjis739q2t00` (`focused-magic-argentum-067-pro`). Do not create a second service.
- Frontend source lineage for this packet: current v0.6.9 WIP, NOT the failed v0.6.6 blank-input archive.

## 1. Provider hard-code
The packet's `frontend/js/provider/argentum.js` is already prepared so the application can no longer fall back to `magic.wingedsheep.com` or stale browser config.
Authoritative endpoints:
- origin: `https://focused-magic-argentum-067-pro.onrender.com`
- WebSocket: `wss://focused-magic-argentum-067-pro.onrender.com/game`
Preserve the lock. Do not reintroduce user-editable origin/WS or a legacy-storage override.

## 2. InuYasha
Follow `INUYASHA_AUTHORITY.txt`. The prepared v0.6.9 file hardens the actual click/resolver boundary, but DO NOT call it accepted from local tests. After deployment, user-live A-H acceptance is mandatory.
Do not use Drive `1yCq8...` as proof or baseline.

## 3. Required-card catalog repair
The live mandatory assessment reaches the new Render provider and reports exactly 27 missing card names listed in `MISSING_27_MATRIX.csv`.
Do NOT satisfy this by registering blank shells. The user's acceptance rule is that the card must do everything its printed text requires in actual gameplay.

Prepared seed source:
- `Daze.kt`: recovered prior TIKUNARI overlay, compile/test before adopting.
- `ForceOfWill.kt`: recovered prior TIKUNARI overlay, compile/test before adopting.
- `ConsignToMemory.kt`: copied from newer upstream Argentum implementation; adapt only if current branch APIs differ.
- `GreyOgre.kt`: safe vanilla definition prepared here.
- `FocusedMagicCatalogCorsConfig.kt`: strengthens startup gate from only Gemstone/Oscorp to all 27 missing names plus those two known critical identities. Keep explicit missing-name error text.

For the remaining 23 cards, implement full printed behavior using engine-native primitives. If an engine primitive is missing, add the primitive + handler/tests rather than approximating or silently dropping text. High-risk engine cards include at minimum Tamiyo DFC, Murktide Regent, Dauthi Voidwalker, Engineered Explosives, Null Rod, Toxic Deluge, Blood Scrivener, Currency Converter, Library of Leng, Surgical Extraction and Barrowgoyf.

## 4. Build / validation gates
Required before pushing/deploying:
1. All new Kotlin source compiles against current `focused-magic` HEAD.
2. Run the relevant card snapshot/scenario tests for each new definition; add scenario coverage for meaningful rules behavior.
3. `./gradlew :game-server:bootJar` passes.
4. Startup gate must PASS with zero missing required names. A registry-size number alone is not acceptance.
5. `/api/cards` exact-name check returns all 27.
6. Mandatory Constructed assessment must admit/start the exact decks with zero registry errors.
7. Runtime regressions in order: Gemstone Caverns pregame; Scalding Tarn pay-life/sac/search/shuffle; Faithless Looting draw-two/discard-two/flashback; then Kasmina/Jace/Way/Reweave; plus representative runtime tests for every newly implemented card.
8. InuYasha A-H user-live acceptance is separate and mandatory before marking that portion complete.

## 5. Deployment
Push to `Hinotoumei/argentum-engine:focused-magic`, deploy the SAME Render service, verify commit SHA in Render logs, build success, server boot on 10000, `/game` mapping, strengthened catalog gate PASS, then run browser/live acceptance.

## 6. Honesty boundary
Do not report "fixed" because source exists, Kotlin compiles, or registry size increases. Record card-by-card gameplay PASS/FAIL and retain unresolved items.
