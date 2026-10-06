# Focused Magic combined playable release — 2026-10-06

The browser app now loads js/inuyasha/setup-engine.js and uses InuYasha rules v0.3.0. This is an actual browser integration, not only a Python source folder. Upload the ZIP root to the existing Cloudflare Pages project.

## Playable InuYasha v0.3

Start Turn performs the preserved Draw Step and begins Set Up. The active player has a complete action block, then the opponent. Finish Set Up transfers the block or completes end windows and reaches Battle Start. Each player may play two Characters per turn, with Items and Locations in any order. Play Selected pays deck costs; attachment choices, overlay/uniqueness checks and the four explicit CRD Setup exceptions are implemented. The dialog requests missing printed cost/type/Unique information and requires checking attachment restrictions against the card image. It does not invent missing database fields. AI plays only Characters with verified type and cost, holds duplicate names conservatively, and skips unsupported or unaffordable plays. Use AI Play Card during its block, then Finish opponent Set Up.

Start/end timing windows are surfaced and support engine callbacks; arbitrary printed triggered/constant effects remain manual unless a handler exists. The scope ends at Battle Start; Battle action alternation, Recovery, full card-text automation and full deck-exhaustion behavior are not added. The accepted Draw/combat core and its existing regression files remain unchanged. Attached cards now follow their host’s defeat state in the visible board.

## Constructed fixes preserved

The newest AI Fatal Push fix and the fetch-search fix are byte-identical to the preceding verified release: Fatal Push is held when no opposing legal creature exists, and optional fetch searches select an eligible land instead of zero. Live native cases previously verified Scalding Tarn to Island, Polluted Delta to Swamp, and both Fatal Push targeting situations. Render backend unchanged.

## Verification

100 Node test entries passed (70 Constructed, 12 new Set Up cases, existing printed-color/Draw tests and the combat-parity runner). The parity runner independently checks 11 attack cases. Browser checks at 1366x680 and 1093x544 exercised the real v0.3 controls with controlled card fixtures: cost payment, Item attachment, two-Character cap, atomic rejection, ordered player blocks, displayed v0.3 and Battle Start. Both layouts fit the viewport. The standalone Python v0.3 source remains included with its 37 passing tests.

Cloudflare public deployment is not performed by building this archive. This package still requires upload. It is not a claim of full printed-card or full-game acceptance.
