package com.wingedsheep.mtg.sets.definitions.`5dn`.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter

val EngineeredExplosives = card("Engineered Explosives") {
    manaCost = "{X}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Sunburst (This artifact enters with a charge counter on it for each color of mana spent to cast it.)\n{2}, Sacrifice this artifact: Destroy each nonland permanent with mana value equal to the number of charge counters on this artifact."
    replacementEffect(EntersWithDynamicCounters(
        counterType = CounterType.CHARGE,
        count = DynamicAmounts.colorsOfManaSpent()
    ))
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
        effect = Effects.DestroyAll(GameObjectFilter.NonlandPermanent.manaValueEqualsDynamic(
            DynamicAmounts.lastKnownSourceCounters(CounterType.CHARGE)
        ))
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "118"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/8492a272-e595-4f94-a6eb-08d29f211fd6.jpg?1783944383"
        ruling("2020-08-07", "You can choose any value for X as you cast Engineered Explosives. The value chosen for X doesn't directly affect the number of charge counters Engineered Explosives enters the battlefield with, but it does let you pay more mana and thus spend more colors of mana to cast it.")
        ruling("2020-08-07", "Colorless mana won't give Engineered Explosives another charge counter. Colorless is not a color.")
        ruling("2020-08-07", "Tokens that aren't a copy of something else don't have a mana cost. Anything without a mana cost normally has a mana value of 0.")
        ruling("2020-08-07", "If a permanent has {X} in its mana cost, X is considered to be 0.")
    }
}
