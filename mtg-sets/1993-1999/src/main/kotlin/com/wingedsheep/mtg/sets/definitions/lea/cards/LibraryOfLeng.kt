package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.PutDiscardOnTopOfLibrary

val LibraryOfLeng = card("Library of Leng") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "You have no maximum hand size.\nIf an effect causes you to discard a card, discard it, but you may put it on top of your library instead of into your graveyard."

    staticAbility {
        ability = NoMaximumHandSize
    }
    replacementEffect(PutDiscardOnTopOfLibrary)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "257"
        artist = "Daniel Gelon"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5c96d55-f79b-42cf-91b5-97a636e26ed5.jpg?1559591501"
        ruling("2004-10-04", "You can choose to put the card on top of your library or into your graveyard.")
        ruling("2004-10-04", "The ability applies only to effects that cause you to discard a card. It does not apply to discarding a card as a cost.")
        ruling("2004-10-04", "If a spell or ability causes you to discard multiple cards, you choose separately for each card.")
    }
}
