package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.collections.shouldContain
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Replicate (CR 702.56a): "As an additional cost to cast this spell, you may pay [cost] any number
 * of times" and "When you cast this spell, if a replicate cost was paid for it, copy it for each
 * time its replicate cost was paid. If the spell has any targets, you may choose new targets for any
 * of the copies."
 */
class ReplicateTest : FunSpec({

    val manaReplicate = card("Test Replicate Ping") {
        manaCost = "{R}"
        typeLine = "Instant"
        keywordAbility(KeywordAbility.replicate("{1}"))
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }

    val energyReplicate = card("Test Energy Replicate Ping") {
        manaCost = "{R}"
        typeLine = "Instant"
        keywordAbility(KeywordAbility.replicate(Costs.additional.PayPlayerCounters(CounterType.ENERGY, 3)))
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }

    val kickerPing = card("Test Kicker Ping") {
        manaCost = "{R}"
        typeLine = "Instant"
        keywordAbility(KeywordAbility.kicker("{1}"))
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }

    val reducer = card("Test Replicate Reducer") {
        typeLine = "Artifact"
        staticAbility {
            ability = ModifySpellCost(SpellCostTarget.YouCast(GameObjectFilter.Instant), CostModification.ReduceGeneric(1))
        }
    }
    val alternativePing = card("Test Alternative Replicate Ping") {
        manaCost = "{R}"
        typeLine = "Instant"
        selfAlternativeCost = SelfAlternativeCost(ManaCost.ZERO)
        keywordAbility(KeywordAbility.replicate("{1}"))
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }
    val freePermission = card("Test Replicate Free Cast") {
        typeLine = "Enchantment"
        staticAbility { ability = MayCastWithoutPayingManaCost(controllerOnly = true, fromHandOnly = true) }
    }
    val counter = card("Test Replicate Counter") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.SpellOnStack)
            effect = Effects.CounterSpell()
        }
    }
    val modalPing = card("Test Modal Replicate Ping") {
        manaCost = "{R}"
        typeLine = "Instant"
        keywordAbility(KeywordAbility.replicate("{1}"))
        spell {
            modal(chooseCount = 1) {
                mode("Deal one damage") {
                    val t = target(Targets.Any)
                    effect = Effects.DealDamage(1, t)
                }
                mode("Draw a card") { effect = Effects.DrawCards(1) }
            }
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(listOf(manaReplicate, energyReplicate, kickerPing, reducer, alternativePing, freePermission, counter, modalPing,
            CardDefinition.basicLand("Mountain", Subtype("Mountain"))))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.giveEnergy(player: EntityId, amount: Int) {
        replaceState(state.updateEntity(player) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.ENERGY, amount))
        })
    }

    fun GameTestDriver.energy(player: EntityId): Int =
        state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    fun GameTestDriver.cast(card: EntityId, target: EntityId, times: Int, slot: ChoiceSlot? = ChoiceSlot.REPLICATED) =
        submit(
            CastSpell(
                playerId = activePlayer!!,
                cardId = card,
                targets = listOf(ChosenTarget.Player(target)),
                paymentStrategy = PaymentStrategy.AutoPay,
                declaredCostSlot = slot,
                declaredCostTimes = times,
            )
        )

    /** Resolve everything, keeping each copy's target when asked to choose new ones. */
    fun GameTestDriver.resolveAll(keepTarget: EntityId): Int {
        var retargetPrompts = 0
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 40) {
            val decision = state.pendingDecision
            if (decision is ChooseTargetsDecision) {
                val json = kotlinx.serialization.json.Json {
                    serializersModule = com.wingedsheep.engine.core.engineSerializersModule
                    allowStructuredMapKeys = true
                }
                replaceState(json.decodeFromString(com.wingedsheep.engine.state.GameState.serializer(),
                    json.encodeToString(com.wingedsheep.engine.state.GameState.serializer(), state)))
                retargetPrompts++
                submitTargetSelection(decision.playerId, listOf(keepTarget)).error shouldBe null
            } else {
                bothPass()
            }
        }
        return retargetPrompts
    }

    test("paying replicate twice charges the cost twice and copies the spell twice") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        repeat(3) { driver.putLandOnBattlefield(caster, "Mountain") }
        val ping = driver.putCardInHand(caster, "Test Replicate Ping")

        driver.cast(ping, opponent, times = 2).outcome shouldBe Outcome.Done

        // {R} + {1}{1}: every land was tapped for the cost.
        driver.getPermanents(caster).all { driver.isTapped(it) } shouldBe true
        // One replicate trigger above the spell (CR 702.56a — one trigger, N copies).
        val trigger = driver.state.stack.last()
        driver.state.getEntity(trigger)?.get<TriggeredAbilityOnStackComponent>() shouldNotBe null

        val prompts = driver.resolveAll(opponent)
        prompts shouldBe 2
        driver.getLifeTotal(opponent) shouldBe 17
    }

    test("the copies are separate spells, each allowed a new target") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        repeat(2) { driver.putLandOnBattlefield(caster, "Mountain") }
        val ping = driver.putCardInHand(caster, "Test Replicate Ping")

        driver.cast(ping, opponent, times = 1).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard++ < 10) driver.bothPass()
        val decision = driver.state.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(decision.playerId, listOf(caster)).outcome shouldBe Outcome.Done

        driver.state.stack.count { driver.state.getEntity(it)?.has<CopyOfComponent>() == true } shouldBe 1
        driver.resolveAll(opponent)
        driver.getLifeTotal(caster) shouldBe 19
        driver.getLifeTotal(opponent) shouldBe 19
    }

    test("casting without replicate puts no copy trigger on the stack") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.putLandOnBattlefield(caster, "Mountain")
        val ping = driver.putCardInHand(caster, "Test Replicate Ping")

        driver.cast(ping, opponent, times = 1, slot = null).outcome shouldBe Outcome.Done
        driver.state.stack.size shouldBe 1
        driver.resolveAll(opponent) shouldBe 0
        driver.getLifeTotal(opponent) shouldBe 19
    }

    test("a replicate count the caster can't pay for is rejected") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        repeat(2) { driver.putLandOnBattlefield(caster, "Mountain") }
        val ping = driver.putCardInHand(caster, "Test Replicate Ping")

        driver.cast(ping, opponent, times = 2).outcome shouldNotBe Outcome.Done
    }

    test("a once-only optional cost can't be declared more than once, and a count must be positive") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        repeat(4) { driver.putLandOnBattlefield(caster, "Mountain") }
        val kicked = driver.putCardInHand(caster, "Test Kicker Ping")
        val ping = driver.putCardInHand(caster, "Test Replicate Ping")

        driver.cast(kicked, opponent, times = 2, slot = ChoiceSlot.KICKED).outcome shouldNotBe Outcome.Done
        driver.cast(ping, opponent, times = 0).outcome shouldNotBe Outcome.Done
    }

    test("an energy replicate cost pays three energy per copy") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.putLandOnBattlefield(caster, "Mountain")
        driver.giveEnergy(caster, 7)
        val ping = driver.putCardInHand(caster, "Test Energy Replicate Ping")

        driver.cast(ping, opponent, times = 3).outcome shouldNotBe Outcome.Done
        driver.energy(caster) shouldBe 7

        driver.cast(ping, opponent, times = 2).outcome shouldBe Outcome.Done
        driver.energy(caster) shouldBe 1
        driver.resolveAll(opponent) shouldBe 2
        driver.getLifeTotal(opponent) shouldBe 17
    }

    test("the enumerator offers one cast per affordable replicate count") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        repeat(3) { driver.putLandOnBattlefield(caster, "Mountain") }
        driver.putCardInHand(caster, "Test Replicate Ping")

        val actions = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, caster)
        val replicated = actions.mapNotNull { a ->
            (a.action as? CastSpell)?.takeIf { it.declaredCostSlot == ChoiceSlot.REPLICATED }?.let { it.declaredCostTimes to a }
        }
        replicated.map { it.first } shouldBe listOf(1, 2)
        replicated.all { it.second.affordable } shouldBe true
        replicated.map { it.second.description } shouldBe listOf(
            "Cast Test Replicate Ping (Replicate ×1)",
            "Cast Test Replicate Ping (Replicate ×2)",
        )
    }

    test("the enumerator caps an energy replicate at the energy the caster has") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        driver.putLandOnBattlefield(caster, "Mountain")
        driver.giveEnergy(caster, 6)
        driver.putCardInHand(caster, "Test Energy Replicate Ping")

        val actions = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, caster)
        actions.mapNotNull { (it.action as? CastSpell)?.takeIf { c -> c.declaredCostSlot == ChoiceSlot.REPLICATED }?.declaredCostTimes }
            .shouldBe(listOf(1, 2))
    }

    test("the enumerator offers affordable replicate counts beyond ten") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        driver.giveMana(caster, Color.RED, 13)
        driver.putCardInHand(caster, "Test Replicate Ping")
        val actions = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, caster)
        actions.mapNotNull { (it.action as? CastSpell)?.takeIf { c -> c.declaredCostSlot == ChoiceSlot.REPLICATED }?.declaredCostTimes }
            .shouldBe((1..12).toList())
    }

    test("eleven replicate payments make eleven copies plus the original") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.giveMana(caster, Color.RED, 12)
        val ping = driver.putCardInHand(caster, "Test Replicate Ping")
        driver.cast(ping, opponent, times = 11).outcome shouldBe Outcome.Done
        driver.resolveAll(opponent) shouldBe 11
        driver.getLifeTotal(opponent) shouldBe 8
    }

    test("a cost reduction applies to replicate mana after it is added") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.putPermanentOnBattlefield(caster, "Test Replicate Reducer")
        driver.giveMana(caster, Color.RED, 1)
        val ping = driver.putCardInHand(caster, "Test Replicate Ping")
        val actions = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, caster)
        actions.any { (it.action as? CastSpell)?.let { c -> c.cardId == ping && c.declaredCostSlot == ChoiceSlot.REPLICATED && c.declaredCostTimes == 1 } == true && it.affordable } shouldBe true
        driver.cast(ping, opponent, times = 1).outcome shouldBe Outcome.Done
        driver.resolveAll(opponent) shouldBe 1
        driver.getLifeTotal(opponent) shouldBe 18
    }

    test("an alternative base cost does not waive replicate payments") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        val ping = driver.putCardInHand(caster, "Test Alternative Replicate Ping")
        val action = CastSpell(caster, ping, targets = listOf(ChosenTarget.Player(opponent)),
            useAlternativeCost = true, alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
            declaredCostSlot = ChoiceSlot.REPLICATED, declaredCostTimes = 1)
        val before = driver.state
        driver.submit(action).outcome shouldNotBe Outcome.Done
        driver.state shouldBe before
        driver.giveMana(caster, Color.RED, 1)
        driver.submit(action).outcome shouldBe Outcome.Done
        driver.resolveAll(opponent) shouldBe 1
        driver.getLifeTotal(opponent) shouldBe 18
    }

    test("huge unaffordable payment counts are rejected without changing the game") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.giveMana(caster, Color.RED, 1)
        val ping = driver.putCardInHand(caster, "Test Replicate Ping")
        val before = driver.state
        for (times in listOf(Int.MAX_VALUE, Int.MAX_VALUE - 1, -1)) {
            driver.cast(ping, opponent, times).outcome shouldNotBe Outcome.Done
            driver.state shouldBe before
        }
    }

    test("a free base cast still pays replicate and is offered by the enumerator") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.putPermanentOnBattlefield(caster, "Test Replicate Free Cast")
        val ping = driver.putCardInHand(caster, "Test Replicate Ping")
        val action = CastSpell(caster, ping, targets = listOf(ChosenTarget.Player(opponent)),
            useWithoutPayingManaCost = true, declaredCostSlot = ChoiceSlot.REPLICATED, declaredCostTimes = 1)
        val before = driver.state
        driver.submit(action).outcome shouldNotBe Outcome.Done
        driver.state shouldBe before
        driver.giveMana(caster, Color.RED, 1)
        val actions = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, caster)
        actions.any { (it.action as? CastSpell)?.let { c -> c.cardId == ping && c.useWithoutPayingManaCost && c.declaredCostSlot == ChoiceSlot.REPLICATED && c.declaredCostTimes == 1 } == true && it.affordable } shouldBe true
        driver.submit(action).outcome shouldBe Outcome.Done
        driver.resolveAll(opponent) shouldBe 1
        driver.getLifeTotal(opponent) shouldBe 18
    }

    for (modal in listOf(false, true)) test("countering the original preserves ${if (modal) "modal" else "plain"} replicate copies and its latest choices") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.giveMana(caster, Color.RED, 3)
        driver.giveMana(opponent, Color.RED, 1)
        val ping = driver.putCardInHand(caster, if (modal) "Test Modal Replicate Ping" else "Test Replicate Ping")
        val counterCard = driver.putCardInHand(opponent, "Test Replicate Counter")
        driver.submit(CastSpell(caster, ping,
            targets = listOf(ChosenTarget.Player(opponent)),
            chosenModes = if (modal) listOf(0) else emptyList(),
            modeTargetsOrdered = if (modal) listOf(listOf(ChosenTarget.Player(opponent))) else emptyList(),
            declaredCostSlot = ChoiceSlot.REPLICATED, declaredCostTimes = 2
        )).outcome shouldBe Outcome.Done
        // Represent a prior retargeting effect: the departing spell's latest choices must survive.
        driver.replaceState(driver.state.updateEntity(ping) { container ->
            val updated = container.with(com.wingedsheep.engine.state.components.stack.TargetsComponent.capture(
                driver.state, listOf(ChosenTarget.Player(caster)), listOf(Targets.Any)))
            if (modal) updated.with(updated.get<com.wingedsheep.engine.state.components.stack.SpellOnStackComponent>()!!.copy(
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(caster)))
            )) else updated
        })
        driver.passPriority(caster)
        driver.submit(CastSpell(opponent, counterCard, targets = listOf(ChosenTarget.Spell(ping)))).outcome shouldBe Outcome.Done
        driver.bothPass()
        val replicate = driver.state.stack.single { driver.state.getEntity(it)?.has<TriggeredAbilityOnStackComponent>() == true }
        driver.state.getEntity(replicate)!!.get<TriggeredAbilityOnStackComponent>()!!.triggerContext!!
            .spellCopySource!!.get<com.wingedsheep.engine.state.components.stack.TargetsComponent>()!!.targets shouldBe listOf(ChosenTarget.Player(caster))
        driver.resolveAll(opponent) shouldBe 2
        driver.getGraveyard(caster) shouldContain ping
        driver.getLifeTotal(opponent) shouldBe 18
        driver.state.stack.isEmpty() shouldBe true
    }
})
