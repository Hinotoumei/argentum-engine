package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player

val InfernalTutor = card("Infernal Tutor") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Reveal a card from your hand. Search your library for a card with the same name as that card, reveal it, put it into your hand, then shuffle.\n" +
        "Hellbent — If you have no cards in hand, instead search your library for a card, put it into your hand, then shuffle."

    spell {
        effect = Effects.If(
            condition = Conditions.EmptyHand,
            then = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any,
                destination = SearchDestination.HAND,
            ),
            otherwise = Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any))
                val revealed = chooseExactly(1, from = hand, prompt = "Reveal a card from your hand")
                reveal(revealed, revealToSelf = false)
                val revealedName = storeCardName(revealed)
                run(
                    Patterns.Library.searchLibrary(
                        filter = GameObjectFilter.Any.namedFromVariable(revealedName),
                        destination = SearchDestination.HAND,
                        reveal = true,
                    )
                )
            }
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a4e4be5-e057-4b50-9f86-76bc0b9987de.jpg?1783943429"
    }
}
