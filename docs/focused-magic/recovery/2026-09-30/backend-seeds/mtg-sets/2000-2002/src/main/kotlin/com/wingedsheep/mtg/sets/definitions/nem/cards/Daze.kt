package com.wingedsheep.mtg.sets.definitions.nem.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/** Focused Magic v0.6.7 overlay: exact active-deck implementation. */
val Daze = card("Daze") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "You may return an Island you control to its owner's hand rather than pay this spell's mana cost.\nCounter target spell unless its controller pays {1}."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(
            Costs.additional.ReturnToHand(
                filter = GameObjectFilter.Land.withSubtype("Island"),
                count = 1,
                youControl = true,
            ),
        ),
    )

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{1}")
    }
}
