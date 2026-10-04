const assert = require('assert');
const E = require('../js/reality-fracture/rules-engine.js');
require('../js/reality-fracture/mandatory-local-db.js');

const db = globalThis.FocusedMagicMandatoryLocalDB;
const clone = x => JSON.parse(JSON.stringify(x));
const card = (id, def, owner = 'User') => ({
  id, def: clone(def), owner, tapped: false, damage: 0, deathtouchDamage: false,
  enteredTurn: 0, tempPower: 0, tempToughness: 0, staticPower: 0, staticToughness: 0,
  faceDown: false, discardedTurn: null, token: false,
  loyalty: /Planeswalker/.test(def.type_line || '') ? Number(def.loyalty || 0) : undefined,
  loyaltyActivatedTurn: null
});

const filler = n => ({
  name: `Filler ${n}`, rarity: 'common', mana_cost: '', cmc: 0, type_line: 'Creature',
  oracle_text: '', colors: [], color_identity: [], keywords: [], produced_mana: [],
  power: '1', toughness: '1'
});

function makeGame() {
  return E.newSparring('rules-regression', 'Regular',
    Array.from({length: 10}, (_, i) => filler(i)),
    Array.from({length: 10}, (_, i) => filler(i + 20)));
}

function testFaithlessLootingRequiresDiscard() {
  const g = makeGame();
  const looting = db['faithless looting'];
  const mountain = E.makeVirtualBasic('R', 1);
  g.players.User.hand = [card('loot', looting), card('h1', filler(101)), card('h2', filler(102))];
  g.players.User.library = [card('d1', filler(201)), card('d2', filler(202)), card('d3', filler(203))];
  g.players.User.battlefield = [card('mountain', mountain)];
  g.players.User.battlefield[0].enteredTurn = -1;
  g.priority = 'User';
  g.active = 'User';
  g.phase = E.PH.MAIN1;
  E.cast(g, 'User', 'loot');
  E.passPriority(g, 'User');
  E.passPriority(g, 'Assistant');
  assert.equal(g.pendingChoice?.type, 'discard');
  assert.equal(g.pendingChoice.count, 2);
  assert.equal(g.players.User.hand.length, 4);
  const discardIds = g.players.User.hand.slice(0, 2).map(c => c.id);
  E.submitPendingChoice(g, {ids: discardIds});
  assert.equal(g.pendingChoice, null);
  assert.equal(g.players.User.hand.length, 2);
  assert(g.players.User.graveyard.some(c => c.def.name === 'Faithless Looting'));
}

function testWayOfThePyromancerGrantsRedManaLoyalty() {
  const g = makeGame();
  const way = db['way of the pyromancer'];
  const jaceDef = {
    name: 'Jace', rarity: 'token', mana_cost: '', cmc: 0, type_line: 'Token Planeswalker — Jace',
    oracle_text: '-1: Surveil 1.\n-3: Draw a card.', colors: ['U'], color_identity: ['U'],
    keywords: [], produced_mana: [], power: null, toughness: null, loyalty: '2'
  };
  const wayCard = card('way', way);
  const jace = card('jace', jaceDef);
  jace.token = true;
  jace.loyalty = 2;
  g.players.User.battlefield = [wayCard, jace];
  g.players.User.hand = [];
  g.priority = 'User';
  g.active = 'User';
  g.phase = E.PH.MAIN1;
  assert.equal(E.canActivatePyroMana(g, 'User', jace), true);
  E.activatePyroMana(g, 'User', 'jace');
  assert.equal(g.players.User.mana.R, 1);
  assert.equal(jace.loyalty, 3);
  assert.equal(E.canActivatePyroMana(g, 'User', jace), false);
}

testFaithlessLootingRequiresDiscard();
testWayOfThePyromancerGrantsRedManaLoyalty();
console.log('focused magic rules regressions passed');
