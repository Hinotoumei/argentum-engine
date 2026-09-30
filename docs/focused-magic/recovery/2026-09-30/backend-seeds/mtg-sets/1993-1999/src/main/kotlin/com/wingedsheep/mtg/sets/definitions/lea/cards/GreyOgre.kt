package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/** Focused Magic required-card implementation: vanilla Grey Ogre. */
val GreyOgre = card("Grey Ogre") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre"
    power = 2
    toughness = 2
    metadata { rarity = Rarity.COMMON }
}
