package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val SireOfInsanity = card("Sire of Insanity") {
    manaCost = "{4}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Demon"
    power = 6
    toughness = 4
    oracleText = "At the beginning of each end step, each player discards their hand."
    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        effect = Effects.ForEachPlayer(
            Player.ActivePlayerFirst,
            Patterns.Hand.discardHand(EffectTarget.Controller)
        )
    }
    metadata { rarity = Rarity.RARE; collectorNumber = "104" }
}
