package com.wingedsheep.mtg.sets.definitions.sok.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val OneWithNothing = card("One with Nothing") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Discard your hand."
    spell { effect = Patterns.Hand.discardHand() }
    metadata { rarity = Rarity.RARE; collectorNumber = "84" }
}
