package com.wingedsheep.mtg.sets.definitions.ice.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val Hydroblast = card("Hydroblast") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Counter target spell if it's red.\n• Destroy target permanent if it's red."

    // Color is a resolution condition; any spell/permanent is a legal target.
    spell {
        modal(chooseCount = 1) {
            mode("Counter target spell if it's red") {
                val spell = target(TargetFilter.SpellOnStack.withCardPredicate(
                    CardPredicate.Not(CardPredicate.IsActivatedOrTriggeredAbility)
                ))
                effect = Effects.If(
                    Conditions.TargetMatchesFilter(GameObjectFilter.Any.withColor(Color.RED), spell),
                    Effects.CounterSpell()
                )
            }
            mode("Destroy target permanent if it's red") {
                val permanent = target(TargetFilter.Permanent)
                effect = Effects.If(
                    Conditions.TargetMatchesFilter(GameObjectFilter.Any.withColor(Color.RED), permanent),
                    Effects.Destroy(permanent)
                )
            }
        }
    }

    metadata {
        ruling("2016-06-08", "Hydroblast can target any spell or permanent, not just a red one. It checks the color of the target only on resolution.")
        ruling("2004-10-04", "The decision to counter a spell or destroy a permanent is a decision made on announcement before the target is selected. If the spell is redirected, this mode can't be changed, so only targets of the selected type are valid.")
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Kaja Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f62716f0-fde2-49ef-b8a4-c1b03f451194.jpg?1783947514"
        flavorText = "\"Heed the lessons of our time: the forms of water may move the land itself and hold captive the fires within.\"\n—Gustha Ebbasdotter, Kjeldoran Royal Mage"
    }
}
