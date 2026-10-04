# Commit // Memory: remaining implementation

Status: not implemented and not gameplay verified.

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
