package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

val KeldonMegaliths = card("Keldon Megaliths") {
    typeLine = "Land"
    colorIdentity = "R"
    oracleText = "Keldon Megaliths enters tapped.\n{T}: Add {R}.\nHellbent — {1}{R}, {T}: Keldon Megaliths deals 1 damage to any target. Activate only if you have no cards in hand."
    replacementEffect(EntersTapped())
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.Tap)
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.EmptyHand))
    }
    metadata { rarity = Rarity.RARE; collectorNumber = "170" }
}
