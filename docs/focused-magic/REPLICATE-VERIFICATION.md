# Consign to Memory and replicate

Consign to Memory is authored in its original MH3 printing. Its target predicate permits triggered abilities and colorless spells, including colored triggered abilities; it rejects colored spells and activated abilities. Replicate is a cast-time optional repeated cost followed by one triggered ability that creates the paid number of spell copies.

The initial card definition and replicate plumbing were adapted from read-only upstream commit `4cbd0efb5f3e9c89527677e2561132c8debebf19`. This fork adds payment/enumeration edge-case coverage and the last-stack-state repair for copy triggers whose original spell is countered. No upstream writes were made.

The implementation carries the repeat count through legal-action enumeration, payment, JSON serialization and the client target/mana pipeline. A mana reducer applies after additional costs have been added. Alternative or free base casting does not waive the replicate payment. Positive-cost legal choices are not arbitrarily capped at ten.

Copy triggers capture the original spell's last stack characteristics when it leaves the stack. This preserves the latest targets, including a retarget before countering, without mistaking a later visit of the same entity for the original spell. Suspended plain and modal copy-target decisions retain that snapshot through serialization.

Targeted verification passed sixteen generic replicate scenarios, four Consign scenarios, the frozen AI baseline, and 234 card snapshot/JSON round-trip checks. The client type check and 54 focused client tests passed. The baseline test additionally proves that removing the new default repeat-count field from the action text restores the previous action-stream hash, turn count, winner and final life totals. Fresh Scryfall checks verified Oracle text, mana cost, type, rarity, original printing metadata and image identity.

Refreshed Assay declines both Consign lines, so it provides no whole-card gameplay proof. The MH3 differential compared seven of 25 authored cards, with seven agreeing and zero model divergences or undecodable goldens. Fifteen cards were grammar-declined, two had unmodelled script slots, and one was excluded for Oracle text drift: the unchanged Arena of Glory definition says “If that mana” where the cached Oracle says “If any of that mana.” This is a pre-existing exclusion, not a Consign model failure.

The full `build :oracle-assay:installDist -x :mtgish-tooling:test` gate passed in 1 hour 28 minutes 31 seconds on September 30. The accepted legacy mtgish-only exclusion is retained. Final XML totals across twenty modules are 19,467 passed and 24 skipped, with zero failures or errors, recorded in `replicate-test-results.json`. The manual scenario fixture is provided for repeatable live verification and has not been executed as live acceptance evidence.

Current generic limits: paying multiple independent replicate abilities on the same spell is rejected explicitly. Zero-resource repeated-cost menu choices are bounded to keep enumeration finite. Other repeatable additional costs must have their effect semantics implemented separately; the presence of the generic repeat-count field does not establish multikicker gameplay support.
