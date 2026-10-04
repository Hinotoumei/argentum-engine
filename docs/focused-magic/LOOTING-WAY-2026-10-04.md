# Faithless Looting controls and Way gameplay verification

October 4: the WayOfThePyromancerScenarioTest backend scenario passed (one test, zero failures/errors). A full 614-file 2026 test compilation completed; the build took 8m20s. Compiler stacks confirmed active type checking and generated-class processing during quiet output. Earlier short retries did not prove a hang.

The scenario casts Way, resolves its empower trigger to a Jace token with two loyalty, activates the provider-enumerated granted +1, verifies three loyalty, resolves to one mana, and checks that a second loyalty activation is unavailable that turn. This is backend gameplay evidence, not live Way tabletop acceptance.

The Focused Magic adapter's SelectCardsDecision and ChooseTargetsDecision human paths still used pickIds, which throws without test-only answers. They now await the existing native browser chooser. Engine rules and protocol types are unchanged. Faithless Looting's already passing normal/flashback backend scenarios remain the engine evidence.

Validation: all 48 adapter checks pass; argentum-selection-browser.test.cjs passes in Chrome, enforcing two selections and cancellation without a response. argentum-looting-browser.cjs passes against the existing local server JAR on 8082 and current frontend on 8781: real cast, draw two Forests, actual dialog selection of both newly drawn cards, discard both into the graveyard, and two original cards left in hand. No page errors. An initial browser assertion compared the provider zone label without normalization; state showed the discard had completed. Normalizing the test assertion fixed that verification failure.

The add-feature workflow guided tracing the existing pending decision through the adapter and testing actual browser controls. No SDK, engine behavior, Assay vocabulary, or server contract changed. Full-deck audit, live production acceptance, remaining modal/combat controls, and deployment are unfinished.
