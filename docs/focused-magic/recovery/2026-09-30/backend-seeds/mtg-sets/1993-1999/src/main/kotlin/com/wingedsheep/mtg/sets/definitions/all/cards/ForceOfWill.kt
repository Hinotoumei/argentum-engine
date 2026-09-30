package com.wingedsheep.mtg.sets.definitions.all.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/** Focused Magic v0.6.7 overlay: exact active-deck implementation. */
val ForceOfWill = card("Force of Will") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "You may pay 1 life and exile a blue card from your hand rather than pay this spell's mana cost.\nCounter target spell."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(
            Costs.additional.PayLife(1),
            Costs.additional.ExileCards(
                count = 1,
                filter = GameObjectFilter.Any.withColor(Color.BLUE),
                fromZone = CostZone.HAND,
            ),
        ),
    )

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpell()
    }
}
