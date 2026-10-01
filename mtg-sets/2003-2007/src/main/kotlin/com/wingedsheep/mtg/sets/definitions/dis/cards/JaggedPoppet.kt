package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val JaggedPoppet = card("Jagged Poppet") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Ogre Warrior"
    power = 3
    toughness = 4
    oracleText = "Whenever this creature is dealt damage, discard that many cards.\nHellbent — Whenever this creature deals combat damage to a player, if you have no cards in hand, that player discards cards equal to the damage."

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.Discard(DynamicAmounts.triggerDamageAmount())
        description = "Whenever this creature is dealt damage, discard that many cards."
    }
    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        interveningIf = Conditions.EmptyHand
        effect = Effects.Discard(DynamicAmounts.triggerDamageAmount(), EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever this creature deals combat damage to a player, if you have no cards in hand, that player discards cards equal to the damage."
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Jeff Miracola"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e796a838-3599-4769-b90b-9bbbaad76fcb.jpg?1783943400"
        flavorText = "Few puppets are so willing."
    }
}
