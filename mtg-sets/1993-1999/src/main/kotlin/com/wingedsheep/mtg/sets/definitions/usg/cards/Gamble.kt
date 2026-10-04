package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player


/**
 * Gamble
 * {R}
 * Sorcery
 * Search your library for a card, put that card into your hand, discard a card at random, then shuffle.
 */
val Gamble = card("Gamble") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Search your library for a card, put that card into your hand, discard a card at random, then shuffle."
    spell {
        effect = Effects.Pipeline {
            val library = gather(CardSource.FromZone(Zone.LIBRARY, Player.You), search = true)
            val found = chooseExactly(1, from = library, prompt = "Search for a card")
            move(found, CardDestination.ToZone(Zone.HAND))
            run(Patterns.Hand.discardRandom(1))
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "188"
        artist = "Andrew Goldhawk"
        flavorText = "When you've got nothing, you might as well trade it for something else."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ee0f160-7339-4d98-8a8c-f08889ee52f5.jpg"
    }
}
