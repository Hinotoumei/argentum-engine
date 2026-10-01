'use strict';
const assert=require('node:assert/strict');
const {test}=require('node:test');
const E=require('../js/inuyasha/score-engine.js');
const profiles=require('../data/inuyasha-printed-character-profiles.json').profiles;
const decks=require('../data/inuyasha-test-decks.json').decks;

function gameFor(attackerAsset,defenderAsset){
  const a=new E.CharacterState({title:profiles[attackerAsset].name,controllerId:'P1',colorValues:profiles[attackerAsset].colors});
  const d=new E.CharacterState({title:profiles[defenderAsset].name,controllerId:'P2',colorValues:profiles[defenderAsset].colors});
  const game=new E.GameState({players:{P1:new E.PlayerState('P1'),P2:new E.PlayerState('P2',[new E.JewelShard('S1')])},turnActivePlayerId:'P1',characters:[a,d]});
  return {a,d,game};
}
const rin='fdfngumlds00sw0s',ginkotsu='qigc6391ge8w0s04';

test('Rin Young Follower defeats Ginkotsu Band of Seven only on blue',()=>{
  const {a,d,game}=gameFor(rin,ginkotsu);
  const ctx=game.attack(a,d,E.Color.BLUE);
  assert.equal(ctx.attackerFinalValue,5);assert.equal(ctx.defenderFinalValue,3);
  assert.equal(ctx.defenderDefeatedByAttack,true);assert.equal(ctx.shardStolen,true);
});
test('Rin loses the purple comparison with no shard stolen',()=>{
  const {a,d,game}=gameFor(rin,ginkotsu);
  const ctx=game.attack(a,d,E.Color.PURPLE);
  assert.equal(ctx.attackerFinalValue,3);assert.equal(ctx.defenderFinalValue,6);
  assert.equal(ctx.defenderDefeatedByAttack,false);assert.equal(ctx.shardStolen,false);
});
for(const color of ['red','green','black','white','orange']){
  test(`Rin/Ginkotsu cannot attack on absent shared color ${color}`,()=>{
    const {a,d,game}=gameFor(rin,ginkotsu);
    assert.throws(()=>game.attack(a,d,color),E.RuleError);
    assert.equal(a.ready,true);assert.equal(d.defeated,false);assert.equal(game.eventLog.length,0);
    assert.equal(game.player('P2').jewelShards.length,1);
  });
}
test('white is an actual attack color',()=>{
  const {a,d,game}=gameFor('gu8zgtc5go0g4k0g','sxy5m2ea6yo4gk4w');
  const ctx=game.attack(a,d,E.Color.WHITE);
  assert.equal(ctx.attackerFinalValue,2);assert.equal(ctx.defenderFinalValue,2);
  assert.equal(ctx.defenderDefeatedByAttack,true);
});
test('negative modifiers clamp the final comparison to zero',()=>{
  const {a,d,game}=gameFor(rin,ginkotsu);
  a.temporaryColorModifiers.blue=-20;d.constantColorModifiers.blue=-10;
  const ctx=game.attack(a,d,E.Color.BLUE);
  assert.equal(ctx.attackerFinalValue,0);assert.equal(ctx.defenderFinalValue,0);
});
test('all 28 reviewed fixture printings retain sparse printed colors in generated deck data',()=>{
  const found=new Set();
  for(const card of decks.flatMap(deck=>deck.cards)){
    const profile=profiles[card.imageAssetId];
    if(!profile){assert.equal(card.colorValues,null);continue;}
    found.add(card.imageAssetId);assert.equal(card.cardType,'Character');
    assert.deepEqual(card.colorValues,profile.colors);assert.equal(Object.keys(card.colorValues).length,3);
    assert.equal(card.printedDeckCost,profile.deckCost);
  }
  assert.equal(found.size,28);
});
test('a color mismatch never bypasses an opposing character for a direct attack',()=>{
  const {a,d,game}=gameFor(rin,'gu8zgtc5go0g4k0g');
  assert.equal(Object.keys(a.colorValues).some(c=>Object.hasOwn(d.colorValues,c)),false);
  assert.throws(()=>game.attackPlayer(a,'P2',E.Color.BLUE),E.RuleError);
  assert.equal(a.ready,true);assert.equal(game.eventLog.length,0);
});
test('with no opposing characters a direct attack expends the attacker and steals one shard',()=>{
  const {a,game}=gameFor(rin,ginkotsu);game.characters=[a];
  const ctx=game.attackPlayer(a,'P2',E.Color.BLUE);
  assert.equal(a.ready,false);assert.equal(ctx.shardStolen,true);assert.equal(ctx.defender,null);
  assert.equal(game.player('P1').jewelShards.length,1);assert.equal(game.player('P2').jewelShards.length,0);
  assert.deepEqual(game.eventLog.map(e=>e.window),[E.TimingWindow.CHARACTER_EXPENDED,E.TimingWindow.WHEN_ATTACKING,E.TimingWindow.JEWEL_SHARD_STOLEN,E.TimingWindow.ATTACK_END]);
});
test('facedown defeated cards are out of play and do not count as controlled characters',()=>{
  const {a,d,game}=gameFor(rin,ginkotsu);d.defeat();
  assert.equal(game.attackPlayer(a,'P2',E.Color.GREEN).shardStolen,true);
});
test('direct attacks cannot use an absent color or target their own controller',()=>{
  const {a,game}=gameFor(rin,ginkotsu);game.characters=[a];
  assert.throws(()=>game.attackPlayer(a,'P2',E.Color.RED),E.RuleError);
  assert.throws(()=>game.attackPlayer(a,'P1',E.Color.BLUE),E.RuleError);
  assert.equal(a.ready,true);assert.equal(game.eventLog.length,0);
});
