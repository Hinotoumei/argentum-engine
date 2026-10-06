# InuYasha Score Rules Engine v0.3.0

Standalone Python engine built from the accepted INUYASHA_SCORE_RULES_ENGINE_v0.2.0.zip. Run `python -m unittest discover -s tests -v` from this directory. All 18 baseline tests are byte-for-byte unchanged; existing core function bodies are unchanged by AST comparison. 19 new Set Up tests pass (37 total).

## Sequence and API

`draw = game.start_turn_and_draw()` preserves the existing Draw Step. `game.start_setup(draw)` opens Set Up start hooks for the active player then opponent. `game.play_setup_card(player_id, card_id, ...)` accepts Characters, Items and Locations in any order. `game.finish_setup_actions(active_id)` transfers the complete action block to the opponent; finishing the opponent's block runs end hooks active player then opponent and sets `phase = BATTLE_START`. Battle start effects and Battle action alternation are not executed in v0.3.

Register `SetupCard` definitions in `game.setup_cards`; hand/deck/discard zones use their opaque IDs. Definitions provide verified deck costs, character names/surnames, colors, uniqueness, attachment/play restrictions and optional when-played handlers. Card text is not automatically interpreted. Register timing effects with the existing `register_window_handler` API. Start/end events retain the turn active player and identify whose effects resolve using subject_player_id and context.acting_player_id.

Costs are validated before mutation and paid from the top of the deck before entry. Each player has a two-Character allowance; overlays and Chokyukai's Tiara count. Items require an eligible faceup character with no item; duplicate locations and unique items cannot overlay. Character overlays require a different surname. Card-specific legality callbacks enforce additional printed restrictions. The engine rejects unknown definitions and unimplemented Setup events.

## CRD Setup-specific exceptions

- Chokyukai's Tiara requires Chokyukai, attaches to an opponent's character, transfers control and counts against the Character allowance. Removing the attachment restores ownership-based control. Its attack restriction belongs to a later combat integration and is not injected into the preserved v0.2 combat resolver.
- Following the Scent returns a controlled character, searches one chosen zone (deck or discard) for up to three locations, records revealed results, shuffles/offers a cut for a deck search, and enforces once per turn. Provide return_character_id, search_ids and search_zone. A shuffle function can be injected for deterministic verification.
- Shippo's Shroom attaches as an event (not an item slot), remains effective while the host is defeated, suppresses Shippo uniqueness across the controller's zones, and blocks effect-based removal from its host.
- Shippo's Item Technique moves a controlled attached item to another legal controlled host without replaying the item or emitting its when-played window. Provide host_id and item_id.

This is a bounded rules engine release, not a complete printed-card interpreter, full response/pass protocol, graphical frontend integration, or full-game deck-exhaustion implementation. Timing hooks are synchronous and callers supply already-resolved player choices/effects. Draw and combat retain the accepted baseline behavior.

## Sources

ARD v4.0 (July 11, 2007): Step 2 Set Up and Character/Item/Location play procedures, attachment restrictions, overlays and windows.
https://drive.google.com/file/d/1PUSjVvXsYnGIF9j5XoTXA0duwPgDSoIU/view

CRD (September 6, 2007): Chokyukai's Tiara, Following the Scent, Shippo's Shroom, Shippo's Item Technique.
https://drive.google.com/file/d/1GRvFgSunsB3OjLsnmmavAsuJBeZcLf60/view

Rules page: https://sites.google.com/view/inuyashatcgdatabase/inuyasha-gameplay-rules
