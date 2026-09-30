package com.wingedsheep.mtg.sets.definitions.nem.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/** Recovered Focused Magic source; gameplay is covered by dedicated scenarios. */
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

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Matthew D. Wilson"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d03bff25-0d5e-4dcf-8d75-6df846afea3b.jpg?1789015977"
    }
}
