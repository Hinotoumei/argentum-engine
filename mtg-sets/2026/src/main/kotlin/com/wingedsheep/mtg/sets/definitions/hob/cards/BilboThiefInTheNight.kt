package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Bilbo, Thief in the Night
 * {1}{U}
 * Legendary Creature — Halfling Rogue (The Hobbit, mythic)
 *
 * "Spells you cast from anywhere other than your hand cost {1} less to cast.
 *  Whenever Bilbo attacks, you may cast an artifact, instant, or sorcery spell from your graveyard.
 *  If an instant or sorcery spell cast this way would be put into your graveyard, exile it instead."
 *
 * The attack trigger offers one paid cast during resolution. The chosen spell face supplies
 * eligibility and the instant/sorcery exile rider; no turn-long permission remains afterwards.
 *
 * Rulings (2026-08-14):
 *  - Cost reduction applies after cost increases and only to generic mana; it can't reduce a
 *    colored requirement. Mana value is unchanged by it.
 *  - An Adventure instant or sorcery may be cast from the graveyard this way. If it resolves, the
 *    Adventure's own replacement exiles it and you may cast the permanent later; if it fails to
 *    resolve, Bilbo's replacement exiles it and you may not.
 */
val BilboThiefInTheNight = card("Bilbo, Thief in the Night") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Halfling Rogue"
    oracleText = "Spells you cast from anywhere other than your hand cost {1} less to cast.\n" +
        "Whenever Bilbo attacks, you may cast an artifact, instant, or sorcery spell from your " +
        "graveyard. If an instant or sorcery spell cast this way would be put into your graveyard, " +
        "exile it instead."
    power = 2
    toughness = 2

    // "Spells you cast from anywhere other than your hand cost {1} less to cast."
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCastFromZones(
                zones = Zone.entries.toSet() - Zone.HAND,
                filter = GameObjectFilter.Any,
            ),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    // "Whenever Bilbo attacks, you may cast an artifact, instant, or sorcery spell from your
    // graveyard. If an instant or sorcery spell cast this way would be put into your graveyard,
    // exile it instead."
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You))
            val chosen = chooseSpell(
                from = graveyard,
                filter = GameObjectFilter.Artifact or GameObjectFilter.InstantOrSorcery,
                prompt = "You may cast an artifact, instant, or sorcery from your graveyard",
                showAllCards = true,
            )
            run(Effects.CastFromCollection(
                from = chosen,
                insteadOfGraveyard = AfterResolveDestination.EXILE,
                riderOnlyInstantOrSorcery = true,
            ))
        }
        description = "Whenever Bilbo attacks, you may cast an artifact, instant, or sorcery spell " +
            "from your graveyard. If an instant or sorcery spell cast this way would be put into " +
            "your graveyard, exile it instead."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "33"
        artist = "Nia Kovalevski"
        flavorText = "\"I am an honest burglar, more or less.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/8/484c7f83-8339-4ae1-8350-68ce1f7d05a3.jpg?1783902786"

        ruling("2026-08-14", "To determine the total cost of a spell, start with the mana cost or " +
            "alternative cost you're paying, add any cost increases, then apply any cost reductions " +
            "(such as that of Bilbo, Thief in the Night). The mana value of the spell is determined " +
            "only by its mana cost, no matter what the total cost to cast the spell was.")
        ruling("2026-08-14", "The cost reduction applies only to generic mana in the costs of " +
            "spells you cast from anywhere other than your hand. It can't reduce requirements of a " +
            "specific color of mana.")
        ruling("2026-08-14", "You can choose to cast an Adventure instant or sorcery spell from " +
            "your graveyard as Bilbo's ability resolves. If that Adventure spell resolves, you can " +
            "exile it using the replacement effect associated with the Adventure, and you can cast " +
            "the permanent spell later from exile. If that Adventure spell fails to resolve " +
            "(because it's countered or its targets become illegal), that card is exiled by the " +
            "replacement effect created by Bilbo's ability; you can't cast the permanent spell " +
            "later from exile.")
    }
}
