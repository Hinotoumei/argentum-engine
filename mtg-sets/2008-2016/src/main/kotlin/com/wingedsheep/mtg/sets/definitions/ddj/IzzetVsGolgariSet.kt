package com.wingedsheep.mtg.sets.definitions.ddj

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Duel Decks: Izzet vs. Golgari (2012)
 *
 * Scaffolded to hold the canonical [CardDefinition]s of cards whose earliest real printing is
 * Duel Decks: Izzet vs. Golgari, with later sets contributing reprint [Printing] rows. Intentionally
 * incomplete relative to the official set.
 *
 * Set Code: DDJ
 * Release Date: 2012-09-07
 */
object IzzetVsGolgariSet : MtgSet {

    override val code = "DDJ"
    override val displayName = "Duel Decks: Izzet vs. Golgari"
    override val releaseDate = "2012-09-07"
    override val incomplete = true
    override val sealedSupported = false

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.ddj.cards"
}
