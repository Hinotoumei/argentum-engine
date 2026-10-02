package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val Persist = card("Persist") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return target nonlegendary creature card from your graveyard to the battlefield with a -1/-1 counter on it."
    spell {
        targets(TargetFilter(GameObjectFilter.Creature.nonlegendary().ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Pipeline {
            val returned = gather(CardSource.ChosenTargets)
            move(returned, CardDestination.WithEntryCounters(
                CardDestination.ToZone(Zone.BATTLEFIELD),
                mapOf(CounterType.MINUS_ONE_MINUS_ONE to 1)
            ))
        }
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Milivoj Ćeran"
        flavorText = "In the tight-knit doun of Mistmeadow, death is less important than duty."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90f390c3-af1c-424f-9721-e26e9321e5a3.jpg?1783926857"
    }
}
