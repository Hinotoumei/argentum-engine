# Remaining packet work

The verified native batch adds Barrowgoyf, Engineered Explosives, and Surgical Extraction to the existing focused-magic branch. Each has a separate native implementation and dedicated scenario suite: 14, 14, and 13 passing cases respectively. Full build and Assay installation succeeded in 24 minutes 6 seconds, with 19,614 passing tests, 24 skipped, and no failures or errors across 20 modules. Unchanged tasks may reuse valid Gradle results; the accepted mtgish-only exclusion remains. See NATIVE-THREE-CARD-VERIFICATION.md and native-three-card-full-build-results.json.

Canonical metadata, official rulings, earliest printing and scaffolded reprints passed verification. The provider has 29 passing control/display tests and two real local Chrome flows for Surgical Extraction and Engineered Explosives. Local browser evidence does not establish production deployment or complete acceptance. Prior failures and authorized retries remain preserved in separate records.

Nine identities remain: Murktide Regent; Sheoldred's Edict; Tamiyo, Inquisitive Student; Dauthi Voidwalker; Currency Converter; Demonfire; Geier Reach Sanitarium; Library of Leng; Persist. Registry presence alone is insufficient evidence.

Next implementation is Geier Reach Sanitarium and Sheoldred's Edict, with generic multiplayer movement attribution and opponent ordering regressions. Their drafts are not passing evidence. Exact Constructed deck admission/start, remaining provider controls, production/live acceptance, and official InuYasha whole-turn/user-live acceptance remain outstanding. No deployment changes, upstream writes, or new remote branch have occurred.

Publication can be verified from the commit containing this record on Hinotoumei/argentum-engine, focused-magic. The preceding card commits are 18cf881d93 (Explosives), 75976c7ad6 (Surgical), and 1b21be4670 (Barrowgoyf).
