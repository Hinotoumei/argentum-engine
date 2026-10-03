# Currency Converter work in progress

Native NCC/81 source is installed, with the ordinary MSC/197 presentation reprint in the existing MSC scaffold. The implementation composes existing linked-exile, optional movement, draw/discard, selection, filtering, and token primitives. It does not infer functionality from registration.

The latest dedicated run passed all 22 gameplay scenarios in 24 seconds (`work/tooling/currency-lifecycle.log`). Converter enters through a paid cast in the scenarios. Coverage includes optional discard exile and saved choices, two-mana tap costs, rejection before payment, land/Treasure and nonland/2/2 black Rogue conversion, unrelated exile exclusion, selecting one of two linked cards, blink invalidation, an already activated ability resolving after blink or source removal, and invalidation when an exiled card leaves and returns. Saved-game variants exercise both choice and source-lifetime paths.

Initial failures are preserved in `currency-preserved-failures.json`. The initial minimal registry omitted the predefined Treasure definition and the Rogue assertion omitted the engine's token-name suffix. Two later lifecycle failures came from direct battlefield placement without an entry timestamp; those scenarios now use actual paid casts. No gameplay failure was removed or weakened to obtain a pass.

Compiled snapshot/round-trip/lint checks passed in 2 minutes 12 seconds alongside Murktide Regent. Only the intended MH2 and NCC canonical card trees changed. Compiled canonical name, mana cost, type, Oracle text, rarity, collector number, artist, image, and official rulings match the cached Scryfall canonical response; both images returned HTTP 200. See `murktide-currency-metadata-verification.json`.

Full regression, freshly built Assay assessment, tabletop provider acceptance, and publication remain outstanding. This record is not a completion or deployment claim.
