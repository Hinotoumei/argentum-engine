package com.wingedsheep.mtg.sets.definitions.wth.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities

val NullRod = card("Null Rod") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Activated abilities of artifacts can't be activated."

    staticAbility {
        ability = PreventActivatedAbilities(GameObjectFilter.Artifact.onBattlefield(), nonManaAbilitiesOnly = false)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "154"
        artist = "Anson Maddocks"
        flavorText = "Gerrard: \"But it doesn't *do* anything!\"\nHanna: \"No—it *does* nothing.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc45f2cb-c256-4a0f-879a-c7db5b1a0b94.jpg?1783946713"
        ruling("2013-07-01", "This effect applies to all activated abilities of artifacts, including mana abilities.")
        ruling("2013-07-01", "This effect does not prevent triggered abilities from triggering and it doesn’t stop the effects of static abilities.")
        ruling("2008-04-01", "This covers only artifacts that are on the battlefield.")
    }
}
