package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val TasteForMayhem = card("Taste for Mayhem") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +2/+0.\nHellbent — Enchanted creature gets an additional +2/+0 as long as you have no cards in hand."

    auraTarget = TargetObject(filter = TargetFilter.Creature)
    staticAbility {
        ability = ModifyStats(2, 0, GroupFilter.attachedCreature())
    }
    staticAbility {
        ability = ConditionalStaticAbility(
            ModifyStats(2, 0, GroupFilter.attachedCreature()),
            Conditions.EmptyHand
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Greg Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20fbb2eb-5738-46e5-a0a6-8ee98c7c8cfb.jpg?1783943417"
        flavorText = "The taste of blood breaks down what little self-control the Rakdos possess."
    }
}
