package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val CurrencyConverter = card("Currency Converter") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever you discard a card, you may exile that card from your graveyard.\n{2}, {T}: Draw a card, then discard a card.\n{T}: Put a card exiled with this artifact into its owner's graveyard. If it's a land card, create a Treasure token. If it's a nonland card, create a 2/2 black Rogue creature token."
    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Any.withStatePredicate(StatePredicate.InZone(Zone.GRAVEYARD)),
                EffectTarget.TriggeringEntity,
            ),
            then = Effects.May(Effects.Move(EffectTarget.TriggeringEntity, Zone.EXILE,
                fromZone = Zone.GRAVEYARD, linkToSource = true)),
        )
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.DrawCards(1) then Effects.Discard(1)
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Pipeline {
            val pile = gather(CardSource.FromLinkedExile())
            ifNotEmpty(pile) {
                val selected = chooseExactly(1, pile, prompt = "Choose a card exiled with Currency Converter")
                val types = filterSplit(selected, GameObjectFilter.Land)
                move(selected, CardDestination.ToZone(Zone.GRAVEYARD))
                ifNotEmpty(types.matching) { run(Effects.CreateTreasure()) }
                ifNotEmpty(types.rest) { run(Effects.CreateToken(2, 2, colors = setOf(Color.BLACK), creatureTypes = setOf("Rogue"))) }
            }
        }
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Sean Murray"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/187b6719-e5ed-4615-a00b-3313ceca055b.jpg?1783923344"
    }
}
