# Preparation status

## Completed in this packet
- Recovered the authoritative current frontend WIP: v0.6.9 Drive `1iRWxWsS-Rbmi8tVK6jIfVAamqTXK5WYj`.
- Did **not** use the failed v0.6.6 artifact `1yCq...` as the fix baseline.
- Hard-locked provider origin and WebSocket to the current Render service.
- Removed stale/local/injected provider override behavior from the prepared frontend.
- Removed the duplicate Provider origin diagnostics field and made provider fields read-only.
- Normalized stale visible v0.6.8 labels inside the v0.6.9 package to v0.6.9.
- Hardened the actual InuYasha click/resolver path so invalid input fails closed before state mutation; ARD combat math was not changed.
- Local supporting checks: provider + InuYasha JS syntax PASS; DOM IDs unique; InuYasha Python attack reference 11/11 PASS; JS attack parity 11/11 PASS. These are **not** user-live acceptance.
- Prepared backend seed sources for Daze, Force of Will, Consign to Memory, and Grey Ogre.
- Prepared a strengthened Render startup catalog gate that names every currently missing required card and fails with the exact missing names.

## Deliberately not faked
23 of the 27 missing provider cards do not have trustworthy recovered implementation source in the current fork packet. They remain explicit Codex implementation work. Do not register blank shells merely to make the catalog gate green.

## Remaining Codex work
Implement full behavior for the 23 cards marked `IMPLEMENT_FULL_TEXT` in `MISSING_27_MATRIX.csv`, compile/test every seed and new card against current `focused-magic` HEAD, push, redeploy the existing Render service, verify `/api/cards`, run mandatory Constructed assessment, then perform gameplay and InuYasha user-live acceptance.
