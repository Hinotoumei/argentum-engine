package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

val GeierReachSanitarium = card("Geier Reach Sanitarium") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Legendary Land"
    oracleText = "{T}: Add {C}.\n{2}, {T}: Each player draws a card, then discards a card."
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.Pipeline {
            run(Effects.ForEachPlayer(Player.ActivePlayerFirst, Effects.DrawCards(1)))
            val selected = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                listOf(chooseExactly(1, from = hand, prompt = "Choose a card to discard"))
            }.single()
            discard(selected)
        }
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96093739-fedc-4d8f-a29d-0e57f571e5a9.jpg?1783937421"
        flavorText = "All are welcome."
        ruling("2016-07-13", "When you activate Geier Reach Sanitarium's last ability, first each player draws a card. Then the player whose turn it is selects a card from their hand and sets it aside without revealing it; proceeding in turn order, each other player does the same. Then the cards that were set aside are discarded at once.")
        ruling("2016-07-13", "If more than one player discards a card with madness simultaneously, the player whose turn it is puts their madness abilities onto the stack first, then each other player does the same in turn order. The last one put onto the stack resolves first, and a spell cast this way will resolve before resolving the next madness ability.")
    }
}
