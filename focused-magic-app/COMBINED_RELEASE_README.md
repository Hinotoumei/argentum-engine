# Combined Focused Magic release — 2026-10-05

Upload the ZIP root to the existing Cloudflare Pages project. It includes the prior playable frontend and all fixes, plus:

1. Opponent AI library searches now select an eligible card when optional selection permits one, instead of automatically choosing zero. Live native Scalding Tarn and Polluted Delta scenarios verified sacrifice, life payment, search and Island/Swamp entry. 69 frontend regression tests passed.
2. Standalone InuYasha Score Rules Engine v0.3.0 source, tests and verification in inuyasha-rules-engine-v0.3.0/. Its 37 tests passed, with all 18 baseline Draw/combat tests unchanged. See its README for Set Up API and bounded rules scope.

The Python v0.3 engine is included as requested in the same archive. It is not yet ported into the browser's JavaScript tabletop; the preserved playable InuYasha frontend remains at its existing integrated version. This package does not deploy itself; Cloudflare Pages upload is still required. Native Render backend unchanged.

## AI removal follow-up

The AI now holds spells with the explicit Destroy target creature instruction when no opponent-controlled legal target exists. This intentionally covers that removal template, not all harmful spells or modal effects. Live Fatal Push scenarios verified holding the spell with only its own creature present and destroying an opponent creature while preserving its own. 70 frontend tests pass. InuYasha v0.3 remains included as the standalone engine; 37 Python tests previously passed.
