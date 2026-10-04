package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

val CommitMemory = card("Commit // Memory") {
    layout = CardLayout.SPLIT
    manaCost = "{7}{U}{U}{U}"
    typeLine = "Instant Sorcery"
    colorIdentity = "U"
    face("Commit") {
        manaCost = "{3}{U}"
        typeLine = "Instant"
        oracleText = "Put target spell or nonland permanent into its owner's library second from the top."
        spell {
            val t = target(TargetSpellOrPermanent(permanentFilter = GameObjectFilter.NonlandPermanent))
            effect = Effects.PutIntoLibraryNthFromTop(t, 1)
        }
    }
    face("Memory") {
        manaCost = "{4}{U}{U}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Each player shuffles their hand and graveyard into their library, then draws seven cards."
        keywords(Keyword.AFTERMATH)
        spell {
            effect = Effects.ForEachPlayer(Player.Each, Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You))
                move(hand, CardDestination.ToZone(Zone.LIBRARY, Player.You))
                move(graveyard, CardDestination.ToZone(Zone.LIBRARY, Player.You))
                run(Effects.ShuffleLibrary())
            }) then Effects.DrawCards(7, EffectTarget.PlayerRef(Player.Each))
        }
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "211"
        artist = "Ryan Alexander Lee"
        ruling("2017-04-18", "If a spell is put into its owner's library, it's removed from the stack and thus will not resolve. The spell isn't countered; it just no longer exists. This works against a spell that can't be countered.")
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from any zone other than a graveyard, you can't cast the half with aftermath.")
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from a graveyard, you may cast either half. If you cast the half that has aftermath, you'll exile the card if it would leave the stack.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06c9e2e8-2b4c-4087-9141-6aa25a506626.jpg"
    }
}
