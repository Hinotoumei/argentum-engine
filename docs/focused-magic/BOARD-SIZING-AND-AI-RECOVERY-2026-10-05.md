# Board sizing and AI recovery

Both game panels fit available dynamic viewport height. InuYasha table uses remaining flex height and zone-relative card sizing; all hands stay visible at 1366x680 and 1093x544. Card inspector is bounded away from lower controls. MTG card heights and widths use allocated zone height; tapped card rotation is bounded and pile thumbnails cannot overflow.

Chrome bounds checks passed both viewports: real InuYasha draw/play populates zones and hand, plus a received MTG hand snapshot with layout-only clones to stress ten cards in each battlefield. This cloned board is a visual stress fixture, not a native ten-permanent gameplay scenario. Public frontend is not yet deployed.

AI INVALID_ACTION now records the submitted action as rejected for that exact state, clears its duplicate-submit latch and requests authoritative resync. On identical resync it tries other completed actions or Pass, avoiding both freeze and retry loops. Rejections are cleared on changed state. 63 frontend controls passed including rejected-command-to-Pass regression. The particular opponent spell in the user's screenshot remains unidentified; this check proves recovery, not a native card-specific target fix.
