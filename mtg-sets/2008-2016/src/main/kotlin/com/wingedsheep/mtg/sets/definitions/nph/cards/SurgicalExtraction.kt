package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val SurgicalExtraction = card("Surgical Extraction") {
    manaCost = "{B/P}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "({B/P} can be paid with either {B} or 2 life.)\nChoose target card in a graveyard other than a basic land card. Search its owner's graveyard, hand, and library for any number of cards with the same name as that card and exile them. Then that player shuffles."
    spell {
        target(TargetFilter(GameObjectFilter.Any.nonbasic(), zone = Zone.GRAVEYARD))
        val owner = Player.OwnerOf("target card")
        effect = Effects.Pipeline {
            val shuffler = storePlayer(owner)
            val chosenName = storeCardName(gather(CardSource.ChosenTargets))
            val searched = gather(CardSource.FromMultipleZones(
                zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                player = owner
            ), search = true)
            val matches = filter(searched, GameObjectFilter.Any.namedFromVariable(chosenName))
            val selected = chooseAnyNumber(from = matches, prompt = "Choose cards to exile")
            exile(selected, owner = owner)
            run(Effects.ForEachPlayer(shuffler.asPlayers, Effects.ShuffleLibrary()))
        }
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dca7e072-edb5-4f7e-bdec-a3a393053c80.jpg?1783941310"
        ruling("2011-06-01", "A card with Phyrexian mana symbols in its mana cost is each color that appears in that mana cost, regardless of how that cost may have been paid.")
        ruling("2011-06-01", "To calculate the mana value of a card with Phyrexian mana symbols in its cost, count each Phyrexian mana symbol as 1.")
        ruling("2011-06-01", "As you cast a spell or activate an activated ability with one or more Phyrexian mana symbols in its cost, you choose how to pay for each Phyrexian mana symbol at the same time you would choose modes or choose a value for X.")
        ruling("2011-06-01", "If you're at 1 life or less, you can't pay 2 life.")
        ruling("2011-06-01", "Phyrexian mana is not a new color. Players can't produce Phyrexian mana.")
        ruling("2011-06-01", "\"Any number of cards\" means just that. If you wish, you can choose to leave some or all of the cards with the same name as the targeted card, including that card, in the zone they're in.")
    }
}
