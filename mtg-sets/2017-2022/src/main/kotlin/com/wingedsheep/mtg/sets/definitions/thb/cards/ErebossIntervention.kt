package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val ErebossIntervention = card("Erebos's Intervention") {
    manaCost = "{X}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Target creature gets -X/-X until end of turn. You gain X life.\n" +
        "• Exile up to twice X target cards from graveyards."
    spell {
        val x = DynamicAmounts.xValue()
        modal(chooseCount = 1) {
            mode("Target creature gets -X/-X until end of turn. You gain X life") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(-x, -x, creature) then Effects.GainLife(x)
            }
            mode("Exile up to twice X target cards from graveyards") {
                targets(TargetFilter.CardInGraveyard, optional = true, unlimited = true,
                    dynamicMaxCount = x * 2)
                effect = Effects.ForEachTarget(Effects.Exile(EffectTarget.ContextTarget(0)))
            }
        }
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88e35c80-6cfe-49fc-8138-562233ccf987.jpg"
    }
}
