# Explicit preview and Reforge investigation

Hovering, focusing, and selecting battlefield/hand cards no longer opens the large preview. Select a card, then choose Enlarge card to open it explicitly. Selecting another card closes the previous preview. Chrome verified hover, select, enlarge and close against a live isolated game.

A native normal Reforge the Soul cast with five untapped Mountains succeeded and reached the stack. This does not establish the reported four-land-plus-Treasure scenario as fixed. Native ManaSolver excludes sacrifice-requiring sources from automatic solve; Treasures must be explicitly activated. A visible instruction now explains this on Treasure cards. Exact Treasure-assisted cast verification remains open. Miracle remains an engine timing gap recorded in UNDO-ATTACK-UX-2026-10-06.md.

Follow-up: Enlarge card now lives in the top toolbar, outside the gameplay action box. Browser interaction with regular Reforge cast still passes. Native Library of Leng regression: discard Reforge and Swamp via One with Nothing, accept Leng only for Reforge, decline for Swamp, observe Reforge as private top, advance both seats to following turn, and verify Reforge drawn. This scenario passes, but does not resolve the reported live mismatch. Need confirm other top placements or shuffle in that game.
