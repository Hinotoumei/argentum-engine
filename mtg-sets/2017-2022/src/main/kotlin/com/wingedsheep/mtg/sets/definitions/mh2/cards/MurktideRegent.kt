package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val MurktideRegent = card("Murktide Regent") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Dragon"
    power = 3
    toughness = 3
    oracleText = "Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)\nFlying\nThis creature enters with a +1/+1 counter on it for each instant and sorcery card exiled with it.\nWhenever an instant or sorcery card leaves your graveyard, put a +1/+1 counter on this creature."
    keywords(Keyword.DELVE, Keyword.FLYING)
    replacementEffect(EntersWithDynamicCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = DynamicAmounts.cardsExiledForDelve(CardType.INSTANT, CardType.SORCERY)
    ))
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.InstantOrSorcery.ownedByYou()).changesZone(from = Zone.GRAVEYARD)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "52"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20c4aae1-7665-4df7-bd51-a1d95bf8a17d.jpg?1783926875"
        ruling("2021-06-18", "Delve doesn't change a spell's mana cost or mana value. For example, Murktide Regent's mana value is 7 even if you exiled three cards to cast it.")
        ruling("2021-06-18", "You can exile cards to pay only for generic mana, and you can't exile more cards than the generic mana requirement of a spell with delve. For example, you can't exile more than five cards from your graveyard to cast Murktide Regent unless an effect has increased its cost.")
        ruling("2021-06-18", "Because delve isn't an alternative cost, it can be used in conjunction with alternative costs, such as flashback. It can also be used to pay for additional costs that include generic mana.")
    }
}
