package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.LoyaltyAbilityBuilder
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedLoyaltyAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private fun fractalForX() =
    Effects.CreateToken(
        power = 0,
        toughness = 0,
        colors = setOf(Color.GREEN, Color.BLUE),
        creatureTypes = setOf("Fractal"),
    ) then Effects.AddDynamicCounters(
        CounterType.PLUS_ONE_PLUS_ONE,
        DynamicAmounts.xValue(),
        EffectTarget.PipelineTarget(CREATED_TOKENS, 0),
    )

private fun kasminaUltimate() = Effects.Pipeline {
    val candidates = gather(
        CardSource.FromZone(
            Zone.LIBRARY,
            Player.You,
            GameObjectFilter.InstantOrSorcery.sharingColorWith(EffectTarget.Self),
        ),
        search = true,
    )
    val found = chooseUpTo(
        1,
        from = candidates,
        prompt = "Search your library for an instant or sorcery card that shares a color with this planeswalker",
        selectedLabel = "Exile and cast for free",
    )
    reveal(found, revealToSelf = false)
    exile(found)
    run(Effects.ShuffleLibrary())
    run(EmitLibrarySearchedEventEffect)
    ifNotEmpty(found) {
        run(
            Effects.May(
                Effects.CastFromCollectionWithoutPayingCost(found),
                descriptionOverride = "You may cast that card without paying its mana cost.",
            )
        )
    }
}

private fun grantedLoyaltyAbilityX(init: LoyaltyAbilityBuilder.() -> Unit) =
    LoyaltyAbilityBuilder(AbilityCost.LoyaltyX).apply(init).build()

val KasminaEnigmaSage = card("Kasmina, Enigma Sage") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Planeswalker — Kasmina"
    startingLoyalty = 2
    oracleText = "Each other planeswalker you control has the loyalty abilities of Kasmina, Enigma Sage.\n" +
        "+2: Scry 1.\n" +
        "−X: Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters on it.\n" +
        "−8: Search your library for an instant or sorcery card that shares a color with this planeswalker, exile that card, then shuffle. You may cast that card without paying its mana cost."

    loyaltyAbility(+2) { effect = Patterns.Library.scry(1) }
    loyaltyAbilityX { effect = fractalForX() }
    loyaltyAbility(-8) { effect = kasminaUltimate() }

    val otherPlaneswalkersYouControl = GroupFilter(GameObjectFilter.Planeswalker.youControl()).other()
    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedLoyaltyAbility(+2) { effect = Patterns.Library.scry(1) },
            filter = otherPlaneswalkersYouControl,
        )
    }
    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedLoyaltyAbilityX { effect = fractalForX() },
            filter = otherPlaneswalkersYouControl,
        )
    }
    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedLoyaltyAbility(-8) { effect = kasminaUltimate() },
            filter = otherPlaneswalkersYouControl,
        )
    }

    metadata { rarity = Rarity.MYTHIC; collectorNumber = "196" }
}
