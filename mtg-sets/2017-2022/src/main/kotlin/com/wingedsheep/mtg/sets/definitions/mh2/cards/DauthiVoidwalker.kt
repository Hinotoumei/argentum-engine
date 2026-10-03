package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWith
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val DauthiVoidwalker = card("Dauthi Voidwalker") {
    manaCost = "{B}{B}"
    typeLine = "Creature — Dauthi Rogue"
    power = 3
    toughness = 2
    keywords(Keyword.SHADOW)
    oracleText = "Shadow (This creature can block or be blocked by only creatures with shadow.)\nIf a card would be put into an opponent's graveyard from anywhere, instead exile it with a void counter on it.\n{T}, Sacrifice this creature: Choose an exiled card an opponent owns with a void counter on it. You may play it this turn without paying its mana cost."
    replacementEffect(RedirectZoneChangeWith(
        newDestination = Zone.EXILE,
        additionalEffect = Effects.AddCounters(CounterType.of("void"), 1, EffectTarget.AffectedEntity),
        appliesTo = EventPattern.ZoneChangeEvent(
            filter = GameObjectFilter.Any.nontoken().ownedByOpponent(),
            to = Zone.GRAVEYARD,
        ),
    ))
    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            val available = gather(CardSource.FromZone(Zone.EXILE, Player.EachOpponent,
                GameObjectFilter.Any.nontoken().ownedByOpponent().withCounter(CounterType.of("void"))))
            ifNotEmpty(available) {
                val chosen = chooseExactly(1, available, prompt = "Choose an opponent's exiled card with a void counter")
                run(Effects.GrantMayPlayFromExile(chosen))
                run(Effects.GrantPlayWithoutPayingCost(chosen))
            }
        }
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Sidharth Chaturvedi"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dce5db87-4a78-4b8d-b5c2-918ccd1ba4e3.jpg?1783926864"
        ruling("2021-06-18", "If an attacking creature has multiple evasion abilities, such as shadow and flying, a creature can block it only if that creature satisfies all of the appropriate evasion abilities.")
        ruling("2021-06-18", "If your opponent discards a card while you control Dauthi Voidwalker, abilities that function when that card is discarded still work, even though that card never reaches that player's graveyard. In addition, spells or abilities that check the characteristics of the discarded card can find that card in exile.")
        ruling("2021-06-18", "While Dauthi Voidwalker is on the battlefield, nontoken creatures your opponents control won't die. They'll be exiled instead. Abilities that would trigger when those creatures die won't trigger.")
        ruling("2021-06-18", "Tokens still die while Dauthi Voidwalker is on the battlefield.")
        ruling("2021-06-18", "If a card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2021-06-18", "Playing a card with Dauthi Voidwalker's last ability is still subject to normal timing restrictions.")
        ruling("2021-06-18", "If you cast a card this way, you may not cast it for any other alternative costs it has, but you may pay for additional costs, such as kicker costs. If the spell requires an additional cost, you must pay that cost.")
    }
}
