# Authorized Syphon Mind blocker repair

The prior full regression run (session 23684) ended after 15m 28s with three failures in `SyphonMindTest`. Opponents discarded correctly, but the controller drew zero cards. The 4,100 engine tests had passed; the full gate did not pass. The user explicitly authorized repairing this untouched-code blocker.

Root cause: `MoveCollectionExecutor.moveToZone`'s unified-discard shortcut returned state and events without `storeMovedAs`. Syphon Mind's collecting player loop therefore received no `discarded_this_opponent` output and computed zero draws. The fix restores synchronous outputs and uses an existing effect continuation to publish the output after replacement decisions complete. That frame also retains later owners' discard groups if an earlier owner pauses. Discards still go through `ZoneTransitionService.discardCards`.

Verification: the original three scenarios and two added Library of Leng scenarios all passed. The latter select a discard, then accept or decline the replacement, and assert the controller still draws one card. Gradle succeeded in 2m 40s. An initial compile failure from a missing `CompositeEffect` import was corrected before this passing run.

The SDK reference now documents discard output collections across replacements and pauses. No card-name special case, replacement bypass, upstream write, or remote branch was added.

The full engine/card regression gate was restarted using `../tooling/focused-regression.just`, process session 49099, and passed in 49m 22s. Its XML reports total 16,825 tests with zero skips, failures, or errors. Erebos's Intervention's sixth scenario also passed, proving that an invalidated creature target prevents life gain. The catalog snapshot/serialization and lint gate passed in 2m 22s. This verifies the backend batch; complete deck/provider/live acceptance remains outstanding.

The source inventory was expanded to include AI lists and sideboards for every profile and completed successfully. Only Commit // Memory lacks a definition. This inventories source existence only, not gameplay acceptance.
