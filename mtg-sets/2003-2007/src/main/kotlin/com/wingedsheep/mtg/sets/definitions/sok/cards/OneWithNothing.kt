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
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Jim Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a5841fa-4f30-495a-b840-3ef5a2af8fad.jpg?1783944151"
    }
}
