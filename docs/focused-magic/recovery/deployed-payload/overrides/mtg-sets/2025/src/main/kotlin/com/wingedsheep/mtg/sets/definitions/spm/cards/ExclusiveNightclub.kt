package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayhem
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Arena alternate name for Oscorp Industries. */
val ExclusiveNightclub = card("Exclusive Nightclub") {
    typeLine = "Land"
    colorIdentity = "UBR"
    oracleText = "This land enters tapped.\n" +
        "When this land enters from a graveyard, you lose 2 life.\n" +
        "{T}: Add {U}, {B}, or {R}.\n" +
        "Mayhem (You may play this card from your graveyard if you discarded it this turn. Timing rules still apply.)"

    replacementEffect(EntersTapped())
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(
            condition = Conditions.TriggeringEntityEnteredOrWasCastFromGraveyard,
            then = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.You))
        )
    }
    activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.BLUE); manaAbility = true; timing = TimingRule.ManaAbility }
    activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.BLACK); manaAbility = true; timing = TimingRule.ManaAbility }
    activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.RED); manaAbility = true; timing = TimingRule.ManaAbility }
    mayhem("")

    metadata { rarity = Rarity.RARE; collectorNumber = "182" }
}
