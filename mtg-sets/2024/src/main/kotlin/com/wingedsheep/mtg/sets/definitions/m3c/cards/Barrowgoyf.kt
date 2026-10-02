package com.wingedsheep.mtg.sets.definitions.m3c.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

val Barrowgoyf = card("Barrowgoyf") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Lhurgoyf"
    keywords(Keyword.DEATHTOUCH, Keyword.LIFELINK)
    dynamicStats(DynamicAmounts.zone(Player.Each, Zone.GRAVEYARD).distinctTypes(), toughnessOffset = 1)
    oracleText = "Deathtouch, lifelink\nBarrowgoyf's power is equal to the number of card types among cards in all graveyards and its toughness is equal to that number plus 1.\nWhenever this creature deals combat damage to a player, you may mill that many cards. If you do, you may put a creature card from among them into your hand."

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.May(
            Effects.Pipeline {
                val milled = mill(DynamicAmounts.triggerDamageAmount())
                val selected = chooseUpTo(1, from = milled,
                    filter = GameObjectFilter.Creature.currentlyIn(Zone.GRAVEYARD),
                    prompt = "You may put a milled creature card into your hand",
                    selectedLabel = "Put in hand", remainderLabel = "Leave in graveyard")
                toHand(selected)
            },
            prompt = "Mill cards equal to the combat damage?"
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "102"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cea3b218-0e6e-443a-84f7-380f1021e8e1.jpg?1783911410"
        ruling("2024-06-07", "The ability that defines Barrowgoyf's power and toughness works in all zones, not just the battlefield.")
        ruling("2024-06-07", "The ability that defines Barrowgoyf's power and toughness counts card types, not cards. If the only card in all graveyards is a single artifact creature card, Barrowgoyf will be a 2/3. If the cards in all graveyards are ten artifact cards and ten creature cards, Barrowgoyf will still be a 2/3.")
        ruling("2024-06-07", "Card types that can appear on cards in a graveyard are artifact, battle, creature, enchantment, instant, kindred, land, planeswalker, and sorcery. Legendary, basic, and snow are supertypes, not card types; Lhurgoyf, Forest, and Siege are subtypes, not card types.")
    }
}
