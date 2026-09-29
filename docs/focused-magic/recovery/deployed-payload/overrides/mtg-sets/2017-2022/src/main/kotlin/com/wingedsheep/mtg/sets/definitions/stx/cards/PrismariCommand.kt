package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val PrismariCommand = card("Prismari Command") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Choose two —\n" +
        "• Prismari Command deals 2 damage to any target.\n" +
        "• Target player draws two cards, then discards two cards.\n" +
        "• Target player creates a Treasure token.\n" +
        "• Destroy target artifact."

    spell {
        modal(chooseCount = 2) {
            mode("Prismari Command deals 2 damage to any target") {
                val t = target(Targets.Any)
                effect = Effects.DealDamage(2, t)
            }
            mode("Target player draws two cards, then discards two cards") {
                val player = target(Targets.Player)
                effect = Effects.DrawCards(2, player) then Patterns.Hand.discardCards(2, player)
            }
            mode("Target player creates a Treasure token") {
                val player = target(Targets.Player)
                effect = Effects.CreateTreasure(1, controller = player)
            }
            mode("Destroy target artifact") {
                val artifact = target(TargetFilter.Artifact)
                effect = Effects.Destroy(artifact)
            }
        }
    }
    metadata { rarity = Rarity.UNCOMMON; collectorNumber = "214" }
}
