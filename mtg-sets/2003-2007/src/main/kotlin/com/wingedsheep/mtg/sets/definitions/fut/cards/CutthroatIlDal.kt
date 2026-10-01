package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

val CutthroatIlDal = card("Cutthroat il-Dal") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 4
    toughness = 1
    oracleText = "Hellbent — This creature has shadow as long as you have no cards in hand. (It can block or be blocked by only creatures with shadow.)"

    staticAbility {
        ability = ConditionalStaticAbility(GrantKeyword(Keyword.SHADOW, GroupFilter.source()), Conditions.EmptyHand)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "64"
        artist = "Vance Kovacs"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/192bc089-696d-41b4-a02f-d7dc166feedd.jpg?1783943115"
        flavorText = "Her blades slashed across the noble's shadow. He scoffed, then fell over dead."
        ruling("2021-03-19", "Once a creature has been blocked, that creature remains blocked and will deal and be dealt combat damage even if it gains or loses shadow or if the blocking creature gains or loses shadow.")
        ruling("2021-03-19", "If an attacking creature has multiple evasion abilities, such as shadow and flying, a creature can block it only if that creature satisfies all of the appropriate evasion abilities.")
        ruling("2021-03-19", "Multiple instances of shadow on the same creature are redundant.")
    }
}
