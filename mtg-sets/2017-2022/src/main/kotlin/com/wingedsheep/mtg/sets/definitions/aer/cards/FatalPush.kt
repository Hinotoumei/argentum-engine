package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

// Any creature is a legal target. Both gates are evaluated when the spell resolves.
val FatalPush = card("Fatal Push") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature if it has mana value 2 or less.\nRevolt — Destroy that creature if it has mana value 4 or less instead if a permanent left the battlefield under your control this turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.YouHadPermanentLeaveBattlefieldThisTurn,
            then = Effects.If(
                Conditions.TargetMatchesFilter(GameObjectFilter.Creature.manaValueAtMost(4), creature),
                Effects.Destroy(creature)
            ),
            otherwise = Effects.If(
                Conditions.TargetMatchesFilter(GameObjectFilter.Creature.manaValueAtMost(2), creature),
                Effects.Destroy(creature)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5e81649-9954-424c-89d1-f87d73b66047.jpg?1783936764"
        ruling("2020-08-07", "Fatal Push can target any creature, even one with mana value 5 or greater. The creature's mana value is checked only as Fatal Push resolves.")
        ruling("2020-08-07", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to. They're equally satisfied by an artifact you sacrificed to pay a cost, a creature you controlled that was destroyed by Cast Down, or an enchantment you returned to your hand with Cyclonic Rift.")
        ruling("2020-08-07", "Tokens that leave the battlefield will satisfy a revolt ability.")
    }
}
