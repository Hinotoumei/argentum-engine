package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.mtg.sets.definitions.zen.cards.VerdantCatacombs

class VerdantCatacombsScenarioTest : FunSpec({
    for (choice in listOf("Swamp", "Forest", "none")) {
        test("Verdant Catacombs pays life and sacrifice, searches for $choice, and shuffles") {
            val d = GameTestDriver()
            d.registerCards(listOf(VerdantCatacombs) + listOf("Swamp", "Forest", "Plains").map { CardDefinition.basicLand(it, Subtype(it)) })
            d.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = d.activePlayer!!
            val first = d.putCardOnTopOfLibrary(me, "Swamp")
            val second = d.putCardOnTopOfLibrary(me, "Forest")
            val wrong = d.putCardOnTopOfLibrary(me, "Plains")
            val land = d.putLandOnBattlefield(me, "Verdant Catacombs")
            d.submit(ActivateAbility(me, land, VerdantCatacombs.script.activatedAbilities.single().id)).outcome shouldBe Outcome.Done
            d.getLifeTotal(me) shouldBe 19
            d.getGraveyard(me) shouldContain land
            d.bothPass()
            val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.options shouldContain first
            decision.options shouldContain second
            decision.options shouldNotContain wrong
            val beforeInvalid = d.state
            d.submit(SubmitDecision(me, CardsSelectedResponse(decision.id, listOf(wrong)))).outcome.shouldBeInstanceOf<Outcome.Rejected>()
            d.state shouldBe beforeInvalid
            val chosen = when (choice) { "Swamp" -> listOf(first); "Forest" -> listOf(second); else -> emptyList() }
            val shuffles = d.events.count { it is LibraryShuffledEvent }
            d.submitCardSelection(me, chosen).error shouldBe null
            for (id in chosen) {
                d.state.getBattlefield() shouldContain id
                d.isTapped(id) shouldBe false
            }
            d.events.count { it is LibraryShuffledEvent } shouldBe shuffles + 1
        }
    }
})
