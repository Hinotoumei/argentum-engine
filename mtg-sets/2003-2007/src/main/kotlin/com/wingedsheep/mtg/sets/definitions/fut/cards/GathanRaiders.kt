package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

val GathanRaiders = card("Gathan Raiders") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 3
    oracleText = "Hellbent — This creature gets +2/+2 as long as you have no cards in hand.\nMorph—Discard a card. (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)"

    morphCost = Costs.pay.Discard()
    staticAbility {
        ability = ConditionalStaticAbility(
            ModifyStats(2, 2, GroupFilter.source()),
            Conditions.EmptyHand
        )
    }
    metadata {
        ruling("2021-03-19", "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal damage dealt to Gathan Raiders while your hand is empty may become lethal if a card is put into your hand during that turn.")

        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "Paolo Parente"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8af5c695-d0f4-4ea7-bc12-f3a24d5b6a11.jpg?1783943106"
    }
}
