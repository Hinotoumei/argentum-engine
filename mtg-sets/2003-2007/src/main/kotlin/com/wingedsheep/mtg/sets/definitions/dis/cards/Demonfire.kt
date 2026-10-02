package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeCountered
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val Demonfire = card("Demonfire") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Demonfire deals X damage to any target. If a creature dealt damage this way would die this turn, exile it instead.\nHellbent — If you have no cards in hand, this spell can't be countered and the damage can't be prevented."
    staticAbility { ability = ConditionalStaticAbility(CantBeCountered, Conditions.CardsInHandAtMost(0)) }
    spell {
        val recipient = target(Targets.Any)
        effect = Effects.If(Conditions.CardsInHandAtMost(0),
            Effects.DealDamage(DynamicAmounts.xValue(), recipient, cantBePrevented = true),
            Effects.DealDamage(DynamicAmounts.xValue(), recipient)) then
            Effects.ForEachInGroup(GroupFilter(GameObjectFilter.Creature.wasDealtDamageBySourceThisTurn()),
                Effects.MarkExileOnDeath(EffectTarget.IterationEntity))
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af2ad333-722e-4d7e-972a-903c24068931.jpg?1783943425"
    }
}
