package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class NotionThiefScenarioTest : ScenarioTestBase() {
    init {
        for (caster in listOf(1, 2)) {
            test("main-phase draw two with caster $caster steals only opponent draws") {
                val game = scenario().withPlayers()
                    .withCardOnBattlefield(1, "Notion Thief")
                    .withCardInHand(caster, "Divination")
                    .withLandsOnBattlefield(caster, "Island", 3)
                    .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain").withCardInLibrary(2, "Mountain")
                    .withActivePlayer(caster)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                game.castSpell(caster, "Divination").error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe 2
                game.handSize(2) shouldBe 0
            }
        }
        test("opposing Thieves each replace a draw once and return it to the original drawer") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Notion Thief")
                .withCardOnBattlefield(2, "Notion Thief")
                .withCardInHand(2, "Divination").withLandsOnBattlefield(2, "Island", 3)
                .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Mountain").withCardInLibrary(2, "Mountain")
                .withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(2, "Divination").error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe 0
            game.handSize(2) shouldBe 2
        }
        for (earlierDraws in listOf(0, 5)) {
        test("own draw-step exemption uses its entry snapshot after $earlierDraws earlier draws") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Notion Thief")
                .withCardInHand(2, "Think Twice").withLandsOnBattlefield(2, "Island", 2)
                .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Mountain").withCardInLibrary(2, "Mountain")
                .withCardsDrawnThisTurn(2, earlierDraws)
                .withActivePlayer(2).inPhase(Phase.BEGINNING, Step.UPKEEP).build()
            game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
            game.handSize(2) shouldBe 2
            game.castSpell(2, "Think Twice").error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe 1
            game.handSize(2) shouldBe 1
        }
        }
        test("a replaced Looting draw does not cancel the opponent's discard instruction") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Notion Thief")
                .withCardInHand(2, "Faithless Looting")
                .withCardInHand(2, "Shock").withCardInHand(2, "Mountain")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Mountain").withCardInLibrary(2, "Mountain")
                .withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(2, "Faithless Looting").error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe 2
            game.handSize(2) shouldBe 0
        }
        test("removing the Thief before a draw stops the replacement") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Notion Thief")
                .withCardInHand(2, "Murder").withCardInHand(2, "Divination")
                .withLandsOnBattlefield(2, "Swamp", 3).withLandsOnBattlefield(2, "Island", 3)
                .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Mountain").withCardInLibrary(2, "Mountain")
                .withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(2, "Murder", game.findPermanent("Notion Thief")!!).error shouldBe null
            game.resolveStack()
            game.castSpell(2, "Divination").error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe 0
            game.handSize(2) shouldBe 2
        }
    }
}
