package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PayXLifeResolutionTest : FunSpec({
    fun assertVisibleX(d: GameTestDriver, spellId: EntityId, expected: Int) {
        val transformer = ClientStateTransformer(d.cardRegistry,
            predicateEvaluator = PredicateEvaluator(cardRegistry = d.cardRegistry))
        for (viewer in listOf(d.player1, d.player2)) {
            val visibleSpell = transformer.transform(d.state, viewer).cards[spellId]!!
            visibleSpell.chosenX shouldBe expected
        }
    }

    val spell = card("Test Paid Life X") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        additionalCost(Costs.additional.PayXLife())
        spell { effect = Effects.GainLife(DynamicAmounts.xValue()) }
    }
    for ((paidLife, submittedX) in listOf(2 to null, 2 to 20, 2 to 0, 0 to 20)) {
        test("paid life $paidLife determines resolution X despite submitted mana X $submittedX") {
            val d = GameTestDriver()
            d.registerCard(spell)
            d.initMirrorMatch(Deck.of(spell.name to 40), skipMulligans = true)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = d.activePlayer!!
            val card = d.putCardInHand(me, spell.name)
            d.giveMana(me, Color.BLACK)
            d.submit(CastSpell(me, card, xValue = submittedX,
                additionalCostPayment = AdditionalCostPayment(payXLifeAmount = paidLife))).outcome shouldBe Outcome.Done
            d.getLifeTotal(me) shouldBe 20 - paidLife
            assertVisibleX(d, card, paidLife)
            d.bothPass()
            // The effect refunds exactly the life paid; no unrelated action field may change X.
            d.getLifeTotal(me) shouldBe 20
        }
    }
    test("an ordinary mana-X spell still resolves with its declared X") {
        val manaXSpell = card("Test Ordinary Mana X") {
            manaCost = "{X}{B}"
            typeLine = "Sorcery"
            spell { effect = Effects.GainLife(DynamicAmounts.xValue()) }
        }
        val d = GameTestDriver()
        d.registerCard(manaXSpell)
        d.initMirrorMatch(Deck.of(manaXSpell.name to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val card = d.putCardInHand(me, manaXSpell.name)
        d.giveMana(me, Color.BLACK, 4)
        d.submit(CastSpell(me, card, xValue = 3)).outcome shouldBe Outcome.Done
        assertVisibleX(d, card, 3)
        d.bothPass()
        d.getLifeTotal(me) shouldBe 23
    }
    test("a spell copy inherits the paid-life X without charging life again") {
        val copier = card("Test Paid Life Copy") {
            manaCost = "{U}"
            typeLine = "Instant"
            spell {
                val original = target(TargetFilter.SpellOnStack)
                effect = Effects.CopyTargetSpell(original)
            }
        }
        val d = GameTestDriver()
        d.registerCards(listOf(spell, copier))
        d.initMirrorMatch(Deck.of(spell.name to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val original = d.putCardInHand(me, spell.name)
        d.giveMana(me, Color.BLACK)
        d.submit(CastSpell(me, original, xValue = 20,
            additionalCostPayment = AdditionalCostPayment(payXLifeAmount = 2))).outcome shouldBe Outcome.Done
        val copy = d.putCardInHand(me, copier.name)
        d.giveMana(me, Color.BLUE)
        d.submit(CastSpell(me, copy, targets = listOf(ChosenTarget.Spell(d.getTopOfStack()!!)))).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getLifeTotal(me) shouldBe 18
        d.state.getEntity(d.getTopOfStack()!!)!!.has<SpellOnStackComponent>() shouldBe true
        assertVisibleX(d, original, 2)
        assertVisibleX(d, d.getTopOfStack()!!, 2)
        d.bothPass()
        d.getLifeTotal(me) shouldBe 20
        d.bothPass()
        d.getLifeTotal(me) shouldBe 22
    }
    for (isModal in listOf(false, true)) {
        test("paid-life damage text displays validated X for modal=$isModal") {
            val damageSpell = card("Test Paid Life Damage $isModal") {
                manaCost = "{B}"
                typeLine = "Sorcery"
                additionalCost(Costs.additional.PayXLife())
                spell {
                    if (isModal) {
                        modal(chooseCount = 1) {
                            mode("Deal X damage to yourself") {
                                effect = Effects.DealDamage(DynamicAmounts.xValue(), EffectTarget.Controller)
                            }
                            mode("Draw no cards") { effect = Effects.DrawCards(0) }
                        }
                    } else {
                        effect = Effects.DealDamage(DynamicAmounts.xValue(), EffectTarget.Controller)
                    }
                }
            }
            val d = GameTestDriver()
            d.registerCard(damageSpell)
            d.initMirrorMatch(Deck.of(damageSpell.name to 40), skipMulligans = true)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = d.activePlayer!!
            val card = d.putCardInHand(me, damageSpell.name)
            d.giveMana(me, Color.BLACK)
            d.submit(CastSpell(me, card, xValue = 20,
                chosenModes = if (isModal) listOf(0) else emptyList(),
                additionalCostPayment = AdditionalCostPayment(payXLifeAmount = 2))).outcome shouldBe Outcome.Done
            assertVisibleX(d, card, 2)
            val transformer = ClientStateTransformer(d.cardRegistry,
                predicateEvaluator = PredicateEvaluator(cardRegistry = d.cardRegistry))
            for (viewer in listOf(d.player1, d.player2)) {
                val visibleSpell = transformer.transform(d.state, viewer).cards[card]!!
                visibleSpell.stackText shouldBe "Deal 2 damage to you"
                if (isModal) visibleSpell.chosenModeDescriptions shouldBe listOf("Deal 2 damage to you")
            }
            d.bothPass()
            d.getLifeTotal(me) shouldBe 16
        }
    }
})
