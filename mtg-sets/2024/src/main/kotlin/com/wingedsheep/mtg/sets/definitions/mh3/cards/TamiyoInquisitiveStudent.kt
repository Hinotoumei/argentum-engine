package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private val TamiyoStudentFront = card("Tamiyo, Inquisitive Student") {
    manaCost = "{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Moonfolk Wizard"
    power = 0
    toughness = 3
    oracleText = "Flying\nWhenever Tamiyo attacks, investigate. (Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")\nWhen you draw your third card in a turn, exile Tamiyo, then return her to the battlefield transformed under her owner's control."
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Investigate()
    }
    triggeredAbility {
        trigger = Triggers.you.drawsNth(3)
        effect = Effects.ExileAndReturnTransformed()
    }
    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "242"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a717b98-cdac-416d-bf6c-f6b6638e65d1.jpg?1783911236"
        ruling("2024-06-07", "In some rare cases, a spell or ability may cause Tamiyo, Inquisitive Student to transform while she's a creature (front face up) on the battlefield. If this happens, Tamiyo, Seasoned Scholar won't have any loyalty counters on her and will subsequently be put into her owner's graveyard.")
        ruling("2024-06-07", "You can activate one of Tamiyo, Seasoned Scholar's loyalty abilities the turn she enters the battlefield. However, you may do so only during one of your main phases when the stack is empty. For example, if Tamiyo, Seasoned Scholar enters the battlefield during combat, there will be an opportunity for your opponent to remove her before you can activate one of her abilities.")
        ruling("2024-06-07", "If multiple effects modify your hand size, apply them in timestamp order. For example, if you put Necrodominance (an enchantment that says your maximum hand size is five) onto the battlefield and then activate Tamiyo, Seasoned Scholar's last ability, you'll have no maximum hand size. However, if the emblem from Tamiyo, Seasoned Scholar's last ability was created before you put Necrodominance onto the battlefield, your maximum hand size would be five.")
        ruling("2024-06-07", "Each face of a transforming double-faced card has its own set of characteristics: name, types, subtypes, abilities, and so on. While a transforming double-faced permanent is on the battlefield, consider only the characteristics of the face that's currently up. The other set of characteristics is ignored.")
        ruling("2024-06-07", "Each transforming double-faced card in this set is cast with its front face up. In every zone other than the battlefield, consider only the characteristics of its front face. If it is on the battlefield, consider only the characteristics of the face that's up; the other face's characteristics are ignored.")
        ruling("2024-06-07", "The mana value of a transforming double-faced card is the mana value of its front face, no matter which face is up.")
        ruling("2024-06-07", "The back face of a transforming double-faced card usually has a color indicator that defines its color. Colorless back faces, such as lands, do not.")
        ruling("2024-06-07", "In the Commander variant, a double-faced card's color identity is determined by the mana costs and mana symbols in the rules text of both faces combined. If either face has a color indicator or basic land type, those are also considered. For example, Ral, Monsoon Mage's color identity is blue and red, since its front face is red and its back face has a blue and red color indicator.")
        ruling("2024-06-07", "A transforming double-faced card enters the battlefield with its front face up by default, unless a spell or ability instructs you to put it onto the battlefield transformed or allows you to cast it transformed, in which case it enters with its back face up.")
        ruling("2024-06-07", "If you are instructed to put a card that isn't a double-faced card onto the battlefield transformed, it will not enter the battlefield at all. In that case, it stays in the zone it was previously in. For example, if a single-faced card is a copy of Ral, Monsoon Mage, choosing to exile that permanent during the resolution of its triggered ability will cause it to remain in exile.")
    }
}

private val TamiyoScholarBack = card("Tamiyo, Seasoned Scholar") {
    manaCost = ""
    colorIdentity = "GU"
    colorIndicator = "GU"
    typeLine = "Legendary Planeswalker — Tamiyo"
    startingLoyalty = 2
    oracleText = "+2: Until your next turn, whenever a creature attacks you or a planeswalker you control, it gets -1/-0 until end of turn.\n−3: Return target instant or sorcery card from your graveyard to your hand. If it's a green card, add one mana of any color.\n−7: Draw cards equal to half the number of cards in your library, rounded up. You get an emblem with \"You have no maximum hand size.\""
    loyaltyAbility(+2) {
        description = "+2: Until your next turn, whenever a creature attacks you or a planeswalker you control, it gets -1/-0 until end of turn."
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.a(GameObjectFilter.Creature.attackingYouOrYourPlaneswalkers()).attacks(),
            effect = Effects.ModifyStats(-1, 0, EffectTarget.TriggeringEntity, Duration.EndOfTurn),
            expiry = DelayedTriggerExpiry.UntilControllersNextTurn
        )
    }
    loyaltyAbility(-3) {
        description = "-3: Return target instant or sorcery card from your graveyard to your hand. If it's a green card, add one mana of any color."
        targets(TargetFilter(GameObjectFilter.InstantOrSorcery.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Pipeline {
            val returned = gather(CardSource.ChosenTargets)
            toHand(returned)
            // Match the returned card's current characteristics, without reusing a stale target stamp.
            ifNotEmpty(returned, filter = GameObjectFilter.Any.withColor(Color.GREEN)) {
                run(Effects.AddAnyColorMana())
            }
        }
    }
    loyaltyAbility(-7) {
        description = "-7: Draw cards equal to half the number of cards in your library, rounded up. You get an emblem with \"You have no maximum hand size.\""
        effect = Effects.DrawCards(DynamicAmounts.zone(Player.You, Zone.LIBRARY).count() divRoundedUp 2) then
            Effects.CreatePermanentEmblem(
                ownedStaticAbilities = listOf(NoMaximumHandSize),
                emblemDescription = "You have no maximum hand size."
            )
    }
    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "242"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/back/2/a/2a717b98-cdac-416d-bf6c-f6b6638e65d1.jpg?1783911236"
    }
}

val TamiyoInquisitiveStudent: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TamiyoStudentFront,
    backFace = TamiyoScholarBack
)
