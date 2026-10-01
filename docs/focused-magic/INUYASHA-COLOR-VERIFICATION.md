# InuYasha printed-color and direct-attack verification

The fork now loads printed combat profiles for all 28 character printings in its two preserved test decks. Each profile is keyed to the exact linked card-image asset. The generator preserves absent colors as absent, normalizes these verified rows to Character, and copies the reviewed values into the browser data. The SQLite source and original packet remain unchanged.

The tabletop offers only colors printed on both selected characters. Printed values are read-only, and the submission guard checks them against the profile again. Unknown profiles and noncharacters cannot become attackers or defenders. White is included alongside the other printed colors, and final numeric modifier totals cannot fall below zero.

For Rin, Young Follower versus Ginkotsu, Band of Seven, blue is 5 versus 3 and purple is 3 versus 6. Browser interaction verified that purple expends Rin but leaves Ginkotsu alive and steals no shard; after using the explicit tabletop Ready control, blue defeats Ginkotsu and transfers exactly one shard. Red, green, black and white are disabled for this pair.

A new Attack Opponent control permits the basic direct attack only when the opponent controls no characters. A mismatch alone permits neither a character attack nor a direct attack. The [rulebook](https://sites.google.com/view/inuyashatcgdatabase/inuyasha-gameplay-rules) defines control as faceup and says defeated facedown cards are out of play, so those cards do not count as controlled characters. A faceup card of unknown type blocks the browser's direct-attack path until its type is established.

Verification: 14 printed-color/direct-attack JavaScript tests, the existing 11 JavaScript parity cases, and 15 Python cases passed. JavaScript syntax checks passed. Actual localhost browser controls exercised both printed comparisons, a direct attack with an empty opponent play area, and the blocked no-match pair Rin, Follower of Sesshomaru versus Ginkotsu, Member of the Band of Seven. No browser state injection was used. Screenshots preserve the tested views:

- [Blue comparison](verification-images/inuyasha-blue.jpg)
- [Purple comparison](verification-images/inuyasha-purple.jpg)
- [Direct attack](verification-images/inuyasha-direct.jpg)
- [No matching colors](verification-images/inuyasha-no-match.jpg)

Scope remains limited. Printed card effects, attack exceptions, actual modifier choices, most-recent-printing errata, complete setup and turn automation, recovery, and live A–H acceptance are unfinished. The prototype still deals seven-card hands and represents shard setup by counts; its preserved Three's Company test fixture has 61 cards. This repair is local source and behavioral evidence, not a claim of a fully official match or a production deployment.
