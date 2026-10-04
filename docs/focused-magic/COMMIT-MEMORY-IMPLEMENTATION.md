# Commit // Memory: remaining implementation

Status: implementation in progress; focused gameplay verification is running. No completion claim yet.

## Rules boundary

Aftermath is a property of one split-card face. That face has an intrinsic cast permission from its owner's graveyard, cannot be cast from other zones, and is exiled whenever the resulting graveyard-cast spell leaves the stack. This must cover countering and return-to-hand effects, not just successful resolution. See [official Comprehensive Rules, Aftermath 702.127a](https://media.wizards.com/2026/downloads/MagicCompRules%2020260619.pdf), verified October 4, 2026.

## Existing components

- `CardLayout.SPLIT`, `CardFace`, and `CastSpell.faceIndex` already model separate costs, types, targets, and effects. `CastSpellEnumerator.enumerateSplitFace` already offers individual faces from hand. It needs to suppress an Aftermath face there.
- `CastValidator` needs to enforce the face's zone restriction even for client-crafted actions and externally granted cast permissions. Its current generic self-zone permission is card-wide and would incorrectly authorize Commit alongside Memory.
- `CastFromZoneEnumerator` needs to offer only the eligible Aftermath face with its own six-mana cost and sorcery timing.
- `Effects.PutIntoLibraryNthFromTop(target, 1)` already expresses Commit's forced position. Commit must accept a spell or nonland permanent, including an uncounterable spell, and use its owner's library.
- Collection gather/move/shuffle primitives can compose Memory's hand-and-graveyard reset. Both players must shuffle before either player's seven-card draw instruction; draw replacements must still apply. The resolving Memory card is on the stack and must not enter the shuffled graveyard collection.
- Exile on successful resolution alone (`selfExile()`) is insufficient for Aftermath. Trace all stack departure routes, including fizzle, counter, and general zone moves, and attach the same replacement semantics to the selected face.

## Required gameplay cases

One `CommitMemoryScenarioTest` should prove Commit from hand, spell and nonland targets, rejected land targets, second-from-top ordering with empty and nonempty libraries, invalidated targets, and owner/controller differences. Memory cases should prove graveyard-only casting, six-mana and sorcery restrictions, both players' zone reset and seven draws, Notion Thief interaction, and exile after resolving, countering, and bouncing. Also reject the unsplit combined spell and invalid face indices, and preserve ordinary split/Room, Adventure, and modal-DFC behavior.

Add reusable Aftermath vocabulary and its SDK reference entry rather than special-casing the card name in engine code. Keep this card absent from completion claims until those scenarios and broader gates pass.

## Current implementation checkpoint

The card is defined natively in AKH. Aftermath is a per-face SDK keyword, mirrored in the client.
The hand enumerator suppresses Memory; the graveyard enumerator publishes its six-mana face with
`sourceZone = GRAVEYARD`. The authority check rejects the combined card, invalid face indices, and
Aftermath from other zones. A stack-departure rider covers resolution, countering, and generic zone
moves. Direct spell-bounce executors also honor that rider. Generic single-card movement now locates
stack spells, enabling Commit's non-counter library movement.

The focused scenario build is running; these changes have not passed gameplay verification yet.
Tests cover library positions, land rejection, invalid faces, cost, timing, hand/graveyard reset,
Notion Thief, uncounterable spell movement, countering, library movement, and bounce. The client
TypeScript check passed. Broader engine/SDK gates, reviewed AKH snapshot, provider checks, and
publication of this batch remain outstanding. Do not treat this checkpoint as completion.
## Focused gameplay result

October 4: the focused Gradle scenario gate passed all 18 tests in 1m 34s. It proves both
spell halves, rejection of invalid faces and land targets, six-mana and sorcery restrictions,
empty/small library placement, uncounterable spell movement, both players' seven-card reset,
Notion Thief replacement, and exile after countering, forced library placement, and both direct
bounce executor forms. The initial run's one failure was a missing Unsubstantiate fixture, not a
gameplay failure; explicit test-only bounce spells replace that unavailable card.

The library-position continuation was subsequently routed through the shared zone-transition
service. That final change still needs the broader gate. Snapshot regeneration is running. The
new batch is uncommitted and unpublished; production acceptance is not established.
## Broader gate checkpoint

The regenerated catalog snapshot and JSON round-trip gate passed in 2m 30s. Reviewed diff:
AKH adds only Commit // Memory (121 lines); FUT remains unrelated and excluded. Snapshot review
caught the omitted combined mana cost/types, now corrected to {7}{U}{U}{U}, Instant Sorcery.
A nineteenth focused scenario checks the resulting in-hand mana value ten and both types.
The earlier eighteen scenarios passed before this final metadata correction.

The full `test` gate is running via `commit-memory-gates.just`, exec session 95363. Do not start
another heavy build or cancel it. It includes the final library-position continuation change and
nineteenth scenario. No commit or publication until that result is inspected. Next: review full
results, refresh source audit, verify native provider/browser acceptance, and publish against
remote head 783f4720c2e20f98e0d292f9cc3a667962177b96 on the existing fork branch. No deployed
completion claim is established.
## October 4 follow-up

The baseline full gate completed and failed on recovered Dauthi Voidwalker's missing VOID counter vocabulary entry. Engine and server results were green. The entry is repaired; the earlier build is not a final gate for the expanded Aftermath permission handling. The first expanded scenario build failed to compile because two test permissions omitted timestamps; these fixtures are repaired. Resolve-time casts now enforce the Aftermath zone restriction, and external exile/graveyard permissions enumerate legal individual faces. Current deck verification is running; this batch remains unpublished.
