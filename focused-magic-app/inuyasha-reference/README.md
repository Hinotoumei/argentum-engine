# InuYasha Score TCG Rules Engine — v0.1.0 Attack Core

This is the first executable rules slice for the **original Score Entertainment InuYasha TCG**.
It is intentionally narrow: only the **Attack Timing Sequence** from the *InuYasha TCG Advanced Rules Document (ARD) v4.0, updated 2007-07-11* is implemented.

## Implemented ARD behavior

1. Declare an attacking character, defending character, and attacking color.
2. Expend the attacking character.
3. Open the `when a character is expended` window.
4. Begin the attack and open `when attacking` and `when attacked` windows.
5. Compute both characters' final values in the chosen attacking color, including temporary and constant modifiers.
6. Open the `when comparing color values` window.
7. If attacker value is **equal to or greater than** defender value, defeat the defender.
8. A defeated defender and its attachments become facedown and expended.
9. Open the `when a character is defeated` window.
10. If the defender was defeated by the attack, move one opaque Jewel Shard from the defending player to the attacking player and open the `when you steal a jewel shard` window.
11. Open the `when the attack ends` window.

During the attack the attacking player is tracked as the attack's active player without overwriting the turn's actual active player.

## Deliberately NOT implemented yet

These are later ARD sections and are intentionally excluded from v0.1 rather than guessed:

- full game setup / opening hand / construction of Jewel Shard piles
- Draw, Set Up, Battle action alternation, Recovery, End of Turn
- complete legal-target / cannot-attack / cannot-be-attacked rules
- Missing Defenders resolution beyond the state model needed to add it later
- Events, Activated Effects, Constant Effects beyond numeric modifier hooks, Triggered Effects beyond timing hooks
- Power Up, Kaitou deck, Unique/overlaying, recovery/killing, card memory, counters, win checking
- specific printed card text or CRD errata execution

## Source ambiguity preserved

The ARD first says to compare the attacker's **final attacking color value** to the defender's **final color value**, then immediately contains the phrase “defending character's attacking color value.” v0.1 treats both sides as their final value in the chosen attack color, because that is the coherent reading of the surrounding procedure, but this wording discrepancy is explicitly preserved for later rules reconciliation rather than hidden.

## Jewel Shards in this slice

The ARD says shard-pile cards are facedown and players cannot look at them. v0.1 therefore represents shards as opaque objects. The attack resolver moves exactly one opaque shard when a defender is defeated by an attack. The precise setup/selection procedure will be implemented when the Jewel Shard setup section is reached.

## Test fixtures

The current database catalog does not contain authoritative printed combat stats/card text for every card, so v0.1's unit tests use clearly named `TEST Attacker` / `TEST Defender` fixtures. This prevents made-up stats from becoming card authority. Real cards will replace/augment fixtures only as printed text/stats are verified.

## Run

```bash
python -m unittest discover -s tests -v
python demo.py
```

## Next ARD slice

**Timing Windows Appearing Within a Turn**:

`Turn Start -> Draw -> Set Up -> Battle -> Recovery -> End of Turn`

That will wrap this already-tested attack state machine inside the actual turn engine.
