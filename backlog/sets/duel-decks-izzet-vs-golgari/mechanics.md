# Duel Decks: Izzet vs. Golgari (DDJ) — Mechanics

Reviewed against the complete DDJ Oracle worksheet, the SDK reference, SDK declarations, and the September 2026 Comprehensive Rules. Support means reusable vocabulary exists; it does not claim these cards are implemented or verified.

A box is ticked when the engine already models the mechanic (an SDK primitive exists and a card in the corpus uses it). An unticked box is `add-feature` work that blocks the cards listed under it.

---

## Keyword mechanics

### - [x] Dredge (8 cards)

A graveyard card optionally replaces a draw with milling N and returning itself (702.52a).

**Engine support:** Supported: `KeywordAbility.dredge(N)`, used by Stinkweed Imp and Shambling Shell.

Cards: Dakmor Salvage, Golgari Grave-Troll, Golgari Thug, Greater Mossdog, Life from the Loam, Nightmare Void, Shambling Shell, Stinkweed Imp

### - [x] Mill (8 cards)

Move the indicated number of top library cards to the graveyard (701.17).

**Engine support:** Supported: `Effects.Mill`; dredge integrates the movement in its draw replacement.

Cards: Dakmor Salvage, Golgari Grave-Troll, Golgari Thug, Greater Mossdog, Life from the Loam, Nightmare Void, Shambling Shell, Stinkweed Imp

### - [x] Sacrifice as an activation cost (7 cards)

Paying the sacrifice occurs during activation, before anyone can respond. The source can leave before its ability resolves.

**Engine support:** `Costs.SacrificeSelf`, filtered sacrifice costs, and immutable activated-ability processing. Dynamic values from sacrificed cards still require card-specific last-known-information tests.

Cards: Brain Weevil, Golgari Rotwurm, Jarad, Golgari Lich Lord, Korozda Guildmage, Plagued Rusalka, Sadistic Hypnotist, Shambling Shell

### - [x] Cycling (4 cards)

Activate from hand by paying its cost and discarding the card to draw (702.29).

**Engine support:** Supported: `KeywordAbility.cycling(cost)`, used by Blasted Landscape.

Cards: Barren Moor, Forgotten Cave, Lonely Sandbar, Tranquil Thicket

### - [x] Flying (4 cards)

Blocking requires flying or reach (702.9).

**Engine support:** Supported: `Keyword.FLYING`.

Cards: Djinn Illuminatus, Niv-Mizzet, the Firemind, Stinkweed Imp, Wee Dragonauts

### - [x] Replicate (4 cards)

Pay a repeatable additional cost; a cast trigger copies the spell for each payment (702.56a).

**Engine support:** Supported for a printed replicate cost: `KeywordAbility.replicate(cost)`; Consign to Memory scenarios cover copy creation and retargeting. Dynamically granting replicate with the spell’s own mana cost, as on Djinn Illuminatus, still needs separate capability assessment.

Cards: Pyromatics, Thunderheads, Train of Thought, Vacuumelt

### - [x] Instant and sorcery casting payoffs (4 cards)

Casting a matching spell triggers a payoff or its cost receives a reduction.

**Engine support:** cast-event filters, triggered abilities, and generic cost reductions. This does not cover Djinn Illuminatus’s dynamic replicate grant.

Cards: Gelectrode, Goblin Electromancer, Sphinx-Bone Wand, Wee Dragonauts

### - [x] Choose one (3 cards)

Choose exactly one mode when casting; only that mode determines required targets (700.2a, 700.2c).

**Engine support:** Supported: `modal(chooseCount = 1)`, used by Azorius Charm.

Cards: Feast or Famine, Invoke the Firemind, Izzet Charm

### - [x] Mana spent on a spell (3 cards)

An effect depends on which color was actually paid, rather than the spell’s printed colors.

**Engine support:** `Conditions.ManaSpentToCastIncludes` and cast payment tracking; see the existing Vigor Mortis implementation before authoring.

Cards: Ogre Savant, Steamcore Weird, Vigor Mortis

### - [x] Graveyard creature-count values (3 cards)

Creature cards in a graveyard determine a characteristic or an effect’s amount.

**Engine support:** filtered zone-count dynamic amounts and characteristic-defining power/toughness. Each card needs its own timing and projection proof.

Cards: Boneyard Wurm, Ghoul's Feast, Grim Flowering

### - [x] Enchant (2 cards)

Restricts an Aura’s legal attachment (702.5).

**Engine support:** Supported: Aura targeting through `targetCreature()` and attachment scripts, as on existing Auras.

Cards: Quicksilver Dagger, Yoke of the Damned

### - [x] Trample (2 cards)

Assign lethal to blockers before assigning excess to the attacked player (702.19).

**Engine support:** Supported: `Keyword.TRAMPLE`.

Cards: Doomgape, Gleancrawler

### - [x] Haste (1 card)

Ignores the usual control-duration restriction for attacking and tap-symbol abilities (702.10).

**Engine support:** Supported: `Keyword.HASTE`.

Cards: Dreg Mangler

### - [ ] Imprint (1 card)

An ability word has no independent rules meaning (207.2c); this card links an exiled instant to later copying and casting.

**Engine support:** Not established: Isochron Scepter’s complete linked-exile and cast-copy flow needs a dedicated implementation assessment. An ability-word label alone is insufficient.

Cards: Isochron Scepter

### - [x] Intimidate (1 card)

Only artifact creatures or creatures sharing a color may block (702.13).

**Engine support:** Supported: `Keyword.INTIMIDATE`.

Cards: Brain Weevil

### - [ ] Overload (1 card)

An alternative cost replaces target with each in spell text (702.96a).

**Engine support:** Not supported by a dedicated overload declaration in the current SDK. Street Spasm’s targeting, alternate X cost, and affected group must be modeled together.

Cards: Street Spasm

### - [x] Reach (1 card)

Allows blocking flying creatures (702.17).

**Engine support:** Supported: `Keyword.REACH`.

Cards: Stingerfling Spider

### - [x] Regenerate (1 card)

Creates a shield replacing the next destruction this turn (701.19).

**Engine support:** Supported: `Effects.Regenerate` with existing regeneration shields.

Cards: Golgari Grave-Troll

### - [ ] Scavenge (1 card)

Activate from the graveyard at sorcery speed, exile the source, and add counters equal to its power (702.97a).

**Engine support:** Not established: no dedicated scavenge factory was found; source power after exiling as a cost needs proof before composing this card.

Cards: Dreg Mangler
