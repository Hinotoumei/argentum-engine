package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val Manamorphose = card("Manamorphose") {
    manaCost = "{1}{R/G}"
    colorIdentity = "RG"
    typeLine = "Instant"
    oracleText = "Add two mana in any combination of colors.\nDraw a card."
    spell {
        effect = Effects.AddManaInAnyCombination(amount = 2) then Effects.DrawCards(1)
    }
    metadata { rarity = Rarity.COMMON; collectorNumber = "211" }
}
