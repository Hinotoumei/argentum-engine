# Undo and attack chooser

The playable frontend now displays Undo last action and enables it only when the server advertises undoAvailable. It sends requestUndo, with no client-side state rollback. A native isolated scenario verified playing Mountain and undoing it back into hand.

The attack destination chooser explicitly asks who the selected creature attacks, explains that this precedes the attack trigger, and uses Confirm attack target rather than Confirm selected cards. Cancellation submits nothing. Chrome verified the Bilbo-labelled opponent chooser.

Existing 70 Constructed control/startup regressions pass. InuYasha v0.3 integration and prior fetch/AI removal fixes remain included.

## Open: Reforge the Soul miracle
The reported first draw did not offer miracle. Source inspection found sorcery-speed filtering before miracle enumeration, and the current native draw implementation creates a turn-long MiracleWindowComponent instead of the optional reveal and resolving-trigger cast flow. Existing Reforge scenarios only test its ordinary wheel effect. This issue is not fixed or verified by this UI release. Do not represent registry presence or an ordinary cast test as miracle coverage. Official miracle rulings require casting during resolution of its reveal trigger, including outside main phases.
