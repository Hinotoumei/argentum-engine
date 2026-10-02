package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val SheoldredsEdict = card("Sheoldred's Edict") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Each opponent sacrifices a nontoken creature of their choice.\n• Each opponent sacrifices a creature token of their choice.\n• Each opponent sacrifices a planeswalker of their choice."

    spell {
        modal(chooseCount = 1) {
            mode("Each opponent sacrifices a nontoken creature of their choice") {
                effect = Effects.Pipeline {
                    val selected = forEachPlayerCollecting(Player.EachOpponent) {
                        val eligible = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature.nontoken()))
                        val choice = chooseExactly(1, from = eligible, prompt = "Choose a permanent to sacrifice")
                        // Announce this public battlefield choice before the next opponent chooses.
                        // RevealCollection is informational only: every sacrifice still happens below.
                        reveal(choice)
                        listOf(choice)
                    }.single()
                    sacrifice(selected)
                }
            }
            mode("Each opponent sacrifices a creature token of their choice") {
                effect = Effects.Pipeline {
                    val selected = forEachPlayerCollecting(Player.EachOpponent) {
                        val eligible = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature.token()))
                        val choice = chooseExactly(1, from = eligible, prompt = "Choose a permanent to sacrifice")
                        // Announce this public battlefield choice before the next opponent chooses.
                        // RevealCollection is informational only: every sacrifice still happens below.
                        reveal(choice)
                        listOf(choice)
                    }.single()
                    sacrifice(selected)
                }
            }
            mode("Each opponent sacrifices a planeswalker of their choice") {
                effect = Effects.Pipeline {
                    val selected = forEachPlayerCollecting(Player.EachOpponent) {
                        val eligible = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Planeswalker))
                        val choice = chooseExactly(1, from = eligible, prompt = "Choose a permanent to sacrifice")
                        // Announce this public battlefield choice before the next opponent chooses.
                        // RevealCollection is informational only: every sacrifice still happens below.
                        reveal(choice)
                        listOf(choice)
                    }.single()
                    sacrifice(selected)
                }
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Helge C. Balzer"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9225cc3-90f0-448f-a8d9-7c6c2796d077.jpg?1783918041"
        flavorText = "\"Congratulations. I am entertained.\""
    }
}
