package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayBeginGameOnBattlefield
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Gemstone Caverns
 * Legendary Land
 *
 * Focused Magic 0.6.7 uses Argentum's opening-hand battlefield phase for the
 * pregame action. The engine patch supplies Gemstone's extra requirements:
 * not playing first, exile another card, and enter with a luck counter.
 */
val GemstoneCaverns = card("Gemstone Caverns") {
    typeLine = "Legendary Land"
    colorIdentity = ""
    oracleText = "If Gemstone Caverns is in your opening hand and you're not playing first, " +
        "you may begin the game with it on the battlefield with a luck counter on it. " +
        "If you do, exile a card from your hand.\n" +
        "{T}: Add {C}. If Gemstone Caverns has a luck counter on it, instead add one mana of any color."

    mayBeginGameOnBattlefield()

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.Not(Conditions.SourceHasCounter(CounterType.LUCK))
            )
        )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.SourceHasCounter(CounterType.LUCK)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "274"
    }
}
