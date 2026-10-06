# Currency Converter / Library of Leng prompt repair

Reported failure: after choosing Library of Leng to discard a card onto the library, Currency Converter still repeatedly asks whether to exile the discarded card from the graveyard.

The Converter definition previously wrapped Move(fromZone=GRAVEYARD) inside May. Move checked the zone only after the optional prompt. The repair preserves the discard trigger on the stack but checks the triggering card is currently in GRAVEYARD before presenting May. It reuses Conditions.TargetMatchesFilter with StatePredicate.InZone and the existing Effects.If composition. No client guesses about hidden zones are introduced.

The regression uses the real Library of Leng definition: discard a chosen creature, accept Leng, settle Converter, assert no decision remains, the card is neither exiled nor in the graveyard, and it is first in the library. Existing ordinary Converter scenarios continue to exercise normal optional exile and token creation.

Validation: all 23 CurrencyConverterScenarioTest cases pass after repairing ConditionEvaluator, and the full rules-engine module succeeds (4,100 passing checks reported). Card-snapshot regeneration succeeds; only NCC.json changes, and only Currency Converter gains the current-zone guard. Deployed and verified live on Render at revision 55e96003891a4c934bf7f2021b00405bc985ef57 (deployment dep-db2mu21srm7s73c24fj0, live 2026-10-06T22:03:49Z). The live two-seat scenario discarded Reforge the Soul with One with Nothing, accepted Library of Leng, observed the private known top, and drew Reforge next turn without a Currency Converter exile prompt. Miracle remains a separate unresolved native timing issue.

The first regression run failed despite the card-level zone guard. ConditionEvaluator matched non-battlefield TriggeringEntity by static card characteristics, ignoring state-axis zone predicates. The evaluator now routes explicit InZone requirements through the real current-state predicate evaluator. Ordinary static characteristic and last-known battlefield paths remain otherwise as before. The second Converter run passes all 23 cases.

